//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_WEBGPU

#include "webgpu/webgpu_geometry_utils.hpp"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <fstream>
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
namespace utils {
namespace {

struct GeoParams {
  uint32_t src_width;
  uint32_t src_height;
  uint32_t dst_width;
  uint32_t dst_height;
  uint32_t src_stride;
  uint32_t dst_stride;
  uint32_t channels;
  uint32_t padding;
};

constexpr uint32_t kRowAlignmentBytes = 256;

auto               AlignRowBytes(size_t row_bytes) -> size_t {
  return ((row_bytes + kRowAlignmentBytes - 1) / kRowAlignmentBytes) * kRowAlignmentBytes;
}

auto ChannelsForFormat(PixelFormat format) -> uint32_t {
  switch (format) {
    case PixelFormat::R16UINT:
    case PixelFormat::R16FLOAT:
    case PixelFormat::R32FLOAT:
      return 1;
    case PixelFormat::RGBA16UINT:
    case PixelFormat::RGBA16FLOAT:
    case PixelFormat::RGBA32FLOAT:
      return 4;
  }
  throw std::runtime_error("WebGPU Geometry Utils: unsupported pixel format.");
}

auto ReadTextFile(const std::filesystem::path& path, const char* label) -> std::string {
  std::ifstream file(path, std::ios::binary | std::ios::ate);
  if (!file) {
    throw std::runtime_error(std::string("WebGPU Geometry Utils: Failed to open ") + label + ": " +
                             path.string());
  }

  const auto size = file.tellg();
  if (size < 0) {
    throw std::runtime_error(std::string("WebGPU Geometry Utils: Failed to stat ") + label + ": " +
                             path.string());
  }

  std::string contents(static_cast<size_t>(size), '\0');
  file.seekg(0, std::ios::beg);
  if (!contents.empty() &&
      !file.read(contents.data(), static_cast<std::streamsize>(contents.size()))) {
    throw std::runtime_error(std::string("WebGPU Geometry Utils: Failed to read ") + label + ": " +
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
    throw std::runtime_error("WebGPU Geometry Utils: Failed to create buffer.");
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

void SubmitAndWait(const wgpu::CommandBuffer& command_buffer) {
  WebGpuContext::Instance().Queue().Submit(1, &command_buffer);
  WebGpuContext::Instance().WaitForSubmittedWork();
}

auto GetOrCreatePipeline(const char* entry_point) -> wgpu::ComputePipeline {
  static std::unordered_map<std::string, wgpu::ComputePipeline> cache;
  auto                                                          it = cache.find(entry_point);
  if (it != cache.end()) {
    return it->second;
  }

#ifndef ALCEDO_WEBGPU_GEOMETRY_UTILS_WGSL_PATH
#error \
    "ALCEDO_WEBGPU_GEOMETRY_UTILS_WGSL_PATH must be defined when WebGPU geometry utils is enabled."
#endif

  auto&      device = WebGpuContext::Instance().Device();
  const auto wgsl_source =
      ReadTextFile(ALCEDO_WEBGPU_GEOMETRY_UTILS_WGSL_PATH, "geometry_utils WGSL shader");

  wgpu::ShaderSourceWGSL       wgsl_desc{};
  wgpu::ShaderModuleDescriptor shader_desc{};
  wgsl_desc.code          = std::string_view(wgsl_source.data(), wgsl_source.size());
  shader_desc.nextInChain = &wgsl_desc;
  auto shader_module      = device.CreateShaderModule(&shader_desc);
  if (!shader_module.Get()) {
    throw std::runtime_error("WebGPU Geometry Utils: Failed to create shader module.");
  }

  std::array<wgpu::BindGroupLayoutEntry, 3> entries{};
  entries[0].binding     = 0;
  entries[0].visibility  = wgpu::ShaderStage::Compute;
  entries[0].buffer.type = wgpu::BufferBindingType::ReadOnlyStorage;

  entries[1].binding     = 1;
  entries[1].visibility  = wgpu::ShaderStage::Compute;
  entries[1].buffer.type = wgpu::BufferBindingType::Storage;

  entries[2].binding     = 2;
  entries[2].visibility  = wgpu::ShaderStage::Compute;
  entries[2].buffer.type = wgpu::BufferBindingType::Uniform;

  wgpu::BindGroupLayoutDescriptor bgl_desc{};
  bgl_desc.entryCount    = entries.size();
  bgl_desc.entries       = entries.data();
  auto bind_group_layout = device.CreateBindGroupLayout(&bgl_desc);
  if (!bind_group_layout.Get()) {
    throw std::runtime_error("WebGPU Geometry Utils: Failed to create bind group layout.");
  }

  wgpu::PipelineLayoutDescriptor pl_desc{};
  pl_desc.bindGroupLayoutCount = 1;
  pl_desc.bindGroupLayouts     = &bind_group_layout;
  auto pipeline_layout         = device.CreatePipelineLayout(&pl_desc);
  if (!pipeline_layout.Get()) {
    throw std::runtime_error("WebGPU Geometry Utils: Failed to create pipeline layout.");
  }

  wgpu::ComputePipelineDescriptor cp_desc{};
  cp_desc.layout             = pipeline_layout;
  cp_desc.compute.module     = shader_module;
  cp_desc.compute.entryPoint = entry_point;
  auto pipeline              = device.CreateComputePipeline(&cp_desc);
  if (!pipeline.Get()) {
    throw std::runtime_error("WebGPU Geometry Utils: Failed to create compute pipeline.");
  }

  cache[entry_point] = pipeline;
  return pipeline;
}

struct GeometryJob {
  WebGpuImage  src;
  WebGpuImage  dst;
  uint32_t     src_row_bytes = 0;
  uint32_t     dst_row_bytes = 0;
  wgpu::Buffer src_buffer = nullptr;
  wgpu::Buffer dst_buffer = nullptr;
  wgpu::Buffer params_buffer = nullptr;
  wgpu::BindGroup bind_group = nullptr;
};

auto CreateGeometryJob(WebGpuImage&& src, uint32_t dst_width, uint32_t dst_height,
                       const wgpu::ComputePipeline& pipeline) -> GeometryJob {
  if (src.Empty()) {
    throw std::runtime_error("WebGPU Geometry Utils: source image is empty.");
  }

  const auto channels  = ChannelsForFormat(src.Format());
  const auto elem_size = (channels == 1) ? sizeof(float) : sizeof(float) * 4;
  const auto src_row_bytes =
      static_cast<uint32_t>(AlignRowBytes(static_cast<size_t>(src.Width()) * elem_size));
  const auto dst_row_bytes =
      static_cast<uint32_t>(AlignRowBytes(static_cast<size_t>(dst_width) * elem_size));
  const auto src_buffer_size = static_cast<uint64_t>(src_row_bytes) * src.Height();
  const auto dst_buffer_size = static_cast<uint64_t>(dst_row_bytes) * dst_height;

  auto       src_buffer =
      MakeBuffer(src_buffer_size, wgpu::BufferUsage::CopyDst | wgpu::BufferUsage::Storage);
  auto dst_buffer =
      MakeBuffer(dst_buffer_size, wgpu::BufferUsage::CopySrc | wgpu::BufferUsage::Storage);

  GeoParams params  = {};
  params.src_width  = src.Width();
  params.src_height = src.Height();
  params.dst_width  = dst_width;
  params.dst_height = dst_height;
  params.src_stride = src_row_bytes / static_cast<uint32_t>(elem_size);
  params.dst_stride = dst_row_bytes / static_cast<uint32_t>(elem_size);
  params.channels   = channels;

  auto params_buffer =
      MakeBuffer(sizeof(GeoParams), wgpu::BufferUsage::Uniform | wgpu::BufferUsage::CopyDst);
  WebGpuContext::Instance().Queue().WriteBuffer(params_buffer, 0, &params, sizeof(params));

  std::array<wgpu::BindGroupEntry, 3> bg_entries{};
  bg_entries[0].binding = 0;
  bg_entries[0].buffer  = src_buffer;
  bg_entries[1].binding = 1;
  bg_entries[1].buffer  = dst_buffer;
  bg_entries[2].binding = 2;
  bg_entries[2].buffer  = params_buffer;

  wgpu::BindGroupDescriptor bg_desc{};
  bg_desc.layout     = pipeline.GetBindGroupLayout(0);
  bg_desc.entryCount = bg_entries.size();
  bg_desc.entries    = bg_entries.data();
  auto bind_group    = WebGpuContext::Instance().Device().CreateBindGroup(&bg_desc);
  if (!bind_group.Get()) {
    throw std::runtime_error("WebGPU Geometry Utils: Failed to create bind group.");
  }

  GeometryJob job{};
  job.src            = std::move(src);
  job.dst.Create(dst_width, dst_height, job.src.Format());
  job.src_row_bytes  = src_row_bytes;
  job.dst_row_bytes  = dst_row_bytes;
  job.src_buffer     = src_buffer;
  job.dst_buffer     = dst_buffer;
  job.params_buffer  = params_buffer;
  job.bind_group     = bind_group;
  return job;
}

void EncodeGeometryJobs(wgpu::CommandEncoder& encoder, const wgpu::ComputePipeline& pipeline,
                        std::vector<GeometryJob>& jobs) {
  for (auto& job : jobs) {
    auto s   = MakeTextureCopy(job.src.Texture());
    auto d   = MakeBufferCopy(job.src_buffer, job.src_row_bytes, job.src.Height());
    auto ext = MakeExtent(job.src.Width(), job.src.Height());
    encoder.CopyTextureToBuffer(&s, &d, &ext);
  }

  {
    auto compute = encoder.BeginComputePass();
    compute.SetPipeline(pipeline);
    for (auto& job : jobs) {
      compute.SetBindGroup(0, job.bind_group);
      compute.DispatchWorkgroups((job.dst.Width() + 7) / 8, (job.dst.Height() + 7) / 8, 1);
    }
    compute.End();
  }

  for (auto& job : jobs) {
    auto s   = MakeBufferCopy(job.dst_buffer, job.dst_row_bytes, job.dst.Height());
    auto d   = MakeTextureCopy(job.dst.Texture());
    auto ext = MakeExtent(job.dst.Width(), job.dst.Height());
    encoder.CopyBufferToTexture(&s, &d, &ext);
  }
}

void DispatchGeometry(WebGpuImage& image, uint32_t dst_width, uint32_t dst_height,
                      const char* entry_point) {
  auto pipeline = GetOrCreatePipeline(entry_point);
  auto jobs     = std::vector<GeometryJob>{
          CreateGeometryJob(std::move(image), dst_width, dst_height, pipeline)};
  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  EncodeGeometryJobs(encoder, pipeline, jobs);
  SubmitAndWait(encoder.Finish());
  image = std::move(jobs.front().dst);
}

auto Rotate180SourceRect(const TileRect& dst_rect, uint32_t src_width, uint32_t src_height)
    -> TileRect {
  return {src_width - (dst_rect.x + dst_rect.width),
          src_height - (dst_rect.y + dst_rect.height), dst_rect.width, dst_rect.height};
}

auto Rotate90CWSourceRect(const TileRect& dst_rect, uint32_t src_height) -> TileRect {
  return {dst_rect.y, src_height - (dst_rect.x + dst_rect.width), dst_rect.height,
          dst_rect.width};
}

auto Rotate90CCWSourceRect(const TileRect& dst_rect, uint32_t src_width) -> TileRect {
  return {src_width - (dst_rect.y + dst_rect.height), dst_rect.x, dst_rect.height,
          dst_rect.width};
}

}  // namespace

void Rotate180(WebGpuImage& image) {
  if (image.Empty()) {
    return;
  }
  DispatchGeometry(image, image.Width(), image.Height(), "rotate_180");
}

void Rotate180(TiledWebGpuImage& image) {
  if (image.Empty()) {
    return;
  }

  const uint32_t src_width  = image.Width();
  const uint32_t src_height = image.Height();
  TiledWebGpuImage output;
  output.Create(src_width, src_height, image.Format(), image.TileShape().width);

  struct TiledGeometryJob {
    TileIndex    dst_index;
    TileRect     dst_rect;
    GeometryJob  geometry;
  };

  auto pipeline = GetOrCreatePipeline("rotate_180");
  std::vector<TiledGeometryJob> jobs;
  jobs.reserve(output.TileCount());

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  for (uint32_t tile_y = 0; tile_y < output.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < output.TileColumns(); ++tile_x) {
      const TileIndex dst_index{tile_x, tile_y};
      const TileRect  dst_rect = output.TileRegion(dst_index);
      const TileRect  src_rect = Rotate180SourceRect(dst_rect, src_width, src_height);

      WebGpuImage patch;
      patch.Create(src_rect.width, src_rect.height, image.Format());
      image.EncodeCopyRegionTo(encoder, patch, src_rect);
      jobs.push_back({dst_index, dst_rect,
                      CreateGeometryJob(std::move(patch), dst_rect.width, dst_rect.height,
                                        pipeline)});
    }
  }

  {
    std::vector<GeometryJob> geometry_jobs;
    geometry_jobs.reserve(jobs.size());
    for (auto& job : jobs) {
      geometry_jobs.push_back(std::move(job.geometry));
    }
    EncodeGeometryJobs(encoder, pipeline, geometry_jobs);
    for (size_t i = 0; i < jobs.size(); ++i) {
      jobs[i].geometry = std::move(geometry_jobs[i]);
    }
  }
  for (auto& job : jobs) {
    job.geometry.dst.EncodeCopyRegionTo(encoder, output.Tile(job.dst_index), 0, 0,
                                        job.dst_rect.width, job.dst_rect.height);
  }
  SubmitAndWait(encoder.Finish());
  image = std::move(output);
}

void Rotate90CW(WebGpuImage& image) {
  if (image.Empty()) {
    return;
  }
  DispatchGeometry(image, image.Height(), image.Width(), "rotate_90_cw");
}

void Rotate90CW(TiledWebGpuImage& image) {
  if (image.Empty()) {
    return;
  }

  const uint32_t src_width  = image.Width();
  const uint32_t src_height = image.Height();
  TiledWebGpuImage output;
  output.Create(src_height, src_width, image.Format(), image.TileShape().width);

  auto pipeline = GetOrCreatePipeline("rotate_90_cw");
  struct TiledGeometryJob {
    TileIndex   dst_index;
    TileRect    dst_rect;
    GeometryJob geometry;
  };
  std::vector<TiledGeometryJob> jobs;
  jobs.reserve(output.TileCount());

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  for (uint32_t tile_y = 0; tile_y < output.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < output.TileColumns(); ++tile_x) {
      const TileIndex dst_index{tile_x, tile_y};
      const TileRect  dst_rect = output.TileRegion(dst_index);
      const TileRect  src_rect = Rotate90CWSourceRect(dst_rect, src_height);

      WebGpuImage patch;
      patch.Create(src_rect.width, src_rect.height, image.Format());
      image.EncodeCopyRegionTo(encoder, patch, src_rect);
      jobs.push_back({dst_index, dst_rect,
                      CreateGeometryJob(std::move(patch), dst_rect.width, dst_rect.height,
                                        pipeline)});
    }
  }

  {
    std::vector<GeometryJob> geometry_jobs;
    geometry_jobs.reserve(jobs.size());
    for (auto& job : jobs) {
      geometry_jobs.push_back(std::move(job.geometry));
    }
    EncodeGeometryJobs(encoder, pipeline, geometry_jobs);
    for (size_t i = 0; i < jobs.size(); ++i) {
      jobs[i].geometry = std::move(geometry_jobs[i]);
    }
  }
  for (auto& job : jobs) {
    job.geometry.dst.EncodeCopyRegionTo(encoder, output.Tile(job.dst_index), 0, 0,
                                        job.dst_rect.width, job.dst_rect.height);
  }
  SubmitAndWait(encoder.Finish());
  image = std::move(output);
}

void Rotate90CCW(WebGpuImage& image) {
  if (image.Empty()) {
    return;
  }
  DispatchGeometry(image, image.Height(), image.Width(), "rotate_90_ccw");
}

void Rotate90CCW(TiledWebGpuImage& image) {
  if (image.Empty()) {
    return;
  }

  const uint32_t src_width  = image.Width();
  const uint32_t src_height = image.Height();
  TiledWebGpuImage output;
  output.Create(src_height, src_width, image.Format(), image.TileShape().width);

  auto pipeline = GetOrCreatePipeline("rotate_90_ccw");
  struct TiledGeometryJob {
    TileIndex   dst_index;
    TileRect    dst_rect;
    GeometryJob geometry;
  };
  std::vector<TiledGeometryJob> jobs;
  jobs.reserve(output.TileCount());

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  for (uint32_t tile_y = 0; tile_y < output.TileRows(); ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < output.TileColumns(); ++tile_x) {
      const TileIndex dst_index{tile_x, tile_y};
      const TileRect  dst_rect = output.TileRegion(dst_index);
      const TileRect  src_rect = Rotate90CCWSourceRect(dst_rect, src_width);

      WebGpuImage patch;
      patch.Create(src_rect.width, src_rect.height, image.Format());
      image.EncodeCopyRegionTo(encoder, patch, src_rect);
      jobs.push_back({dst_index, dst_rect,
                      CreateGeometryJob(std::move(patch), dst_rect.width, dst_rect.height,
                                        pipeline)});
    }
  }

  {
    std::vector<GeometryJob> geometry_jobs;
    geometry_jobs.reserve(jobs.size());
    for (auto& job : jobs) {
      geometry_jobs.push_back(std::move(job.geometry));
    }
    EncodeGeometryJobs(encoder, pipeline, geometry_jobs);
    for (size_t i = 0; i < jobs.size(); ++i) {
      jobs[i].geometry = std::move(geometry_jobs[i]);
    }
  }
  for (auto& job : jobs) {
    job.geometry.dst.EncodeCopyRegionTo(encoder, output.Tile(job.dst_index), 0, 0,
                                        job.dst_rect.width, job.dst_rect.height);
  }
  SubmitAndWait(encoder.Finish());
  image = std::move(output);
}

}  // namespace utils
}  // namespace webgpu
}  // namespace alcedo

#endif
