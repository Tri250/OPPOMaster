import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts
import QtQuick.Effects

Dialog {
    id: root

    modal: true
    focus: true
    padding: 0
    width: Math.min(parent ? parent.width - 48 : 760, 760)
    height: Math.min(parent ? parent.height - 48 : 660, 660)
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    closePolicy: running ? Popup.NoAutoClose
                         : Popup.CloseOnEscape | Popup.CloseOnPressOutside
    font.family: appTheme.uiFontFamily

    property Item blurSource: null
    property var analysisController: null
    property var profileController: null
    property var backend: null
    property var selectionTargets: []
    property bool backendInteractive: false

    readonly property bool running: analysisController && analysisController.running
    readonly property int selectedImageCount: selectionTargets ? selectionTargets.length : 0
    readonly property int selectedTaskCount: (descriptionTask.checked ? 1 : 0)
                                           + ((ratingTask.checked || ratingReasonTask.checked) ? 1 : 0)
    readonly property int totalUnits: selectedImageCount * selectedTaskCount
    readonly property int controllerDone: analysisController
                                          ? Number(analysisController.analyzed)
                                            + Number(analysisController.failed)
                                            + Number(analysisController.canceled)
                                          : 0
    readonly property int completedUnits: Math.min(totalUnits, completedBefore + controllerDone)
    readonly property real progressValue: totalUnits > 0 ? completedUnits / totalUnits : 0
    readonly property string providerDisplay: profileController && profileController.activeDisplayName.length > 0
                                              ? profileController.activeDisplayName
                                              : qsTr("No provider selected")
    readonly property string modelDisplay: profileController && profileController.activeModelDisplayName.length > 0
                                           ? profileController.activeModelDisplayName
                                           : qsTr("No model selected")
    readonly property string outputLanguageDisplay: {
        const value = profileController ? String(profileController.outputLanguage) : "follow"
        if (value === "zh") {
            return qsTr("Chinese")
        }
        if (value === "en") {
            return qsTr("English")
        }
        return qsTr("Follow app language")
    }

    property int completedBefore: 0
    property int phaseIndex: -1
    property bool startedOnce: false
    property bool finalReady: false
    property bool cancelRequested: false
    property string finalSummary: ""
    property string localError: ""
    property var phaseQueue: []
    property var phaseTargets: ({})
    property int skippedUnits: 0

    signal messageRequested(string message)

    Overlay.modal: Item {
        anchors.fill: parent

        MultiEffect {
            anchors.fill: parent
            source: root.blurSource
            visible: root.blurSource !== null
            blurEnabled: true
            blur: 0.62
            blurMax: 64
            saturation: -0.2
            brightness: -0.08
        }

        Rectangle {
            anchors.fill: parent
            color: appTheme.overlayColor
        }

        MouseArea { anchors.fill: parent; hoverEnabled: true }
    }

    function openWithTargets(targets) {
        selectionTargets = targets ? targets : []
        resetSession()
        open()
    }

    function resetSession() {
        completedBefore = 0
        phaseIndex = -1
        startedOnce = false
        finalReady = false
        cancelRequested = false
        finalSummary = ""
        localError = ""
        phaseQueue = []
        phaseTargets = ({})
        skippedUnits = 0
    }

    function buildPhaseQueue() {
        const phases = []
        if (descriptionTask.checked) {
            phases.push("describe")
        }
        if (ratingTask.checked || ratingReasonTask.checked) {
            phases.push("score")
        }
        return phases
    }

    function hasExistingDescription(target) {
        if (!backend || !target) {
            return false
        }
        const result = backend.GetImageDescription(Number(target.elementId))
        return result && result.hasDescription === true
    }

    function hasExistingRating(target) {
        if (!backend || !target) {
            return false
        }
        const result = backend.GetImageRating(Number(target.elementId), Number(target.imageId))
        return result && result.success === true && Number(result.rating) > 0
    }

    function hasExistingReason(target) {
        if (!backend || !target) {
            return false
        }
        const result = backend.GetImageRatingReasons(Number(target.elementId))
        return result && result.hasReasons === true
    }

    function filteredTargetsForPhase(phase) {
        const out = []
        const source = selectionTargets ? selectionTargets : []
        for (let i = 0; i < source.length; ++i) {
            const target = source[i]
            if (phase === "describe" && !overwriteDescription.checked
                    && hasExistingDescription(target)) {
                continue
            }
            if (phase === "score") {
                const skipRating = ratingTask.checked && !overwriteRating.checked
                                   && hasExistingRating(target)
                const skipReason = ratingReasonTask.checked && !overwriteReason.checked
                                   && hasExistingReason(target)
                if (skipRating || skipReason) {
                    continue
                }
            }
            out.push(target)
        }
        return out
    }

    function taskLabel(task) {
        return task === "describe" ? qsTr("Description") : qsTr("Rating")
    }

    function controllerError() {
        return analysisController ? String(analysisController.lastError || "") : ""
    }

    function startAnalysis() {
        if (!analysisController || running) {
            return
        }
        if (!backendInteractive) {
            localError = qsTr("Open a project before running remote analysis.")
            return
        }
        if (selectedImageCount <= 0) {
            localError = qsTr("Select at least one image to analyze.")
            return
        }
        phaseQueue = buildPhaseQueue()
        if (phaseQueue.length === 0) {
            localError = qsTr("Choose at least one analysis task.")
            return
        }
        const nextPhaseTargets = ({})
        let nextSkippedUnits = 0
        for (let i = 0; i < phaseQueue.length; ++i) {
            const phase = phaseQueue[i]
            const targets = filteredTargetsForPhase(phase)
            nextPhaseTargets[phase] = targets
            nextSkippedUnits += Math.max(0, selectedImageCount - targets.length)
        }
        phaseTargets = nextPhaseTargets
        skippedUnits = nextSkippedUnits
        localError = ""
        startedOnce = true
        finalReady = false
        cancelRequested = false
        completedBefore = 0
        phaseIndex = 0
        startCurrentPhase()
    }

    function startCurrentPhase() {
        if (!analysisController || phaseIndex < 0 || phaseIndex >= phaseQueue.length) {
            finishAllPhases()
            return
        }
        const phase = phaseQueue[phaseIndex]
        const targets = phaseTargets[phase] ? phaseTargets[phase] : []
        if (targets.length === 0) {
            advanceAfterControllerStopped()
            return
        }
        if (phase === "describe") {
            analysisController.StartDescribeForTargets(targets)
        } else {
            analysisController.StartScoreForTargets(targets)
        }
    }

    function advanceAfterControllerStopped() {
        if (!startedOnce || finalReady || phaseIndex < 0) {
            return
        }
        const error = controllerError()
        const canceled = analysisController && Number(analysisController.canceled) > 0
        if (cancelRequested || canceled || error.length > 0) {
            finishAllPhases()
            return
        }
        completedBefore += selectedImageCount
        if (phaseIndex + 1 < phaseQueue.length) {
            phaseIndex += 1
            startCurrentPhase()
        } else {
            finishAllPhases()
        }
    }

    function finishAllPhases() {
        finalReady = true
        const ok = analysisController ? Number(analysisController.analyzed) : 0
        const failed = analysisController ? Number(analysisController.failed) : 0
        const canceled = analysisController ? Number(analysisController.canceled) : 0
        const error = controllerError()
        if (cancelRequested || canceled > 0) {
            finalSummary = qsTr("Canceled. Successful results already saved remain in place.")
        } else if (error.length > 0) {
            finalSummary = error
        } else if (failed > 0) {
            finalSummary = qsTr("Finished with %1 successful item(s) and %2 failed item(s).").arg(ok).arg(failed)
        } else if (skippedUnits > 0) {
            finalSummary = qsTr("Analysis complete. Skipped %1 existing item-task(s).").arg(skippedUnits)
        } else {
            finalSummary = qsTr("Analysis complete.")
        }
        phaseIndex = -1
    }

    function cancelAnalysis() {
        cancelRequested = true
        if (analysisController && analysisController.running) {
            analysisController.CancelAnalysis()
        }
    }

    onOpened: {
        if (analysisController) {
            analysisController.RefreshCredentialState()
        }
    }

    onClosing: function(close) {
        if (running) {
            close.accepted = false
        }
    }

    Connections {
        target: root.analysisController
        ignoreUnknownSignals: true

        function onStateChanged() {
            if (root.startedOnce && !root.running) {
                Qt.callLater(root.advanceAfterControllerStopped)
            }
        }
    }

    background: Rectangle {
        radius: appTheme.panelRadius
        color: appTheme.bgDeepColor
        border.width: 1
        border.color: appTheme.glassStrokeColor
    }

    contentItem: ColumnLayout {
        spacing: 0

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 86
            radius: appTheme.panelRadius
            color: appTheme.bgPanelColor

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 28
                anchors.rightMargin: 22
                spacing: 16

                Image {
                    Layout.preferredWidth: 28
                    Layout.preferredHeight: 28
                    source: "qrc:/panel_icons/flask.svg"
                    sourceSize.width: 28
                    sourceSize.height: 28
                }

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4

                    Label {
                        text: qsTr("Advanced Content Analysis")
                        color: appTheme.textColor
                        font.family: appTheme.headlineFontFamily
                        font.pixelSize: 24
                        font.weight: 700
                    }

                    Label {
                        text: qsTr("%1 selected image(s)").arg(root.selectedImageCount)
                        color: appTheme.textMutedColor
                        font.pixelSize: 13
                    }
                }

                Button {
                    text: qsTr("Close")
                    enabled: !root.running
                    visible: !root.running
                    onClicked: root.close()
                }
            }
        }

        ScrollView {
            id: analysisScroll
            Layout.fillWidth: true
            Layout.fillHeight: true
            contentWidth: availableWidth
            clip: true

            ColumnLayout {
                width: analysisScroll.availableWidth
                spacing: 18

                GridLayout {
                    Layout.fillWidth: true
                    Layout.topMargin: 24
                    Layout.leftMargin: 28
                    Layout.rightMargin: 28
                    columns: width > 560 ? 3 : 1
                    columnSpacing: 12
                    rowSpacing: 12

                    component SummaryTile: Rectangle {
                        property string label: ""
                        property string value: ""
                        Layout.fillWidth: true
                        Layout.preferredHeight: 76
                        radius: 8
                        color: appTheme.bgBaseColor
                        border.width: 1
                        border.color: appTheme.glassStrokeColor

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 12
                            spacing: 6
                            Label {
                                text: label
                                color: appTheme.textMutedColor
                                font.pixelSize: 11
                                font.weight: 700
                                elide: Text.ElideRight
                                Layout.fillWidth: true
                            }
                            Label {
                                text: value
                                color: appTheme.textColor
                                font.pixelSize: 15
                                font.weight: 700
                                elide: Text.ElideRight
                                Layout.fillWidth: true
                            }
                        }
                    }

                    SummaryTile { label: qsTr("Provider"); value: root.providerDisplay }
                    SummaryTile { label: qsTr("Model"); value: root.modelDisplay }
                    SummaryTile { label: qsTr("Output language"); value: root.outputLanguageDisplay }
                }

                Rectangle {
                    Layout.fillWidth: true
                    Layout.leftMargin: 28
                    Layout.rightMargin: 28
                    Layout.preferredHeight: taskColumn.implicitHeight + 26
                    radius: 8
                    color: appTheme.bgPanelColor
                    border.width: 1
                    border.color: appTheme.glassStrokeColor

                    ColumnLayout {
                        id: taskColumn
                        anchors.fill: parent
                        anchors.margins: 13
                        spacing: 12

                        Label {
                            text: qsTr("Tasks")
                            color: appTheme.textColor
                            font.pixelSize: 15
                            font.weight: 800
                        }

                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 14

                            CheckBox { id: descriptionTask; text: qsTr("Description"); checked: true; enabled: !root.running }
                            CheckBox { id: ratingTask; text: qsTr("Rating"); checked: true; enabled: !root.running }
                            CheckBox { id: ratingReasonTask; text: qsTr("Rating reason"); checked: true; enabled: !root.running }
                        }

                        Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 1; color: appTheme.dividerColor }

                        Label {
                            text: qsTr("Overwrite")
                            color: appTheme.textMutedColor
                            font.pixelSize: 12
                            font.weight: 700
                        }

                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 14

                            CheckBox { id: overwriteRating; text: qsTr("Overwrite photo rating"); checked: true; enabled: !root.running }
                            CheckBox { id: overwriteReason; text: qsTr("Overwrite rating reason"); checked: true; enabled: !root.running }
                            CheckBox { id: overwriteDescription; text: qsTr("Overwrite image description"); checked: true; enabled: !root.running }
                        }
                    }
                }

                Rectangle {
                    Layout.fillWidth: true
                    Layout.leftMargin: 28
                    Layout.rightMargin: 28
                    Layout.preferredHeight: 190
                    radius: 8
                    color: appTheme.bgBaseColor
                    border.width: 1
                    border.color: appTheme.glassStrokeColor

                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 18
                        spacing: 22

                        ImportProgressRing {
                            Layout.preferredWidth: 118
                            Layout.preferredHeight: 118
                            ringWidth: 11
                            progress: root.progressValue
                            indeterminate: root.running && root.totalUnits <= 0
                            fillColor: appTheme.accentColor
                            trackColor: appTheme.hoverColor
                        }

                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 8

                            Label {
                                text: root.running && root.phaseIndex >= 0
                                      ? qsTr("Running %1").arg(root.taskLabel(root.phaseQueue[root.phaseIndex]))
                                      : (root.finalReady ? qsTr("Finished") : qsTr("Ready"))
                                color: appTheme.textColor
                                font.pixelSize: 20
                                font.weight: 800
                            }

                            Label {
                                text: qsTr("%1 / %2 item-task(s)").arg(root.completedUnits).arg(root.totalUnits)
                                color: appTheme.textMutedColor
                                font.family: appTheme.dataFontFamily
                                font.pixelSize: 14
                            }

                            Label {
                                Layout.fillWidth: true
                                text: root.localError.length > 0
                                      ? root.localError
                                      : (root.finalReady
                                         ? root.finalSummary
                                         : (root.analysisController ? root.analysisController.statusText : ""))
                                color: root.localError.length > 0 || root.controllerError().length > 0
                                       ? appTheme.dangerColor
                                       : appTheme.textMutedColor
                                wrapMode: Text.WordWrap
                                font.pixelSize: 13
                            }

                            Label {
                                Layout.fillWidth: true
                                visible: root.analysisController && Number(root.analysisController.lastUsage.totalTokens || 0) > 0
                                text: qsTr("Usage: %1 token(s)").arg(root.analysisController
                                                                     ? Number(root.analysisController.lastUsage.totalTokens || 0)
                                                                     : 0)
                                color: appTheme.textMutedColor
                                font.pixelSize: 12
                            }
                        }
                    }
                }

                Rectangle {
                    Layout.fillWidth: true
                    Layout.leftMargin: 28
                    Layout.rightMargin: 28
                    Layout.preferredHeight: hintText.implicitHeight + 24
                    radius: 8
                    color: Qt.rgba(appTheme.accentColor.r, appTheme.accentColor.g, appTheme.accentColor.b, 0.10)
                    border.width: 1
                    border.color: Qt.rgba(appTheme.accentColor.r, appTheme.accentColor.g, appTheme.accentColor.b, 0.22)

                    Label {
                        id: hintText
                        anchors.fill: parent
                        anchors.margins: 12
                        text: qsTr("Results refresh the focused photo's Image inspector. Open the Image page to review and edit description, rating, and reasons.")
                        color: appTheme.textColor
                        wrapMode: Text.WordWrap
                        font.pixelSize: 13
                    }
                }

                Item { Layout.preferredHeight: 8 }
            }
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 74
            color: appTheme.bgPanelColor
            border.width: 1
            border.color: appTheme.glassStrokeColor

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 28
                anchors.rightMargin: 28
                spacing: 12

                Label {
                    Layout.fillWidth: true
                    text: root.running ? qsTr("Remote provider calls may incur cost.") : ""
                    color: appTheme.textMutedColor
                    font.pixelSize: 12
                }

                Button {
                    text: qsTr("Cancel")
                    visible: root.running
                    enabled: root.running
                    Material.background: appTheme.dangerColor
                    Material.foreground: appTheme.textColor
                    onClicked: root.cancelAnalysis()
                }

                Button {
                    text: qsTr("Analyze Selected")
                    visible: !root.running && !root.finalReady
                    enabled: root.backendInteractive && root.selectedImageCount > 0
                    Material.background: appTheme.accentColor
                    Material.foreground: appTheme.textColor
                    onClicked: root.startAnalysis()
                }

                Button {
                    text: qsTr("Close")
                    visible: !root.running && root.finalReady
                    onClicked: root.close()
                }
            }
        }
    }
}