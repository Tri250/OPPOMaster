import QtQml

QtObject {
    id: root

    property var exportQueueById: ({})
    property var exportPreviewRows: []
    readonly property int exportQueueCount: Object.keys(exportQueueById).length

    // P2-12: Enhanced queue state
    property bool isPaused: false
    property var perItemProgress: ({})       // key -> {progress: 0-1, status: "queued"/"running"/"succeeded"/"failed"/"cancelled", fileSize: 0}
    property var exportStartTime: null
    property var estimatedTimeRemaining: null
    property int completedCount: 0
    property int failedCount: 0
    property int totalBytesWritten: 0

    function keyForElement(elementId) {
        return String(Number(elementId))
    }

    function exportStatusKey(elementId, imageId) {
        return String(Number(elementId)) + ":" + String(Number(imageId))
    }

    function addTargets(items) {
        if (!items || items.length === 0) {
            return
        }

        const next = Object.assign({}, exportQueueById)
        for (let i = 0; i < items.length; ++i) {
            const item = items[i]
            const key = keyForElement(item.elementId)
            next[key] = {
                elementId: Number(item.elementId),
                imageId: Number(item.imageId),
                fileName: item.fileName ? item.fileName : qsTr("(unnamed)"),
                isHdr: item.isHdr === true
            }
            // Initialize per-item progress
            if (!perItemProgress[key]) {
                perItemProgress[key] = {
                    progress: 0.0,
                    status: "queued",
                    fileSize: 0,
                    thumbnail: ""
                }
            }
        }
        exportQueueById = next
        refreshExportPreview()
    }

    function clearQueue() {
        exportQueueById = ({})
        perItemProgress = ({})
        isPaused = false
        exportStartTime = null
        estimatedTimeRemaining = null
        completedCount = 0
        failedCount = 0
        totalBytesWritten = 0
        refreshExportPreview()
    }

    function pruneDeletedElements(elementIds) {
        if (!elementIds || elementIds.length === 0) {
            return
        }

        const deleted = {}
        for (let i = 0; i < elementIds.length; ++i) {
            deleted[keyForElement(elementIds[i])] = true
        }

        const nextQueue = {}
        const nextProgress = {}
        const queueRows = Object.values(exportQueueById)
        for (let i = 0; i < queueRows.length; ++i) {
            const row = queueRows[i]
            const key = keyForElement(row.elementId)
            if (!deleted[key]) {
                nextQueue[key] = row
                if (perItemProgress[key]) {
                    nextProgress[key] = perItemProgress[key]
                }
            }
        }
        exportQueueById = nextQueue
        perItemProgress = nextProgress
        refreshExportPreview()
    }

    function pruneCompleted(statusMap) {
        if (!statusMap) {
            return
        }

        const rows = Object.values(exportQueueById)
        if (rows.length === 0) {
            return
        }

        let removed = 0
        const next = {}
        const nextProgress = {}
        for (let i = 0; i < rows.length; ++i) {
            const row = rows[i]
            const status = String(statusMap[exportStatusKey(row.elementId, row.imageId)] || "")
            if (status === "succeeded" || status === "failed") {
                removed += 1
                continue
            }
            const key = keyForElement(row.elementId)
            next[key] = row
            if (perItemProgress[key]) {
                nextProgress[key] = perItemProgress[key]
            }
        }

        if (removed > 0) {
            exportQueueById = next
            perItemProgress = nextProgress
            refreshExportPreview()
        }
    }

    function exportQueueTargets() {
        const rows = Object.values(exportQueueById)
        const targets = []
        for (let i = 0; i < rows.length; ++i) {
            targets.push({
                elementId: rows[i].elementId,
                imageId: rows[i].imageId,
                isHdr: rows[i].isHdr === true
            })
        }
        return targets
    }

    function hasHdrItems() {
        const rows = Object.values(exportQueueById)
        for (let i = 0; i < rows.length; ++i) {
            if (rows[i].isHdr === true) {
                return true
            }
        }
        return false
    }

    // P2-12: Update per-item progress
    function updateItemProgress(elementId, progress, status, fileSize) {
        const key = keyForElement(elementId)
        const next = Object.assign({}, perItemProgress)
        next[key] = {
            progress: progress !== undefined ? progress : (next[key] ? next[key].progress : 0),
            status: status !== undefined ? status : (next[key] ? next[key].status : "queued"),
            fileSize: fileSize !== undefined ? fileSize : (next[key] ? next[key].fileSize : 0),
            thumbnail: next[key] ? next[key].thumbnail : ""
        }
        perItemProgress = next
        updateEstimatedTime()
    }

    // P2-12: Move item up in queue
    function moveItemUp(elementId) {
        const keys = Object.keys(exportQueueById)
        const idx = keys.indexOf(keyForElement(elementId))
        if (idx <= 0) return
        // Swap
        const prevKey = keys[idx - 1]
        const temp = exportQueueById[keys[idx]]
        const next = Object.assign({}, exportQueueById)
        next[keys[idx]] = next[prevKey]
        next[prevKey] = temp
        exportQueueById = next
        refreshExportPreview()
    }

    // P2-12: Move item down in queue
    function moveItemDown(elementId) {
        const keys = Object.keys(exportQueueById)
        const idx = keys.indexOf(keyForElement(elementId))
        if (idx < 0 || idx >= keys.length - 1) return
        const nextKey = keys[idx + 1]
        const next = Object.assign({}, exportQueueById)
        const temp = next[keys[idx]]
        next[keys[idx]] = next[nextKey]
        next[nextKey] = temp
        exportQueueById = next
        refreshExportPreview()
    }

    // P2-12: Cancel a single item
    function cancelItem(elementId) {
        const key = keyForElement(elementId)
        const nextProgress = Object.assign({}, perItemProgress)
        if (nextProgress[key]) {
            nextProgress[key] = Object.assign({}, nextProgress[key], {status: "cancelled", progress: 0})
        }
        perItemProgress = nextProgress

        const next = Object.assign({}, exportQueueById)
        delete next[key]
        exportQueueById = next
        refreshExportPreview()
    }

    // P2-12: Toggle pause/resume
    function togglePause() {
        isPaused = !isPaused
    }

    // P2-12: Update estimated time remaining
    function updateEstimatedTime() {
        if (!exportStartTime) {
            estimatedTimeRemaining = null
            return
        }
        const now = new Date()
        const elapsed = (now.getTime() - exportStartTime.getTime()) / 1000
        const rows = Object.values(perItemProgress)
        let completed = 0
        let total = rows.length
        for (let i = 0; i < rows.length; ++i) {
            if (rows[i].status === "succeeded" || rows[i].status === "failed" || rows[i].status === "cancelled") {
                completed++
            }
        }
        if (completed === 0 || total === 0) {
            estimatedTimeRemaining = null
            return
        }
        const rate = completed / elapsed
        const remaining = (total - completed) / rate
        estimatedTimeRemaining = Math.ceil(remaining)
    }

    // P2-12: Get completion summary
    function getCompletionSummary() {
        const rows = Object.values(perItemProgress)
        let succeeded = 0
        let failed = 0
        let totalSize = 0
        for (let i = 0; i < rows.length; ++i) {
            if (rows[i].status === "succeeded") {
                succeeded++
                totalSize += rows[i].fileSize || 0
            } else if (rows[i].status === "failed") {
                failed++
            }
        }
        return {
            totalFiles: rows.length,
            succeeded: succeeded,
            failed: failed,
            totalSize: totalSize
        }
    }

    // P2-12: Format bytes for display
    function formatBytes(bytes) {
        if (bytes < 1024) return bytes + " B"
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB"
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + " MB"
        return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB"
    }

    // P2-12: Format seconds for display
    function formatTime(seconds) {
        if (seconds < 60) return seconds + "s"
        if (seconds < 3600) return Math.floor(seconds / 60) + "m " + (seconds % 60) + "s"
        return Math.floor(seconds / 3600) + "h " + Math.floor((seconds % 3600) / 60) + "m"
    }

    function refreshExportPreview() {
        const src = Object.values(exportQueueById)
        if (src.length <= 200) {
            src.sort((a, b) => String(a.fileName).localeCompare(String(b.fileName)))
        }
        const next = []
        for (let i = 0; i < src.length; ++i) {
            const item = src[i]
            const key = keyForElement(item.elementId)
            const progress = perItemProgress[key] || {}
            next.push({
                statusKey: exportStatusKey(item.elementId, item.imageId),
                summaryRow: false,
                label: item.fileName ? item.fileName : qsTr("(unnamed)"),
                isHdr: item.isHdr === true,
                // P2-12: Enhanced fields
                elementId: item.elementId,
                itemProgress: progress.progress || 0,
                itemStatus: progress.status || "queued",
                itemFileSize: progress.fileSize || 0,
                itemThumbnail: progress.thumbnail || ""
            })
        }
        exportPreviewRows = next
    }
}
