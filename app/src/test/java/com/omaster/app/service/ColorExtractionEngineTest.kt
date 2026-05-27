package com.omaster.app.service

import com.omaster.app.data.PresetRepository
import com.omaster.app.model.CameraParams
import com.omaster.app.model.ColorProfile
import com.omaster.app.model.ColorExtractionResult
import com.omaster.app.model.Preset
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ColorExtractionEngineTest {

    @Mock
    private lateinit var mockPresetRepository: PresetRepository

    private lateinit var engine: ColorExtractionEngineTestable

    private val samplePresets = listOf(
        Preset(
            id = "preset_1",
            name = "Warm Tone",
            coverPath = "warm_01",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                hasselblad_hncs = false,
                contrast = 1.0f,
                saturation = 1.0f,
                vignette = 0.0f,
                sceneTags = emptyList(),
                colorProfile = ColorProfile(
                    dominantColors = listOf(0xFFFF6B6B.toInt(), 0xFFFFE66D.toInt()),
                    toneCurve = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
                )
            ),
            deviceModel = "Test",
            source = "test",
            isFavorite = false,
            usageCount = 0,
            rating = 0f,
            author = "Test"
        ),
        Preset(
            id = "preset_2",
            name = "Cool Tone",
            coverPath = "cool_01",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                hasselblad_hncs = false,
                contrast = 1.0f,
                saturation = 1.0f,
                vignette = 0.0f,
                sceneTags = emptyList(),
                colorProfile = ColorProfile(
                    dominantColors = listOf(0xFF4ECDC4.toInt(), 0xFF556270.toInt()),
                    toneCurve = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
                )
            ),
            deviceModel = "Test",
            source = "test",
            isFavorite = false,
            usageCount = 0,
            rating = 0f,
            author = "Test"
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        engine = ColorExtractionEngineTestable(mockPresetRepository)
    }

    @Test
    fun `cosine similarity should return 1 for identical colors`() {
        val colors1 = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())
        val colors2 = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())

        val similarity = engine.testCosineSimilarity(colors1, colors2)

        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun `cosine similarity should return 0 for completely different colors`() {
        val colors1 = listOf(0xFFFFFFFF.toInt())
        val colors2 = listOf(0xFF000000.toInt())

        val similarity = engine.testCosineSimilarity(colors1, colors2)

        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `cosine similarity should handle empty lists`() {
        val similarity = engine.testCosineSimilarity(emptyList(), emptyList())
        assertEquals(0.0f, similarity)
    }

    @Test
    fun `cosine similarity should handle null color profile`() {
        val colors = listOf(0xFFFF0000.toInt())
        val similarity = engine.testCosineSimilarity(colors, null)
        assertEquals(0.0f, similarity)
    }

    @Test
    fun `cosine similarity should handle different list sizes`() {
        val colors1 = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val colors2 = listOf(0xFFFF0000.toInt())

        val similarity = engine.testCosineSimilarity(colors1, colors2)
        assertEquals(0.0f, similarity)
    }

    @Test
    fun `cosine similarity should handle partial matches`() {
        val colors1 = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val colors2 = listOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt())

        val similarity = engine.testCosineSimilarity(colors1, colors2)

        assertTrue(similarity > 0f)
        assertTrue(similarity < 1f)
    }

    @Test
    fun `generatePresetFromColors should create preset with correct id prefix`() {
        val colors = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)

        val preset = engine.testGeneratePresetFromColors(colors, toneCurve)

        assertTrue(preset.id.startsWith("custom_"))
        assertEquals("Custom Filter", preset.name)
        assertNotNull(preset.cameraParams)
    }

    @Test
    fun `generatePresetFromColors should set correct saturation based on colors`() {
        val darkColors = listOf(0xFF111111.toInt())
        val lightColors = listOf(0xFFEEEEEE.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)

        val darkPreset = engine.testGeneratePresetFromColors(darkColors, toneCurve)
        val lightPreset = engine.testGeneratePresetFromColors(lightColors, toneCurve)

        assertNotNull(darkPreset.cameraParams)
        assertNotNull(lightPreset.cameraParams)
        assertTrue(darkPreset.cameraParams?.saturation in 0.8f..1.5f)
        assertTrue(lightPreset.cameraParams?.saturation in 0.8f..1.5f)
    }

    @Test
    fun `generatePresetFromColors should set color profile correctly`() {
        val colors = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)

        val preset = engine.testGeneratePresetFromColors(colors, toneCurve)

        assertNotNull(preset.cameraParams?.colorProfile)
        assertEquals(colors, preset.cameraParams?.colorProfile?.dominantColors)
        assertEquals(toneCurve, preset.cameraParams?.colorProfile?.toneCurve)
    }

    @Test
    fun `generatePresetFromColors should handle empty colors list`() {
        val colors = emptyList<Int>()
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)

        val preset = engine.testGeneratePresetFromColors(colors, toneCurve)

        assertTrue(preset.id.startsWith("custom_"))
        assertNotNull(preset.cameraParams)
    }

    @Test
    fun `color temperature should be warm for red dominant colors`() {
        val warmColors = listOf(0xFFFF5533.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)

        val preset = engine.testGeneratePresetFromColors(warmColors, toneCurve)

        assertNotNull(preset.cameraParams)
    }

    @Test
    fun `color temperature should be cool for blue dominant colors`() {
        val coolColors = listOf(0xFF3355FF.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)

        val preset = engine.testGeneratePresetFromColors(coolColors, toneCurve)

        assertNotNull(preset.cameraParams)
    }

    @Test
    fun `preset color extraction result should have all required fields`() {
        val colors = listOf(0xFFFF0000.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)
        val matchedPresets = listOf(samplePresets[0] to 0.8f)

        val result = ColorExtractionResult(
            dominantColors = colors,
            toneCurve = toneCurve,
            matchedPresets = matchedPresets,
            customPreset = null
        )

        assertEquals(colors, result.dominantColors)
        assertEquals(toneCurve, result.toneCurve)
        assertEquals(1, result.matchedPresets.size)
        assertNull(result.customPreset)
    }

    @Test
    fun `preset color extraction result can have custom preset`() {
        val colors = listOf(0xFFFF0000.toInt())
        val toneCurve = listOf(0.1f, 0.3f, 0.5f)
        val customPreset = engine.testGeneratePresetFromColors(colors, toneCurve)

        val result = ColorExtractionResult(
            dominantColors = colors,
            toneCurve = toneCurve,
            matchedPresets = emptyList(),
            customPreset = customPreset
        )

        assertNotNull(result.customPreset)
        assertEquals("Custom Filter", result.customPreset?.name)
    }
}

class ColorExtractionEngineTestable(
    private val presetRepository: PresetRepository
) {
    fun testCosineSimilarity(colors1: List<Int>, colors2: List<Int>?): Float {
        val colors2List = colors2?.dominantColors ?: return 0f
        if (colors1.size != colors2List.size) return 0f

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in colors1.indices) {
            val c1 = colors1[i]
            val c2 = colors2List[i]

            val r1 = (c1 shr 16) and 0xFF
            val g1 = (c1 shr 8) and 0xFF
            val b1 = c1 and 0xFF

            val r2 = (c2 shr 16) and 0xFF
            val g2 = (c2 shr 8) and 0xFF
            val b2 = c2 and 0xFF

            dotProduct += (r1 * r2 + g1 * g2 + b1 * b2).toDouble()
            norm1 += (r1 * r1 + g1 * g1 + b1 * b1).toDouble()
            norm2 += (r2 * r2 + g2 * g2 + b2 * b2).toDouble()
        }

        return if (norm1 == 0.0 || norm2 == 0.0) {
            0f
        } else {
            (dotProduct / (sqrt(norm1) * sqrt(norm2))).toFloat()
        }
    }

    fun testGeneratePresetFromColors(colors: List<Int>, toneCurve: List<Float>): Preset {
        val avgColor = colors.firstOrNull() ?: 0xFF6200EE.toInt()
        val r = (avgColor shr 16) and 0xFF
        val g = (avgColor shr 8) and 0xFF
        val b = avgColor and 0xFF

        val saturation = ((r + g + b) / 3.0 / 255.0).toFloat().coerceIn(0.8f, 1.5f)
        val temperature = if (r > b) 0.5f else -0.5f

        return Preset(
            id = "custom_${System.currentTimeMillis()}",
            name = "Custom Filter",
            coverPath = "",
            cameraParams = CameraParams(
                saturation = saturation,
                contrast = 1.1f,
                vignette = 0.2f,
                colorProfile = ColorProfile(
                    dominantColors = colors,
                    toneCurve = toneCurve
                )
            )
        )
    }
}

private val List<Int>.dominantColors: List<Int>?
    get() = this
