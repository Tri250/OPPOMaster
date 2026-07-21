import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Dialogs

Dialog {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    width: Math.min(parent ? parent.width - 40 : 480, 480)
    height: Math.min(parent ? parent.height - 40 : 420, 420)
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    padding: 0
    closePolicy: Popup.CloseOnEscape

    property var credentialPortability: null

    readonly property color panelColor: appTheme.bgPanelColor
    readonly property color cardColor: appTheme.bgBaseColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color dangerColor: appTheme.dangerColor
    readonly property string dataFontFamily: appTheme.dataFontFamily
    readonly property string headlineFontFamily: appTheme.headlineFontFamily

    property int currentStep: 0  // 0: mode select, 1: export password, 2: import file, 3: import password, 4: conflict, 5: done
    property string statusMessage: ""
    property bool isExportMode: true
    property string selectedFilePath: ""
    property int entriesProcessed: 0
    property int entriesSkipped: 0

    signal exportCompleted(int entriesProcessed)
    signal importCompleted(int entriesProcessed, int entriesSkipped)

    function reset() {
        currentStep = 0
        statusMessage = ""
        isExportMode = true
        selectedFilePath = ""
        entriesProcessed = 0
        entriesSkipped = 0
        passwordField.text = ""
        confirmPasswordField.text = ""
    }

    onOpened: reset()

    background: Rectangle {
        radius: 14
        color: root.panelColor
        border.width: 1
        border.color: Qt.rgba(1, 1, 1, 0.06)
    }

    contentItem: ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // Header
        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 22
            Layout.leftMargin: 24
            Layout.rightMargin: 22
            Layout.bottomMargin: 14
            spacing: 12

            Label {
                text: qsTr("Credential Import / Export")
                font.family: root.headlineFontFamily
                font.pixelSize: 20
                font.weight: Font.Medium
                color: root.textColor
            }

            Item { Layout.fillWidth: true }

            Button {
                text: "✕"
                flat: true
                onClicked: root.close()
                implicitWidth: 28
                implicitHeight: 28
                font.pixelSize: 13
                opacity: hovered ? 1.0 : 0.55
                Material.foreground: root.textColor
            }
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: Qt.rgba(1, 1, 1, 0.08)
        }

        // Step: Mode selection
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 24
            visible: root.currentStep === 0
            spacing: 16

            Label {
                Layout.fillWidth: true
                text: qsTr("Transfer your AI provider credentials between devices. Credentials are encrypted with a password you choose.")
                color: root.mutedTextColor
                font.pixelSize: 13
                wrapMode: Text.WordWrap
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 10

                Button {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 56
                    text: qsTr("Export Credentials")
                    onClicked: {
                        root.isExportMode = true
                        root.currentStep = 1
                    }
                    Material.foreground: root.textColor
                    background: Rectangle {
                        radius: 10
                        color: parent.down ? Qt.darker("#457B9D", 1.18)
                             : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                              : "#457B9D"
                        border.width: 1
                        border.color: Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.18)
                    }
                }

                Button {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 56
                    text: qsTr("Import Credentials")
                    onClicked: {
                        root.isExportMode = false
                        root.currentStep = 2
                    }
                    Material.foreground: root.textColor
                    background: Rectangle {
                        radius: 10
                        color: parent.down ? Qt.rgba(1, 1, 1, 0.06)
                             : parent.hovered ? Qt.rgba(1, 1, 1, 0.12)
                                              : Qt.rgba(1, 1, 1, 0.07)
                        border.width: 1
                        border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                    }
                }
            }
        }

        // Step: Export password
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 24
            visible: root.currentStep === 1 && root.isExportMode
            spacing: 16

            Label {
                Layout.fillWidth: true
                text: qsTr("Set a password to encrypt your credentials. You'll need this password to import them on another device.")
                color: root.mutedTextColor
                font.pixelSize: 13
                wrapMode: Text.WordWrap
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                Label {
                    text: qsTr("Password")
                    color: root.mutedTextColor
                    font.pixelSize: 11
                }

                TextField {
                    id: passwordField
                    Layout.fillWidth: true
                    echoMode: TextInput.Password
                    placeholderText: qsTr("Enter encryption password")
                    font.family: root.dataFontFamily
                    font.pixelSize: 12
                }
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                Label {
                    text: qsTr("Confirm Password")
                    color: root.mutedTextColor
                    font.pixelSize: 11
                }

                TextField {
                    id: confirmPasswordField
                    Layout.fillWidth: true
                    echoMode: TextInput.Password
                    placeholderText: qsTr("Re-enter password")
                    font.family: root.dataFontFamily
                    font.pixelSize: 12
                }

                Label {
                    visible: passwordField.text.length > 0 && confirmPasswordField.text.length > 0
                              && passwordField.text !== confirmPasswordField.text
                    text: qsTr("Passwords do not match")
                    color: root.dangerColor
                    font.pixelSize: 11
                }
            }
        }

        // Step: Import file selection
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 24
            visible: root.currentStep === 2 && !root.isExportMode
            spacing: 16

            Label {
                Layout.fillWidth: true
                text: qsTr("Select a credential bundle file (.alcedo_cred) to import.")
                color: root.mutedTextColor
                font.pixelSize: 13
                wrapMode: Text.WordWrap
            }

            RowLayout {
                Layout.fillWidth: true
                spacing: 8

                TextField {
                    id: importFilePathField
                    Layout.fillWidth: true
                    placeholderText: qsTr("Select file...")
                    text: root.selectedFilePath
                    font.family: root.dataFontFamily
                    font.pixelSize: 12
                    readOnly: true
                }

                Button {
                    text: qsTr("Browse")
                    onClicked: importFileDialog.open()
                    Material.foreground: root.textColor
                    background: Rectangle {
                        radius: 8
                        color: parent.down ? Qt.rgba(1, 1, 1, 0.06)
                             : parent.hovered ? Qt.rgba(1, 1, 1, 0.12)
                                              : Qt.rgba(1, 1, 1, 0.07)
                        border.width: 1
                        border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                    }
                }
            }

            Label {
                Layout.fillWidth: true
                visible: root.selectedFilePath.length > 0
                text: qsTr("File selected. Enter the password to decrypt.")
                color: root.accentColor
                font.pixelSize: 12
            }
        }

        // Step: Import password + conflict resolution
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 24
            visible: root.currentStep === 3 && !root.isExportMode
            spacing: 16

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                Label {
                    text: qsTr("Decryption Password")
                    color: root.mutedTextColor
                    font.pixelSize: 11
                }

                TextField {
                    id: importPasswordField
                    Layout.fillWidth: true
                    echoMode: TextInput.Password
                    placeholderText: qsTr("Enter the password used during export")
                    font.family: root.dataFontFamily
                    font.pixelSize: 12
                }
            }

            Label {
                Layout.fillWidth: true
                text: qsTr("If a credential already exists:")
                color: root.mutedTextColor
                font.pixelSize: 12
            }

            RadioButton {
                id: skipExistingRadio
                text: qsTr("Skip existing credentials")
                checked: true
                font.pixelSize: 12
            }

            RadioButton {
                id: overwriteExistingRadio
                text: qsTr("Overwrite existing credentials")
                font.pixelSize: 12
            }
        }

        // Step: Done
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 24
            visible: root.currentStep === 5
            spacing: 16

            Label {
                Layout.fillWidth: true
                text: root.isExportMode
                      ? qsTr("Export completed successfully! %1 credential(s) exported.").arg(root.entriesProcessed)
                      : qsTr("Import completed! %1 credential(s) imported, %2 skipped.").arg(root.entriesProcessed).arg(root.entriesSkipped)
                color: root.textColor
                font.pixelSize: 14
                wrapMode: Text.WordWrap
            }

            Label {
                Layout.fillWidth: true
                visible: root.statusMessage.length > 0
                text: root.statusMessage
                color: root.dangerColor
                font.pixelSize: 12
                wrapMode: Text.WordWrap
            }
        }

        // Footer
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: Qt.rgba(1, 1, 1, 0.08)
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 14
            Layout.bottomMargin: 14
            Layout.leftMargin: 24
            Layout.rightMargin: 22
            spacing: 12

            Button {
                text: qsTr("Back")
                visible: root.currentStep > 0 && root.currentStep < 5
                onClicked: {
                    if (root.currentStep === 3) root.currentStep = 2
                    else if (root.currentStep === 1 || root.currentStep === 2) root.currentStep = 0
                }
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.down ? Qt.rgba(1, 1, 1, 0.06)
                         : parent.hovered ? Qt.rgba(1, 1, 1, 0.12)
                                          : Qt.rgba(1, 1, 1, 0.07)
                    border.width: 1
                    border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                }
            }

            Item { Layout.fillWidth: true }

            Button {
                visible: root.currentStep === 0
                text: qsTr("Cancel")
                onClicked: root.close()
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: Qt.rgba(1, 1, 1, 0.07)
                    border.width: 1
                    border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                }
            }

            Button {
                highlighted: true
                visible: root.currentStep === 1 && root.isExportMode
                text: qsTr("Export")
                enabled: passwordField.text.length >= 6 && passwordField.text === confirmPasswordField.text
                onClicked: {
                    exportFileDialog.open()
                }
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.enabled
                           ? (parent.down ? Qt.darker("#457B9D", 1.18)
                              : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                               : "#457B9D")
                           : Qt.rgba("#457B9D".r, "#457B9D".g, "#457B9D".b, 0.45)
                }
            }

            Button {
                highlighted: true
                visible: root.currentStep === 2 && !root.isExportMode
                text: qsTr("Next")
                enabled: root.selectedFilePath.length > 0
                onClicked: root.currentStep = 3
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.enabled
                           ? (parent.down ? Qt.darker("#457B9D", 1.18)
                              : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                               : "#457B9D")
                           : Qt.rgba(0.27, 0.48, 0.62, 0.45)
                }
            }

            Button {
                highlighted: true
                visible: root.currentStep === 3 && !root.isExportMode
                text: qsTr("Import")
                enabled: importPasswordField.text.length >= 1
                onClicked: {
                    if (root.credentialPortability) {
                        var result = root.credentialPortability.ImportFromFile(
                            root.selectedFilePath,
                            importPasswordField.text,
                            overwriteExistingRadio.checked
                        )
                        root.entriesProcessed = result.entriesProcessed
                        root.entriesSkipped = result.entriesSkipped
                        root.statusMessage = result.success ? "" : result.error
                        root.currentStep = 5
                        root.importCompleted(result.entriesProcessed, result.entriesSkipped)
                    }
                }
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.enabled
                           ? (parent.down ? Qt.darker("#457B9D", 1.18)
                              : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                               : "#457B9D")
                           : Qt.rgba(0.27, 0.48, 0.62, 0.45)
                }
            }

            Button {
                highlighted: true
                visible: root.currentStep === 5
                text: qsTr("Done")
                onClicked: root.close()
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.down ? Qt.darker("#457B9D", 1.18)
                         : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                          : "#457B9D"
                }
            }
        }
    }

    FileDialog {
        id: importFileDialog
        title: qsTr("Select Credential Bundle")
        nameFilters: ["Alcedo credential bundle (*.alcedo_cred)", "All files (*)"]
        onAccepted: {
            root.selectedFilePath = selectedFile.toString().replace("file://", "")
        }
    }

    FileDialog {
        id: exportFileDialog
        title: qsTr("Save Credential Bundle")
        fileMode: FileDialog.SaveFile
        nameFilters: ["Alcedo credential bundle (*.alcedo_cred)", "All files (*)"]
        defaultSuffix: "alcedo_cred"
        onAccepted: {
            if (root.credentialPortability) {
                var result = root.credentialPortability.ExportToFile(
                    selectedFile.toString().replace("file://", ""),
                    passwordField.text
                )
                root.entriesProcessed = result.entriesProcessed
                root.entriesSkipped = result.entriesSkipped
                root.statusMessage = result.success ? "" : result.error
                root.currentStep = 5
                root.exportCompleted(result.entriesProcessed)
            }
        }
    }
}
