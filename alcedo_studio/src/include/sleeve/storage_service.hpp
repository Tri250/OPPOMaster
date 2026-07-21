//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <memory>
#include <mutex>
#include <unordered_map>

#include "sleeve/sleeve_element/sleeve_element.hpp"
#include "sleeve/sleeve_element/sleeve_folder.hpp"
#include "storage/controller/ai/ai_storage_controller.hpp"
#include "storage/controller/db_controller.hpp"
#include "storage/controller/image/image_controller.hpp"
#include "storage/controller/semantic/semantic_storage_controller.hpp"
#include "storage/controller/sleeve/element_controller.hpp"
#include "type/type.hpp"

namespace alcedo {
class CPUPipelineExecutor;
class EditHistory;

class NodeStorageHandler {
 private:
  ElementController&                                                   db_ctrl_;

  std::unordered_map<sl_element_id_t, std::shared_ptr<SleeveElement>>& storage_;

 public:
  NodeStorageHandler(ElementController&                                                   db_ctrl,
                     std::unordered_map<sl_element_id_t, std::shared_ptr<SleeveElement>>& storage);
  void AddToStorage(std::shared_ptr<SleeveElement> new_element);
  void EnsureChildrenLoaded(std::shared_ptr<SleeveFolder> folder);
  auto GetElement(sl_element_id_t id) -> std::shared_ptr<SleeveElement>;
  void GarbageCollect();
};

class StorageService {
 private:
  DBController                                                              db_ctrl_;
  ElementController                                                         el_ctrl_;
  ImageController                                                           img_ctrl_;
  SemanticStorageController                                                 semantic_ctrl_;
  AiStorageController                                                       ai_ctrl_;
  std::mutex                                                                live_state_lock_;

  std::unordered_map<sl_element_id_t, std::weak_ptr<EditHistory>>           live_histories_;
  std::unordered_map<sl_element_id_t, std::shared_ptr<CPUPipelineExecutor>> live_pipelines_;

 public:
  StorageService(std::filesystem::path db_path);

  auto GetDBController() -> DBController&;
  auto GetElementController() -> ElementController&;
  auto GetImageController() -> ImageController&;
  auto GetSemanticStorageController() -> SemanticStorageController&;
  auto GetAiStorageController() -> AiStorageController&;

  void RememberLiveEditHistory(sl_element_id_t                     file_id,
                               const std::shared_ptr<EditHistory>& history);
  auto GetLiveEditHistory(sl_element_id_t file_id) -> std::shared_ptr<EditHistory>;
  void ForgetLiveEditHistory(sl_element_id_t file_id);

  void RememberLivePipeline(sl_element_id_t                             file_id,
                            const std::shared_ptr<CPUPipelineExecutor>& pipeline);
  auto GetLivePipeline(sl_element_id_t file_id) -> std::shared_ptr<CPUPipelineExecutor>;
  void ForgetLivePipeline(sl_element_id_t file_id);
};
};  // namespace alcedo
