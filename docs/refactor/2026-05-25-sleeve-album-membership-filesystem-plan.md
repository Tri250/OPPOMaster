# Sleeve Album Membership Filesystem Refactor Plan

Date: 2026-05-25

## Background

当前 Sleeve 内部文件系统已经具备接近硬链接的底层形态：

- `Element` 存储文件和文件夹身份。
- `FolderContent(folder_id, element_id)` 存储目录到 element 的引用关系。
- `FileImage(file_id, image_id)` 将 `SleeveFile` 绑定到实际图片。
- 编辑历史、pipeline、image pool 等上层服务基本以 `file_id` / `element_id` 为身份。

新的产品语义希望把 Root 文件夹改成“全部图片”，子文件夹改成相册/子集：

- 所有导入图片都属于 Root。
- 子文件夹只保存 Root 中 File 的引用。
- 用户在任意子相册中编辑图片后，Root 下的同一图片也应看到相同结果。
- 用户可以右键将某个文件添加到其他目录。
- 实际 UI 暂时只支持两级结构，但底层仍保留多级文件树能力，方便未来扩展。
- 搜索应优先以 Root 全量图片为基础，子相册检索通过 join membership 条件完成。

现有实现的主要风险是 `SleeveElement::ref_count_` 同时承担了目录引用计数、CoW 共享判断和删除生命周期判断。当前 `PathResolver::ResolveForWrite()` 会在 `ref_count_ > 1` 时触发 CoW。如果直接把“多个相册引用同一个文件”映射到现有 `ref_count_`，用户在子相册编辑图片时会被复制成新的 File，违背“同一图片多处可见并共享修改”的目标语义。

## Decision

不要实现 POSIX 风格的 symbolic link。目标设计应采用 DAM 常见的 collection membership model：

- `File` 是照片的逻辑身份。
- `FolderContent` / `AlbumMembership` 是某个相册是否包含该 File 的关系。
- “添加到其他目录”只是增加 membership，不复制 File，也不创建 path-based symlink。
- CoW 只服务于显式“复制为独立副本”或未来的 payload 共享优化，不服务于相册归属。

对用户而言，这个行为接近硬链接：同一张照片出现在多个相册，编辑任一位置会影响同一 File identity。对实现而言，应避免叫做 symlink，因为 symlink 会引入目标路径失效、重命名解析、broken link、递归解析和 UI id 歧义。

## Target Semantics

Root 语义：

- Root 表示“全部图片”。
- Root 可以实现为虚拟视图：查询所有 `Element.type = FILE` 的 File。
- 如果短期为了兼容现有 `ListFolderContent("/")` 选择 materialized Root membership，则必须维护不变量：每个 live File 都有一条 `FolderContent(root_id, file_id)`。

子文件夹/相册语义：

- 子文件夹表示相册或图片子集。
- 子文件夹中只允许引用 File membership。UI 层第一阶段只暴露两级结构。
- 底层可以继续支持 Folder element，以便未来做多级相册树。

导入语义：

- 导入目标无论是 Root 还是子相册，都先创建一个新的 File identity。
- 新 File 必须进入 Root。
- 如果导入目标是子相册，再额外插入 `FolderContent(target_folder_id, file_id)`。
- 导入失败回滚时，要同时移除 Root membership、目标相册 membership、FileImage、ImagePool 占位对象和相关 history/pipeline。

删除语义：

- 在子相册删除图片：只删除 `FolderContent(album_id, file_id)`，不删除 File 本体。
- 在 Root 删除图片：删除 File 本体，并级联移除所有相册 membership、FileImage、edit history、pipeline、image pool/storage 绑定。
- API 必须区分 `UnlinkFileFromFolder(folder_id, file_id)` 和 `DeleteFileEverywhere(file_id)`，不能只靠 `DeleteElement(file_id)` 表达两种行为。

编辑语义：

- 相册 membership 不触发 CoW。
- 编辑任意相册中的同一 File，修改的是同一个 edit history / pipeline。
- Root 和所有包含该 File 的相册都显示同一编辑结果。

复制语义：

- “添加到相册”不复制。
- “复制为独立副本”才创建新的 File identity。
- 独立副本可以初始共享底层 original image payload，但 edit history / pipeline 必须独立。
- 如果未来实现 payload-level CoW，只有独立副本共享 payload 时才使用 CoW ref count。

## Data Model

第一阶段可以保留现有表名，但要明确 `FolderContent` 是 membership 关系，不是拥有关系：

```sql
FolderContent(
  folder_id INTEGER NOT NULL,
  element_id INTEGER NOT NULL,
  added_time TIMESTAMP,
  sort_key BIGINT,
  PRIMARY KEY(folder_id, element_id)
);

CREATE INDEX idx_folder_content_folder ON FolderContent(folder_id);
CREATE INDEX idx_folder_content_element ON FolderContent(element_id);
```

如果后续需要同一 File 在不同相册里有不同显示名、排序名或局部标注，可以迁移到显式 link row：

```sql
FolderContent(
  link_id INTEGER PRIMARY KEY,
  folder_id INTEGER NOT NULL,
  element_id INTEGER NOT NULL,
  display_name TEXT,
  added_time TIMESTAMP,
  sort_key BIGINT,
  UNIQUE(folder_id, element_id),
  UNIQUE(folder_id, display_name)
);
```

第一阶段不建议引入 `link_id`，除非 UI 已经需要 per-album display name。保持 `(folder_id, element_id)` 主键更简单，也更贴合搜索 join。

## CoW Boundary

当前 `SleeveElement::ref_count_` 的职责需要拆开。目标状态应至少区分：

- membership/link count：由 `FolderContent` 行数表达，或者以单独字段缓存。
- CoW ref count：只用于独立副本共享底层 payload，不用于相册 membership。
- object cache lifetime：由 `NodeStorageHandler` / cache 策略管理，不应混入文件系统语义计数。

推荐长期模型：

```sql
FileContent(
  content_id INTEGER PRIMARY KEY,
  image_id INTEGER NOT NULL,
  ref_count INTEGER NOT NULL
);

FileImage(
  file_id INTEGER PRIMARY KEY,
  content_id INTEGER NOT NULL
);
```

在该模型中：

- `LinkFileToFolder(file_id, folder_id)` 不增加 `FileContent.ref_count`。
- `DuplicateFile(file_id)` 创建新的 `file_id`，初始可以引用同一 `content_id` 并增加 `FileContent.ref_count`。
- 编辑 duplicate 时，如果 `FileContent.ref_count > 1`，才 detach payload。
- 编辑普通 album membership 时，不触发 detach，因为它不是副本。

短期如果不引入 `FileContent` 表，也至少应做到：

- `FolderContent` membership 变化不再让 `ResolveForWrite()` 对 File 触发 CoW。
- `Copy()` / `DuplicateFileToFolder()` 成为显式独立副本 API。
- `SleeveFile::Copy()` 不应共享 mutable edit history pointer；独立副本需要 fork 或 clone history。

## API Plan

新增或重命名 FileSystem / SleeveService 层 API：

```cpp
auto CreateFileInLibrary(file_name_t name) -> std::shared_ptr<SleeveFile>;
void LinkFileToFolder(sl_element_id_t file_id, sl_element_id_t folder_id);
void UnlinkFileFromFolder(sl_element_id_t file_id, sl_element_id_t folder_id);
auto DuplicateFileToFolder(sl_element_id_t file_id, sl_element_id_t folder_id)
    -> std::shared_ptr<SleeveFile>;
void DeleteFileEverywhere(sl_element_id_t file_id);
```

路径 API 保留给 folder tree 和兼容场景，但相册图片操作优先使用 id-based API：

- UI 列表项持有 `file_id` 和当前 `folder_id`。
- 删除按钮根据当前 folder scope 调用 unlink 或 delete-everywhere。
- “添加到相册”通过 `file_id + target_folder_id` 执行。
- 编辑器打开图片时只关心 `file_id`，不关心它来自哪个相册路径。

旧 API 迁移建议：

- `FileSystem::Copy(from, dest)` 改语义或废弃。对于 File，它应调用 `LinkFileToFolder`；对于 Folder，第一阶段可以禁用跨 folder copy，避免递归 membership 语义不清。
- `FileSystem::Delete(path)` 继续可用于 folder 删除；删除 File path 时根据 parent 是否 Root 决定 unlink/delete-everywhere。
- `FileSystem::Delete(element_id)` 在多相册语义下不应用于 UI 普通删除。保留时应明确表示 delete everywhere。

## Search Plan

Root 全部图片搜索：

```sql
SELECT e.id
FROM Element e
JOIN FileImage fi ON fi.file_id = e.id
JOIN Image i ON i.id = fi.image_id
WHERE e.type = 0
  AND <filter>;
```

相册内搜索：

```sql
SELECT e.id
FROM FolderContent fc
JOIN Element e ON e.id = fc.element_id
JOIN FileImage fi ON fi.file_id = e.id
JOIN Image i ON i.id = fi.image_id
WHERE fc.folder_id = ?
  AND e.type = 0
  AND <filter>;
```

实现要求：

- `FilterCombo` 继续生成 filter predicate，但 scope 由查询构造层决定。
- Root scope 不强制依赖 `FolderContent(0, file_id)`，长期优先虚拟 Root 查询。
- 子相册 scope 只多一个 `FolderContent` join 和 `folder_id` 条件。
- 统计、筛选、缩略图分页都复用同一套 scope query builder。

## Lazy Loading And Cache Plan

当前 `NodeStorageHandler::EnsureChildrenLoaded()` 使用 `ContentSize() == 0` 判断是否已加载，这会混淆“空文件夹”和“未加载文件夹”。应改为显式状态：

```cpp
bool children_loaded_ = false;
```

第一阶段：

- `SleeveFolder` 增加 `children_loaded_`。
- DB-backed load 完成后标记 loaded。
- 空文件夹也可以被正确标记为 loaded。

长期：

- Root 不应把所有图片都加载进 `SleeveFolder::contents_`。
- 缩略图网格、搜索和统计应走 DB-first 分页查询。
- `storage_` 只作为 object cache，不作为完整数据库镜像。
- 为 `NodeStorageHandler` 增加容量上限或 LRU 策略，避免大库长期驻留所有 `SleeveElement`。

## Storage Container Plan

不建议把 `storage_` 从 `unordered_map<id, shared_ptr<SleeveElement>>` 重构成可 compact 的 dense array。原因：

- `element_id` 已经是 DB 主键和外键，必须稳定。
- UI、history、pipeline、FileImage、FolderContent 都依赖稳定 id。
- compact/renumber 会带来全库外键重写、缓存失效和项目包迁移风险。

建议：

- 保持 id 单调递增，不复用。
- `storage_` 继续使用 `unordered_map` 作为对象缓存。
- 删除后使用 tombstone / `SyncFlag::DELETED` / DB 删除，再通过 database `VACUUM` 或 project repack 回收空间。
- 如果未来需要数组形式，也只能使用 `vector<optional<...>>` 或 sparse set，不允许改变已分配 id。

## Current Progress

最新基线 commit：

- `06e7c4a3 refact: Complete Phase 1 refactor`

该 commit 已经落地了 Phase 1 的核心基础设施：

- `FileSystem` / `SleeveService` 已新增 id-based membership API：
  - `CreateFileInLibrary`
  - `LinkFileToFolder`
  - `UnlinkFileFromFolder`
  - `DuplicateFileToFolder`
  - `DeleteFileEverywhere`
- `FileSystem::Delete(path)` 已经可以根据 parent scope 区分 Root 删除和子相册 unlink。
- `FileSystem::Copy(from, dest)` 对 File 已经转为 link membership，而不是复制 File identity。
- `SleeveServiceTest` 已覆盖 link、unlink、delete-everywhere、copy/link、duplicate 等基础语义。

本轮 review 后的工作区修复进一步补齐了 Phase 1 缺口：

- `ImportService::ImportToFolder` 已改为先在 library/root 创建 File，再按需 link 到目标相册。
- 导入到不存在目标时会先校验目标 folder，不再先创建 Root 文件后失败。
- metadata/import 失败回滚已改为按 `element_id` 调用 `DeleteFileEverywhere`，避免子相册导入失败后遗留 Root membership。
- `ImportServiceTest` 已补充：
  - 导入不存在目标不会残留 Root 文件。
  - 导入子相册后，Root 和目标相册看到同一 `element_id`。
  - sync 前后 Root/album membership 都保持正确。
- `EditHistoryService::GetEditHistoryByFileId` 在无记录时返回 `nullptr`，避免手工 filesystem 测试文件被加载时构造默认 `EditHistory` 导致崩溃。
- `ElementController::UpdateElement` 仅在文件已有 history 时更新 history，避免对无 history 文件执行无意义 upsert。
- `PathResolver::Tree()` 输出已排序，避免 DB 恢复后 membership 枚举顺序导致测试不稳定。

已验证：

- `SleeveServiceTest.exe`: 12/12 passed
- `ImportServiceTest.exe`: 13/13 passed
- `git diff --check`: passed

当前收口项，也就是新的 Phase 1：

- `FolderContent(folder_id, element_id)` 的唯一约束和索引迁移尚未确认。
- DB 重启恢复后的 Root/album membership 行为还需要测试锁定。
- 同一 File 从任意相册编辑后，Root/其他相册可见同一结果的服务层测试还需要补齐。
- 重复 link 同一 File 到同一相册的幂等或错误语义还需要明确。

后续结构项，进入 Phase 2/3：

- UI 层仍主要通过 folder path 浏览，列表项和操作路径还没有全面迁到 `file_id + folder_id`。
- 搜索、统计、缩略图分页还没有统一 scope query builder。
- `ref_count_`、`ResolveForWrite()` 和 `SleeveFile::Copy()` 的 CoW 边界还没有彻底拆分。
- `NodeStorageHandler::EnsureChildrenLoaded()` 仍存在“空文件夹”和“未加载”状态混淆风险。

## Migration Phases

### Phase 1: Current Closeout And Schema Hardening

目标：把当前已经实现的 membership/import 语义收口成可长期依赖的基础，不再留下“运行时看起来可用，但 DB/schema/test 没锁住”的风险。

范围：

- 收口 `FolderContent` 数据约束。
- 收口导入、link/unlink/delete-everywhere 的 DB 恢复行为。
- 收口编辑共享可见性测试。
- 收口重复 membership 的幂等或错误语义。

详细工作项：

- 检查现有 `FolderContent` schema 是否已经有 `PRIMARY KEY(folder_id, element_id)` 或等价唯一约束。
- 如果没有唯一约束，增加 migration：
  - 先 deduplicate 现有重复 `(folder_id, element_id)`。
  - 再添加唯一约束。
  - 再添加 `folder_id` 和 `element_id` 查询索引。
- 决定 Root 的第一版实现方式：
  - 如果继续 materialized Root membership，启动或 migration 时补齐所有 live File 的 Root membership。
  - 如果改为虚拟 Root 查询，`ListFolderEntries("/")`、搜索和统计必须统一走 Root scope query。
- 补齐 DB 重启恢复测试：
  - 导入到 Root 后关闭/重开 DB，Root 能看到同一 File。
  - 导入到子相册后关闭/重开 DB，Root 和相册都能看到同一 `file_id`。
  - 从子相册 unlink 后关闭/重开 DB，Root 仍能看到 File，相册看不到。
  - 从 Root delete-everywhere 后关闭/重开 DB，所有相册都看不到 File。
- 补齐编辑共享可见性测试：
  - 同一 File link 到两个相册。
  - 从任一相册加载并修改 edit history / pipeline。
  - Root 和另一个相册读取到同一 `file_id` 和同一编辑结果。
- 明确重复 link 行为：
  - 推荐 `LinkFileToFolder(file_id, folder_id)` 幂等。
  - 如果选择抛错，错误类型和调用方处理必须稳定。
  - 对应增加重复添加测试。
- 保留当前 `ImportService` 修复，并扩展失败路径测试：
  - 目标 folder 不存在时不创建 Root 文件。
  - metadata 失败时 `DeleteFileEverywhere(file_id)` 清理 Root、目标相册、FileImage 和 placeholder。

Acceptance criteria:

- DB 层无法持久化重复 `(folder_id, element_id)`。
- 旧项目或旧测试库升级后，Root membership 一致。
- 同一 File 可以稳定出现在 Root 和多个相册，重启后仍成立。
- 子相册删除只 unlink，Root 删除 delete-everywhere。
- 导入失败不会留下孤儿 membership、无 image 绑定 File 或不可见 placeholder。
- 相册 membership 不触发 CoW，并且编辑共享可见性有测试覆盖。

### Phase 2: Album Scope API, UI, Search And Stats

目标：把“照片身份是 `file_id`，相册只是 scope/membership”贯穿到应用服务、UI、搜索、统计和缩略图分页，避免继续用 path 表达照片身份。

范围：

- UI 和 AlbumBrowseService 迁到 id-based scope。
- “添加到相册”和删除操作接入 membership API。
- 搜索、统计、缩略图分页统一使用 scope query builder。
- Root 和 album 的列表/查询语义保持一致。

详细工作项：

- `AlbumBrowseService` 列表项返回：
  - `file_id`
  - 当前 `folder_id` 或 folder path
  - 当前 scope 类型：Root 或 album
  - UI 仍需要的显示名、缩略图 id、metadata 摘要
- UI 打开编辑器时只传 `file_id`，不再依赖 album path 定位照片。
- UI 删除路径改为：
  - 当前 scope 是 Root：调用 `DeleteFileEverywhere(file_id)`。
  - 当前 scope 是 album：调用 `UnlinkFileFromFolder(file_id, folder_id)`。
- 增加“添加到相册”入口：
  - 选择目标 folder。
  - 调用 `LinkFileToFolder(file_id, target_folder_id)`。
  - 对重复添加给出稳定结果。
- 建立 shared scope query builder：
  - Root scope 查询所有 live File，或查询 materialized Root membership。
  - Album scope 通过 `FolderContent.folder_id = ?` join File。
  - filter predicate 仍由 `FilterCombo` 或现有 filter builder 生成。
- 迁移搜索和统计：
  - `BuildFolderStats`
  - `GetElementIdsInFolderByFilter`
  - 缩略图 grid reload / pagination
  - 任何 album count 或 filtered count
- 保留 path API 作为兼容层，但新 UI 和新服务逻辑不再依赖 path 表达图片身份。

Acceptance criteria:

- UI 可以把同一 File 添加到多个相册。
- UI 从相册删除图片不会删除 Root 中的 File。
- UI 从 Root 删除图片会从所有相册消失，并有明确确认路径。
- Root 搜索覆盖全库 live File。
- 子相册搜索只返回当前相册 membership 中的 File。
- 统计、筛选、缩略图分页使用同一 scope 定义，不出现列表和计数不一致。
- 编辑器入口只依赖 `file_id`。

### Phase 3: CoW Boundary, Duplicate Semantics And Cache Cleanup

目标：把当前还混在一起的 membership、duplicate、CoW 和 object cache 生命周期拆清楚，并让大库场景不依赖全量加载。

范围：

- 拆分 `ref_count_` 的职责。
- 明确 duplicate 和 album membership 的差异。
- 修正 `SleeveFile::Copy()` / duplicate 的 edit history 和 pipeline 独立性。
- 清理 lazy loading 和 object cache。

详细工作项：

- 停止用 `SleeveElement::ref_count_` 判断 album membership 是否共享。
- 将 CoW 触发限制到显式 duplicate / payload sharing 场景。
- 审核 `ResolveForWrite()`：
  - album membership 写入不触发 File CoW。
  - duplicate 写入如果共享 payload，才允许 detach。
- 修正 `SleeveFile::Copy()` 或替换为明确 duplicate API：
  - “添加到相册”绝不复制 File identity。
  - “复制为独立副本”创建新 `file_id`。
  - 新副本的 edit history / pipeline 独立。
- 评估是否引入 `FileContent` 表：
  - 如果只需要独立 File，不共享 payload，可以暂不引入。
  - 如果需要 duplicate 初始共享原始 payload，则用 `FileContent.ref_count` 承载 payload-level CoW。
- 为 `SleeveFolder` 增加显式 `children_loaded_`。
- `NodeStorageHandler::EnsureChildrenLoaded()` 不再用 `ContentSize() == 0` 判断是否加载。
- Root 列表、搜索和缩略图分页逐步改为 DB-first 查询。
- `storage_` 退化为有边界的 object cache，而不是完整数据库镜像。

Acceptance criteria:

- 同一 File 的多相册 membership 不会因为写入而复制 File。
- 显式 duplicate 后，两个 File 的 edit history / pipeline 独立。
- 如存在 payload-level sharing，只在 duplicate detach 时触发。
- 空文件夹和未加载文件夹状态可区分。
- 大库 Root 不需要一次性加载所有 File element。
- 重启恢复、搜索、相册切换和缩略图分页行为稳定。

## Compatibility Notes

- 旧项目可以通过启动时 migration 补齐 Root membership，或直接把 Root 列表改成虚拟查询来避免补齐。
- 如果 `FolderContent` 现有数据中存在重复 `(folder_id, element_id)`，迁移前需要 deduplicate。
- 如果当前 `ref_count_` 数据已经因为历史 CoW/Copy 行为不准确，不能直接拿它作为新 link count 的来源。
- `SleeveBase` 旧接口和 `FileSystem` 新接口存在重复实现，重构时应优先让应用层只依赖 `FileSystem` / `SleeveService`，再决定是否删除或降级 `SleeveBase`。

## Open Questions

- Root 是否第一版就改为虚拟视图，还是先保留 materialized Root membership 以降低改动量？
- 是否允许子文件夹里再创建子文件夹，还是只在数据层保留能力、UI 层禁止？
- 同一 File 在不同相册是否需要不同排序、显示名或封面状态？
- 独立副本的 edit history 是 fork 当前 version tree，还是只复制当前 pipeline snapshot？
- Delete from Root 是否应总是强确认，并提示会从所有相册移除？

## Recommended Next PR Scope

当前代码已经越过了原计划的“只加测试”阶段。下一 PR 建议只做 Phase 1，不再混入 UI、搜索、CoW 或懒加载：

- 确认或添加 `FolderContent(folder_id, element_id)` 唯一约束和索引。
- 补齐 DB 重启恢复、编辑共享可见性、重复 link 幂等/失败的测试。
- 明确 Root 第一版采用 materialized membership 还是虚拟查询。
- 如果继续 materialized Root membership，补齐旧项目/旧测试库的 Root membership migration。
- 扩展 ImportService 失败路径测试，确保目标不存在、metadata 失败和 sync 失败都不会留下孤儿 membership。
- 保持 UI、搜索、CoW 和 lazy loading 不动，避免 Phase 1 的验收面扩大。

这样可以先把当前已经实现的 membership/import 语义真正锁住，再进入 Phase 2 的 UI 和查询迁移。
