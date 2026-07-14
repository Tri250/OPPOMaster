//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/mapper/pipeline/pipeline_mapper.hpp"

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {
auto PipelineMapper::FromRawData(std::vector<duckorm::VarTypes>&& data)
    -> PipelineMapperParams {
  if (data.size() != FieldCount()) {
    qCCritical(alcedo::diag::appLog, "PipelineMapper: Invalid DuckFieldDesc for PipelineParam (expected %zu fields, got %zu)",
               FieldCount(), data.size());
    throw std::runtime_error("PipelineMapper: Invalid DuckFieldDesc for PipelineParam");
  }
  auto file_id = std::get_if<sl_element_id_t>(&data[0]);
  auto param_json = std::get_if<std::unique_ptr<std::string>>(&data[1]);

  if (file_id == nullptr || param_json == nullptr) {
    qCCritical(alcedo::diag::appLog, "PipelineMapper: Encountered unmatching types when parsing the data from the DB");
    throw std::runtime_error(
        "PipelineMapper: Encountered unmatching types when parsing the data from the DB");
  }

  return {*file_id, std::move(*param_json)};
}
};