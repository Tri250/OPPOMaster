//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/mapper/duckorm/duckdb_types.hpp"

#include <duckdb.h>

#include <exception>
#include <iostream>
#include <stdexcept>

#include "utils/diagnostics/app_logging.hpp"

namespace duckorm {
void PreparedStatement::RecycleResources() {
  if (stmt_) {
    duckdb_destroy_prepare(&stmt_);
    stmt_ = nullptr;
  }
  if (result_.deprecated_columns) duckdb_destroy_result(&result_);
}

PreparedStatement::PreparedStatement(duckdb_connection& con) : stmt_(), con_(con) {
  std::memset(&result_, 0, sizeof(result_));
}

PreparedStatement::PreparedStatement(duckdb_connection& con, const std::string& prepare_query)
    : stmt_(), con_(con) {
  std::memset(&result_, 0, sizeof(result_));

  // ALCEDO_DESIGN_NOTE: Unified error handling for prepared statements is needed.
  // The current pattern uses a mix of std::cerr and exceptions.  Once the error-handling
  // strategy is finalized, all DuckDB errors should go through a single path (log + throw
  // or log + return error code) so callers don't have to catch in two places.
  try {
    if (duckdb_prepare(con_, prepare_query.c_str(), &stmt_) != DuckDBSuccess) {
      const char* err = duckdb_prepare_error(stmt_);
      std::string msg = "PreparedStatement failed in GetStmtGuard";
      if (err && std::strlen(err) > 0) {
        msg += ": ";
        msg += err;
      }
      RecycleResources();
      throw std::runtime_error(msg);
    }
  } catch (const std::exception& e) {
    qCCritical(alcedo::diag::appLog, "Exception in PreparedStatement constructor: %s", e.what());
    RecycleResources();
    throw;
  }
  prepared_ = true;
}

PreparedStatement::~PreparedStatement() { RecycleResources(); }

auto PreparedStatement::GetStmtGuard(const std::string& prepare_query)
    -> duckdb_prepared_statement& {
  if (duckdb_prepare(con_, prepare_query.c_str(), &stmt_) != DuckDBSuccess) {
    RecycleResources();
    throw std::runtime_error("PreparedStatement failed when inserting images");
  }
  prepared_ = true;
  return stmt_;
}

void PreparedStatement::SetConnection(duckdb_connection& con) { con_ = con; }
}  // namespace duckorm
