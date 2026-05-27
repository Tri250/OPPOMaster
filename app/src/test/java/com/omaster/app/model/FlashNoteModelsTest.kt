package com.omaster.app.model

import android.net.Uri
import com.omaster.app.config.FlashNoteConstants
import org.junit.Assert.*
import org.junit.Test

class FlashNoteModelsTest {

    private val testCameraParams = CameraParams(
        mode = "master",
        filter = "复古",
        iso = 200,
        shutter = "1/250",
        ev = "+0.3",
        wb = "5600K",
        hasselblad_hncs = true,
        contrast = 1.1f,
        saturation = 1.2f,
        sharpness = 1.0f,
        vignette = 0.2f,
        videoLut = "",
        sceneTags = listOf("portrait", "indoor", "warm")
    )

    private val testPreset = Preset(
        id = "test_preset_1",
        name = "Test Preset",
        coverPath = "test_cover.png",
        sections = emptyList(),
        cameraParams = testCameraParams,
        deviceModel = "Find X8 Pro",
        source = "omaster_cloud",
        isFavorite = false,
        createdAt = 1234567890L,
        updatedAt = 1234567890L,
        usageCount = 100,
        rating = 4.5f,
        author = "OPPO"
    )

    @Test
    fun `FlashNoteData should have correct default values`() {
        val data = FlashNoteData(
            title = "Test Title",
            content = "Test Content"
        )

        assertEquals("Test Title", data.title)
        assertEquals("Test Content", data.content)
        assertEquals(FlashNoteConstants.CATEGORY_PRESET, data.category)
        assertTrue(data.tags.isEmpty())
        assertEquals(FlashNoteConstants.SOURCE_OPPO_MASTER, data.source)
        assertNull(data.attachmentUri)
        assertEquals(FlashNoteConstants.ATTACHMENT_TYPE_TEXT, data.attachmentType)
        assertTrue(data.timestamp > 0)
        assertEquals(FlashNoteConstants.DEFAULT_PRIORITY, data.priority)
        assertNull(data.metadata)
    }

    @Test
    fun `FlashNoteData should accept custom values`() {
        val metadata = FlashNoteMetadata(
            presetId = "preset_123",
            cameraParams = testCameraParams,
            deviceModel = "Find X8 Pro",
            author = "Test Author",
            rating = 4.5f,
            usageCount = 50
        )

        val data = FlashNoteData(
            title = "Custom Title",
            content = "Custom Content",
            category = FlashNoteConstants.CATEGORY_CAMERA_PARAMS,
            tags = listOf("tag1", "tag2"),
            source = FlashNoteConstants.SOURCE_USER_CREATED,
            attachmentUri = Uri.parse("content://test/uri"),
            attachmentType = FlashNoteConstants.ATTACHMENT_TYPE_IMAGE,
            timestamp = 1234567890L,
            priority = 5,
            metadata = metadata
        )

        assertEquals("Custom Title", data.title)
        assertEquals("Custom Content", data.content)
        assertEquals(FlashNoteConstants.CATEGORY_CAMERA_PARAMS, data.category)
        assertEquals(2, data.tags.size)
        assertEquals("tag1", data.tags[0])
        assertEquals("tag2", data.tags[1])
        assertEquals(FlashNoteConstants.SOURCE_USER_CREATED, data.source)
        assertNotNull(data.attachmentUri)
        assertEquals(FlashNoteConstants.ATTACHMENT_TYPE_IMAGE, data.attachmentType)
        assertEquals(1234567890L, data.timestamp)
        assertEquals(5, data.priority)
        assertNotNull(data.metadata)
        assertEquals("preset_123", data.metadata?.presetId)
    }

    @Test
    fun `FlashNoteMetadata should have correct default values`() {
        val metadata = FlashNoteMetadata(
            presetId = "test_id",
            cameraParams = testCameraParams,
            deviceModel = "Test Device"
        )

        assertEquals("test_id", metadata.presetId)
        assertNotNull(metadata.cameraParams)
        assertEquals("Test Device", metadata.deviceModel)
        assertNull(metadata.author)
        assertNull(metadata.rating)
        assertNull(metadata.usageCount)
        assertNull(metadata.deviceInfo)
    }

    @Test
    fun `FlashNoteMetadata should accept all values`() {
        val deviceInfo = DeviceInfo(
            brand = "OPPO",
            model = "Find X8 Pro",
            colorosVersion = "ColorOS 16",
            cameraApp = "OPPO Camera"
        )

        val metadata = FlashNoteMetadata(
            presetId = "test_id",
            cameraParams = testCameraParams,
            deviceModel = "Test Device",
            author = "Test Author",
            rating = 4.5f,
            usageCount = 100,
            deviceInfo = deviceInfo
        )

        assertEquals("Test Author", metadata.author)
        assertEquals(4.5f, metadata.rating)
        assertEquals(100, metadata.usageCount)
        assertNotNull(metadata.deviceInfo)
        assertEquals("ColorOS 16", metadata.deviceInfo?.colorosVersion)
    }

    @Test
    fun `DeviceInfo should have correct defaults`() {
        val deviceInfo = DeviceInfo(model = "Find X8 Pro")

        assertEquals("OPPO", deviceInfo.brand)
        assertEquals("Find X8 Pro", deviceInfo.model)
        assertNull(deviceInfo.colorosVersion)
        assertNull(deviceInfo.cameraApp)
    }

    @Test
    fun `FlashNoteResult should indicate success correctly`() {
        val successResult = FlashNoteResult(
            success = true,
            noteId = "note_123",
            timestamp = 1234567890L
        )

        assertTrue(successResult.success)
        assertEquals("note_123", successResult.noteId)
        assertNull(successResult.errorMessage)
    }

    @Test
    fun `FlashNoteResult should indicate failure correctly`() {
        val failureResult = FlashNoteResult(
            success = false,
            errorMessage = "Test error",
            timestamp = 1234567890L
        )

        assertFalse(failureResult.success)
        assertNull(failureResult.noteId)
        assertEquals("Test error", failureResult.errorMessage)
    }

    @Test
    fun `QuickNoteRequest should have correct defaults`() {
        val request = QuickNoteRequest(
            preset = testPreset
        )

        assertEquals(testPreset, request.preset)
        assertTrue(request.includeCover)
        assertTrue(request.includeParams)
        assertTrue(request.customTags.isEmpty())
        assertEquals(FlashNoteConstants.DEFAULT_PRIORITY, request.priority)
    }

    @Test
    fun `toFlashNoteData should correctly convert preset`() {
        val flashNoteData = testPreset.toFlashNoteData()

        assertEquals("🎨 ${testPreset.name}", flashNoteData.title)
        assertTrue(flashNoteData.content.contains(testPreset.name))
        assertEquals(FlashNoteConstants.CATEGORY_PRESET, flashNoteData.category)
        assertTrue(flashNoteData.tags.contains(FlashNoteConstants.TAG_PRESET))
        assertTrue(flashNoteData.tags.contains(FlashNoteConstants.TAG_CAMERA))
        assertTrue(flashNoteData.tags.contains(FlashNoteConstants.TAG_PHOTO))
        assertEquals(FlashNoteConstants.SOURCE_OPPO_MASTER, flashNoteData.source)
        assertNotNull(flashNoteData.metadata)
    }

    @Test
    fun `toFlashNoteData should include camera params when requested`() {
        val flashNoteData = testPreset.toFlashNoteData(includeParams = true)

        assertTrue(flashNoteData.content.contains("ISO: ${testCameraParams.iso}"))
        assertTrue(flashNoteData.content.contains("快门: ${testCameraParams.shutter}"))
        assertTrue(flashNoteData.content.contains("曝光补偿: ${testCameraParams.ev}"))
        assertTrue(flashNoteData.content.contains("白平衡: ${testCameraParams.wb}"))
    }

    @Test
    fun `toFlashNoteData should include contrast when not default`() {
        val flashNoteData = testPreset.toFlashNoteData(includeParams = true)

        assertTrue(flashNoteData.content.contains("对比度: ${testCameraParams.contrast}"))
    }

    @Test
    fun `toFlashNoteData should include saturation when not default`() {
        val flashNoteData = testPreset.toFlashNoteData(includeParams = true)

        assertTrue(flashNoteData.content.contains("饱和度: ${testCameraParams.saturation}"))
    }

    @Test
    fun `toFlashNoteData should include vignette when not zero`() {
        val flashNoteData = testPreset.toFlashNoteData(includeParams = true)

        assertTrue(flashNoteData.content.contains("暗角: ${testCameraParams.vignette}"))
    }

    @Test
    fun `toFlashNoteData should include scene tags`() {
        val flashNoteData = testPreset.toFlashNoteData(includeParams = true)

        assertTrue(flashNoteData.content.contains("场景标签"))
        testCameraParams.sceneTags.forEach { tag ->
            assertTrue(flashNoteData.content.contains(tag))
        }
    }

    @Test
    fun `toFlashNoteData should include custom tags`() {
        val customTags = listOf("custom1", "custom2")
        val flashNoteData = testPreset.toFlashNoteData(customTags = customTags)

        customTags.forEach { tag ->
            assertTrue(flashNoteData.tags.contains(tag))
        }
    }

    @Test
    fun `toFlashNoteData should include cover when preset has cover path`() {
        val flashNoteData = testPreset.toFlashNoteData(includeCover = true)

        assertEquals(FlashNoteConstants.ATTACHMENT_TYPE_IMAGE, flashNoteData.attachmentType)
    }

    @Test
    fun `toFlashNoteData should not include cover when preset has empty cover path`() {
        val presetWithoutCover = testPreset.copy(coverPath = "")
        val flashNoteData = presetWithoutCover.toFlashNoteData(includeCover = true)

        assertEquals(FlashNoteConstants.ATTACHMENT_TYPE_TEXT, flashNoteData.attachmentType)
    }

    @Test
    fun `toFlashNoteData should include preset metadata`() {
        val flashNoteData = testPreset.toFlashNoteData()

        assertNotNull(flashNoteData.metadata)
        assertEquals(testPreset.id, flashNoteData.metadata?.presetId)
        assertEquals(testPreset.deviceModel, flashNoteData.metadata?.deviceModel)
        assertEquals(testPreset.author, flashNoteData.metadata?.author)
        assertEquals(testPreset.rating, flashNoteData.metadata?.rating)
        assertEquals(testPreset.usageCount, flashNoteData.metadata?.usageCount)
    }

    @Test
    fun `toFlashNoteData should handle preset without camera params`() {
        val presetWithoutParams = testPreset.copy(cameraParams = null)
        val flashNoteData = presetWithoutParams.toFlashNoteData(includeParams = true)

        assertEquals("🎨 ${presetWithoutParams.name}", flashNoteData.title)
        assertTrue(flashNoteData.content.contains("设备: ${presetWithoutParams.deviceModel}"))
        assertTrue(flashNoteData.tags.contains(FlashNoteConstants.TAG_PRESET))
        assertTrue(flashNoteData.tags.contains(FlashNoteConstants.TAG_CAMERA))
        assertTrue(flashNoteData.tags.contains(FlashNoteConstants.TAG_PHOTO))
    }

    @Test
    fun `toFlashNoteContent should format camera params correctly`() {
        val content = testCameraParams.toFlashNoteContent()

        assertTrue(content.contains("相机参数详情"))
        assertTrue(content.contains("ISO: ${testCameraParams.iso}"))
        assertTrue(content.contains("快门: ${testCameraParams.shutter}"))
        assertTrue(content.contains("曝光补偿: ${testCameraParams.ev}"))
        assertTrue(content.contains("白平衡: ${testCameraParams.wb}"))
        assertTrue(content.contains("对比度: ${testCameraParams.contrast}"))
        assertTrue(content.contains("饱和度: ${testCameraParams.saturation}"))
        assertTrue(content.contains("清晰度: ${testCameraParams.sharpness}"))
        assertTrue(content.contains("暗角: ${testCameraParams.vignette}"))
    }

    @Test
    fun `toFlashNoteContent should include scene tags`() {
        val content = testCameraParams.toFlashNoteContent()

        testCameraParams.sceneTags.forEach { tag ->
            assertTrue(content.contains(tag))
        }
    }

    @Test
    fun `toFlashNoteContent should indicate HNCS when enabled`() {
        val content = testCameraParams.toFlashNoteContent()

        assertTrue(content.contains("哈苏HNCS色彩优化已启用"))
    }

    @Test
    fun `toFlashNoteContent should not indicate HNCS when disabled`() {
        val paramsWithoutHncs = testCameraParams.copy(hasselblad_hncs = false)
        val content = paramsWithoutHncs.toFlashNoteContent()

        assertFalse(content.contains("哈苏HNCS色彩优化已启用"))
    }

    @Test
    fun `FlashNoteConstants should have correct values`() {
        assertEquals("com.coloros.flashnote.action.ADD_NOTE", FlashNoteConstants.ACTION_FLASH_NOTE)
        assertEquals("com.coloros.flashnote.action.V2_ADD_NOTE", FlashNoteConstants.ACTION_FLASH_NOTE_V2)
        assertEquals("preset", FlashNoteConstants.CATEGORY_PRESET)
        assertEquals("camera_params", FlashNoteConstants.CATEGORY_CAMERA_PARAMS)
        assertEquals("photo_style", FlashNoteConstants.CATEGORY_PHOTO_STYLE)
        assertEquals("image", FlashNoteConstants.ATTACHMENT_TYPE_IMAGE)
        assertEquals("video", FlashNoteConstants.ATTACHMENT_TYPE_VIDEO)
        assertEquals("text", FlashNoteConstants.ATTACHMENT_TYPE_TEXT)
        assertEquals("oppo_master", FlashNoteConstants.SOURCE_OPPO_MASTER)
        assertEquals("preset_cloud", FlashNoteConstants.SOURCE_PRESET_CLOUD)
        assertEquals("user_created", FlashNoteConstants.SOURCE_USER_CREATED)
        assertEquals("photo", FlashNoteConstants.TAG_PHOTO)
        assertEquals("preset", FlashNoteConstants.TAG_PRESET)
        assertEquals("camera", FlashNoteConstants.TAG_CAMERA)
        assertEquals("style", FlashNoteConstants.TAG_STYLE)
        assertEquals(0, FlashNoteConstants.DEFAULT_PRIORITY)
        assertEquals(5000, FlashNoteConstants.MAX_CONTENT_LENGTH)
        assertEquals(200, FlashNoteConstants.MAX_TITLE_LENGTH)
    }

    @Test
    fun `FlashNoteConstants enabled features should contain all expected features`() {
        val features = FlashNoteConstants.ENABLED_FEATURES
        assertTrue(features.contains(FlashNoteConstants.FEATURE_PRESET_SAVE))
        assertTrue(features.contains(FlashNoteConstants.FEATURE_CAMERA_PARAMS_SAVE))
        assertTrue(features.contains(FlashNoteConstants.FEATURE_IMAGE_ATTACHMENT))
        assertTrue(features.contains(FlashNoteConstants.FEATURE_AUTO_TAG))
        assertTrue(features.contains(FlashNoteConstants.FEATURE_QUICK_EXPORT))
    }
}
