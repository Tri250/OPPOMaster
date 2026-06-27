import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts

// Advanced Content Analysis provider settings (Frontend 1).
//
// Drives the remote (paid) multimodal image-analysis path: protocol/preset
// selection, API key management in the OS credential store, connection test +
// model refresh, model selection from live-discovered candidates, and output
// language. This is deliberately separate from SemanticGenerationSettingsPanel,
// which covers the local CLIP/SigLIP label path. The two must stay visually and
// conceptually distinct so a user never mistakes free local labeling for paid
// remote analysis.
//
// All non-secret fields round-trip through AiProviderPresetController (QSettings
// ai/preset/*). The raw API key NEVER enters QSettings, the preset DTO, status
// text, or logs — it goes straight from the password field to the OS credential
// store via ImageAnalysisController.SaveApiKey, and only a masked label is
// persisted for display.

ColumnLayout {
    id: panel

    property var presetController: null        // albumBackend.aiProviderPresetController
    property var analysisController: null      // albumBackend.imageAnalysisController
    property color primaryAccent: "#457B9D"
    property color secondaryAccent: "#9FC7D8"
    property color textColor: "#F5F1EA"
    property color mutedTextColor: "#B6B0A7"
    property color canvasColor: "#111214"
    property color dividerColor: Qt.rgba(1, 1, 1, 0.08)
    property color dangerColor: "#D96C75"
    property string dataFontFamily: appTheme.dataFontFamily

    signal messageRequested(string message)

    readonly property bool hasPreset: presetController !== null
    readonly property bool hasAnalysis: analysisController !== null
    readonly property bool providerConfigured: hasAnalysis ? analysisController.providerConfigured : false
    readonly property bool credentialAvailable: hasAnalysis ? analysisController.credentialAvailable : false
    readonly property string maskedKeyLabel: hasPreset ? presetController.maskedKeyLabel : ""
    readonly property string connectionStatus: hasAnalysis ? analysisController.connectionStatus : ""
    readonly property string lastError: hasAnalysis ? analysisController.lastError : ""
    readonly property var discoveredModels: hasAnalysis ? analysisController.discoveredModels : []

    // Built-in presets keyed by provider_id. Selecting one backfills the advanced
    // fields and clears any custom/modified state. Keep in sync with the Rust
    // built-in configs (rust/puerh_mind/configs/providers/*.json).
    readonly property var builtinPresets: [
        {
            providerId: "opencode_go_anthropic",
            displayName: qsTr("Opencode Go (Anthropic-compatible)"),
            protocolFamily: "anthropic_messages",
            baseUrl: "https://opencode.ai/zen/go/v1",
            endpoint: "/messages",
            authType: "bearer",
            credentialSlot: "opencode_api_key",
            structuredOutputMode: "tool"
        },
        {
            providerId: "opencode_go_openai",
            displayName: qsTr("Opencode Go (OpenAI-compatible)"),
            protocolFamily: "openai_chat_compatible",
            baseUrl: "https://opencode.ai/zen/go/v1",
            endpoint: "/chat/completions",
            authType: "bearer",
            credentialSlot: "opencode_api_key",
            structuredOutputMode: "response_format_json_schema"
        },
        {
            providerId: "openrouter",
            displayName: qsTr("OpenRouter (legacy)"),
            protocolFamily: "openai_chat_compatible",
            baseUrl: "https://openrouter.ai/api/v1",
            endpoint: "/chat/completions",
            authType: "bearer",
            credentialSlot: "openrouter_api_key",
            structuredOutputMode: "response_format_json_schema"
        }
    ]

    // The currently-selected preset index, or -1 if the saved config does not
    // match any built-in (custom/modified). Recomputed whenever the preset
    // changes.
    readonly property int selectedPresetIndex: {
        if (!hasPreset) {
            return -1
        }
        const id = presetController.providerId
        for (let i = 0; i < builtinPresets.length; ++i) {
            if (builtinPresets[i].providerId === id) {
                return i
            }
        }
        return -1
    }
    // ComboBox model for the Preset box: a leading "Custom" entry, then the
    // built-ins. Selecting "Custom" leaves the advanced fields as-is; selecting
    // a built-in backfills them.
    readonly property var presetOptions: {
        const opts = builtinPresets.slice()
        opts.unshift({ providerId: "", displayName: qsTr("Custom") })
        return opts
    }
    // "Modified" when a built-in preset is selected but an advanced field no
    // longer matches it; "Custom" when no built-in matches.
    readonly property string presetStateLabel: {
        if (selectedPresetIndex < 0) {
            return qsTr("Custom")
        }
        const b = builtinPresets[selectedPresetIndex]
        if (presetController.protocolFamily !== b.protocolFamily
                || presetController.baseUrl !== b.baseUrl
                || presetController.endpoint !== b.endpoint
                || presetController.authType !== b.authType
                || presetController.credentialSlot !== b.credentialSlot
                || presetController.structuredOutputMode !== b.structuredOutputMode) {
            return qsTr("Modified")
        }
        return ""
    }

    // The model ComboBox model: a "use preset default" entry first, then the
    // preset's currently-set model (if any), then live-discovered candidates.
    // Discovered models are committed by the sidecar during ListModels, so each
    // is immediately usable as an explicit model_id.
    readonly property var modelOptions: {
        const out = [{ modelId: "", displayName: qsTr("Use preset default") }]
        const seen = { "": true }
        if (hasPreset && presetController.modelId.length > 0) {
            const id = presetController.modelId
            if (!seen[id]) {
                seen[id] = true
                out.push({
                    modelId: id,
                    displayName: presetController.modelDisplayName.length > 0
                                 ? presetController.modelDisplayName : id
                })
            }
        }
        for (let i = 0; i < discoveredModels.length; ++i) {
            const m = discoveredModels[i]
            const id = m.modelId
            if (!seen[id]) {
                seen[id] = true
                out.push({ modelId: id, displayName: m.displayName })
            }
        }
        return out
    }
    function modelIndexForId(id) {
        for (let i = 0; i < modelOptions.length; ++i) {
            if (modelOptions[i].modelId === id) {
                return i
            }
        }
        return 0
    }

    function applyPreset(preset) {
        if (!hasPreset) {
            return
        }
        presetController.SetProviderId(preset.providerId)
        presetController.SetDisplayName(preset.displayName)
        presetController.SetProtocolFamily(preset.protocolFamily)
        presetController.SetBaseUrl(preset.baseUrl)
        presetController.SetEndpoint(preset.endpoint)
        presetController.SetAuthType(preset.authType)
        presetController.SetCredentialSlot(preset.credentialSlot)
        presetController.SetStructuredOutputMode(preset.structuredOutputMode)
        // A fresh preset resets the explicit model to its default.
        presetController.SetModelId("")
        presetController.SetModelDisplayName("")
        if (hasAnalysis) {
            analysisController.RefreshCredentialState()
        }
    }

    spacing: 20

    // ── Protocol + Preset ────────────────────────────────────────────────────
    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("Provider preset")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6

                Label {
                    text: qsTr("Protocol")
                    color: panel.mutedTextColor
                    font.pixelSize: 12
                    font.weight: 700
                }
                ComboBox {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 44
                    enabled: panel.hasPreset
                    model: [
                        { label: qsTr("OpenAI-compatible chat"), value: "openai_chat_compatible" },
                        { label: qsTr("Anthropic-compatible messages"), value: "anthropic_messages" }
                    ]
                    textRole: "label"
                    currentIndex: {
                        if (!panel.hasPreset) {
                            return 0
                        }
                        const fam = panel.presetController.protocolFamily
                        for (let i = 0; i < model.length; ++i) {
                            if (model[i].value === fam) {
                                return i
                            }
                        }
                        return 0
                    }
                    onActivated: function(index) {
                        panel.presetController.SetProtocolFamily(model[index].value)
                    }
                }
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6

                RowLayout {
                    spacing: 8
                    Label {
                        text: qsTr("Preset")
                        color: panel.mutedTextColor
                        font.pixelSize: 12
                        font.weight: 700
                    }
                    Label {
                        visible: panel.presetStateLabel.length > 0
                        text: panel.presetStateLabel
                        color: panel.secondaryAccent
                        font.pixelSize: 11
                        font.weight: 700
                    }
                }
                ComboBox {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 44
                    enabled: panel.hasPreset
                    model: panel.presetOptions
                    textRole: "displayName"
                    currentIndex: panel.selectedPresetIndex < 0 ? 0 : panel.selectedPresetIndex + 1
                    onActivated: function(index) {
                        if (index === 0) {
                            return  // "Custom" — leave fields as-is
                        }
                        const preset = panel.presetOptions[index]
                        if (preset && preset.providerId.length > 0) {
                            panel.applyPreset(preset)
                        }
                    }
                }
            }
        }
    }

    // ── API key ──────────────────────────────────────────────────────────────
    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("API key")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 8

            Label {
                Layout.fillWidth: true
                text: qsTr("Saved in the system credential store — never written to settings files.")
                color: panel.mutedTextColor
                font.pixelSize: 12
                wrapMode: Text.WordWrap
            }

            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                TextField {
                    id: keyField
                    Layout.fillWidth: true
                    Layout.preferredHeight: 44
                    echoMode: TextInput.Password
                    placeholderText: qsTr("Paste API key")
                    color: panel.textColor
                    enabled: panel.hasAnalysis
                    font.family: panel.dataFontFamily
                }

                Button {
                    Layout.preferredHeight: 42
                    text: qsTr("Save Key")
                    enabled: panel.hasAnalysis && keyField.text.length > 0
                    Material.foreground: panel.textColor
                    onClicked: {
                        const err = panel.analysisController.SaveApiKey(keyField.text)
                        keyField.text = ""
                        if (err.length > 0) {
                            panel.messageRequested(err)
                        } else {
                            panel.messageRequested(qsTr("API key saved to the system credential store"))
                        }
                    }
                }

                Button {
                    Layout.preferredHeight: 42
                    text: qsTr("Delete Key")
                    enabled: panel.hasAnalysis && panel.credentialAvailable
                    Material.foreground: panel.dangerColor
                    onClicked: {
                        panel.analysisController.DeleteApiKey()
                        panel.messageRequested(qsTr("API key deleted"))
                    }
                }
            }

            Label {
                Layout.fillWidth: true
                visible: panel.maskedKeyLabel.length > 0
                text: panel.credentialAvailable
                      ? qsTr("Key saved: %1").arg(panel.maskedKeyLabel)
                      : qsTr("No key saved")
                color: panel.credentialAvailable ? panel.mutedTextColor : panel.dangerColor
                font.pixelSize: 12
            }
        }
    }

    // ── Connection + model ───────────────────────────────────────────────────
    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("Connection and model")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 8

            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Button {
                    Layout.preferredHeight: 42
                    text: qsTr("Test & Refresh Models")
                    enabled: panel.hasAnalysis && panel.providerConfigured && panel.credentialAvailable
                    Material.foreground: panel.textColor
                    onClicked: panel.analysisController.ValidateConnection()
                }

                Label {
                    Layout.fillWidth: true
                    text: panel.connectionStatus
                    color: panel.connectionStatus.length > 0
                           && panel.connectionStatus.indexOf(qsTr("Connected")) === 0
                           ? panel.secondaryAccent : panel.mutedTextColor
                    font.pixelSize: 12
                    elide: Text.ElideRight
                    visible: panel.connectionStatus.length > 0
                }
            }

            Label {
                Layout.fillWidth: true
                visible: panel.lastError.length > 0
                text: panel.lastError
                color: panel.dangerColor
                font.pixelSize: 12
                wrapMode: Text.WordWrap
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6

                Label {
                    text: qsTr("Model")
                    color: panel.mutedTextColor
                    font.pixelSize: 12
                    font.weight: 700
                }
                ComboBox {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 44
                    enabled: panel.hasPreset
                    model: panel.modelOptions
                    textRole: "displayName"
                    currentIndex: panel.modelIndexForId(panel.hasPreset ? panel.presetController.modelId : "")
                    onActivated: function(index) {
                        const m = panel.modelOptions[index]
                        if (m) {
                            panel.presetController.SetModelId(m.modelId)
                            panel.presetController.SetModelDisplayName(m.modelId.length > 0 ? m.displayName : "")
                        }
                    }
                }
                Label {
                    Layout.fillWidth: true
                    text: qsTr("Discovered models are committed on refresh and become immediately usable.")
                    color: panel.mutedTextColor
                    font.pixelSize: 11
                    wrapMode: Text.WordWrap
                }
            }
        }
    }

    // ── Output language ──────────────────────────────────────────────────────
    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("Output language")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            Label {
                Layout.preferredWidth: 160
                text: qsTr("Language")
                color: panel.textColor
                font.pixelSize: 15
                font.weight: 600
            }

            ComboBox {
                Layout.fillWidth: true
                Layout.preferredHeight: 44
                enabled: panel.hasPreset
                model: [
                    { label: qsTr("Follow app language"), value: "follow" },
                    { label: qsTr("English"), value: "en" },
                    { label: qsTr("中文"), value: "zh" }
                ]
                textRole: "label"
                currentIndex: {
                    if (!panel.hasPreset) {
                        return 0
                    }
                    const v = panel.presetController.outputLanguage
                    for (let i = 0; i < model.length; ++i) {
                        if (model[i].value === v) {
                            return i
                        }
                    }
                    return 0
                }
                onActivated: function(index) {
                    panel.presetController.SetOutputLanguage(model[index].value)
                }
            }
        }
    }

    // ── Advanced collapsible ─────────────────────────────────────────────────
    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("Advanced")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 8

            Button {
                flat: true
                text: advancedToggle.checked ? qsTr("Hide advanced fields") : qsTr("Show advanced fields")
                Material.foreground: panel.secondaryAccent
                font.pixelSize: 13
                onClicked: advancedToggle.toggle()
                checkable: true
                id: advancedToggle
                checked: false
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 10
                visible: advancedToggle.checked

                AdvancedField {
                    label: qsTr("Provider id")
                    value: panel.hasPreset ? panel.presetController.providerId : ""
                    onEdited: function(text) { panel.presetController.SetProviderId(text) }
                }
                AdvancedField {
                    label: qsTr("Display name")
                    value: panel.hasPreset ? panel.presetController.displayName : ""
                    onEdited: function(text) { panel.presetController.SetDisplayName(text) }
                }
                AdvancedField {
                    label: qsTr("Base URL")
                    value: panel.hasPreset ? panel.presetController.baseUrl : ""
                    onEdited: function(text) { panel.presetController.SetBaseUrl(text) }
                }
                AdvancedField {
                    label: qsTr("Endpoint")
                    value: panel.hasPreset ? panel.presetController.endpoint : ""
                    onEdited: function(text) { panel.presetController.SetEndpoint(text) }
                }
                AdvancedField {
                    label: qsTr("Auth type")
                    value: panel.hasPreset ? panel.presetController.authType : ""
                    onEdited: function(text) { panel.presetController.SetAuthType(text) }
                }
                AdvancedField {
                    label: qsTr("Credential slot")
                    value: panel.hasPreset ? panel.presetController.credentialSlot : ""
                    onEdited: function(text) { panel.presetController.SetCredentialSlot(text) }
                }
                AdvancedField {
                    label: qsTr("Structured output mode")
                    value: panel.hasPreset ? panel.presetController.structuredOutputMode : ""
                    onEdited: function(text) { panel.presetController.SetStructuredOutputMode(text) }
                }
                AdvancedField {
                    label: qsTr("Timeout (ms)")
                    value: panel.hasPreset ? String(panel.presetController.timeoutMs) : ""
                    onEdited: function(text) {
                        const n = parseInt(text, 10)
                        if (!isNaN(n)) {
                            panel.presetController.SetTimeoutMs(n)
                        }
                    }
                }
                AdvancedField {
                    label: qsTr("Max image bytes")
                    value: panel.hasPreset ? String(panel.presetController.maxImageBytes) : ""
                    onEdited: function(text) {
                        const n = parseInt(text, 10)
                        if (!isNaN(n)) {
                            panel.presetController.SetMaxImageBytes(n)
                        }
                    }
                }
                AdvancedField {
                    label: qsTr("Recommended rendition")
                    value: panel.hasPreset ? panel.presetController.recommendedRendition : ""
                    onEdited: function(text) { panel.presetController.SetRecommendedRendition(text) }
                }
            }
        }
    }

    // ── Inline components ────────────────────────────────────────────────────
    component SettingsSection: ColumnLayout {
        property string title: ""
        property color textColor: "white"
        property color mutedTextColor: "#999999"
        property color dividerColor: Qt.rgba(1, 1, 1, 0.08)

        spacing: 14

        Label {
            Layout.fillWidth: true
            text: title
            color: textColor
            font.pixelSize: 18
            font.weight: 800
        }
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: dividerColor
        }
    }

    component AdvancedField: ColumnLayout {
        property string label: ""
        property string value: ""
        signal edited(string text)

        spacing: 4
        Layout.fillWidth: true

        Label {
            text: label
            color: panel.mutedTextColor
            font.pixelSize: 11
            font.weight: 700
        }
        TextField {
            Layout.fillWidth: true
            Layout.preferredHeight: 40
            text: value
            color: panel.textColor
            font.family: panel.dataFontFamily
            font.pixelSize: 13
            onEditingFinished: edited(text)
        }
    }
}
