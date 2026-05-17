//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_WEBGPU

#include "decoders/processor/operators/gpu/webgpu_cvt_ref_space.hpp"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <stdexcept>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

#include "image/tiled_webgpu_image.hpp"
#include "webgpu/webgpu_context.hpp"

namespace alcedo {
namespace webgpu {
namespace {

struct ImageParams {
  uint32_t width;
  uint32_t height;
  uint32_t stride;
  uint32_t channels;
};

struct OrientParams {
  uint32_t src_width;
  uint32_t src_height;
  uint32_t dst_width;
  uint32_t dst_height;
  uint32_t src_stride;
  uint32_t dst_stride;
  uint32_t flip;
  uint32_t padding;
  float    gain[4];
};

constexpr float    kMinGain           = 1e-6f;
constexpr uint32_t kRowAlignmentBytes = 256;

auto               AlignRowBytes(size_t row_bytes) -> size_t {
  return ((row_bytes + kRowAlignmentBytes - 1) / kRowAlignmentBytes) * kRowAlignmentBytes;
}

auto ChannelCount(PixelFormat format) -> uint32_t {
  switch (format) {
    case PixelFormat::R32FLOAT:
      return 1;
    case PixelFormat::RGBA32FLOAT:
      return 4;
    default:
      throw std::runtime_error("WebGPU CvtRefSpace: expected R32FLOAT or RGBA32FLOAT image.");
  }
}

auto ReadTextFile(const std::filesystem::path& path, const char* label) -> std::string {
  std::ifstream file(path, std::ios::binary | std::ios::ate);
  if (!file) {
    throw std::runtime_error(std::string("WebGPU CvtRefSpace: Failed to open ") + label + ": " +
                             path.string());
  }

  const auto size = file.tellg();
  if (size < 0) {
    throw std::runtime_error(std::string("WebGPU CvtRefSpace: Failed to stat ") + label + ": " +
                             path.string());
  }

  std::string contents(static_cast<size_t>(size), '\0');
  file.seekg(0, std::ios::beg);
  if (!contents.empty() &&
      !file.read(contents.data(), static_cast<std::streamsize>(contents.size()))) {
    throw std::runtime_error(std::string("WebGPU CvtRefSpace: Failed to read ") + label + ": " +
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
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create buffer.");
  }
  return buffer;
}

auto MakeExtent(uint32_t width, uint32_t height) -> wgpu::Extent3D {
  return wgpu::Extent3D{width, height, 1};
}

auto MakeTextureCopy(const wgpu::Texture& texture) -> wgpu::TexelCopyTextureInfo {
  wgpu::TexelCopyTextureInfo copy{};
  copy.texture  = texture;
  copy.mipLevel = 0;
  copy.origin   = wgpu::Origin3D{0, 0, 0};
  copy.aspect   = wgpu::TextureAspect::All;
  return copy;
}

auto MakeBufferCopy(const wgpu::Buffer& buffer, uint32_t row_bytes, uint32_t rows)
    -> wgpu::TexelCopyBufferInfo {
  wgpu::TexelCopyBufferInfo copy{};
  copy.buffer              = buffer;
  copy.layout.offset       = 0;
  copy.layout.bytesPerRow  = row_bytes;
  copy.layout.rowsPerImage = rows;
  return copy;
}

auto MakeTextureView(const WebGpuImage& image) -> wgpu::TextureView {
  wgpu::TextureViewDescriptor descriptor{};
  descriptor.dimension = wgpu::TextureViewDimension::e2D;
  auto view            = image.Texture().CreateView(&descriptor);
  if (!view.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create texture view.");
  }
  return view;
}

void SubmitAndWait(const wgpu::CommandBuffer& command_buffer) {
  WebGpuContext::Instance().Queue().Submit(1, &command_buffer);
  WebGpuContext::Instance().WaitForSubmittedWork();
}

auto GetOrCreatePipeline(const char* entry_point, const std::vector<wgpu::BufferBindingType>& types)
    -> wgpu::ComputePipeline {
  static std::unordered_map<std::string, wgpu::ComputePipeline> cache;
  auto                                                          it = cache.find(entry_point);
  if (it != cache.end()) {
    return it->second;
  }

#ifndef ALCEDO_WEBGPU_CVT_REF_SPACE_WGSL_PATH
#error "ALCEDO_WEBGPU_CVT_REF_SPACE_WGSL_PATH must be defined when WebGPU CvtRefSpace is enabled."
#endif

  auto&      device = WebGpuContext::Instance().Device();
  const auto wgsl_source =
      ReadTextFile(ALCEDO_WEBGPU_CVT_REF_SPACE_WGSL_PATH, "cvt_ref_space WGSL shader");

  wgpu::ShaderSourceWGSL       wgsl_desc{};
  wgpu::ShaderModuleDescriptor shader_desc{};
  wgsl_desc.code          = std::string_view(wgsl_source.data(), wgsl_source.size());
  shader_desc.nextInChain = &wgsl_desc;
  auto shader_module      = device.CreateShaderModule(&shader_desc);
  if (!shader_module.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create shader module.");
  }

  std::vector<wgpu::BindGroupLayoutEntry> entries;
  entries.reserve(types.size());
  for (uint32_t i = 0; i < types.size(); ++i) {
    wgpu::BindGroupLayoutEntry e{};
    e.binding     = i;
    e.visibility  = wgpu::ShaderStage::Compute;
    e.buffer.type = types[i];
    entries.push_back(e);
  }

  wgpu::BindGroupLayoutDescriptor bgl_desc{};
  bgl_desc.entryCount    = entries.size();
  bgl_desc.entries       = entries.data();
  auto bind_group_layout = device.CreateBindGroupLayout(&bgl_desc);
  if (!bind_group_layout.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create bind group layout.");
  }

  wgpu::PipelineLayoutDescriptor pl_desc{};
  pl_desc.bindGroupLayoutCount = 1;
  pl_desc.bindGroupLayouts     = &bind_group_layout;
  auto pipeline_layout         = device.CreatePipelineLayout(&pl_desc);
  if (!pipeline_layout.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create pipeline layout.");
  }

  wgpu::ComputePipelineDescriptor cp_desc{};
  cp_desc.layout             = pipeline_layout;
  cp_desc.compute.module     = shader_module;
  cp_desc.compute.entryPoint = entry_point;
  auto pipeline              = device.CreateComputePipeline(&cp_desc);
  if (!pipeline.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create compute pipeline.");
  }

  cache[entry_point] = pipeline;
  return pipeline;
}

auto CreateBindGroup(const wgpu::ComputePipeline&     pipeline,
                     const std::vector<wgpu::Buffer>& buffers) -> wgpu::BindGroup {
  std::vector<wgpu::BindGroupEntry> entries;
  entries.reserve(buffers.size());
  for (uint32_t i = 0; i < buffers.size(); ++i) {
    wgpu::BindGroupEntry e{};
    e.binding = i;
    e.buffer  = buffers[i];
    entries.push_back(e);
  }

  wgpu::BindGroupDescriptor desc{};
  desc.layout     = pipeline.GetBindGroupLayout(0);
  desc.entryCount = entries.size();
  desc.entries    = entries.data();
  return WebGpuContext::Instance().Device().CreateBindGroup(&desc);
}

auto GetOrCreateOrientTexturePipeline() -> wgpu::ComputePipeline {
  static wgpu::ComputePipeline pipeline = nullptr;
  if (pipeline.Get()) {
    return pipeline;
  }

#ifndef ALCEDO_WEBGPU_CVT_REF_SPACE_WGSL_PATH
#error "ALCEDO_WEBGPU_CVT_REF_SPACE_WGSL_PATH must be defined when WebGPU CvtRefSpace is enabled."
#endif

  auto&      device = WebGpuContext::Instance().Device();
  const auto wgsl_source =
      ReadTextFile(ALCEDO_WEBGPU_CVT_REF_SPACE_WGSL_PATH, "cvt_ref_space WGSL shader");

  wgpu::ShaderSourceWGSL       wgsl_desc{};
  wgpu::ShaderModuleDescriptor shader_desc{};
  wgsl_desc.code          = std::string_view(wgsl_source.data(), wgsl_source.size());
  shader_desc.nextInChain = &wgsl_desc;
  auto shader_module      = device.CreateShaderModule(&shader_desc);
  if (!shader_module.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create shader module.");
  }

  std::array<wgpu::BindGroupLayoutEntry, 3> entries{};
  entries[0].binding                      = 0;
  entries[0].visibility                   = wgpu::ShaderStage::Compute;
  entries[0].texture.sampleType           = wgpu::TextureSampleType::UnfilterableFloat;
  entries[0].texture.viewDimension        = wgpu::TextureViewDimension::e2D;

  entries[1].binding                      = 1;
  entries[1].visibility                   = wgpu::ShaderStage::Compute;
  entries[1].storageTexture.access        = wgpu::StorageTextureAccess::WriteOnly;
  entries[1].storageTexture.format        = wgpu::TextureFormat::RGBA32Float;
  entries[1].storageTexture.viewDimension = wgpu::TextureViewDimension::e2D;

  entries[2].binding                      = 2;
  entries[2].visibility                   = wgpu::ShaderStage::Compute;
  entries[2].buffer.type                  = wgpu::BufferBindingType::Uniform;

  wgpu::BindGroupLayoutDescriptor bgl_desc{};
  bgl_desc.entryCount    = entries.size();
  bgl_desc.entries       = entries.data();
  auto bind_group_layout = device.CreateBindGroupLayout(&bgl_desc);
  if (!bind_group_layout.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create bind group layout.");
  }

  wgpu::PipelineLayoutDescriptor pl_desc{};
  pl_desc.bindGroupLayoutCount = 1;
  pl_desc.bindGroupLayouts     = &bind_group_layout;
  auto pipeline_layout         = device.CreatePipelineLayout(&pl_desc);
  if (!pipeline_layout.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create pipeline layout.");
  }

  wgpu::ComputePipelineDescriptor cp_desc{};
  cp_desc.layout             = pipeline_layout;
  cp_desc.compute.module     = shader_module;
  cp_desc.compute.entryPoint = "apply_inverse_cam_mul_oriented_rgba";
  pipeline                   = device.CreateComputePipeline(&cp_desc);
  if (!pipeline.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create compute pipeline.");
  }
  return pipeline;
}

auto CreateOrientTextureBindGroup(const wgpu::ComputePipeline& pipeline, const WebGpuImage& src,
                                  const WebGpuImage& dst, const wgpu::Buffer& params_buffer)
    -> wgpu::BindGroup {
  auto                                src_view = MakeTextureView(src);
  auto                                dst_view = MakeTextureView(dst);

  std::array<wgpu::BindGroupEntry, 3> entries{};
  entries[0].binding     = 0;
  entries[0].textureView = src_view;
  entries[1].binding     = 1;
  entries[1].textureView = dst_view;
  entries[2].binding     = 2;
  entries[2].buffer      = params_buffer;

  wgpu::BindGroupDescriptor desc{};
  desc.layout     = pipeline.GetBindGroupLayout(0);
  desc.entryCount = entries.size();
  desc.entries    = entries.data();
  auto bind_group = WebGpuContext::Instance().Device().CreateBindGroup(&desc);
  if (!bind_group.Get()) {
    throw std::runtime_error("WebGPU CvtRefSpace: Failed to create texture bind group.");
  }
  return bind_group;
}

auto OrientedWidth(uint32_t width, uint32_t height, int flip) -> uint32_t {
  return (flip == 5 || flip == 6) ? height : width;
}

auto OrientedHeight(uint32_t width, uint32_t height, int flip) -> uint32_t {
  return (flip == 5 || flip == 6) ? width : height;
}

struct ClampJob {
  WebGpuImage*  image = nullptr;
  uint32_t      row_bytes = 0;
  wgpu::Buffer  image_buffer = nullptr;
  wgpu::Buffer  params_buffer = nullptr;
  wgpu::BindGroup bind_group = nullptr;
};

auto CreateClampJob(WebGpuImage& image, const wgpu::ComputePipeline& pipeline) -> ClampJob {
  if (image.Empty()) {
    throw std::runtime_error("WebGPU CvtRefSpace: clamp image is empty.");
  }

  const uint32_t channels   = ChannelCount(image.Format());
  const uint32_t elem_bytes = static_cast<uint32_t>(sizeof(float) * channels);
  const uint32_t row_bytes =
      static_cast<uint32_t>(AlignRowBytes(static_cast<size_t>(image.Width()) * elem_bytes));
  const uint64_t buffer_size = static_cast<uint64_t>(row_bytes) * image.Height();
  const uint32_t stride      = row_bytes / elem_bytes;
  auto           image_buffer =
      MakeBuffer(buffer_size, wgpu::BufferUsage::CopyDst | wgpu::BufferUsage::CopySrc |
                                  wgpu::BufferUsage::Storage);
  auto params_buffer =
      MakeBuffer(sizeof(ImageParams), wgpu::BufferUsage::Uniform | wgpu::BufferUsage::CopyDst);
  const ImageParams params{
      .width    = image.Width(),
      .height   = image.Height(),
      .stride   = stride,
      .channels = channels,
  };
  WebGpuContext::Instance().Queue().WriteBuffer(params_buffer, 0, &params, sizeof(params));

  return {.image         = &image,
          .row_bytes     = row_bytes,
          .image_buffer  = image_buffer,
          .params_buffer = params_buffer,
          .bind_group    = CreateBindGroup(pipeline, {image_buffer, params_buffer})};
}

void EncodeClampCopiesToBuffers(wgpu::CommandEncoder& encoder, const std::vector<ClampJob>& jobs) {
  for (const auto& job : jobs) {
    auto src = MakeTextureCopy(job.image->Texture());
    auto dst = MakeBufferCopy(job.image_buffer, job.row_bytes, job.image->Height());
    auto ext = MakeExtent(job.image->Width(), job.image->Height());
    encoder.CopyTextureToBuffer(&src, &dst, &ext);
  }
}

void EncodeClampDispatches(wgpu::CommandEncoder& encoder, const wgpu::ComputePipeline& pipeline,
                           const std::vector<ClampJob>& jobs) {
  auto compute = encoder.BeginComputePass();
  compute.SetPipeline(pipeline);
  for (const auto& job : jobs) {
    compute.SetBindGroup(0, job.bind_group);
    compute.DispatchWorkgroups((job.image->Width() + 7) / 8, (job.image->Height() + 7) / 8, 1);
  }
  compute.End();
}

void EncodeClampCopiesToTextures(wgpu::CommandEncoder& encoder, const std::vector<ClampJob>& jobs) {
  for (const auto& job : jobs) {
    auto src = MakeBufferCopy(job.image_buffer, job.row_bytes, job.image->Height());
    auto dst = MakeTextureCopy(job.image->Texture());
    auto ext = MakeExtent(job.image->Width(), job.image->Height());
    encoder.CopyBufferToTexture(&src, &dst, &ext);
  }
}

auto SourceRectForDestinationRect(const TileRect& dst_rect, uint32_t src_width,
                                  uint32_t src_height, int flip) -> TileRect {
  switch (flip) {
    case 3:
      return {src_width - (dst_rect.x + dst_rect.width),
              src_height - (dst_rect.y + dst_rect.height), dst_rect.width, dst_rect.height};
    case 5:
      return {src_width - (dst_rect.y + dst_rect.height), dst_rect.x, dst_rect.height,
              dst_rect.width};
    case 6:
      return {dst_rect.y, src_height - (dst_rect.x + dst_rect.width), dst_rect.height,
              dst_rect.width};
    default:
      return dst_rect;
  }
}

auto CreateOrientParamsBuffer(uint32_t src_width, uint32_t src_height, const float* cam_mul,
                              int flip) -> wgpu::Buffer {
  const uint32_t dst_width  = OrientedWidth(src_width, src_height, flip);
  const uint32_t dst_height = OrientedHeight(src_width, src_height, flip);
  auto           params_buffer =
      MakeBuffer(sizeof(OrientParams), wgpu::BufferUsage::Uniform | wgpu::BufferUsage::CopyDst);

  const float        g = std::max(cam_mul[1], kMinGain);
  const OrientParams params{
      .src_width  = src_width,
      .src_height = src_height,
      .dst_width  = dst_width,
      .dst_height = dst_height,
      .src_stride = src_width,
      .dst_stride = dst_width,
      .flip       = static_cast<uint32_t>(flip),
      .padding    = 0,
      .gain = {g / std::max(cam_mul[0], kMinGain), 1.0f, g / std::max(cam_mul[2], kMinGain), 1.0f},
  };
  WebGpuContext::Instance().Queue().WriteBuffer(params_buffer, 0, &params, sizeof(params));
  return params_buffer;
}

struct OrientJob {
  WebGpuImage  src;
  WebGpuImage  dst;
  wgpu::Buffer params_buffer = nullptr;
  wgpu::BindGroup bind_group = nullptr;
};

auto CreateOrientJob(WebGpuImage&& src, const float* cam_mul, int flip,
                     const wgpu::ComputePipeline& pipeline) -> OrientJob {
  OrientJob job{};
  job.src = std::move(src);
  job.dst.Create(OrientedWidth(job.src.Width(), job.src.Height(), flip),
                 OrientedHeight(job.src.Width(), job.src.Height(), flip),
                 PixelFormat::RGBA32FLOAT);
  job.params_buffer = CreateOrientParamsBuffer(job.src.Width(), job.src.Height(), cam_mul, flip);
  job.bind_group =
      CreateOrientTextureBindGroup(pipeline, job.src, job.dst, job.params_buffer);
  return job;
}

void EncodeOrientJobs(wgpu::CommandEncoder& encoder, const wgpu::ComputePipeline& pipeline,
                      std::vector<OrientJob>& jobs) {
  auto compute = encoder.BeginComputePass();
  compute.SetPipeline(pipeline);
  for (auto& job : jobs) {
    compute.SetBindGroup(0, job.bind_group);
    compute.DispatchWorkgroups((job.src.Width() + 7) / 8, (job.src.Height() + 7) / 8, 1);
  }
  compute.End();
}

}  // namespace

void Clamp01(WebGpuImage& image) {
  if (image.Empty()) {
    return;
  }

  auto pipeline = GetOrCreatePipeline(
      "clamp01", {wgpu::BufferBindingType::Storage, wgpu::BufferBindingType::Uniform});
  auto jobs     = std::vector<ClampJob>{CreateClampJob(image, pipeline)};

  auto encoder    = WebGpuContext::Instance().Device().CreateCommandEncoder();
  EncodeClampCopiesToBuffers(encoder, jobs);
  EncodeClampDispatches(encoder, pipeline, jobs);
  EncodeClampCopiesToTextures(encoder, jobs);
  SubmitAndWait(encoder.Finish());
}

void Clamp01(TiledWebGpuImage& image) {
  if (image.Empty()) {
    return;
  }
  auto pipeline = GetOrCreatePipeline(
      "clamp01", {wgpu::BufferBindingType::Storage, wgpu::BufferBindingType::Uniform});
  std::vector<ClampJob> jobs;
  jobs.reserve(image.TileCount());
  for (uint32_t tile_y = 0; tile_y < image.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < image.TileColumns(); ++tile_x) {
      jobs.push_back(CreateClampJob(image.Tile({tile_x, tile_y}), pipeline));
    }
  }

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  EncodeClampCopiesToBuffers(encoder, jobs);
  EncodeClampDispatches(encoder, pipeline, jobs);
  EncodeClampCopiesToTextures(encoder, jobs);
  SubmitAndWait(encoder.Finish());
}

void ApplyInverseCamMulAndOrientRGBA(WebGpuImage& image, const float* cam_mul, int flip) {
  if (image.Empty()) {
    return;
  }
  if (image.Format() != PixelFormat::RGBA32FLOAT) {
    throw std::runtime_error("WebGPU CvtRefSpace: expected RGBA32FLOAT image.");
  }
  if (cam_mul == nullptr) {
    throw std::runtime_error("WebGPU CvtRefSpace: cam_mul is null.");
  }

  auto pipeline = GetOrCreateOrientTexturePipeline();
  auto jobs     = std::vector<OrientJob>{CreateOrientJob(std::move(image), cam_mul, flip, pipeline)};

  auto encoder    = WebGpuContext::Instance().Device().CreateCommandEncoder();
  EncodeOrientJobs(encoder, pipeline, jobs);
  SubmitAndWait(encoder.Finish());

  image = std::move(jobs.front().dst);
}

void ApplyInverseCamMulAndOrientRGBA(TiledWebGpuImage& image, const float* cam_mul, int flip) {
  if (image.Empty()) {
    return;
  }
  if (image.Format() != PixelFormat::RGBA32FLOAT) {
    throw std::runtime_error("WebGPU CvtRefSpace: expected RGBA32FLOAT image.");
  }
  if (cam_mul == nullptr) {
    throw std::runtime_error("WebGPU CvtRefSpace: cam_mul is null.");
  }

  const uint32_t src_width  = image.Width();
  const uint32_t src_height = image.Height();
  auto           pipeline   = GetOrCreateOrientTexturePipeline();
  TiledWebGpuImage output;
  output.Create(OrientedWidth(src_width, src_height, flip),
                OrientedHeight(src_width, src_height, flip), PixelFormat::RGBA32FLOAT,
                image.TileShape().width);

  struct TiledOrientJob {
    TileIndex dst_index;
    TileRect  dst_rect;
    TileRect  src_rect;
    OrientJob orient;
  };

  std::vector<TiledOrientJob> jobs;
  jobs.reserve(output.TileCount());

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  for (uint32_t tile_y = 0; tile_y < output.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < output.TileColumns(); ++tile_x) {
      const TileIndex dst_index{tile_x, tile_y};
      const TileRect  dst_rect = output.TileRegion(dst_index);
      const TileRect  src_rect =
          SourceRectForDestinationRect(dst_rect, src_width, src_height, flip);

      WebGpuImage patch;
      patch.Create(src_rect.width, src_rect.height, PixelFormat::RGBA32FLOAT);
      image.EncodeCopyRegionTo(encoder, patch, src_rect);
      jobs.push_back(
          {dst_index, dst_rect, src_rect, CreateOrientJob(std::move(patch), cam_mul, flip, pipeline)});
    }
  }

  {
    std::vector<OrientJob> orient_jobs;
    orient_jobs.reserve(jobs.size());
    for (auto& job : jobs) {
      orient_jobs.push_back(std::move(job.orient));
    }
    EncodeOrientJobs(encoder, pipeline, orient_jobs);
    for (size_t i = 0; i < jobs.size(); ++i) {
      jobs[i].orient = std::move(orient_jobs[i]);
    }
  }

  for (auto& job : jobs) {
    job.orient.dst.EncodeCopyRegionTo(encoder, output.Tile(job.dst_index), 0, 0,
                                      job.dst_rect.width, job.dst_rect.height);
  }

  SubmitAndWait(encoder.Finish());
  image = std::move(output);
}

}  // namespace webgpu
}  // namespace alcedo

#endif
