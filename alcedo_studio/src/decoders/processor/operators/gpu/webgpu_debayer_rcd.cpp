//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_WEBGPU

#include "decoders/processor/operators/gpu/webgpu_debayer_rcd.hpp"

#include <array>
#include <chrono>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

#include "image/tiled_webgpu_image.hpp"
#include "webgpu/webgpu_context.hpp"

namespace alcedo {
namespace webgpu {
namespace {

using Clock = std::chrono::steady_clock;

struct SinglePlaneParams {
  std::array<uint32_t, 4> rgb_fc;
  uint32_t                width;
  uint32_t                height;
  uint32_t                stride;
  uint32_t                padding;
};

auto MsSince(Clock::time_point start) -> double {
  return std::chrono::duration<double, std::milli>(Clock::now() - start).count();
}

enum class BindingKind {
  ReadTextureR32F,
  WriteTextureR32F,
  WriteTextureRGBA16F,
  WriteTextureRGBA32F,
  UniformBuffer,
};

enum class Kernel : uint32_t {
  InitAndVH,
  GreenAtRB,
  FinalRGBA,
};

auto KernelNameFor(Kernel kernel) -> const char* {
  switch (kernel) {
    case Kernel::InitAndVH:
      return "rcd_init_and_vh";
    case Kernel::GreenAtRB:
      return "rcd_green_at_rb";
    case Kernel::FinalRGBA:
      return "rcd_final_rgba";
  }
  throw std::runtime_error("WebGPU Debayer RCD: unknown kernel.");
}

auto BindingKindsFor(Kernel kernel) -> std::vector<BindingKind> {
  switch (kernel) {
    case Kernel::InitAndVH:
      return {BindingKind::ReadTextureR32F, BindingKind::WriteTextureR32F,
              BindingKind::WriteTextureRGBA16F, BindingKind::UniformBuffer};
    case Kernel::GreenAtRB:
      return {BindingKind::ReadTextureR32F, BindingKind::ReadTextureR32F,
              BindingKind::ReadTextureR32F, BindingKind::WriteTextureR32F,
              BindingKind::UniformBuffer};
    case Kernel::FinalRGBA:
      return {BindingKind::ReadTextureR32F,  BindingKind::ReadTextureR32F,
              BindingKind::ReadTextureR32F,
              BindingKind::WriteTextureRGBA32F,
              BindingKind::UniformBuffer};
  }
  return {};
}

auto ReadTextFile(const std::filesystem::path& path, const char* label) -> std::string {
  std::ifstream file(path, std::ios::binary | std::ios::ate);
  if (!file) {
    throw std::runtime_error(std::string("WebGPU Debayer RCD: Failed to open ") + label + ": " +
                             path.string());
  }

  const auto size = file.tellg();
  if (size < 0) {
    throw std::runtime_error(std::string("WebGPU Debayer RCD: Failed to stat ") + label + ": " +
                             path.string());
  }

  std::string contents(static_cast<size_t>(size), '\0');
  file.seekg(0, std::ios::beg);
  if (!contents.empty() &&
      !file.read(contents.data(), static_cast<std::streamsize>(contents.size()))) {
    throw std::runtime_error(std::string("WebGPU Debayer RCD: Failed to read ") + label + ": " +
                             path.string());
  }
  return contents;
}

auto MakeBuffer(uint64_t size, wgpu::BufferUsage usage) -> wgpu::Buffer {
  wgpu::BufferDescriptor descriptor{};
  descriptor.usage = usage;
  descriptor.size  = size;
  auto buffer      = WebGpuContext::Instance().Device().CreateBuffer(&descriptor);
  if (!buffer.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create buffer.");
  }
  return buffer;
}

void SubmitAndWait(const wgpu::CommandBuffer& command_buffer) {
  const auto submit_start = Clock::now();
  WebGpuContext::Instance().Queue().Submit(1, &command_buffer);
  const auto submit_ms = MsSince(submit_start);
  const auto wait_start = Clock::now();
  WebGpuContext::Instance().WaitForSubmittedWork();
  const auto wait_ms = MsSince(wait_start);
  std::cout << "[WebGPU RCD timing] queue_submit=" << submit_ms << " ms"
            << " wait_submitted_work=" << wait_ms << " ms"
            << " submit_wait_total=" << (submit_ms + wait_ms) << " ms\n";
}

auto MakeTextureView(const WebGpuImage& image) -> wgpu::TextureView {
  wgpu::TextureViewDescriptor descriptor{};
  descriptor.dimension = wgpu::TextureViewDimension::e2D;
  auto view            = image.Texture().CreateView(&descriptor);
  if (!view.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create texture view.");
  }
  return view;
}

void ConfigureLayoutEntry(wgpu::BindGroupLayoutEntry& entry, BindingKind kind) {
  entry.visibility = wgpu::ShaderStage::Compute;
  switch (kind) {
    case BindingKind::ReadTextureR32F:
      entry.texture.sampleType    = wgpu::TextureSampleType::UnfilterableFloat;
      entry.texture.viewDimension = wgpu::TextureViewDimension::e2D;
      break;
    case BindingKind::WriteTextureR32F:
      entry.storageTexture.access        = wgpu::StorageTextureAccess::WriteOnly;
      entry.storageTexture.format        = wgpu::TextureFormat::R32Float;
      entry.storageTexture.viewDimension = wgpu::TextureViewDimension::e2D;
      break;
    case BindingKind::WriteTextureRGBA16F:
      entry.storageTexture.access        = wgpu::StorageTextureAccess::WriteOnly;
      entry.storageTexture.format        = wgpu::TextureFormat::RGBA16Float;
      entry.storageTexture.viewDimension = wgpu::TextureViewDimension::e2D;
      break;
    case BindingKind::WriteTextureRGBA32F:
      entry.storageTexture.access        = wgpu::StorageTextureAccess::WriteOnly;
      entry.storageTexture.format        = wgpu::TextureFormat::RGBA32Float;
      entry.storageTexture.viewDimension = wgpu::TextureViewDimension::e2D;
      break;
    case BindingKind::UniformBuffer:
      entry.buffer.type = wgpu::BufferBindingType::Uniform;
      break;
  }
}

auto GetOrCreatePipeline(Kernel kernel, bool* cache_hit, double* lookup_ms)
    -> wgpu::ComputePipeline {
  const auto start = Clock::now();
  static std::unordered_map<std::string, wgpu::ComputePipeline> cache;
  const std::string                                             key = KernelNameFor(kernel);
  auto                                                          it  = cache.find(key);
  if (it != cache.end()) {
    if (cache_hit != nullptr) {
      *cache_hit = true;
    }
    if (lookup_ms != nullptr) {
      *lookup_ms = MsSince(start);
    }
    return it->second;
  }
  if (cache_hit != nullptr) {
    *cache_hit = false;
  }

#ifndef ALCEDO_WEBGPU_DEBAYER_RCD_WGSL_PATH
#error "ALCEDO_WEBGPU_DEBAYER_RCD_WGSL_PATH must be defined when WebGPU Debayer RCD is enabled."
#endif

  auto&      device = WebGpuContext::Instance().Device();
  auto       stage_start = Clock::now();
  const auto wgsl_source =
      ReadTextFile(ALCEDO_WEBGPU_DEBAYER_RCD_WGSL_PATH, "debayer_rcd WGSL shader");
  const auto read_wgsl_ms = MsSince(stage_start);

  wgpu::ShaderSourceWGSL       wgsl_desc{};
  wgpu::ShaderModuleDescriptor shader_desc{};
  wgsl_desc.code          = std::string_view(wgsl_source.data(), wgsl_source.size());
  shader_desc.nextInChain = &wgsl_desc;
  stage_start             = Clock::now();
  auto shader_module      = device.CreateShaderModule(&shader_desc);
  const auto shader_module_ms = MsSince(stage_start);
  if (!shader_module.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create shader module.");
  }

  stage_start                              = Clock::now();
  const auto                              kinds = BindingKindsFor(kernel);
  std::vector<wgpu::BindGroupLayoutEntry> entries;
  entries.reserve(kinds.size());
  for (uint32_t i = 0; i < kinds.size(); ++i) {
    wgpu::BindGroupLayoutEntry entry{};
    entry.binding = i;
    ConfigureLayoutEntry(entry, kinds[i]);
    entries.push_back(entry);
  }
  const auto layout_entries_ms = MsSince(stage_start);

  wgpu::BindGroupLayoutDescriptor bgl_desc{};
  bgl_desc.entryCount    = entries.size();
  bgl_desc.entries       = entries.data();
  stage_start            = Clock::now();
  auto bind_group_layout = device.CreateBindGroupLayout(&bgl_desc);
  const auto bind_group_layout_ms = MsSince(stage_start);
  if (!bind_group_layout.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create bind group layout.");
  }

  wgpu::PipelineLayoutDescriptor pl_desc{};
  pl_desc.bindGroupLayoutCount = 1;
  pl_desc.bindGroupLayouts     = &bind_group_layout;
  stage_start                  = Clock::now();
  auto pipeline_layout         = device.CreatePipelineLayout(&pl_desc);
  const auto pipeline_layout_ms = MsSince(stage_start);
  if (!pipeline_layout.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create pipeline layout.");
  }

  wgpu::ComputePipelineDescriptor cp_desc{};
  cp_desc.layout             = pipeline_layout;
  cp_desc.compute.module     = shader_module;
  cp_desc.compute.entryPoint = key.c_str();
  stage_start                = Clock::now();
  auto pipeline              = device.CreateComputePipeline(&cp_desc);
  const auto compute_pipeline_ms = MsSince(stage_start);
  if (!pipeline.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create compute pipeline.");
  }

  cache[key] = pipeline;
  const auto total_ms = MsSince(start);
  if (lookup_ms != nullptr) {
    *lookup_ms = total_ms;
  }
  std::cout << "[WebGPU RCD timing] pipeline kernel=" << key << " cache=miss"
            << " read_wgsl=" << read_wgsl_ms << " ms"
            << " shader_module=" << shader_module_ms << " ms"
            << " layout_entries=" << layout_entries_ms << " ms"
            << " bind_group_layout=" << bind_group_layout_ms << " ms"
            << " pipeline_layout=" << pipeline_layout_ms << " ms"
            << " compute_pipeline=" << compute_pipeline_ms << " ms"
            << " total=" << total_ms << " ms\n";
  return pipeline;
}

struct BindingResource {
  const WebGpuImage* texture = nullptr;
  wgpu::Buffer       buffer  = nullptr;
};

auto ShiftBayerPattern(const BayerPattern2x2& pattern, uint32_t y_offset, uint32_t x_offset)
    -> BayerPattern2x2 {
  BayerPattern2x2 shifted = {};
  for (int y = 0; y < 2; ++y) {
    for (int x = 0; x < 2; ++x) {
      const int src_idx = BayerCellIndex(y + static_cast<int>(y_offset),
                                         x + static_cast<int>(x_offset));
      const int dst_idx = BayerCellIndex(y, x);
      shifted.raw_fc[dst_idx] = pattern.raw_fc[src_idx];
      shifted.rgb_fc[dst_idx] = pattern.rgb_fc[src_idx];
    }
  }
  return shifted;
}

auto Intersect(const TileRect& lhs, const TileRect& rhs) -> TileRect {
  const uint32_t left   = std::max(lhs.x, rhs.x);
  const uint32_t top    = std::max(lhs.y, rhs.y);
  const uint32_t right  = std::min(lhs.x + lhs.width, rhs.x + rhs.width);
  const uint32_t bottom = std::min(lhs.y + lhs.height, rhs.y + rhs.height);
  if (right <= left || bottom <= top) {
    return {};
  }
  return {left, top, right - left, bottom - top};
}

auto ExpandWithHalo(const TileRect& rect, uint32_t halo, uint32_t width, uint32_t height)
    -> TileRect {
  const uint32_t left   = rect.x > halo ? rect.x - halo : 0;
  const uint32_t top    = rect.y > halo ? rect.y - halo : 0;
  const uint32_t right  = std::min(width, rect.x + rect.width + halo);
  const uint32_t bottom = std::min(height, rect.y + rect.height + halo);
  return {left, top, right - left, bottom - top};
}

void EncodeCopyLogicalRegionIntoTile(wgpu::CommandEncoder& encoder, const TiledWebGpuImage& src,
                                     const TileRect& logical_region, WebGpuImage& dst) {
  for (uint32_t tile_y = 0; tile_y < src.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < src.TileColumns(); ++tile_x) {
      const TileIndex src_index{tile_x, tile_y};
      const TileRect  src_rect = src.TileRegion(src_index);
      const TileRect  overlap  = Intersect(src_rect, logical_region);
      if (overlap.width == 0 || overlap.height == 0) {
        continue;
      }
      src.Tile(src_index)
          .EncodeCopyRegionTo(encoder, dst, overlap.x - src_rect.x, overlap.y - src_rect.y,
                              overlap.width, overlap.height, overlap.x - logical_region.x,
                              overlap.y - logical_region.y);
    }
  }
}

auto CreateBindGroup(const wgpu::ComputePipeline&        pipeline,
                     const std::vector<BindingResource>& resources) -> wgpu::BindGroup {
  std::vector<wgpu::TextureView> views;
  views.reserve(resources.size());
  std::vector<wgpu::BindGroupEntry> entries;
  entries.reserve(resources.size());

  for (uint32_t i = 0; i < resources.size(); ++i) {
    wgpu::BindGroupEntry entry{};
    entry.binding = i;
    if (resources[i].texture != nullptr) {
      views.push_back(MakeTextureView(*resources[i].texture));
      entry.textureView = views.back();
    } else {
      entry.buffer = resources[i].buffer;
    }
    entries.push_back(entry);
  }

  wgpu::BindGroupDescriptor desc{};
  desc.layout     = pipeline.GetBindGroupLayout(0);
  desc.entryCount = entries.size();
  desc.entries    = entries.data();
  auto bind_group = WebGpuContext::Instance().Device().CreateBindGroup(&desc);
  if (!bind_group.Get()) {
    throw std::runtime_error("WebGPU Debayer RCD: Failed to create bind group.");
  }
  return bind_group;
}

void Dispatch(wgpu::ComputePassEncoder& compute, Kernel kernel,
              const std::vector<BindingResource>& resources, uint32_t width, uint32_t height) {
  constexpr uint32_t kWorkgroupWidth  = 32;
  constexpr uint32_t kWorkgroupHeight = 8;
  bool              pipeline_cache_hit = false;
  double            pipeline_lookup_ms = 0.0;
  auto pipeline = GetOrCreatePipeline(kernel, &pipeline_cache_hit, &pipeline_lookup_ms);
  auto stage_start = Clock::now();
  auto bind_group = CreateBindGroup(pipeline, resources);
  const auto bind_group_ms = MsSince(stage_start);
  stage_start              = Clock::now();
  compute.SetPipeline(pipeline);
  compute.SetBindGroup(0, bind_group);
  const auto groups_x = (width + kWorkgroupWidth - 1) / kWorkgroupWidth;
  const auto groups_y = (height + kWorkgroupHeight - 1) / kWorkgroupHeight;
  compute.DispatchWorkgroups(groups_x, groups_y, 1);
  const auto encode_ms = MsSince(stage_start);
  std::cout << "[WebGPU RCD timing] dispatch kernel=" << KernelNameFor(kernel)
            << " pipeline_cache=" << (pipeline_cache_hit ? "hit" : "miss")
            << " pipeline_lookup=" << pipeline_lookup_ms << " ms"
            << " bind_group=" << bind_group_ms << " ms"
            << " encode=" << encode_ms << " ms"
            << " workgroups=" << groups_x << 'x' << groups_y << "\n";
}

struct RcdJob {
  WebGpuImage input;
  WebGpuImage g0;
  WebGpuImage g1;
  WebGpuImage dir;
  WebGpuImage output;
  wgpu::Buffer params_buffer = nullptr;
  uint32_t     width         = 0;
  uint32_t     height        = 0;
};

auto CreatePlaneParamsBuffer(const WebGpuImage& image, const BayerPattern2x2& pattern)
    -> wgpu::Buffer {
  const SinglePlaneParams plane_params{
      .rgb_fc = {static_cast<uint32_t>(pattern.rgb_fc[0]), static_cast<uint32_t>(pattern.rgb_fc[1]),
                 static_cast<uint32_t>(pattern.rgb_fc[2]),
                 static_cast<uint32_t>(pattern.rgb_fc[3])},
      .width  = image.Width(),
      .height = image.Height(),
      .stride = image.Width(),
      .padding = 0,
  };
  auto params_buffer = MakeBuffer(sizeof(SinglePlaneParams),
                                  wgpu::BufferUsage::Uniform | wgpu::BufferUsage::CopyDst);
  WebGpuContext::Instance().Queue().WriteBuffer(params_buffer, 0, &plane_params,
                                                sizeof(plane_params));
  return params_buffer;
}

auto CreateRcdJob(WebGpuImage&& input, const BayerPattern2x2& pattern) -> RcdJob {
  if (input.Empty()) {
    throw std::runtime_error("WebGPU Debayer RCD: job input image is empty.");
  }

  RcdJob job{};
  job.width  = input.Width();
  job.height = input.Height();
  job.input  = std::move(input);
  job.g0.Create(job.width, job.height, PixelFormat::R32FLOAT);
  job.g1.Create(job.width, job.height, PixelFormat::R32FLOAT);
  job.dir.Create(job.width, job.height, PixelFormat::RGBA16FLOAT);
  job.output.Create(job.width, job.height, PixelFormat::RGBA32FLOAT);
  job.params_buffer = CreatePlaneParamsBuffer(job.input, pattern);
  return job;
}

void EncodeRcdJob(wgpu::ComputePassEncoder& compute, RcdJob& job) {
  Dispatch(compute, Kernel::InitAndVH,
           {{&job.input}, {&job.g0}, {&job.dir}, {nullptr, job.params_buffer}}, job.width,
           job.height);
  Dispatch(compute, Kernel::GreenAtRB,
           {{&job.input}, {&job.dir}, {&job.g0}, {&job.g1}, {nullptr, job.params_buffer}},
           job.width, job.height);
  Dispatch(compute, Kernel::FinalRGBA,
           {{&job.dir}, {&job.g1}, {&job.input}, {&job.output}, {nullptr, job.params_buffer}},
           job.width, job.height);
}

}  // namespace

void Bayer2x2ToRGB_RCD(WebGpuImage& image, const BayerPattern2x2& pattern) {
  if (image.Empty()) {
    throw std::runtime_error("WebGPU Debayer RCD: input image is empty.");
  }
  if (image.Format() != PixelFormat::R32FLOAT) {
    throw std::runtime_error("WebGPU Debayer RCD: expected R32FLOAT Bayer input.");
  }

  if (image.Width() == 0 || image.Height() == 0) {
    return;
  }

  auto        stage_start = Clock::now();
  RcdJob      job         = CreateRcdJob(std::move(image), pattern);
  const auto  job_create_ms = MsSince(stage_start);
  stage_start = Clock::now();
  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  const auto encoder_create_ms = MsSince(stage_start);
  {
    stage_start  = Clock::now();
    auto compute = encoder.BeginComputePass();
    const auto begin_compute_ms = MsSince(stage_start);
    std::cout << "[WebGPU RCD timing] setup"
              << " job_create=" << job_create_ms << " ms"
              << " encoder_create=" << encoder_create_ms << " ms"
              << " begin_compute=" << begin_compute_ms << " ms\n";

    EncodeRcdJob(compute, job);

    stage_start = Clock::now();
    compute.End();
    std::cout << "[WebGPU RCD timing] compute_end=" << MsSince(stage_start) << " ms\n";
  }
  stage_start = Clock::now();
  auto command_buffer = encoder.Finish();
  std::cout << "[WebGPU RCD timing] encoder_finish=" << MsSince(stage_start) << " ms\n";
  SubmitAndWait(command_buffer);

  image = std::move(job.output);
}

void Bayer2x2ToRGB_RCD(TiledWebGpuImage& image, const BayerPattern2x2& pattern) {
  if (image.Empty()) {
    throw std::runtime_error("WebGPU Debayer RCD: tiled input image is empty.");
  }
  if (image.Format() != PixelFormat::R32FLOAT) {
    throw std::runtime_error("WebGPU Debayer RCD: expected R32FLOAT Bayer input.");
  }

  // The tiled path reruns the whole three-stage RCD chain on a temporary patch, so the
  // required halo is the composed dependency radius, not the largest radius of any single
  // kernel. Final green reconstruction reaches helper RB samples at +/-3; those helper
  // samples read green values at +/-2; and green reconstruction itself depends on raw input
  // out to +/-5 through the dir/VH path. 3 + 2 + 5 => 10.
  constexpr uint32_t kHalo = 10;
  TiledWebGpuImage   output;
  output.Create(image.Width(), image.Height(), PixelFormat::RGBA32FLOAT,
                image.TileShape().width);

  struct TiledRcdJob {
    TileIndex out_index;
    TileRect  out_rect;
    TileRect  halo_rect;
    RcdJob    rcd;
  };

  std::vector<TiledRcdJob> jobs;
  jobs.reserve(image.TileCount());

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  for (uint32_t tile_y = 0; tile_y < image.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < image.TileColumns(); ++tile_x) {
      const TileIndex out_index{tile_x, tile_y};
      const TileRect  out_rect = image.TileRegion(out_index);
      const TileRect  halo_rect = ExpandWithHalo(out_rect, kHalo, image.Width(), image.Height());

      WebGpuImage halo_input;
      halo_input.Create(halo_rect.width, halo_rect.height, PixelFormat::R32FLOAT);
      EncodeCopyLogicalRegionIntoTile(encoder, image, halo_rect, halo_input);

      const auto shifted_pattern = ShiftBayerPattern(pattern, halo_rect.y, halo_rect.x);
      jobs.push_back({out_index, out_rect, halo_rect,
                      CreateRcdJob(std::move(halo_input), shifted_pattern)});
    }
  }

  {
    auto compute = encoder.BeginComputePass();
    for (auto& job : jobs) {
      EncodeRcdJob(compute, job.rcd);
    }
    compute.End();
  }

  for (auto& job : jobs) {
    auto& out_tile = output.Tile(job.out_index);
    job.rcd.output.EncodeCopyRegionTo(
        encoder, out_tile, job.out_rect.x - job.halo_rect.x, job.out_rect.y - job.halo_rect.y,
        job.out_rect.width, job.out_rect.height);
  }

  SubmitAndWait(encoder.Finish());
  image = std::move(output);
}

}  // namespace webgpu
}  // namespace alcedo

#endif
