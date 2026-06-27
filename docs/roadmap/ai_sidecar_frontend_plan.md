# AI Sidecar Frontend Plan

Date: 2026-06-27

Status: planning. This document is the frontend companion to
`docs/roadmap/ai_sidecar_backend_plan.md`. Backend phases 1-7 are treated as
mostly complete for the remote image-analysis path; this plan covers the QML
product surface that still needs to be built.

## Scope

The frontend work has three user-visible areas:

1. Advanced content-analysis provider settings: protocol/preset switching, API
   key input, connection test, model refresh, model selection, and output
   language.
2. Advanced content-analysis execution: a selected-images launcher and a modal
   progress/control dialog for description, rating, and rating-reason generation.
3. Right-side inspection: split the inspector into Album and Image pages, then
   turn the current `ImageDetailsDialog.qml` information into a persistent Image
   inspector page with editable AI/manual text fields.

The UI must clearly distinguish local CLIP-based content recognition from remote
paid multimodal analysis.

## Product Decisions From Grill Session

- Remote analysis operates on selected images only. Empty selection is an error;
  it must never fall back to the current album or folder.
- Provider settings are a separate settings page, not mixed into local CLIP
  model download/settings.
- Settings navigation names:
  - `Local Content Recognition` / `本地内容识别`
  - `Advanced Content Analysis` / `高级内容分析`
- Provider settings use two primary ComboBoxes:
  - `Protocol`
  - `Preset`
- Preset selection backfills the advanced fields. Advanced fields remain
  editable under a collapsed advanced section; manual edits should mark the
  preset as custom/modified.
- API keys are saved by default to the OS credential store. The first frontend
  slice does not need a temporary "use only this session" mode.
- Output language is an explicit setting, but defaults to the current
  application language. Users may override it for AI-generated content.
- Analysis task selection is one control panel with checkboxes and a single
  primary action, not separate top-level buttons:
  - `Description`
  - `Rating`
  - `Rating reason`
- Default overwrite options are all enabled:
  - overwrite photo rating
  - overwrite rating reason
  - overwrite image description
- The analysis launcher lives on the left side of the album page, below the
  existing search button/long search action. Its icon should be a flask SVG to
  signal experimentation/innovation.
- Clicking the launcher opens an analysis Dialog. The Dialog shows current
  provider, model, selected task options, selected image count, a progress ring,
  and control buttons.
- The Dialog cannot be closed while analysis is running. Users must cancel or
  wait for completion/failure before closing.
- When analysis completes, the app does not automatically switch the right
  inspector to the Image page. The Dialog should include a hint that results can
  be reviewed and edited in the Image inspector.
- The right inspector uses a vertical navbar, similar in spirit to the editor
  dialog's side navigation. Initial pages:
  - Album
  - Image
- The Image inspector shows only the current focused image. It does not show a
  multi-selection status line.
- Description and rating reason are edited inline inside the Image inspector.

## Visual Direction

The Image inspector should adapt the vehicle-dashboard reference image into an
Alcedo-native dark utility panel:

- Use instrument-like metric tiles: small labels, large primary values,
  restrained secondary values, and clear grouping.
- Do not copy the reference's light grey automotive palette. Use the current
  Alcedo theme tokens from `app_theme.cpp`:
  - `appTheme.bgPanelColor` for the inspector shell (`#1A1A1A` in the default theme)
  - `appTheme.bgBaseColor` for tiles (`#242424`)
  - `appTheme.bgCanvasColor` for the surrounding workspace (`#121212`)
  - `appTheme.bgDeepColor` for elevated modal/dialog surfaces (`#2E2E2E`)
  - `appTheme.accentColor` / `appTheme.accentSecondaryColor` for active states
    (`#6892B9` / `#76A0C7`)
  - `appTheme.textColor` / `appTheme.textMutedColor` for hierarchy
  - `appTheme.dividerColor` / `appTheme.glassStrokeColor` for quiet separators
- The result should feel like a precise dashboard, not a decorative card wall:
  compact, readable, and comfortable for repeated photo review.

## Settings UX

Create a standalone QML panel, recommended name:

- `alcedo_studio/src/ui/alcedo_main/qml/AiProviderSettingsPanel.qml`

Wire it into `SettingDialog.qml` as its own page named `Advanced Content
Analysis`. Rename the current AI settings page to `Local Content Recognition`
and keep `SemanticGenerationSettingsPanel.qml` focused on local CLIP/SigLIP
label generation and local model download/activation.

The Advanced Content Analysis settings page should contain:

- Protocol ComboBox:
  - OpenAI-compatible chat
  - Anthropic-compatible messages
  - Volcengine Ark / compatible response path when supported
- Preset ComboBox:
  - Built-in defaults, including Opencode-compatible defaults from the backend
    plan.
  - `Custom` / `Modified` state when advanced fields no longer match a built-in
    preset.
- API key section:
  - password TextField
  - `Save Key`
  - `Delete Key`
  - masked saved-key label
  - short security copy: saved in the system credential store
- Connection/model section:
  - `Test & Refresh Models`
  - status line with success/failure
  - model ComboBox populated from the refreshed candidates
  - editable model field only if the selected protocol/preset can safely commit
    the selected model into the sidecar provider config before analysis
- Output language ComboBox:
  - `Follow app language` as the default behavior, resolving to `English` or
    `中文`
  - explicit `English`
  - explicit `中文`
- Advanced collapsible section:
  - provider id
  - display name
  - protocol family
  - base URL
  - endpoint
  - auth type
  - credential slot
  - structured output mode
  - timeout
  - max image bytes
  - recommended rendition

The page must not write raw API keys to QSettings. QSettings may store only
non-secret preset data and masked labels.

## Analysis Launcher And Dialog

Add a left-side long button below the existing search launcher:

- Label: `Advanced Content Analysis`
- Icon: `qrc:/panel_icons/flask.svg`
- Enabled only when the backend is interactive and at least one image is
  selected.
- Disabled state should still explain why it is unavailable via tooltip/status
  text if the existing UI pattern supports that.

Create a standalone Dialog, recommended name:

- `alcedo_studio/src/ui/alcedo_main/qml/AdvancedContentAnalysisDialog.qml`

Dialog content:

- Header:
  - title: `Advanced Content Analysis`
  - selected image count
  - provider display name
  - model display name or model id
  - output language
- Task options:
  - Description
  - Rating
  - Rating reason
- Overwrite options, default checked:
  - Overwrite photo rating
  - Overwrite rating reason
  - Overwrite image description
- Progress:
  - progress ring, visually aligned with `ImportProgressRing.qml`
  - completed / total task count
  - status text from `ImageAnalysisController.statusText`
  - optional token/usage summary from `lastUsage`
- Controls:
  - idle: `Analyze Selected`, `Close`
  - running: `Cancel`; no escape/outside close
  - finished/canceled/failed: summary + `Close`
- Hint:
  - Results refresh the focused photo's Image inspector. Open the Image page to
    review and edit description, rating, and reasons.

Execution behavior:

- The Dialog sends `selectionState.currentSelectedItems()` to the analysis
  controller.
- If both description and rating/reason are selected, the UI may run the
  existing backend task calls sequentially until a combined backend RPC exists:
  describe first, score second.
- Progress should count item-task units so a 10-image describe+score run reads
  as 20 units, not an ambiguous 10.
- If overwrite for a selected field is disabled, the controller/frontend should
  skip items that already have that field. If overwrite is enabled, successful
  results replace the active value.
- Cancel stops remaining work and leaves already-persisted successful results in
  place; failed/canceled item-task units must not create active annotations.

## Right Inspector Redesign

Refactor the current `InspectorPanel.qml` into an inspector shell with a narrow
vertical navbar and a content stack.

Recommended file split:

- `InspectorPanel.qml`: shell, vertical nav, page stack, shared colors
- `AlbumInspectorPanel.qml`: current album stats/search-filter content moved out
  of the old `InspectorPanel.qml`
- `ImageInspectorPanel.qml`: focused-image inspection page
- optional small components:
  - `ImageMetricTile.qml`
  - `EditableInspectorText.qml`
  - `InspectorStarRating.qml`

The vertical navbar should start with two icon buttons:

- Album: use an album/folder/library icon from existing panel icons if suitable.
- Image: use `qrc:/panel_icons/image.svg`.

The Image page is a persistent version of the current image details dialog,
not a popup. The old right-click `Details` action can either switch the
inspector to Image or remain as a compatibility shortcut during the transition,
but the primary inspection path should be the right panel.

## Image Inspector Content

The Image page shows six tiles:

1. Camera
2. Lens
3. Aperture / Shutter
4. ISO
5. Description
6. Rating

Responsive layout:

- Wide inspector: 2 columns x 3 rows.
- Narrow inspector: 1 column x 6 rows.
- Description and Rating get taller minimum heights than the first four metric
  tiles.

Tile behavior:

- Camera tile:
  - camera brand/model
  - captured date/time as secondary data when useful
- Lens tile:
  - lens brand/model
  - focal length / 35mm equivalent as compact secondary data
- Aperture/Shutter tile:
  - aperture and shutter as large paired values
- ISO tile:
  - ISO as large numeric value
  - optional focus distance as secondary data
- Description tile:
  - shows active image description, empty state, or inline TextArea in edit mode
  - edit/save/cancel happens in the tile
  - manual save marks the active description as manual-authored
- Rating tile:
  - top half: unified 0-5 photo rating, editable by clicking stars
  - bottom half: rating reason summary or inline TextArea in edit mode
  - manual reason save marks the active reason as manual-authored

The Image page uses the current focused image only. Multi-selection does not
change the displayed content unless focus changes.

## Required C++/QML API Gaps

The backend already exposes much of the remote-analysis machinery, but the
frontend plan needs several small host-facing additions before the QML can be
clean and reliable.

Provider settings:

- Add a QML-facing way to save/delete/check credentials for the selected
  `credential_slot`. This can be a new controller or an extension around
  `AiProviderPresetController`; keep raw keys out of the preset DTO.
- Expose model-discovery results as structured QML data, not only a status/error
  string. `ImageAnalysisController.ValidateConnection()` currently maps to the
  backend dry-run path, but the settings page needs a model list.
- Define how a refreshed model candidate becomes selectable for paid analysis.
  Backend Phase 6c rejects unknown explicit model ids before provider calls, so
  the frontend must either:
  - keep `model_id` empty and use the preset default, or
  - commit the discovered model into a generated/updated provider config before
    using it as an explicit model.
- Persist output-language preference near the provider preset. The request path
  must pass this target language into description/rating prompts.

Image inspector:

- Prefer a single QML DTO method such as `GetImageInspection(elementId, imageId)`
  over parsing the existing detail-row list in QML. It should return the six-tile
  data plus active description and active rating reason.
- Expose active AI/manual description. Storage already has active understanding
  rows, but the current QML surface only exposes image details and rating reason.
- Add manual edit APIs:
  - `SetImageDescription(elementId, text)`
  - `SetImageRatingReason(elementId, text)`
  - existing `SetImageRating(elementId, imageId, rating)` can remain the star
    path
- Manual description/reason can use synthetic identity in the existing AI tables,
  e.g. `provider_id = "manual"` and `model_id = "user"`, unless a later storage
  migration adds an explicit source column.
- Manual description saves should refresh active search results, like AI
  description persistence.

Analysis dialog:

- `ImageAnalysisController` currently exposes separate describe/score invokables.
  Add a QML-friendly wrapper for selected task sets and overwrite flags, or keep
  orchestration in the Dialog for the first slice.
- If orchestration stays in QML, guard against starting the score task before the
  describe task finishes/cancels.
- Surface clear per-task status so the Dialog can distinguish provider failure,
  credential missing, canceled, skipped-existing, and success.

## Implementation Phases

### Frontend 1 - Provider Settings Page

- Add `AiProviderSettingsPanel.qml`.
- Split the settings dialog navigation:
  - rename the current AI page to `Local Content Recognition`
  - add `Advanced Content Analysis`
- Add protocol/preset ComboBoxes and advanced collapsible fields.
- Add API key save/delete/test UI wired through a QML-safe credential API.
- Add model refresh and model selection UI.
- Add output language preference, defaulting to follow app language.

Acceptance:

- Raw API key never appears in QSettings, logs, status text, or preset DTOs.
- Selecting a built-in preset fills advanced fields.
- Editing an advanced field marks the selected configuration as custom/modified.
- Test/refresh reports success/failure and populates the model ComboBox when
  supported.

### Frontend 1-Fix - Provider Settings Repair

This phase is inserted before Frontend 2 because the first settings slice is not
yet a reliable product surface. Frontend 2 must not build the analysis launcher
or execution dialog on top of the current provider-settings behavior.

Audit findings from manual testing:

- Protocol and preset/provider identity are incorrectly coupled in the UI. The
  current panel exposes `OpenCode (Anthropic)` and `OpenCode (OpenAI)` as
  separate provider presets, then lets the protocol ComboBox appear to follow
  the preset selection. The intended model is a combination: provider/service
  choice plus compatible protocol path.
- API keys do not persist reliably. The current text claims a system credential
  store, but the QML/C++ path must prove that save, reload, availability check,
  delete, and analysis-time loading all use the same credential slot on every
  supported desktop OS.
- The API-key input is visually too small for paste-oriented long secrets. Even
  when masked, the field should signal that long keys are expected.
- The lower half of `AiProviderSettingsPanel.qml` is effectively unverified
  because credential persistence blocks connection/model testing.
- The built-in preset list is wrong for the intended first product slice:
  OpenCode appears, OpenRouter appears when it should not, and Volcengine Ark /
  火山方舟 is missing.
- Provider labels must not use stale/deprecated marketing markers. Provider UI
  copy should name the actual service and protocol path plainly.

Fix scope:

- Replace the current single mixed preset model with an explicit provider +
  protocol combination model:
  - `Provider` selects the service identity and shared account/credential slot
    such as `OpenCode`, `Volcengine Ark / 火山方舟`, or `Custom`.
  - `Protocol` selects the compatible request/response path for that provider
    such as `OpenAI-compatible chat`, `Anthropic-compatible messages`, or
    `Volcengine Ark responses` when available.
  - The selected combination resolves to the concrete backend `provider_id`,
    `driver`/`protocol_family`, endpoint, structured-output mode, model
    defaults, and credential slot.
  - Changing Provider must not silently overwrite a manually chosen Protocol
    unless that provider does not support the current protocol; in that case the
    UI must show the fallback clearly.
  - Changing Protocol must not pretend that the provider changed. It only
    changes the transport path within the selected provider.
- Make the built-in presets data-driven from the backend provider configs, or
  keep one QML/C++ table generated from the same source. The first fixed list
  must include:
  - `OpenCode` with supported Anthropic-compatible and OpenAI-compatible paths,
    sharing `opencode_api_key`.
  - `Volcengine Ark / 火山方舟` with the normal Ark responses path and the coding
    plan Anthropic-compatible path when both backend configs are present,
    sharing `volcengine_ark_api_key`.
  - `Custom`.
  - OpenRouter must be removed from the default visible list for this slice.
- Redesign the API-key section:
  - use a wide paste field spanning the panel width, with a minimum height that
    feels like a long secret entry rather than a short option field;
  - keep password masking, but show enough placeholder/help text to make paste
    behavior obvious;
  - keep the action buttons adjacent but secondary to the full-width field;
  - show saved state using only a masked label and credential slot/service name,
    never raw key material.
- Fix and verify credential persistence:
  - Windows: use Credential Manager through the existing store or a corrected
    wrapper, with deterministic target names.
  - macOS: do not use a process-only in-memory fallback for saved keys; either
    implement Keychain-backed persistence for this slice or explicitly disable
    persistent save with a truthful status until implemented.
  - Linux: do not claim persistent OS storage unless a Secret Service/libsecret
    path exists; otherwise use a truthful unsupported/temporary state.
  - Saving a key must immediately flip `credentialAvailable` without requiring
    a settings dialog reopen.
  - Closing and reopening the app must preserve the saved-key state where the OS
    credential path is supported.
  - Deleting a key must clear the OS credential, masked label, and availability
    state.
- Re-verify the connection/model section after credential save works:
  - `Test & Refresh Models` must be disabled with a clear reason until provider,
    protocol, model/default, and credential are sufficient.
  - Live-discovered models may only become selectable if they can actually be
    used by the analysis path without local rejection.
  - Status lines must distinguish missing key, unsupported credential backend,
    provider failure, and successful model refresh.
- Add focused tests before Frontend 2 starts:
  - provider + protocol matrix maps to the expected concrete backend
    `provider_id`, endpoint, structured output mode, model default, and
    credential slot;
  - OpenCode protocol switching keeps the provider identity stable;
  - Volcengine Ark appears in the built-in list;
  - OpenRouter is not visible in the default built-in list;
  - credential save/delete/availability survives controller recreation on each
    platform with persistent support;
  - raw keys never enter QSettings, preset DTOs, status text, logs, or test
    failure output;
  - API-key field sizing is covered by a QML/manual visual check at narrow and
    wide settings-dialog widths.

Acceptance:

- The settings page presents Provider and Protocol as two parts of one selected
  backend combination, not as duplicated provider presets.
- OpenCode is one provider entry with compatible protocol choices underneath it.
- Volcengine Ark / 火山方舟 is visible when its backend configs are present.
- OpenRouter is absent from the default visible built-in preset list.
- API keys can be saved, detected, used for connection refresh, deleted, and
  detected as deleted on the supported credential backend.
- The UI never claims persistent OS credential storage on a platform where the
  implementation is only in-memory.
- The API-key field visually accommodates long pasted keys.
- The lower connection/model/output-language sections have been manually tested
  after credential persistence is fixed.

### Frontend 2 - Advanced Analysis Launcher And Dialog

- Add `panel_icons/flask.svg` and register it in `resource.qrc`.
- Add the left-side launcher below the search button.
- Add `AdvancedContentAnalysisDialog.qml`.
- Wire selected images, provider/model display, output language, task choices,
  overwrite choices, progress, cancel, and final summary.
- Make the Dialog non-closeable while running.

Acceptance:

- Empty selection cannot start a remote analysis.
- Running analysis cannot be dismissed without canceling.
- Default overwrite state is on for rating, rating reason, and description.
- Completion does not automatically switch the inspector page.
- Dialog hint explains where to review/edit results.

### Frontend 3 - Inspector Shell And Album Page Extraction

- Refactor `InspectorPanel.qml` into a shell with vertical nav and page stack.
- Move existing album overview/stats/search-filter UI into
  `AlbumInspectorPanel.qml`.
- Add Image page placeholder with empty/focused-image states.

Acceptance:

- Album page looks and behaves the same as the current inspector content.
- Vertical nav switches between Album and Image.
- Existing inspector collapse/expand and resizing still work.

### Frontend 4 - Image Inspector Tiles And Inline Editing

- Build the six responsive tiles.
- Reuse/replace the current `ImageDetailsDialog.qml` data flow with a compact
  focused-image inspection DTO.
- Wire star editing through the existing rating path.
- Wire inline description and rating-reason editing.
- Refresh Image inspector when focused image changes or analysis results arrive.

Acceptance:

- Focused image drives all Image page content.
- Wide inspector uses 2 columns; narrow inspector uses 1 column.
- Text never overflows tile boundaries.
- Manual edits persist and re-render without reopening the app.
- AI results update the visible focused image when applicable, without switching
  pages automatically.

### Frontend 5 - Localization, Polish, And Tests

- Add English and Chinese translations for new UI strings.
- Add QML/component tests where available; otherwise add focused C++ controller
  tests for any new QML-facing methods.
- Manually test:
  - missing credential
  - failed connection test
  - model refresh success
  - empty selection
  - one selected image
  - multi-selected images
  - cancel while running
  - overwrite off/on behavior
  - inline edit save/cancel
  - language switch

## Open Implementation Notes

- The current `ImageDetailsDialog.qml` can be retired after the Image inspector
  reaches feature parity, or kept temporarily as a right-click compatibility
  path that switches/focuses the Image inspector.
- The UI should not present remote analysis as "free background labeling." Its
  placement, selected-images requirement, and progress Dialog should make the
  paid remote-call nature clear without being alarming.
- If a combined describe+rating backend call lands later, replace sequential
  Dialog orchestration with one controller call. The UI shape should remain the
  same.
- The model list UX depends on the backend's provider-config merge story. Do not
  let the user select a model id that will be rejected locally as unknown.
