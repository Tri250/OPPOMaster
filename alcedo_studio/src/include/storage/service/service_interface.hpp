//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <duckdb.h>

#include <cstddef>
#include <memory>
#include <span>
#include <utility>
#include <vector>

#include "storage/mapper/mapper_interface.hpp"

namespace alcedo {
template <typename Derived, typename InternalType, typename Mappable, typename Mapper, typename ID>
class ServiceInterface {
 private:
  duckdb_connection&                    conn_;
  MapperInterface<Mapper, Mappable, ID> mapper_;

 public:
  ServiceInterface(duckdb_connection& conn) : conn_(conn), mapper_(conn) {}
  void InsertParams(const Mappable& param) { mapper_.Insert(param); }
  void Insert(const InternalType& obj) { mapper_.Insert(Derived::ToParams(obj)); }
  void InsertBatch(std::span<const InternalType> objects) {
    std::vector<Mappable> params;
    params.reserve(objects.size());
    for (const auto& obj : objects) {
      params.push_back(Derived::ToParams(obj));
    }
    mapper_.InsertBatch(params);
  }
  void InsertParamsBatch(std::span<const Mappable> params) { mapper_.InsertBatch(params); }

  /**
   * @brief Get the objects by a SQL predicate (WHERE clause)
   *
   * @param predicate
   * @return std::vector<InternalType>
   */
  auto GetByPredicate(std::string&& predicate) -> std::vector<InternalType> {
    std::vector<Mappable>     param_results = mapper_.Get(predicate.c_str());
    std::vector<InternalType> results;
    results.resize(param_results.size());
    size_t idx = 0;
    for (auto& param : param_results) {
      results[idx] = Derived::FromParams(std::move(param));
      ++idx;
    }
    return results;
  }

  /**
   * @brief Get the objects by a full SQL query, results may not compatible with Mappable.
   *        Should only be used internally (e.g., filter).
   *
   * @param query
   * @return std::vector<InternalType>
   */
  auto GetByQuery(std::string&& query) -> std::vector<InternalType> {
    std::vector<Mappable>     param_results = mapper_.GetByQuery(query.c_str());
    std::vector<InternalType> results;
    results.resize(param_results.size());
    size_t idx = 0;
    for (auto& param : param_results) {
      results[idx] = Derived::FromParams(std::move(param));
      ++idx;
    }
    return results;
  }

  void RemoveById(const ID remove_id) { mapper_.Remove(remove_id); }
  void RemoveByIds(std::span<const ID> remove_ids) { mapper_.RemoveByIds(remove_ids); }
  void RemoveByClause(const std::string& clause) { mapper_.RemoveByClause(clause); }
  void Update(const InternalType& obj, const ID update_id) {
    mapper_.Update(update_id, Derived::ToParams(obj));
  }
  void UpdateBatch(std::span<const std::pair<ID, InternalType>> updates) {
    std::vector<std::pair<ID, Mappable>> params;
    params.reserve(updates.size());
    for (const auto& [id, obj] : updates) {
      params.emplace_back(id, Derived::ToParams(obj));
    }
    mapper_.UpdateBatch(params);
  }
  void UpdateParamsBatch(std::span<const std::pair<ID, Mappable>> updates) {
    mapper_.UpdateBatch(updates);
  }
};
}  // namespace alcedo
