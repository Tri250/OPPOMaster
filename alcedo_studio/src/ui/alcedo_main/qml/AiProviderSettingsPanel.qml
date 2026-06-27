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
    readonly property int pageMargin: 34

    signal messageRequested(string message)

    readonly property bool hasPreset: presetController !== null
    readonly property bool hasAnalysis: analysisController !== null
    readonly property bool providerConfigured: hasAnalysis ? analysisController.providerConfigured : false
    readonly property bool credentialAvailable: hasAnalysis ? analysisController.credentialAvailable : false
    readonly property string maskedKeyLabel: hasPreset ? presetController.maskedKeyLabel : ""
    readonly property string savedKeyDisplayMask: "•••• •••• •••• ••••"
    readonly property string connectionStatus: hasAnalysis ? analysisController.connectionStatus : ""
    readonly property string lastError: hasAnalysis ? analysisController.lastError : ""
    readonly property var discoveredModels: hasAnalysis ? analysisController.discoveredModels : []
    property bool advancedExpanded: false

    readonly property var providerOptions: hasPreset ? presetController.BuiltinProviderOptions() : []
    readonly property string selectedProviderKey: {
        if (!hasPreset) {
            return "custom"
        }
        for (let i = 0; i < providerOptions.length; ++i) {
            const protocols = presetController.BuiltinProtocolOptions(providerOptions[i].providerKey)
            for (let j = 0; j < protocols.length; ++j) {
                if (protocols[j].providerId === presetController.providerId) {
                    return providerOptions[i].providerKey
                }
            }
        }
        return "custom"
    }
    readonly property var protocolOptions: hasPreset && selectedProviderKey !== "custom"
                                          ? presetController.BuiltinProtocolOptions(selectedProviderKey) : []
    readonly property int selectedProviderIndex: {
        for (let i = 0; i < providerOptions.length; ++i) {
            if (providerOptions[i].providerKey === selectedProviderKey) {
                return i
            }
        }
        return Math.max(0, providerOptions.length - 1)
    }
    readonly property int selectedProtocolIndex: {
        if (!hasPreset) {
            return 0
        }
        for (let i = 0; i < protocolOptions.length; ++i) {
            if (protocolOptions[i].providerId === presetController.providerId) {
                return i
            }
        }
        return 0
    }
    readonly property string presetStateLabel: {
        if (!hasPreset || selectedProviderKey === "custom" || protocolOptions.length === 0) {
            return qsTr("Custom")
        }
        const b = protocolOptions[selectedProtocolIndex]
        if (!b || presetController.protocolFamily !== b.protocolFamily
                || presetController.baseUrl !== b.baseUrl
                || presetController.endpoint !== b.endpoint
                || presetController.authType !== b.authType
                || presetController.credentialSlot !== b.credentialSlot
                || presetController.structuredOutputMode !== b.structuredOutputMode) {
            return qsTr("Modified")
        }
        return ""
    }
    readonly property bool connectionConfigReady: hasPreset
        && presetController.providerId.length > 0
        && presetController.credentialSlot.length > 0
    readonly property string validationBlockReason: !hasAnalysis ? qsTr("Image analysis runtime is unavailable.")
        : !connectionConfigReady ? qsTr("Select a provider and protocol before testing.")
        : !credentialAvailable ? qsTr("Save an API key before testing the connection.") : ""
    readonly property string credentialStorageCopy: Qt.platform.os === "windows"
        ? qsTr("Saved in Windows Credential Manager. Only a masked label is kept in settings.")
        : qsTr("Persistent OS credential storage is not available on this platform yet; saved keys are temporary for this app session.")

    function applyProvider(providerKey) {
        if (!hasPreset || providerKey === "custom") {
            return
        }
        const protocols = presetController.BuiltinProtocolOptions(providerKey)
        if (protocols.length === 0) {
            return
        }
        let protocolFamily = presetController.protocolFamily
        let supported = false
        for (let i = 0; i < protocols.length; ++i) {
            if (protocols[i].protocolFamily === protocolFamily) {
                supported = true
                break
            }
        }
        if (!supported) {
            protocolFamily = protocols[0].protocolFamily
            messageRequested(qsTr("Protocol changed to %1 because %2 does not support the previous path.")
                             .arg(protocols[0].protocolLabel).arg(protocols[0].providerLabel))
        }
        if (presetController.ApplyBuiltinProviderProtocol(providerKey, protocolFamily) && hasAnalysis) {
            analysisController.RefreshCredentialState()
        }
    }

    function applyProtocol(protocolFamily) {
        if (!hasPreset || selectedProviderKey === "custom") {
            presetController.SetProtocolFamily(protocolFamily)
            return
        }
        if (presetController.ApplyBuiltinProviderProtocol(selectedProviderKey, protocolFamily) && hasAnalysis) {
            analysisController.RefreshCredentialState()
        }
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

    spacing: 20

    // ── Provider + Protocol ─────────────────────────────────────────────────
    SettingsSection {
        Layout.preferredWidth: Math.max(0, panel.width - panel.pageMargin * 2)
        Layout.leftMargin: panel.pageMargin
        Layout.rightMargin: panel.pageMargin
        title: qsTr("Provider and protocol")
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
                    text: qsTr("Provider")
                    color: panel.mutedTextColor
                    font.pixelSize: 12
                    font.weight: 700
                }
                ComboBox {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 44
                    enabled: panel.hasPreset
                    model: panel.providerOptions
                    textRole: "label"
                    currentIndex: panel.selectedProviderIndex
                    onActivated: function(index) {
                        const p = model[index]
                        if (p) {
                            panel.applyProvider(p.providerKey)
                        }
                    }
                }
                Label {
                    Layout.fillWidth: true
                    text: panel.providerOptions.length > panel.selectedProviderIndex
                          ? panel.providerOptions[panel.selectedProviderIndex].help : ""
                    color: panel.mutedTextColor
                    font.pixelSize: 11
                    wrapMode: Text.WordWrap
                }
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6

                RowLayout {
                    spacing: 8
                    Label {
                        text: qsTr("Protocol")
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
                    enabled: panel.hasPreset && panel.selectedProviderKey !== "custom" && panel.protocolOptions.length > 0
                    model: panel.protocolOptions
                    textRole: "protocolLabel"
                    currentIndex: panel.selectedProtocolIndex
                    onActivated: function(index) {
                        const p = model[index]
                        if (p) {
                            panel.applyProtocol(p.protocolFamily)
                        }
                    }
                }
                Label {
                    Layout.fillWidth: true
                    text: panel.selectedProviderKey === "custom"
                          ? qsTr("Custom providers are edited in Advanced fields.")
                          : qsTr("Protocol changes only the request path for the selected provider.")
                    color: panel.mutedTextColor
                    font.pixelSize: 11
                    wrapMode: Text.WordWrap
                }
            }
        }
    }
    // ── API key ──────────────────────────────────────────────────────────────
    SettingsSection {
        Layout.preferredWidth: Math.max(0, panel.width - panel.pageMargin * 2)
        Layout.leftMargin: panel.pageMargin
        Layout.rightMargin: panel.pageMargin
        title: qsTr("API key")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 10

            Label {
                Layout.fillWidth: true
                text: panel.credentialStorageCopy
                color: panel.mutedTextColor
                font.pixelSize: 12
                wrapMode: Text.WordWrap
            }

            TextField {
                id: keyField
                Layout.fillWidth: true
                Layout.preferredHeight: 64
                echoMode: TextInput.Password
                placeholderText: qsTr("Paste a long provider API key for %1").arg(panel.hasPreset ? panel.presetController.credentialSlot : qsTr("this provider"))
                color: panel.textColor
                enabled: panel.hasAnalysis
                font.family: panel.dataFontFamily
                verticalAlignment: TextInput.AlignVCenter
            }

            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Button {
                    Layout.preferredHeight: 38
                    text: qsTr("Save Key")
                    enabled: panel.hasAnalysis && keyField.text.length > 0
                    Material.foreground: panel.textColor
                    onClicked: {
                        const err = panel.analysisController.SaveApiKey(keyField.text)
                        keyField.text = ""
                        if (err.length > 0) {
                            panel.messageRequested(err)
                        } else {
                            panel.messageRequested(Qt.platform.os === "windows"
                                                   ? qsTr("API key saved to Windows Credential Manager")
                                                   : qsTr("API key saved for this app session"))
                        }
                    }
                }

                Button {
                    Layout.preferredHeight: 38
                    text: qsTr("Delete Key")
                    enabled: panel.hasAnalysis && panel.credentialAvailable
                    Material.foreground: panel.dangerColor
                    onClicked: {
                        panel.analysisController.DeleteApiKey()
                        panel.messageRequested(qsTr("API key deleted"))
                    }
                }


                Item { Layout.fillWidth: true }
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: 48
                radius: 8
                color: panel.credentialAvailable
                       ? Qt.rgba(panel.primaryAccent.r, panel.primaryAccent.g, panel.primaryAccent.b, 0.16)
                       : Qt.rgba(panel.dangerColor.r, panel.dangerColor.g, panel.dangerColor.b, 0.10)
                border.width: 1
                border.color: panel.credentialAvailable
                              ? Qt.rgba(panel.secondaryAccent.r, panel.secondaryAccent.g, panel.secondaryAccent.b, 0.34)
                              : Qt.rgba(panel.dangerColor.r, panel.dangerColor.g, panel.dangerColor.b, 0.28)

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 14
                    anchors.rightMargin: 14
                    spacing: 12

                    Rectangle {
                        Layout.preferredWidth: 9
                        Layout.preferredHeight: 9
                        radius: 5
                        color: panel.credentialAvailable ? panel.secondaryAccent : panel.dangerColor
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 1

                        Label {
                            Layout.fillWidth: true
                            text: panel.credentialAvailable ? qsTr("API key saved") : qsTr("No API key saved")
                            color: panel.credentialAvailable ? panel.textColor : panel.dangerColor
                            font.pixelSize: 12
                            font.weight: 800
                            elide: Text.ElideRight
                        }

                        Label {
                            Layout.fillWidth: true
                            text: panel.credentialAvailable
                                  ? panel.savedKeyDisplayMask
                                  : qsTr("Save a key for %1").arg(panel.hasPreset ? panel.presetController.credentialSlot : qsTr("this provider"))
                            color: panel.credentialAvailable ? panel.secondaryAccent : panel.mutedTextColor
                            font.family: panel.dataFontFamily
                            font.pixelSize: 13
                            font.weight: 700
                            elide: Text.ElideRight
                        }
                    }

                    Label {
                        visible: panel.credentialAvailable && panel.hasPreset
                        Layout.maximumWidth: 190
                        text: panel.presetController.credentialSlot
                        color: panel.mutedTextColor
                        font.family: panel.dataFontFamily
                        font.pixelSize: 11
                        elide: Text.ElideRight
                    }
                }
            }
        }
    }
    // ── Connection + model ───────────────────────────────────────────────────
    SettingsSection {
        Layout.preferredWidth: Math.max(0, panel.width - panel.pageMargin * 2)
        Layout.leftMargin: panel.pageMargin
        Layout.rightMargin: panel.pageMargin
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
                    enabled: panel.hasAnalysis && panel.connectionConfigReady && panel.credentialAvailable
                    ToolTip.visible: hovered && !enabled && panel.validationBlockReason.length > 0
                    ToolTip.text: panel.validationBlockReason
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

                Label {
                    Layout.fillWidth: true
                    visible: panel.connectionStatus.length === 0 && panel.validationBlockReason.length > 0
                    text: panel.validationBlockReason
                    color: panel.mutedTextColor
                    font.pixelSize: 12
                    elide: Text.ElideRight
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
        Layout.preferredWidth: Math.max(0, panel.width - panel.pageMargin * 2)
        Layout.leftMargin: panel.pageMargin
        Layout.rightMargin: panel.pageMargin
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
        Layout.preferredWidth: Math.max(0, panel.width - panel.pageMargin * 2)
        Layout.leftMargin: panel.pageMargin
        Layout.rightMargin: panel.pageMargin
        title: qsTr("Advanced")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 10

            Label {
                Layout.fillWidth: true
                text: qsTr("Low-level provider fields for custom endpoints and debugging preset mappings.")
                color: panel.mutedTextColor
                font.pixelSize: 12
                wrapMode: Text.WordWrap
                lineHeight: 1.25
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: 46
                radius: 10
                color: advancedToggleMouse.containsMouse
                       ? Qt.rgba(1, 1, 1, 0.08)
                       : Qt.rgba(1, 1, 1, 0.04)
                border.width: 1
                border.color: Qt.rgba(1, 1, 1, 0.08)

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 14
                    anchors.rightMargin: 14
                    spacing: 10

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 1

                        Label {
                            Layout.fillWidth: true
                            text: panel.advancedExpanded
                                  ? qsTr("Hide provider fields")
                                  : qsTr("Show provider fields")
                            color: panel.secondaryAccent
                            font.pixelSize: 14
                            font.weight: 800
                            elide: Text.ElideRight
                        }

                        Label {
                            Layout.fillWidth: true
                            text: panel.hasPreset
                                  ? qsTr("%1 · %2").arg(panel.presetController.providerId).arg(panel.presetController.protocolFamily)
                                  : qsTr("No provider selected")
                            color: panel.mutedTextColor
                            font.family: panel.dataFontFamily
                            font.pixelSize: 11
                            elide: Text.ElideRight
                        }
                    }

                    Label {
                        text: panel.advancedExpanded ? "▲" : "▼"
                        color: panel.secondaryAccent
                        font.pixelSize: 14
                        font.weight: 800
                    }
                }

                MouseArea {
                    id: advancedToggleMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: panel.advancedExpanded = !panel.advancedExpanded
                }
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: panel.advancedExpanded ? advancedFields.implicitHeight + 24 : 0
                Layout.bottomMargin: 26
                visible: panel.advancedExpanded
                radius: 10
                color: Qt.rgba(panel.canvasColor.r, panel.canvasColor.g, panel.canvasColor.b, 0.45)
                border.width: 1
                border.color: Qt.rgba(1, 1, 1, 0.06)
                clip: true
                opacity: panel.advancedExpanded ? 1 : 0

                Behavior on Layout.preferredHeight {
                    NumberAnimation { duration: 160; easing.type: Easing.OutCubic }
                }
                Behavior on opacity {
                    NumberAnimation { duration: 120; easing.type: Easing.OutCubic }
                }

                ColumnLayout {
                    id: advancedFields
                    anchors.left: parent.left
                    anchors.right: parent.right
                    anchors.top: parent.top
                    anchors.margins: 12
                    spacing: 10

                    GridLayout {
                        Layout.fillWidth: true
                        columns: panel.width > 760 ? 2 : 1
                        columnSpacing: 12
                        rowSpacing: 10

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
                            label: qsTr("Protocol family")
                            value: panel.hasPreset ? panel.presetController.protocolFamily : ""
                            onEdited: function(text) { panel.presetController.SetProtocolFamily(text) }
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
        Layout.minimumWidth: 220

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
