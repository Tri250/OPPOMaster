//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_WEBGPU

#include "decoders/processor/operators/gpu/webgpu_to_linear_ref.hpp"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

#include "decoders/processor/raw_normalization.hpp"
#include "image/tiled_webgpu_image.hpp"
#include "webgpu/webgpu_context.hpp"

namespace alcedo {
namespace webgpu {
namespace {

struct WBParams {
  float    black_level[4];
  float    white_level[4];
  float    wb_multipliers[4];
  uint32_t apply_white_balance;
  uint32_t padding[3];
};

struct ToLinearRefParams {
  uint32_t width;
  uint32_t height;
  uint32_t stride;
  uint32_t origin_x;
  uint32_t origin_y;
  uint32_t tile_width;
  uint32_t tile_height;
  uint32_t black_tile_width;
  uint32_t black_tile_height;
  uint32_t padding[3];
  uint32_t raw_fc[36];
};

auto ReadTextFile(const std::filesystem::path& path, const char* label) -> std::string {
  std::ifstream file(path, std::ios::binary | std::ios::ate);
  if (!file) {
    throw std::runtime_error(std::string("WebGPU ToLinearRef: Failed to open ") + label + ": " +
                             path.string());
  }

  const auto size = file.tellg();
  if (size < 0) {
    throw std::runtime_error(std::string("WebGPU ToLinearRef: Failed to stat ") + label + ": " +
                             path.string());
  }

  std::string contents(static_cast<size_t>(size), '\0');
  file.seekg(0, std::ios::beg);
  if (!contents.empty() &&
      !file.read(contents.data(), static_cast<std::streamsize>(contents.size()))) {
    throw std::runtime_error(std::string("WebGPU ToLinearRef: Failed to read ") + label + ": " +
                             path.string());
  }
  return contents;
}

auto MakeBuffer(uint64_t size, wgpu::BufferUsage usage, bool mapped_at_creation = false)
    -> wgpu::Buffer {
  wgpu::BufferDescriptor descriptor{};
  descriptor.usage            = usage;
  descriptor.size             = size;
  descriptor.mappedAtCreation = mapped_at_creation;
  auto buffer                 = WebGpuContext::Instance().Device().CreateBuffer(&descriptor);
  if (!buffer.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create buffer.");
  }
  return buffer;
}

void SubmitAndWait(const wgpu::CommandBuffer& command_buffer) {
  WebGpuContext::Instance().Queue().Submit(1, &command_buffer);
  WebGpuContext::Instance().WaitForSubmittedWork();
}

auto MakeTextureView(const WebGpuImage& image) -> wgpu::TextureView {
  wgpu::TextureViewDescriptor descriptor{};
  descriptor.dimension = wgpu::TextureViewDimension::e2D;
  auto view            = image.Texture().CreateView(&descriptor);
  if (!view.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create texture view.");
  }
  return view;
}

auto GetOrCreatePipeline() -> wgpu::ComputePipeline {
  static wgpu::ComputePipeline pipeline = nullptr;
  if (pipeline) {
    return pipeline;
  }

#ifndef ALCEDO_WEBGPU_TO_LINEAR_REF_WGSL_PATH
#error "ALCEDO_WEBGPU_TO_LINEAR_REF_WGSL_PATH must be defined when WebGPU ToLinearRef is enabled."
#endif

  auto&      device = WebGpuContext::Instance().Device();
  const auto wgsl_source =
      ReadTextFile(ALCEDO_WEBGPU_TO_LINEAR_REF_WGSL_PATH, "to_linear_ref WGSL shader");

  wgpu::ShaderSourceWGSL       wgsl_desc{};
  wgpu::ShaderModuleDescriptor shader_desc{};
  wgsl_desc.code          = std::string_view(wgsl_source.data(), wgsl_source.size());
  shader_desc.nextInChain = &wgsl_desc;
  auto shader_module      = device.CreateShaderModule(&shader_desc);
  if (!shader_module.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create shader module.");
  }

  std::array<wgpu::BindGroupLayoutEntry, 5> entries{};

  entries[0].binding              = 0;
  entries[0].visibility           = wgpu::ShaderStage::Compute;
  entries[0].texture.sampleType   = wgpu::TextureSampleType::Uint;
  entries[0].texture.viewDimension = wgpu::TextureViewDimension::e2D;

  entries[1].binding                      = 1;
  entries[1].visibility                   = wgpu::ShaderStage::Compute;
  entries[1].storageTexture.access        = wgpu::StorageTextureAccess::WriteOnly;
  entries[1].storageTexture.format        = wgpu::TextureFormat::R32Float;
  entries[1].storageTexture.viewDimension = wgpu::TextureViewDimension::e2D;

  entries[2].binding     = 2;
  entries[2].visibility  = wgpu::ShaderStage::Compute;
  entries[2].buffer.type = wgpu::BufferBindingType::Uniform;

  entries[3].binding     = 3;
  entries[3].visibility  = wgpu::ShaderStage::Compute;
  entries[3].buffer.type = wgpu::BufferBindingType::Uniform;

  entries[4].binding     = 4;
  entries[4].visibility  = wgpu::ShaderStage::Compute;
  entries[4].buffer.type = wgpu::BufferBindingType::ReadOnlyStorage;

  wgpu::BindGroupLayoutDescriptor bgl_desc{};
  bgl_desc.entryCount    = entries.size();
  bgl_desc.entries       = entries.data();
  auto bind_group_layout = device.CreateBindGroupLayout(&bgl_desc);
  if (!bind_group_layout.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create bind group layout.");
  }

  wgpu::PipelineLayoutDescriptor pl_desc{};
  pl_desc.bindGroupLayoutCount = 1;
  pl_desc.bindGroupLayouts     = &bind_group_layout;
  auto pipeline_layout         = device.CreatePipelineLayout(&pl_desc);
  if (!pipeline_layout.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create pipeline layout.");
  }

  wgpu::ComputePipelineDescriptor cp_desc{};
  cp_desc.layout             = pipeline_layout;
  cp_desc.compute.module     = shader_module;
  cp_desc.compute.entryPoint = "main";
  pipeline                   = device.CreateComputePipeline(&cp_desc);
  if (!pipeline.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create compute pipeline.");
  }

  return pipeline;
}

auto CreateBlackPatternBuffer(const std::vector<float>& black_pattern) -> wgpu::Buffer {
  const auto black_pattern_size = std::max<size_t>(black_pattern.size(), 1) * sizeof(float);
  auto       black_pattern_buffer =
      MakeBuffer(black_pattern_size, wgpu::BufferUsage::Storage | wgpu::BufferUsage::CopyDst);
  std::vector<float> padded_black(std::max<size_t>(black_pattern.size(), 1), 0.0f);
  if (!black_pattern.empty()) {
    std::copy(black_pattern.begin(), black_pattern.end(), padded_black.begin());
  }
  WebGpuContext::Instance().Queue().WriteBuffer(black_pattern_buffer, 0, padded_black.data(),
                                                padded_black.size() * sizeof(float));
  return black_pattern_buffer;
}

auto CreateWbParamsBuffer(const WBParams& wb_params) -> wgpu::Buffer {
  constexpr size_t kUniformAlign  = 256;
  const auto       wb_buffer_size = (sizeof(WBParams) + kUniformAlign - 1) & ~(kUniformAlign - 1);
  auto             wb_buffer =
      MakeBuffer(wb_buffer_size, wgpu::BufferUsage::Uniform | wgpu::BufferUsage::CopyDst);
  WebGpuContext::Instance().Queue().WriteBuffer(wb_buffer, 0, &wb_params, sizeof(wb_params));
  return wb_buffer;
}

auto CreateToLinearRefParamsBuffer(const WebGpuImage& image, const RawCfaPattern& pattern,
                                   uint32_t black_tile_width, uint32_t black_tile_height,
                                   uint32_t origin_x, uint32_t origin_y) -> wgpu::Buffer {
  if (image.Empty()) {
    throw std::runtime_error("[ERROR] WebGPU ToLinearRef: image is empty.");
  }
  if (image.Format() != PixelFormat::R16UINT) {
    throw std::runtime_error("[ERROR] WebGPU ToLinearRef: expected R16UINT image.");
  }

  ToLinearRefParams params = {};
  params.width             = image.Width();
  params.height            = image.Height();
  params.stride            = image.Width();
  params.origin_x          = origin_x;
  params.origin_y          = origin_y;
  if (pattern.kind == RawCfaKind::XTrans6x6) {
    params.tile_width  = 6;
    params.tile_height = 6;
    for (int i = 0; i < 36; ++i) {
      params.raw_fc[i] = static_cast<uint32_t>(pattern.xtrans_pattern.raw_fc[i]);
    }
  } else {
    params.tile_width  = 2;
    params.tile_height = 2;
    for (int i = 0; i < 4; ++i) {
      params.raw_fc[i] = static_cast<uint32_t>(pattern.bayer_pattern.raw_fc[i]);
    }
  }
  params.black_tile_width        = black_tile_width;
  params.black_tile_height       = black_tile_height;

  constexpr size_t kUniformAlign = 256;
  const auto       params_buffer_size =
      (sizeof(ToLinearRefParams) + kUniformAlign - 1) & ~(kUniformAlign - 1);
  auto params_buffer =
      MakeBuffer(params_buffer_size, wgpu::BufferUsage::Uniform | wgpu::BufferUsage::CopyDst);
  WebGpuContext::Instance().Queue().WriteBuffer(params_buffer, 0, &params, sizeof(params));
  return params_buffer;
}

auto CreateBindGroup(const wgpu::ComputePipeline& pipeline, const WebGpuImage& src,
                     const WebGpuImage& dst, const wgpu::Buffer& params_buffer,
                     const wgpu::Buffer& wb_buffer, const wgpu::Buffer& black_pattern_buffer)
    -> wgpu::BindGroup {
  auto src_view = MakeTextureView(src);
  auto dst_view = MakeTextureView(dst);

  std::array<wgpu::BindGroupEntry, 5> bg_entries{};
  bg_entries[0].binding     = 0;
  bg_entries[0].textureView = src_view;
  bg_entries[1].binding     = 1;
  bg_entries[1].textureView = dst_view;
  bg_entries[2].binding     = 2;
  bg_entries[2].buffer      = params_buffer;
  bg_entries[3].binding     = 3;
  bg_entries[3].buffer      = wb_buffer;
  bg_entries[4].binding     = 4;
  bg_entries[4].buffer      = black_pattern_buffer;
  wgpu::BindGroupDescriptor bg_desc{};
  bg_desc.layout     = pipeline.GetBindGroupLayout(0);
  bg_desc.entryCount = bg_entries.size();
  bg_desc.entries    = bg_entries.data();
  auto bind_group = WebGpuContext::Instance().Device().CreateBindGroup(&bg_desc);
  if (!bind_group.Get()) {
    throw std::runtime_error("WebGPU ToLinearRef: Failed to create bind group.");
  }
  return bind_group;
}

void DispatchToLinearRef(wgpu::ComputePassEncoder& compute, const wgpu::ComputePipeline& pipeline,
                         const wgpu::BindGroup& bind_group, uint32_t width, uint32_t height) {
  compute.SetPipeline(pipeline);
  compute.SetBindGroup(0, bind_group);
  compute.DispatchWorkgroups((width + 7) / 8, (height + 7) / 8, 1);
}

void LinearizeSingleImage(WebGpuImage& image, const WBParams& wb_params,
                          const RawCfaPattern& pattern, const std::vector<float>& black_pattern,
                          uint32_t black_tile_width, uint32_t black_tile_height,
                          uint32_t origin_x = 0, uint32_t origin_y = 0) {
  auto output = WebGpuImage{};
  output.Create(image.Width(), image.Height(), PixelFormat::R32FLOAT);

  auto pipeline             = GetOrCreatePipeline();
  auto black_pattern_buffer = CreateBlackPatternBuffer(black_pattern);
  auto wb_buffer            = CreateWbParamsBuffer(wb_params);
  auto params_buffer        = CreateToLinearRefParamsBuffer(
      image, pattern, black_tile_width, black_tile_height, origin_x, origin_y);
  auto bind_group =
      CreateBindGroup(pipeline, image, output, params_buffer, wb_buffer, black_pattern_buffer);

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  {
    auto compute = encoder.BeginComputePass();
    DispatchToLinearRef(compute, pipeline, bind_group, image.Width(), image.Height());
    compute.End();
  }
  SubmitAndWait(encoder.Finish());
  image = std::move(output);
}

static auto GetWBCoeff(const libraw_rawdata_t& raw_data) -> const float* {
  return raw_data.color.cam_mul;
}

static auto GetPatternBlackLevels(const libraw_rawdata_t& raw_data) -> std::vector<float> {
  const uint32_t tile_width  = raw_data.color.cblack[4];
  const uint32_t tile_height = raw_data.color.cblack[5];
  const uint32_t entries     = tile_width * tile_height;
  if (entries == 0U) {
    return {};
  }

  std::vector<float> pattern_black(entries, 0.0f);
  for (uint32_t i = 0; i < entries; ++i) {
    pattern_black[i] = static_cast<float>(raw_data.color.cblack[6 + i]);
  }
  return pattern_black;
}

}  // namespace

void ToLinearRef(WebGpuImage& img, LibRaw& raw_processor, const RawCfaPattern& pattern) {
  const auto     raw_curve     = raw_norm::BuildLinearizationCurve(raw_processor.imgdata.rawdata);
  const auto     wb            = GetWBCoeff(raw_processor.imgdata.rawdata);
  auto           black_pattern = GetPatternBlackLevels(raw_processor.imgdata.rawdata);
  const uint32_t black_tile_width  = raw_processor.imgdata.rawdata.color.cblack[4];
  const uint32_t black_tile_height = raw_processor.imgdata.rawdata.color.cblack[5];

  if (img.Format() != PixelFormat::R16UINT) {
    throw std::runtime_error("WebGPU ToLinearRef: expected R16UINT raw input.");
  }

  WBParams wb_params = {};
  for (int c = 0; c < 4; ++c) {
    wb_params.black_level[c]    = raw_curve.black_level[c];
    wb_params.white_level[c]    = raw_curve.white_level[c];
    wb_params.wb_multipliers[c] = wb[c];
  }
  wb_params.apply_white_balance = raw_processor.imgdata.color.as_shot_wb_applied != 1 ? 1u : 0u;

  LinearizeSingleImage(img, wb_params, pattern, black_pattern, black_tile_width,
                       black_tile_height);
}

void ToLinearRef(TiledWebGpuImage& img, LibRaw& raw_processor, const RawCfaPattern& pattern) {
  if (img.Empty()) {
    throw std::runtime_error("WebGPU ToLinearRef: tiled image is empty.");
  }
  if (img.Format() != PixelFormat::R16UINT) {
    throw std::runtime_error("WebGPU ToLinearRef: expected R16UINT raw input.");
  }

  const auto     raw_curve     = raw_norm::BuildLinearizationCurve(raw_processor.imgdata.rawdata);
  const auto     wb            = GetWBCoeff(raw_processor.imgdata.rawdata);
  auto           black_pattern = GetPatternBlackLevels(raw_processor.imgdata.rawdata);
  const uint32_t black_tile_width  = raw_processor.imgdata.rawdata.color.cblack[4];
  const uint32_t black_tile_height = raw_processor.imgdata.rawdata.color.cblack[5];

  WBParams wb_params = {};
  for (int c = 0; c < 4; ++c) {
    wb_params.black_level[c]    = raw_curve.black_level[c];
    wb_params.white_level[c]    = raw_curve.white_level[c];
    wb_params.wb_multipliers[c] = wb[c];
  }
  wb_params.apply_white_balance = raw_processor.imgdata.color.as_shot_wb_applied != 1 ? 1u : 0u;

  auto pipeline             = GetOrCreatePipeline();
  auto black_pattern_buffer = CreateBlackPatternBuffer(black_pattern);
  auto wb_buffer            = CreateWbParamsBuffer(wb_params);
  TiledWebGpuImage output;
  output.Create(img.Width(), img.Height(), PixelFormat::R32FLOAT, img.TileShape().width);

  std::vector<wgpu::Buffer>    params_buffers;
  std::vector<wgpu::BindGroup> bind_groups;
  params_buffers.reserve(img.TileCount());
  bind_groups.reserve(img.TileCount());

  for (uint32_t tile_y = 0; tile_y < img.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < img.TileColumns(); ++tile_x) {
      const TileIndex index{tile_x, tile_y};
      const TileRect  rect = img.TileRegion(index);
      const auto&      src_tile = img.Tile(index);
      auto&            dst_tile = output.Tile(index);
      auto             params_buffer = CreateToLinearRefParamsBuffer(
          src_tile, pattern, black_tile_width, black_tile_height, rect.x, rect.y);
      bind_groups.push_back(CreateBindGroup(pipeline, src_tile, dst_tile, params_buffer, wb_buffer,
                                            black_pattern_buffer));
      params_buffers.push_back(std::move(params_buffer));
    }
  }

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  {
    auto   compute   = encoder.BeginComputePass();
    size_t job_index = 0;
    for (uint32_t tile_y = 0; tile_y < img.TileRows(); ++tile_y) {
      for (uint32_t tile_x = 0; tile_x < img.TileColumns(); ++tile_x) {
        const TileIndex index{tile_x, tile_y};
        const TileRect  rect = img.TileRegion(index);
        DispatchToLinearRef(compute, pipeline, bind_groups[job_index], rect.width, rect.height);
        ++job_index;
      }
    }
    compute.End();
  }
  SubmitAndWait(encoder.Finish());

  img = std::move(output);
}

}  // namespace webgpu
}  // namespace alcedo

#endif
