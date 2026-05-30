//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <duckdb.h>

#include <cstdint>
#include <format>
#include <memory>
#include <sstream>
#include <span>
#include <stdexcept>
#include <string>
#include <unordered_set>
#include <vector>

#include "storage/mapper/duckorm/duckdb_orm.hpp"
#include "storage/mapper/duckorm/duckdb_types.hpp"

namespace alcedo {
template <typename Derived, typename Mappable, typename ID>
class MapperInterface {
 public:
  duckdb_connection& conn_;

  MapperInterface(duckdb_connection& conn) : conn_(conn) {}

  /**
   * @brief Insert a new record into the table
   *
   * @param obj
   */
  void Insert(const Mappable& obj) {
    duckorm::insert(conn_, Derived::TableName(), &obj, Derived::FieldDesc(), Derived::FieldCount());
  }

  void InsertBatch(std::span<const Mappable> objects) {
    if (objects.empty()) {
      return;
    }
    duckorm::begin_transaction(conn_);
    try {
      for (const auto& obj : objects) {
        duckorm::insert(conn_, Derived::TableName(), &obj, Derived::FieldDesc(),
                        Derived::FieldCount());
      }
      duckorm::commit_transaction(conn_);
    } catch (...) {
      duckorm::rollback_transaction(conn_);
      throw;
    }
  }

  /**
   * @brief Remove a record from the table by its primary key
   *
   * @param remove_id
   */
  void Remove(const ID remove_id) {
    std::string remove_clause = std::format(Derived::PrimeKeyClause(), remove_id);
    duckorm::remove(conn_, Derived::TableName(), remove_clause.c_str());
  }

  void RemoveByIds(std::span<const ID> remove_ids) {
    if (remove_ids.empty()) {
      return;
    }

    std::ostringstream            id_list;
    std::unordered_set<uint64_t>  seen;
    const std::string             key_clause = Derived::PrimeKeyClause();
    const auto                    equals_pos = key_clause.find('=');
    if (equals_pos == std::string::npos || equals_pos == 0) {
      throw std::runtime_error("MapperInterface: invalid primary key clause");
    }
    const std::string key_column = key_clause.substr(0, equals_pos);

    bool first = true;
    for (const auto id : remove_ids) {
      const auto normalized_id = static_cast<uint64_t>(id);
      if (!seen.insert(normalized_id).second) {
        continue;
      }
      if (!first) {
        id_list << ",";
      }
      id_list << normalized_id;
      first = false;
    }

    if (first) {
      return;
    }

    duckorm::remove(conn_, Derived::TableName(),
                    std::format("{} IN ({})", key_column, id_list.str()).c_str());
  }

  /**
   * @brief Remove records from the table by a custom SQL predicate
   *
   * @param predicate
   */
  void RemoveByClause(const std::string& predicate) {
    duckorm::remove(conn_, Derived::TableName(), predicate.c_str());
  }

  /**
   * @brief Get records from the table by a custom SQL predicate
   *
   * @param where_clause
   * @return std::vector<Mappable>
   */
  auto Get(const char* where_clause) -> std::vector<Mappable> {
    auto                  raw = duckorm::select(conn_, Derived::TableName(), Derived::FieldDesc(),
                                                Derived::FieldCount(), where_clause);
    std::vector<Mappable> result;
    for (auto& row : raw) {
      result.emplace_back(Derived::FromRawData(std::move(row)));
    }
    return result;
  }

  auto GetByQuery(const char* query) -> std::vector<Mappable> {
    auto raw = duckorm::select_by_query(conn_, Derived::FieldDesc(), Derived::FieldCount(), query);
    std::vector<Mappable> result;
    for (auto& row : raw) {
      result.emplace_back(Derived::FromRawData(std::move(row)));
    }
    return result;
  }

  /**
   * @brief Update a record in the table by its primary key
   *
   * @param target_id
   * @param updated
   */
  void Update(const ID target_id, const Mappable& updated) {
    std::string where_clause = std::format(Derived::PrimeKeyClause(), target_id);
    duckorm::update(conn_, Derived::TableName(), &updated, Derived::FieldDesc(),
                    Derived::FieldCount(), where_clause.c_str());
  }

  void UpdateBatch(std::span<const std::pair<ID, Mappable>> updates) {
    if (updates.empty()) {
      return;
    }
    duckorm::begin_transaction(conn_);
    try {
      for (const auto& [target_id, updated] : updates) {
        std::string where_clause = std::format(Derived::PrimeKeyClause(), target_id);
        duckorm::update(conn_, Derived::TableName(), &updated, Derived::FieldDesc(),
                        Derived::FieldCount(), where_clause.c_str());
      }
      duckorm::commit_transaction(conn_);
    } catch (...) {
      duckorm::rollback_transaction(conn_);
      throw;
    }
  }
};

// Don't understand what heck this is... They call it CRTP (C++ Recurring Tremendous Pain, maybe).
template <typename Derived>
struct FieldReflectable {
 public:
  using FieldArrayType = std::span<const duckorm::DuckFieldDesc>;
  static constexpr FieldArrayType FieldDesc() { return Derived::field_descs_; }
  static constexpr uint32_t       FieldCount() { return Derived::field_count_; }
  static constexpr const char*    TableName() { return Derived::table_name_; }
  static constexpr const char*    PrimeKeyClause() { return Derived::prime_key_clause_; }
};
};  // namespace alcedo
