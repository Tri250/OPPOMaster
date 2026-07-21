//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/adjustment_transfer_service.hpp"

#include <algorithm>
#include <array>
#include <mutex>
#include <optional>
#include <stdexcept>
#include <string>
#include <string_view>
#include <utility>

#include "edit/history/edit_transaction.hpp"
#include "edit/history/version.hpp"
#include "edit/pipeline/pipeline_cpu.hpp"

namespace alcedo {
namespace {

constexpr std::string_view kSchema = "alcedo.adjustment_transfer.v1";

constexpr auto             kStages = std::array{
    PipelineStageName::Image_Loading,    PipelineStageName::Geometry_Adjustment,
    PipelineStageName::To_WorkingSpace,  PipelineStageName::Basic_Adjustment,
    PipelineStageName::Color_Adjustment, PipelineStageName::Detail_Adjustment,
    PipelineStageName::Output_Transform,
};

auto StageName(PipelineStageName stage) -> std::string_view {
  switch (stage) {
    case PipelineStageName::Image_Loading:
      return "Image Loading";
    case PipelineStageName::Geometry_Adjustment:
      return "Geometry Adjustment";
    case PipelineStageName::To_WorkingSpace:
      return "To Working Space";
    case PipelineStageName::Basic_Adjustment:
      return "Basic Adjustment";
    case PipelineStageName::Color_Adjustment:
      return "Color Adjustment";
    case PipelineStageName::Detail_Adjustment:
      return "Detail Adjustment";
    case PipelineStageName::Output_Transform:
      return "Output Transform";
    default:
      return "Unknown Stage";
  }
}

auto ParseStageName(const std::string& name) -> PipelineStageName {
  for (PipelineStageName stage : kStages) {
    if (name == StageName(stage)) {
      return stage;
    }
  }
  return PipelineStageName::Stage_Count;
}

auto OperatorScriptName(OperatorType op_type) -> std::string_view {
  switch (op_type) {
    case OperatorType::RAW_DECODE:
      return "raw_decode";
    case OperatorType::LENS_CALIBRATION:
      return "lens_calib";
    case OperatorType::CROP_ROTATE:
      return "crop_rotate";
    case OperatorType::COLOR_TEMP:
      return "color_temp";
    case OperatorType::EXPOSURE:
      return "exposure";
    case OperatorType::CONTRAST:
      return "contrast";
    case OperatorType::WHITE:
      return "white";
    case OperatorType::BLACK:
      return "black";
    case OperatorType::SHADOWS:
      return "shadows";
    case OperatorType::HIGHLIGHTS:
      return "highlights";
    case OperatorType::CURVE:
      return "curve";
    case OperatorType::HLS:
      return "HLS";
    case OperatorType::SATURATION:
      return "saturation";
    case OperatorType::TINT:
      return "tint";
    case OperatorType::VIBRANCE:
      return "vibrance";
    case OperatorType::LMT:
      return "ocio_lmt";
    case OperatorType::COLOR_WHEEL:
      return "color_wheel";
    case OperatorType::CLARITY:
      return "clarity";
    case OperatorType::SHARPEN:
      return "sharpen";
    case OperatorType::ODT:
      return "odt";
    default:
      return "unknown";
  }
}

auto ParseOperatorScriptName(const std::string& name) -> OperatorType {
  for (OperatorType op_type : {OperatorType::RAW_DECODE,  OperatorType::LENS_CALIBRATION,
                               OperatorType::CROP_ROTATE, OperatorType::COLOR_TEMP,
                               OperatorType::EXPOSURE,    OperatorType::CONTRAST,
                               OperatorType::WHITE,       OperatorType::BLACK,
                               OperatorType::SHADOWS,     OperatorType::HIGHLIGHTS,
                               OperatorType::CURVE,       OperatorType::HLS,
                               OperatorType::SATURATION,  OperatorType::TINT,
                               OperatorType::VIBRANCE,    OperatorType::LMT,
                               OperatorType::COLOR_WHEEL, OperatorType::CLARITY,
                               OperatorType::SHARPEN,     OperatorType::ODT}) {
    if (name == OperatorScriptName(op_type)) {
      return op_type;
    }
  }
  return OperatorType::UNKNOWN;
}

auto DefaultStageForOperator(OperatorType op_type) -> PipelineStageName {
  switch (op_type) {
    case OperatorType::RAW_DECODE:
    case OperatorType::LENS_CALIBRATION:
      return PipelineStageName::Image_Loading;
    case OperatorType::CROP_ROTATE:
      return PipelineStageName::Geometry_Adjustment;
    case OperatorType::COLOR_TEMP:
      return PipelineStageName::To_WorkingSpace;
    case OperatorType::EXPOSURE:
    case OperatorType::CONTRAST:
    case OperatorType::WHITE:
    case OperatorType::BLACK:
    case OperatorType::SHADOWS:
    case OperatorType::HIGHLIGHTS:
    case OperatorType::CURVE:
      return PipelineStageName::Basic_Adjustment;
    case OperatorType::HLS:
    case OperatorType::SATURATION:
    case OperatorType::TINT:
    case OperatorType::VIBRANCE:
    case OperatorType::LMT:
    case OperatorType::COLOR_WHEEL:
      return PipelineStageName::Color_Adjustment;
    case OperatorType::CLARITY:
    case OperatorType::SHARPEN:
      return PipelineStageName::Detail_Adjustment;
    case OperatorType::ODT:
      return PipelineStageName::Output_Transform;
    default:
      return PipelineStageName::Stage_Count;
  }
}

auto IsInFilter(OperatorType op_type, const AdjustmentTransferSelection& selection) -> bool {
  return !selection.operator_filter_.has_value() || selection.operator_filter_->contains(op_type);
}

auto IsOperatorSelected(OperatorType op_type, const AdjustmentTransferSelection& selection)
    -> bool {
  if (!IsInFilter(op_type, selection)) {
    return false;
  }

  switch (op_type) {
    case OperatorType::RAW_DECODE:
      return selection.include_image_loading_;
    case OperatorType::LENS_CALIBRATION:
      return selection.include_lens_calibration_;
    case OperatorType::CROP_ROTATE:
      return selection.include_geometry_;
    case OperatorType::COLOR_TEMP:
      return selection.include_color_temperature_;
    case OperatorType::EXPOSURE:
    case OperatorType::CONTRAST:
    case OperatorType::WHITE:
    case OperatorType::BLACK:
    case OperatorType::SHADOWS:
    case OperatorType::HIGHLIGHTS:
    case OperatorType::CURVE:
      return selection.include_tone_;
    case OperatorType::HLS:
    case OperatorType::SATURATION:
    case OperatorType::TINT:
    case OperatorType::VIBRANCE:
    case OperatorType::LMT:
    case OperatorType::COLOR_WHEEL:
      return selection.include_color_;
    case OperatorType::CLARITY:
    case OperatorType::SHARPEN:
      return selection.include_detail_;
    case OperatorType::ODT:
      return selection.include_output_transform_;
    case OperatorType::RESIZE:
    case OperatorType::UNKNOWN:
    default:
      return false;
  }
}

auto SanitizeColorTemperatureParams(nlohmann::json                     params,
                                    const AdjustmentTransferSelection& selection)
    -> nlohmann::json {
  if (selection.include_color_temperature_resolved_values_) {
    return params;
  }
  if (!params.contains("color_temp") || !params["color_temp"].is_object()) {
    return params;
  }

  auto& inner = params["color_temp"];
  inner.erase("resolved_cct");
  inner.erase("resolved_tint");
  if (inner.value("mode", std::string{}) == "as_shot") {
    inner.erase("cct");
    inner.erase("tint");
  }
  return params;
}

auto SanitizeLensCalibrationParams(nlohmann::json                     params,
                                   const AdjustmentTransferSelection& selection) -> nlohmann::json {
  if (selection.include_lens_calibration_runtime_metadata_) {
    return params;
  }
  if (!params.contains("lens_calib") || !params["lens_calib"].is_object()) {
    return params;
  }

  auto& inner = params["lens_calib"];
  for (const char* key : {"cam_maker", "cam_model", "lens_maker", "lens_model", "focal_length_mm",
                          "aperture_f_number", "distance_m", "focal_35mm_mm", "crop_factor_hint"}) {
    inner.erase(key);
  }
  return params;
}

auto SanitizeParams(OperatorType op_type, nlohmann::json params,
                    const AdjustmentTransferSelection& selection) -> nlohmann::json {
  switch (op_type) {
    case OperatorType::COLOR_TEMP:
      return SanitizeColorTemperatureParams(std::move(params), selection);
    case OperatorType::LENS_CALIBRATION:
      return SanitizeLensCalibrationParams(std::move(params), selection);
    default:
      return params;
  }
}

/// Validate that ODT/DRT parameters have the required structure.
/// Ensures the `odt.open_drt` sub-object exists with expected keys.
auto ValidateDRTParams(nlohmann::json& params) -> bool {
  if (!params.is_object()) {
    return false;
  }
  if (!params.contains("odt") || !params["odt"].is_object()) {
    return true;  // No ODT section yet - valid (will be filled by defaults)
  }
  auto& odt = params["odt"];
  if (!odt.contains("open_drt") || !odt["open_drt"].is_object()) {
    return true;  // No open_drt section yet - valid
  }
  auto& drt = odt["open_drt"];
  // Validate required DRT fields have correct types
  const std::vector<std::pair<std::string, nlohmann::json::value_t>> required_fields = {
      {"look_preset",          nlohmann::json::value_t::string},
      {"tonescale_preset",     nlohmann::json::value_t::string},
      {"creative_white",       nlohmann::json::value_t::string},
      {"creative_white_limit", nlohmann::json::value_t::number_float},
      {"display_grey_luminance", nlohmann::json::value_t::number_float},
      {"hdr_grey_boost",      nlohmann::json::value_t::number_float},
      {"hdr_purity",          nlohmann::json::value_t::number_float},
  };
  bool valid = true;
  for (const auto& [key, expected_type] : required_fields) {
    if (drt.contains(key) && drt[key].type() != expected_type &&
        drt[key].type() != nlohmann::json::value_t::number_integer &&
        drt[key].type() != nlohmann::json::value_t::number_unsigned) {
      valid = false;
      break;
    }
  }
  return valid;
}

/// Preserve DRT sub-parameters when merging ODT adjustments.
/// When overwrite/merge happens, the `open_drt` nested settings must be
/// carried forward unless explicitly replaced.
auto PreserveDRTParams(nlohmann::json& target_params,
                       const nlohmann::json& source_params) -> void {
  if (!target_params.is_object() || !source_params.is_object()) {
    return;
  }
  if (!target_params.contains("odt") || !target_params["odt"].is_object()) {
    return;
  }
  if (!source_params.contains("odt") || !source_params["odt"].is_object()) {
    return;
  }

  auto& target_odt = target_params["odt"];
  const auto& source_odt = source_params["odt"];

  // If source has open_drt but target doesn't, copy it over
  if (source_odt.contains("open_drt") && source_odt["open_drt"].is_object()) {
    if (!target_odt.contains("open_drt") || !target_odt["open_drt"].is_object()) {
      target_odt["open_drt"] = source_odt["open_drt"];
    } else {
      // Merge: carry forward any open_drt keys that the target doesn't have
      const auto& source_drt = source_odt["open_drt"];
      auto& target_drt = target_odt["open_drt"];
      for (const auto& [key, value] : source_drt.items()) {
        if (!target_drt.contains(key)) {
          target_drt[key] = value;
        }
      }
      // Also preserve the detailed parameters sub-object if present
      if (source_drt.contains("parameters") && source_drt["parameters"].is_object()) {
        if (!target_drt.contains("parameters") || !target_drt["parameters"].is_object()) {
          target_drt["parameters"] = source_drt["parameters"];
        } else {
          const auto& src_params = source_drt["parameters"];
          auto& tgt_params = target_drt["parameters"];
          for (const auto& [key, value] : src_params.items()) {
            if (!tgt_params.contains(key)) {
              tgt_params[key] = value;
            }
          }
        }
      }
    }
  }
}

auto RebuildExecutionStagesIfPossible(PipelineExecutor& pipeline) -> void {
  auto* cpu_pipeline = dynamic_cast<CPUPipelineExecutor*>(&pipeline);
  if (cpu_pipeline == nullptr) {
    return;
  }
  cpu_pipeline->SetExecutionStages();
}

void MergeJsonObject(nlohmann::json& target, const nlohmann::json& patch) {
  if (!target.is_object() || !patch.is_object()) {
    target = patch;
    return;
  }
  for (const auto& [key, value] : patch.items()) {
    if (target.contains(key) && target[key].is_object() && value.is_object()) {
      MergeJsonObject(target[key], value);
    } else {
      target[key] = value;
    }
  }
}

struct TransferEdit {
  EditTransaction transaction_;
};

auto BuildTransferEdit(PipelineExecutor& target, const AdjustmentTransferEntry& op)
    -> std::optional<TransferEdit> {
  if (op.stage_ == PipelineStageName::Stage_Count || op.operator_type_ == OperatorType::UNKNOWN ||
      op.operator_type_ == OperatorType::RESIZE) {
    return std::nullopt;
  }

  auto&          stage            = target.GetStage(op.stage_);
  nlohmann::json effective_params = op.params_;
  nlohmann::json before_params    = nlohmann::json(nullptr);
  bool           before_enabled   = false;
  bool           entry_changed    = true;
  const auto     current          = stage.GetOperator(op.operator_type_);
  const bool     has_current =
      current.has_value() && current.value() != nullptr && current.value()->op_ != nullptr;

  if (has_current) {
    before_params  = current.value()->op_->GetParams();
    before_enabled = current.value()->enable_;
    if (op.merge_params_) {
      effective_params = before_params;
      MergeJsonObject(effective_params, op.params_);
    }
    // P2-14: Preserve DRT sub-parameters when overwriting ODT adjustments.
    // The merge above may have overwritten the nested open_drt sub-object
    // with incomplete data. Restore missing DRT fields from the before state.
    if (op.operator_type_ == OperatorType::ODT) {
      PreserveDRTParams(effective_params, before_params);
      ValidateDRTParams(effective_params);
    }
    entry_changed = before_enabled != op.enabled_ || before_params != effective_params;
  }

  if (!entry_changed) {
    return std::nullopt;
  }

  const auto tx_type = has_current ? TransactionType::_EDIT : TransactionType::_ADD;
  return TransferEdit{
      .transaction_ = EditTransaction{tx_type, op.operator_type_, op.stage_, before_params,
                                      effective_params, before_enabled, op.enabled_},
  };
}

auto ApplyAsTransactions(PipelineExecutor& target, WorkingVersion& working_version,
                         const AdjustmentTransferPackage& package) -> bool {
  bool changed = false;
  for (const auto& op : package.operators_) {
    auto edit = BuildTransferEdit(target, op);
    if (!edit.has_value()) {
      continue;
    }
    if (!edit->transaction_.ApplyForward(target)) {
      continue;
    }
    working_version.AppendEditTransaction(std::move(edit->transaction_));
    changed = true;
  }
  if (changed) {
    working_version.SetHeadPipelineParams(target.ExportPipelineParams());
    RebuildExecutionStagesIfPossible(target);
  }
  return changed;
}

auto UniqueVersionDisplayName(const EditHistory& history, const std::string& requested_name,
                              std::string_view fallback_name) -> std::string {
  const std::string base_name =
      requested_name.empty() ? std::string{fallback_name} : requested_name;
  bool              base_exists = false;
  int               max_suffix  = 1;

  const std::string prefix      = base_name + " (";
  for (const auto& node : history.GetVersions()) {
    const std::string& existing_name = node.ver_ref_.GetDisplayName();
    if (existing_name == base_name) {
      base_exists = true;
      continue;
    }
    if (existing_name.size() <= prefix.size() + 1 || existing_name.back() != ')' ||
        existing_name.rfind(prefix, 0) != 0) {
      continue;
    }

    const std::string suffix =
        existing_name.substr(prefix.size(), existing_name.size() - prefix.size() - 1);
    try {
      const int parsed = std::stoi(suffix);
      if (parsed > 1) {
        max_suffix  = std::max(max_suffix, parsed);
        base_exists = true;
      }
    } catch (...) {
    }
  }

  if (!base_exists) {
    return base_name;
  }
  return base_name + " (" + std::to_string(max_suffix + 1) + ")";
}

}  // namespace

auto AdjustmentTransferService::Capture(PipelineExecutor&                  source,
                                        const AdjustmentTransferSelection& selection)
    -> AdjustmentTransferPackage {
  AdjustmentTransferPackage package;

  for (PipelineStageName stage_name : kStages) {
    auto& stage = source.GetStage(stage_name);
    for (const auto& [op_type, op_entry] : stage.GetAllOperators()) {
      if (!op_entry.op_ || !IsOperatorSelected(op_type, selection)) {
        continue;
      }

      package.operators_.push_back({
          .stage_         = stage_name,
          .operator_type_ = op_type,
          .enabled_       = op_entry.enable_,
          .merge_params_  = false,
          .params_        = SanitizeParams(op_type, op_entry.op_->GetParams(), selection),
      });
    }
  }

  return package;
}

auto AdjustmentTransferService::ImportPackage(const nlohmann::json& package_json)
    -> AdjustmentTransferPackage {
  if (!package_json.is_object()) {
    throw std::runtime_error("AdjustmentTransferService: package must be a JSON object.");
  }

  const std::string schema = package_json.value("schema", std::string{kSchema});
  if (schema != kSchema) {
    throw std::runtime_error("AdjustmentTransferService: unsupported adjustment package schema.");
  }
  if (!package_json.contains("operators") || !package_json["operators"].is_array()) {
    throw std::runtime_error("AdjustmentTransferService: package operators must be an array.");
  }

  AdjustmentTransferPackage package;
  package.schema_ = schema;

  for (const auto& entry_json : package_json["operators"]) {
    if (!entry_json.is_object()) {
      throw std::runtime_error("AdjustmentTransferService: operator entry must be an object.");
    }

    const std::string  operator_name = entry_json.value("operator", std::string{});
    const OperatorType op_type       = ParseOperatorScriptName(operator_name);
    if (op_type == OperatorType::UNKNOWN || op_type == OperatorType::RESIZE) {
      throw std::runtime_error("AdjustmentTransferService: unknown or unsupported operator: " +
                               operator_name);
    }

    PipelineStageName stage = DefaultStageForOperator(op_type);
    if (entry_json.contains("stage")) {
      stage = ParseStageName(entry_json.value("stage", std::string{}));
    }
    if (stage == PipelineStageName::Stage_Count) {
      throw std::runtime_error("AdjustmentTransferService: unknown stage for operator: " +
                               operator_name);
    }
    if (!entry_json.contains("params")) {
      throw std::runtime_error("AdjustmentTransferService: operator is missing params: " +
                               operator_name);
    }

    package.operators_.push_back({
        .stage_         = stage,
        .operator_type_ = op_type,
        .enabled_       = entry_json.value("enabled", true),
        .merge_params_  = entry_json.value("mergeParams", false),
        .params_        = entry_json.at("params"),
    });
  }

  return package;
}

auto AdjustmentTransferService::ExportPackage(const AdjustmentTransferPackage& package)
    -> nlohmann::json {
  nlohmann::json operators = nlohmann::json::array();
  for (const auto& entry : package.operators_) {
    if (entry.operator_type_ == OperatorType::UNKNOWN ||
        entry.operator_type_ == OperatorType::RESIZE) {
      continue;
    }
    operators.push_back({
        {"stage", std::string(StageName(entry.stage_))},
        {"operator", std::string(OperatorScriptName(entry.operator_type_))},
        {"enabled", entry.enabled_},
        {"mergeParams", entry.merge_params_},
        {"params", entry.params_},
    });
  }

  return {
      {"schema", package.schema_.empty() ? std::string{kSchema} : package.schema_},
      {"operators", std::move(operators)},
  };
}

auto AdjustmentTransferService::Apply(PipelineExecutor&                target,
                                      const AdjustmentTransferPackage& package) -> bool {
  bool changed = false;

  for (const auto& op : package.operators_) {
    if (op.stage_ == PipelineStageName::Stage_Count || op.operator_type_ == OperatorType::UNKNOWN ||
        op.operator_type_ == OperatorType::RESIZE) {
      continue;
    }

    auto&          stage            = target.GetStage(op.stage_);
    bool           entry_changed    = true;
    nlohmann::json effective_params = op.params_;
    auto           current          = stage.GetOperator(op.operator_type_);
    if (current.has_value() && current.value() != nullptr && current.value()->op_) {
      if (op.merge_params_) {
        effective_params = current.value()->op_->GetParams();
        MergeJsonObject(effective_params, op.params_);
      }
      // P2-14: Preserve DRT sub-parameters when overwriting ODT adjustments.
      if (op.operator_type_ == OperatorType::ODT) {
        const nlohmann::json before_params = current.value()->op_->GetParams();
        PreserveDRTParams(effective_params, before_params);
        ValidateDRTParams(effective_params);
      }
      entry_changed = current.value()->enable_ != op.enabled_ ||
                      current.value()->op_->GetParams() != effective_params;
    }

    if (!entry_changed) {
      continue;
    }

    stage.SetOperator(op.operator_type_, effective_params, target.GetGlobalParams());
    stage.EnableOperator(op.operator_type_, op.enabled_, target.GetGlobalParams());
    changed = true;
  }

  if (changed) {
    RebuildExecutionStagesIfPossible(target);
  }
  return changed;
}

auto AdjustmentTransferService::Apply(PipelineMgmtService&             pipeline_service,
                                      std::span<const sl_element_id_t> target_ids,
                                      const AdjustmentTransferPackage& package)
    -> AdjustmentApplyResult {
  AdjustmentApplyResult result;
  if (package.Empty()) {
    result.unchanged_ids_.assign(target_ids.begin(), target_ids.end());
    return result;
  }

  for (sl_element_id_t target_id : target_ids) {
    if (target_id == 0) {
      continue;
    }

    std::shared_ptr<PipelineGuard> guard;
    try {
      guard = pipeline_service.LoadPipeline(target_id);
      if (!guard || !guard->pipeline_) {
        result.failures_.push_back({target_id, "Pipeline was not available."});
        continue;
      }

      bool changed = false;
      {
        auto render_guard = guard->pipeline_->TryAcquireRenderLock(std::chrono::milliseconds(10000));
        if (!render_guard.owns_lock()) {
          result.failures_.push_back({target_id, "Pipeline is busy, could not acquire lock."});
          continue;
        }
        changed = Apply(*guard->pipeline_, package);
      }

      if (changed) {
        guard->dirty_ = true;
        result.applied_ids_.push_back(target_id);
      } else {
        result.unchanged_ids_.push_back(target_id);
      }
      pipeline_service.SavePipeline(guard);
    } catch (const std::exception& e) {
      if (guard) {
        pipeline_service.SavePipeline(guard);
      }
      result.failures_.push_back({target_id, e.what()});
    } catch (...) {
      if (guard) {
        pipeline_service.SavePipeline(guard);
      }
      result.failures_.push_back({target_id, "Unknown adjustment apply failure."});
    }
  }

  pipeline_service.Sync();
  return result;
}

auto AdjustmentTransferService::Apply(PipelineMgmtService&             pipeline_service,
                                      EditHistoryMgmtService&          history_service,
                                      std::span<const sl_element_id_t> target_ids,
                                      const AdjustmentTransferPackage& package,
                                      std::string                      version_display_name,
                                      AdjustmentVersionApplyMode mode) -> AdjustmentApplyResult {
  AdjustmentApplyResult result;
  if (package.Empty()) {
    result.unchanged_ids_.assign(target_ids.begin(), target_ids.end());
    return result;
  }

  for (sl_element_id_t target_id : target_ids) {
    if (target_id == 0) {
      continue;
    }

    std::shared_ptr<PipelineGuard>    pipeline_guard;
    std::shared_ptr<EditHistoryGuard> history_guard;
    try {
      pipeline_guard = pipeline_service.LoadPipeline(target_id);
      history_guard  = history_service.LoadHistory(target_id);
      if (!pipeline_guard || !pipeline_guard->pipeline_) {
        result.failures_.push_back({target_id, "Pipeline was not available."});
        continue;
      }
      if (!history_guard || !history_guard->history_) {
        result.failures_.push_back({target_id, "Edit history was not available."});
        pipeline_service.SavePipeline(pipeline_guard);
        continue;
      }

      bool changed = false;
      {
        auto render_guard = pipeline_guard->pipeline_->TryAcquireRenderLock(
            std::chrono::milliseconds(10000));
        if (!render_guard.owns_lock()) {
          result.failures_.push_back({target_id, "Pipeline is busy, could not acquire lock."});
          pipeline_service.SavePipeline(pipeline_guard);
          history_service.SaveHistory(history_guard);
          continue;
        }
        const auto        base_params  = pipeline_guard->pipeline_->ExportPipelineParams();
        const std::string display_name = UniqueVersionDisplayName(
            *history_guard->history_, version_display_name,
            mode == AdjustmentVersionApplyMode::kMerge ? "Merged Adjustments"
                                                       : "Pasted Adjustments");

        if (mode == AdjustmentVersionApplyMode::kMerge) {
          changed = Apply(*pipeline_guard->pipeline_, package);
          if (changed) {
            Version merged_version = Version::Empty(
                target_id, display_name, pipeline_guard->pipeline_->ExportPipelineParams());
            history_service.CommitVersion(history_guard, std::move(merged_version));
          }
        } else {
          Version        pasted_version = Version::Empty(target_id, display_name, base_params);
          WorkingVersion working_version{target_id, pasted_version.GetVersionID(), base_params};

          changed = ApplyAsTransactions(*pipeline_guard->pipeline_, working_version, package);
          if (changed) {
            pasted_version.UpdateFromWorkingVersion(
                working_version, pipeline_guard->pipeline_->ExportPipelineParams());
            history_service.CommitVersion(history_guard, std::move(pasted_version));
          }
        }
      }

      if (changed) {
        pipeline_guard->dirty_ = true;
        result.applied_ids_.push_back(target_id);
      } else {
        result.unchanged_ids_.push_back(target_id);
      }
      history_service.SaveHistory(history_guard);
      pipeline_service.SavePipeline(pipeline_guard);
    } catch (const std::exception& e) {
      if (history_guard) {
        history_service.SaveHistory(history_guard);
      }
      if (pipeline_guard) {
        pipeline_service.SavePipeline(pipeline_guard);
      }
      result.failures_.push_back({target_id, e.what()});
    } catch (...) {
      if (history_guard) {
        history_service.SaveHistory(history_guard);
      }
      if (pipeline_guard) {
        pipeline_service.SavePipeline(pipeline_guard);
      }
      result.failures_.push_back({target_id, "Unknown versioned adjustment apply failure."});
    }
  }

  history_service.Sync();
  pipeline_service.Sync();
  return result;
}

}  // namespace alcedo
