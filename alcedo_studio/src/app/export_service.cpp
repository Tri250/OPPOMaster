//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/export_service.hpp"

#include <atomic>
#include <filesystem>
#include <memory>
#include <mutex>
#include <optional>

#include "image/image.hpp"
#include "image/image_buffer.hpp"
#include "io/image/image_loader.hpp"
#include "io/image/image_writer.hpp"
#include "sleeve/sleeve_filesystem.hpp"
#include "type/type.hpp"

namespace alcedo {
namespace {

auto ResolveExportColorProfileConfig(const OperatorParams& params) -> ExportColorProfileConfig {
  return ExportColorProfileConfig{params.to_output_params_.encoding_space_,
                                  params.to_output_params_.eotf_,
                                  params.to_output_params_.peak_luminance_};
}

auto HasExportMetadata(const ExifDisplayMetaData& metadata) -> bool {
  return !metadata.make_.empty() || !metadata.model_.empty() || !metadata.lens_.empty() ||
         !metadata.lens_make_.empty() || !metadata.date_time_str_.empty() ||
         metadata.aperture_ > 0.0f || metadata.focal_ > 0.0f || metadata.focal_35mm_ > 0.0f ||
         metadata.focus_distance_m_ > 0.0f || metadata.iso_ > 0 ||
         (metadata.shutter_speed_.first > 0 && metadata.shutter_speed_.second > 0) ||
         ExifDisplayMetaData::NormalizeRating(metadata.rating_) > 0;
}

auto ResolveImageExportMetadata(const std::shared_ptr<Image>& image)
    -> std::optional<ExifDisplayMetaData> {
  if (!image) {
    return std::nullopt;
  }
  ExifDisplayMetaData metadata;
  if (image->has_exif_display_.load()) {
    metadata = image->exif_display_;
  } else if (image->has_exif_json_.load()) {
    metadata.FromJson(image->exif_json_);
  } else {
    return std::nullopt;
  }
  metadata.rating_ = ExifDisplayMetaData::NormalizeRating(metadata.rating_);
  return HasExportMetadata(metadata) ? std::optional<ExifDisplayMetaData>(std::move(metadata))
                                     : std::nullopt;
}

}  // namespace

auto ExportService::RunExportRenderTask(const ExportTask& task) -> ExportResult {
  ExportResult result;

  // Get the pipeline executor from sleeve service
  auto pipeline_guard = pipeline_service_->LoadPipeline(task.sleeve_id_);
  if (!pipeline_guard || !pipeline_guard->pipeline_) {
    throw std::runtime_error("[ERROR] ExportService: Failed to load pipeline for sleeve id " +
                             std::to_string(task.sleeve_id_));
  }
  // Get the image from image pool service
  auto source_img = image_pool_service_->Read<std::shared_ptr<Image>>(
      task.image_id_, [](const std::shared_ptr<Image>& img) { return img; });
  if (!source_img) {
    throw std::runtime_error("[ERROR] ExportService: Failed to load image for id " +
                             std::to_string(task.image_id_));
  }
  auto img_src_path = source_img->image_path_;
  const auto export_metadata = ResolveImageExportMetadata(source_img);

  // Create a pipeline task for export
  PipelineTask render_task;
  // To avoid reading too many images into memory at once, we let the pipeline load the image
  // So we create a dummy Image object with only the path set
  render_task.input_desc_ =
      std::make_shared<Image>(img_src_path, ImageType::DEFAULT);
  render_task.pipeline_executor_                 = pipeline_guard->pipeline_;
  render_task.options_.is_blocking_              = true;
  render_task.options_.is_callback_              = false;

  // Inject pre-extracted raw metadata from the real Image into the pipeline
  // so downstream operators resolve eagerly.
  try {
    if (source_img->HasRawColorContext()) {
      pipeline_guard->pipeline_->InjectRawMetadata(source_img->GetRawColorContext());
    }
  } catch (...) {
    // Non-fatal: metadata injection is best-effort.
  }

  // Use full res export, even though the task requires resizing,
  // to benefit from the super sampling
  render_task.options_.render_desc_.render_type_ = RenderType::FULL_RES_EXPORT;
  // Set export options in the pipeline executor
  auto render_promise = std::make_shared<std::promise<std::shared_ptr<ImageBuffer>>>();
  render_task.result_ = render_promise;
  auto render_future  = render_promise->get_future();
  // Schedule the render task
  pipeline_scheduler_->ScheduleTask(std::move(render_task));

  std::shared_ptr<ImageBuffer> rendered_image;
  try {
    // Wait for the render to complete
    rendered_image = render_future.get();
  } catch (...) {
    pipeline_service_->SavePipeline(pipeline_guard);
    throw;
  }

  // Save pipeline back to storage
  const auto export_profile =
      ResolveExportColorProfileConfig(pipeline_guard->pipeline_->GetGlobalParams());
  const bool wrote_ultra_hdr = ImageWriter::ShouldWriteUltraHdr(task.options_, export_profile);
  pipeline_service_->SavePipeline(pipeline_guard);
  // Use ImageWriter to write the image to disk
  ImageWriter::WriteImageToPath(img_src_path, rendered_image, task.options_, export_profile,
                                export_metadata);
  result.success_ = true;
  result.wrote_ultra_hdr_ = wrote_ultra_hdr;
  result.used_embedded_profile_fallback_ = false;
  return result;
}

void ExportService::ExportAll(
    std::function<void(std::shared_ptr<std::vector<ExportResult>>)> callback) {
  ExportAll({}, std::move(callback));
}

void ExportService::ExportAll(
    std::function<void(const ExportProgress&)>                    progress_callback,
    std::function<void(std::shared_ptr<std::vector<ExportResult>>)> callback) {
  auto results = std::make_shared<std::vector<ExportResult>>();
  std::vector<ExportTask> tasks;

  {
    std::lock_guard<std::mutex> lock(queue_mutex_);
    tasks.reserve(export_queue_.size());
    while (!export_queue_.empty()) {
      tasks.push_back(export_queue_.front());
      export_queue_.pop_front();
    }
  }

  const size_t queue_size = tasks.size();
  if (queue_size == 0) {
    try {
      callback(results);
    } catch (...) {
    }
    return;
  }

  auto completed = std::make_shared<std::atomic_size_t>(0);
  auto succeeded = std::make_shared<std::atomic_size_t>(0);
  auto failed    = std::make_shared<std::atomic_size_t>(0);
  for (const auto& task : tasks) {
    // Export in thread pool
    export_thread_pool_.Submit([this, task, results, progress_callback, callback, completed,
                                succeeded, failed, queue_size]() {
      if (progress_callback) {
        try {
          progress_callback(ExportProgress{
              .total_        = queue_size,
              .completed_    = completed->load(std::memory_order_acquire),
              .succeeded_    = succeeded->load(std::memory_order_acquire),
              .failed_       = failed->load(std::memory_order_acquire),
              .sleeve_id_    = task.sleeve_id_,
              .image_id_     = task.image_id_,
              .task_started_ = true,
          });
        } catch (...) {
        }
      }

      ExportResult result;
      // Do export, this call will block until done
      try {
        result = RunExportRenderTask(task);
      } catch (const std::exception& e) {
        result.success_ = false;
        result.message_ = e.what();
      } catch (...) {
        result.success_ = false;
        result.message_ = "Unknown export error";
      }

      const bool export_ok = result.success_;

      // Store result
      {
        std::lock_guard<std::mutex> res_lock(result_mutex_);
        results->push_back(std::move(result));
      }

      if (export_ok) {
        succeeded->fetch_add(1, std::memory_order_acq_rel);
      } else {
        failed->fetch_add(1, std::memory_order_acq_rel);
      }

      // If all done, call the callback
      const size_t finished = completed->fetch_add(1, std::memory_order_acq_rel) + 1;
      if (progress_callback) {
        try {
          progress_callback(ExportProgress{
              .total_         = queue_size,
              .completed_     = finished,
              .succeeded_     = succeeded->load(std::memory_order_acquire),
              .failed_        = failed->load(std::memory_order_acquire),
              .sleeve_id_     = task.sleeve_id_,
              .image_id_      = task.image_id_,
              .task_finished_ = true,
              .task_success_  = export_ok,
          });
        } catch (...) {
        }
      }
      if (finished == queue_size) {
        try {
          callback(results);
        } catch (...) {
        }
      }
    });
  }
};
};  // namespace alcedo
