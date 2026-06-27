import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts

SwipeView {
    id: panel

    property var profileController: null       // albumBackend.aiProviderProfileController
    property var analysisController: null      // albumBackend.imageAnalysisController
    property color primaryAccent: "#457B9D"
    property color secondaryAccent: "#9FC7D8"
    property color textColor: "#F5F1EA"
    property color mutedTextColor: "#B6B0A7"
    property color canvasColor: "#111214"
    property color dividerColor: Qt.rgba(1, 1, 1, 0.08)
    property color dangerColor: "#D96C75"
    property string dataFontFamily: appTheme.dataFontFamily
    property int dataRevision: 0
    property string editingProfileId: ""
    readonly property bool hasProfilesController: profileController !== null
    readonly property bool hasAnalysis: analysisController !== null
    readonly property var profiles: hasProfilesController ? profileController.profiles : []
    readonly property var templates: hasProfilesController ? profileController.templateOptions : []
    readonly property var editProfile: {
        dataRevision
        return hasProfilesController && editingProfileId.length > 0
                ? profileController.Profile(editingProfileId) : ({})
    }
    readonly property var modelOptions: {
        dataRevision
        return hasProfilesController && editingProfileId.length > 0
                ? profileController.ModelOptions(editingProfileId) : []
    }
    signal messageRequested(string message)

    interactive: false
    clip: true
    currentIndex: 0

    Connections {
        target: panel.profileController
        function onProfilesChanged() {
            panel.dataRevision += 1
        }
    }

    function openEditor(profileId) {
        editingProfileId = profileId
        currentIndex = 1
    }

    function modelIndexFor(modelId) {
        for (let i = 0; i < modelOptions.length; ++i) {
            if (modelOptions[i].modelId === modelId) {
                return i
            }
        }
        return modelOptions.length > 0 ? 0 : -1
    }

    function languageIndexFor(value) {
        const options = languageModel
        for (let i = 0; i < options.length; ++i) {
            if (options[i].value === value) {
                return i
            }
        }
        return 0
    }

    function setField(field, value) {
        if (!hasProfilesController || editingProfileId.length === 0) {
            return
        }
        if (!profileController.SetProfileField(editingProfileId, field, value)) {
            messageRequested(qsTr("The field value could not be saved."))
        }
    }

    readonly property var languageModel: [
        { label: qsTr("Follow app language"), value: "follow" },
        { label: qsTr("English"), value: "en" },
        { label: qsTr("中文"), value: "zh" }
    ]

    Item {
        id: listPage

        ColumnLayout {
            anchors.fill: parent
            anchors.leftMargin: 34
            anchors.rightMargin: 34
            anchors.topMargin: 26
            anchors.bottomMargin: 26
            spacing: 18

            SettingsSection {
                Layout.fillWidth: true
                title: qsTr("Output language")

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12

                    ComboBox {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 42
                        enabled: panel.hasProfilesController
                        model: panel.languageModel
                        textRole: "label"
                        currentIndex: panel.languageIndexFor(panel.hasProfilesController ? panel.profileController.outputLanguage : "follow")
                        onActivated: function(index) {
                            if (panel.hasProfilesController) {
                                panel.profileController.SetOutputLanguage(model[index].value)
                            }
                        }
                    }

                    Button {
                        id: addButton
                        Layout.preferredWidth: 116
                        Layout.preferredHeight: 42
                        text: qsTr("Add")
                        enabled: panel.hasProfilesController
                        Material.foreground: panel.textColor
                        onClicked: addDialog.open()
                    }
                }
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: 1
                color: panel.dividerColor
            }

            Item {
                Layout.fillWidth: true
                Layout.fillHeight: true

                ListView {
                    id: cardList
                    anchors.fill: parent
                    clip: true
                    spacing: 12
                    boundsBehavior: Flickable.StopAtBounds
                    model: panel.profiles

                    delegate: Rectangle {
                        width: ListView.view.width
                        height: 92
                        radius: 8
                        color: modelData.active
                               ? Qt.rgba(panel.primaryAccent.r, panel.primaryAccent.g, panel.primaryAccent.b, 0.14)
                               : (cardMouse.containsMouse ? Qt.rgba(1, 1, 1, 0.06) : Qt.rgba(0, 0, 0, 0.16))
                        border.width: 1
                        border.color: modelData.active
                                      ? Qt.rgba(panel.secondaryAccent.r, panel.secondaryAccent.g, panel.secondaryAccent.b, 0.72)
                                      : Qt.rgba(1, 1, 1, 0.08)

                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 18
                            anchors.rightMargin: 14
                            spacing: 14

                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 5

                                Label {
                                    Layout.fillWidth: true
                                    text: modelData.displayName
                                    color: panel.textColor
                                    font.pixelSize: 16
                                    font.weight: 800
                                    elide: Text.ElideRight
                                }

                                Label {
                                    Layout.fillWidth: true
                                    text: modelData.baseUrl
                                    color: panel.mutedTextColor
                                    font.family: panel.dataFontFamily
                                    font.pixelSize: 12
                                    elide: Text.ElideMiddle
                                }

                                Label {
                                    Layout.fillWidth: true
                                    text: modelData.modelDisplayName && modelData.modelDisplayName.length > 0
                                          ? modelData.modelDisplayName : modelData.modelId
                                    color: panel.secondaryAccent
                                    font.family: panel.dataFontFamily
                                    font.pixelSize: 11
                                    elide: Text.ElideRight
                                }
                            }

                            Button {
                                Layout.preferredWidth: 96
                                Layout.preferredHeight: 38
                                text: modelData.active ? qsTr("✓ In use") : qsTr("Use")
                                enabled: !modelData.active && panel.hasProfilesController
                                Material.foreground: panel.textColor
                                onClicked: panel.profileController.SetActiveProfile(modelData.uuid)
                            }

                            Button {
                                Layout.preferredWidth: 82
                                Layout.preferredHeight: 38
                                text: qsTr("Edit")
                                Material.foreground: panel.textColor
                                onClicked: panel.openEditor(modelData.uuid)
                            }
                        }

                        MouseArea {
                            id: cardMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            acceptedButtons: Qt.NoButton
                        }
                    }
                }

                ColumnLayout {
                    anchors.centerIn: parent
                    width: Math.min(parent.width - 40, 420)
                    visible: panel.profiles.length === 0
                    spacing: 12

                    Label {
                        Layout.fillWidth: true
                        text: qsTr("No provider profiles")
                        color: panel.textColor
                        font.pixelSize: 18
                        font.weight: 800
                        horizontalAlignment: Text.AlignHCenter
                    }

                    Button {
                        Layout.alignment: Qt.AlignHCenter
                        Layout.preferredWidth: 170
                        Layout.preferredHeight: 42
                        text: qsTr("Add provider")
                        enabled: panel.hasProfilesController
                        Material.foreground: panel.textColor
                        onClicked: addDialog.open()
                    }
                }
            }
        }

        Dialog {
            id: addDialog
            parent: Overlay.overlay
            modal: true
            focus: true
            closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside
            x: parent ? Math.round((parent.width - width) / 2) : 0
            y: parent ? Math.round((parent.height - height) / 2) : 0
            width: Math.min((parent ? parent.width : 620) - 72, 560)
            title: qsTr("Add provider")

            background: Rectangle {
                radius: 12
                color: Qt.rgba(46 / 255, 46 / 255, 46 / 255, 0.98)
                border.width: 1
                border.color: panel.dividerColor
            }

            contentItem: ColumnLayout {
                spacing: 10

                Repeater {
                    model: panel.templates
                    delegate: Button {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 52
                        text: modelData.label
                        Material.foreground: panel.textColor
                        onClicked: {
                            const id = panel.profileController.AddProfileFromTemplate(modelData.templateId)
                            addDialog.close()
                            if (id.length > 0) {
                                panel.openEditor(id)
                            }
                        }
                    }
                }
            }
        }
    }

    Item {
        id: editPage

        ColumnLayout {
            anchors.fill: parent
            spacing: 0

            RowLayout {
                Layout.fillWidth: true
                Layout.preferredHeight: 74
                Layout.leftMargin: 24
                Layout.rightMargin: 28
                spacing: 14

                Button {
                    Layout.preferredWidth: 42
                    Layout.preferredHeight: 42
                    flat: true
                    text: "‹"
                    font.pixelSize: 30
                    Material.foreground: panel.mutedTextColor
                    onClicked: panel.currentIndex = 0
                }

                Label {
                    Layout.fillWidth: true
                    text: panel.editProfile.displayName || qsTr("Provider")
                    color: panel.textColor
                    font.pixelSize: 22
                    font.weight: 800
                    elide: Text.ElideRight
                }
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: 1
                color: panel.dividerColor
            }

            ScrollView {
                id: editScroll
                Layout.fillWidth: true
                Layout.fillHeight: true
                contentWidth: availableWidth
                clip: true

                ColumnLayout {
                    width: editScroll.availableWidth
                    spacing: 18

                    SettingsSection {
                        Layout.fillWidth: true
                        Layout.topMargin: 20
                        Layout.leftMargin: 34
                        Layout.rightMargin: 34
                        title: qsTr("API key")

                        TextField {
                            id: keyField
                            Layout.fillWidth: true
                            Layout.preferredHeight: 58
                            echoMode: TextInput.Password
                            placeholderText: qsTr("Paste API key")
                            color: panel.textColor
                            enabled: panel.hasProfilesController
                            font.family: panel.dataFontFamily
                            selectByMouse: true
                        }

                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 12

                            Button {
                                Layout.preferredHeight: 38
                                text: qsTr("Save Key")
                                enabled: panel.hasProfilesController && keyField.text.length > 0
                                Material.foreground: panel.textColor
                                onClicked: {
                                    const err = panel.profileController.SaveApiKey(panel.editingProfileId, keyField.text)
                                    keyField.text = ""
                                    panel.messageRequested(err.length > 0 ? err : qsTr("API key saved"))
                                }
                            }

                            Button {
                                Layout.preferredHeight: 38
                                text: qsTr("Delete Key")
                                enabled: panel.editProfile.credentialAvailable
                                Material.foreground: panel.dangerColor
                                onClicked: {
                                    panel.profileController.DeleteApiKey(panel.editingProfileId)
                                    panel.messageRequested(qsTr("API key deleted"))
                                }
                            }

                            Label {
                                Layout.fillWidth: true
                                text: panel.editProfile.credentialAvailable
                                      ? (panel.editProfile.maskedKeyLabel && panel.editProfile.maskedKeyLabel.length > 0
                                         ? panel.editProfile.maskedKeyLabel : qsTr("Key saved"))
                                      : qsTr("No key saved")
                                color: panel.editProfile.credentialAvailable ? panel.secondaryAccent : panel.mutedTextColor
                                font.family: panel.dataFontFamily
                                font.pixelSize: 12
                                elide: Text.ElideRight
                            }
                        }
                    }

                    SettingsSection {
                        Layout.fillWidth: true
                        Layout.leftMargin: 34
                        Layout.rightMargin: 34
                        title: qsTr("Model")

                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 12

                            ComboBox {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 42
                                enabled: panel.modelOptions.length > 0
                                model: panel.modelOptions
                                textRole: "displayName"
                                currentIndex: panel.modelIndexFor(panel.editProfile.modelId || "")
                                onActivated: function(index) {
                                    const item = model[index]
                                    if (item) {
                                        panel.setField("modelId", item.modelId)
                                        panel.setField("modelDisplayName", item.displayName)
                                    }
                                }
                            }

                            Button {
                                Layout.preferredWidth: 178
                                Layout.preferredHeight: 42
                                text: qsTr("Test & Refresh")
                                enabled: panel.hasAnalysis && panel.editingProfileId.length > 0
                                         && (panel.editProfile.credentialAvailable || panel.editProfile.authType === "none")
                                Material.foreground: panel.textColor
                                onClicked: panel.analysisController.ValidateConnectionForProfile(panel.editingProfileId)
                            }
                        }

                        Label {
                            Layout.fillWidth: true
                            visible: panel.hasAnalysis && (panel.analysisController.connectionStatus.length > 0
                                      || panel.analysisController.lastError.length > 0)
                            text: panel.hasAnalysis
                                  ? (panel.analysisController.lastError.length > 0
                                     ? panel.analysisController.lastError
                                     : panel.analysisController.connectionStatus)
                                  : ""
                            color: panel.hasAnalysis && panel.analysisController.lastError.length > 0 ? panel.dangerColor : panel.secondaryAccent
                            font.pixelSize: 12
                            wrapMode: Text.WordWrap
                        }
                    }

                    SettingsSection {
                        Layout.fillWidth: true
                        Layout.leftMargin: 34
                        Layout.rightMargin: 34
                        title: qsTr("Advanced")

                        GridLayout {
                            Layout.fillWidth: true
                            columns: panel.width > 760 ? 2 : 1
                            columnSpacing: 12
                            rowSpacing: 10

                            AdvancedField { label: qsTr("Display name"); field: "displayName"; value: panel.editProfile.displayName || "" }
                            AdvancedField { label: qsTr("Provider id"); field: "providerId"; value: panel.editProfile.providerId || "" }
                            AdvancedField { label: qsTr("Driver"); field: "driver"; value: panel.editProfile.driver || "" }
                            AdvancedField { label: qsTr("Base URL"); field: "baseUrl"; value: panel.editProfile.baseUrl || "" }
                            AdvancedField { label: qsTr("Endpoint"); field: "endpoint"; value: panel.editProfile.endpoint || "" }
                            AdvancedField { label: qsTr("Models endpoint"); field: "modelsEndpoint"; value: panel.editProfile.modelsEndpoint || "" }
                            AdvancedField { label: qsTr("Auth type"); field: "authType"; value: panel.editProfile.authType || "" }
                            AdvancedField { label: qsTr("Credential slot"); field: "credentialSlot"; value: panel.editProfile.credentialSlot || "" }
                            AdvancedField { label: qsTr("Structured output"); field: "structuredOutputMode"; value: panel.editProfile.structuredOutputMode || "" }
                            AdvancedField { label: qsTr("Timeout ms"); field: "timeoutMs"; value: String(panel.editProfile.timeoutMs || 60000); numeric: true }
                            AdvancedField { label: qsTr("Max image bytes"); field: "maxImageBytes"; value: String(panel.editProfile.maxImageBytes || 4194304); numeric: true }
                            AdvancedField { label: qsTr("Recommended rendition"); field: "recommendedRendition"; value: panel.editProfile.recommendedRendition || "preview" }
                        }
                    }

                    RowLayout {
                        Layout.fillWidth: true
                        Layout.leftMargin: 34
                        Layout.rightMargin: 34
                        Layout.bottomMargin: 28
                        spacing: 12

                        Button {
                            Layout.preferredHeight: 40
                            text: qsTr("Duplicate")
                            enabled: panel.hasProfilesController && panel.editingProfileId.length > 0
                            Material.foreground: panel.textColor
                            onClicked: {
                                const id = panel.profileController.CloneProfile(panel.editingProfileId)
                                if (id.length > 0) {
                                    panel.openEditor(id)
                                }
                            }
                        }

                        Button {
                            Layout.preferredHeight: 40
                            text: qsTr("Delete")
                            enabled: panel.hasProfilesController && panel.editingProfileId.length > 0
                            Material.foreground: panel.dangerColor
                            onClicked: deleteDialog.open()
                        }

                        Item { Layout.fillWidth: true }
                    }
                }
            }
        }

        Dialog {
            id: deleteDialog
            parent: Overlay.overlay
            modal: true
            focus: true
            title: qsTr("Delete provider")
            x: parent ? Math.round((parent.width - width) / 2) : 0
            y: parent ? Math.round((parent.height - height) / 2) : 0
            width: Math.min((parent ? parent.width : 520) - 72, 460)

            background: Rectangle {
                radius: 12
                color: Qt.rgba(46 / 255, 46 / 255, 46 / 255, 0.98)
                border.width: 1
                border.color: panel.dividerColor
            }

            contentItem: ColumnLayout {
                spacing: 14

                Label {
                    Layout.fillWidth: true
                    text: qsTr("Delete this provider profile?")
                    color: panel.textColor
                    font.pixelSize: 14
                    wrapMode: Text.WordWrap
                }

                CheckBox {
                    id: wipeKeyCheck
                    checked: true
                    text: qsTr("Delete saved key")
                    Material.foreground: panel.textColor
                }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    Item { Layout.fillWidth: true }

                    Button {
                        text: qsTr("Cancel")
                        Material.foreground: panel.textColor
                        onClicked: deleteDialog.close()
                    }

                    Button {
                        text: qsTr("Delete")
                        Material.foreground: panel.dangerColor
                        onClicked: {
                            panel.profileController.DeleteProfile(panel.editingProfileId, wipeKeyCheck.checked)
                            deleteDialog.close()
                            panel.editingProfileId = ""
                            panel.currentIndex = 0
                        }
                    }
                }
            }
        }
    }

    component SettingsSection: ColumnLayout {
        property string title: ""
        spacing: 12

        Label {
            Layout.fillWidth: true
            text: title
            color: panel.textColor
            font.pixelSize: 18
            font.weight: 800
        }
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: panel.dividerColor
        }
    }

    component AdvancedField: ColumnLayout {
        property string label: ""
        property string field: ""
        property string value: ""
        property bool numeric: false

        Layout.fillWidth: true
        Layout.minimumWidth: 220
        spacing: 5

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
            selectByMouse: true
            onEditingFinished: {
                panel.setField(field, numeric ? parseInt(text, 10) : text)
            }
        }
    }
}