package com.omaster.app.navigation

import org.junit.Assert.*
import org.junit.Test

/**
 * 导航系统全面单元测试
 * 覆盖：路由定义、路由创建、底部导航、页面标识
 */
class NavigationTest {

    // ==================== OMasterScreen 测试 ====================

    @Test
    fun `Home screen should have correct route`() {
        assertEquals("home", OMasterScreen.Home.route)
    }

    @Test
    fun `Home screen should have correct title`() {
        assertEquals("哈苏预设", OMasterScreen.Home.title)
    }

    @Test
    fun `Detail screen should have correct route pattern`() {
        assertEquals("detail/{preset_id}", OMasterScreen.Detail.route)
    }

    @Test
    fun `Detail screen should create correct route with preset id`() {
        val route = OMasterScreen.Detail.createRoute("test-preset-123")
        assertEquals("detail/test-preset-123", route)
    }

    @Test
    fun `Detail screen should handle special characters in preset id`() {
        val route = OMasterScreen.Detail.createRoute("hncs_portrait_master")
        assertEquals("detail/hncs_portrait_master", route)
    }

    @Test
    fun `SceneDetection screen should have correct route`() {
        assertEquals("scene_detection", OMasterScreen.SceneDetection.route)
    }

    @Test
    fun `CameraConfig screen should have correct route`() {
        assertEquals("camera_config", OMasterScreen.CameraConfig.route)
    }

    @Test
    fun `AiFineTune screen should have correct route`() {
        assertEquals("ai_fine_tune", OMasterScreen.AiFineTune.route)
    }

    @Test
    fun `Profile screen should have correct route`() {
        assertEquals("profile", OMasterScreen.Profile.route)
    }

    @Test
    fun `Settings screen should have correct route`() {
        assertEquals("settings", OMasterScreen.Settings.route)
    }

    @Test
    fun `Watermark screen should have correct route`() {
        assertEquals("watermark", OMasterScreen.Watermark.route)
    }

    @Test
    fun `all screens should have unique routes`() {
        val screens = listOf(
            OMasterScreen.Home,
            OMasterScreen.Detail,
            OMasterScreen.SceneDetection,
            OMasterScreen.CameraConfig,
            OMasterScreen.AiFineTune,
            OMasterScreen.Profile,
            OMasterScreen.Settings,
            OMasterScreen.Watermark
        )
        val routes = screens.map { it.route }.toSet()
        assertEquals("All routes should be unique", screens.size, routes.size)
    }

    @Test
    fun `all screens should have non-empty titles`() {
        val screens = listOf(
            OMasterScreen.Home,
            OMasterScreen.Detail,
            OMasterScreen.SceneDetection,
            OMasterScreen.CameraConfig,
            OMasterScreen.AiFineTune,
            OMasterScreen.Profile,
            OMasterScreen.Settings,
            OMasterScreen.Watermark
        )
        screens.forEach { screen ->
            assertTrue("Screen ${screen.route} should have non-empty title", screen.title.isNotEmpty())
        }
    }

    // ==================== 底部导航测试 ====================

    @Test
    fun `bottom tab screens should have 5 items`() {
        assertEquals(5, omasterBottomTabScreens.size)
    }

    @Test
    fun `bottom tab should contain Home`() {
        assertTrue(omasterBottomTabScreens.contains(OMasterScreen.Home))
    }

    @Test
    fun `bottom tab should contain SceneDetection`() {
        assertTrue(omasterBottomTabScreens.contains(OMasterScreen.SceneDetection))
    }

    @Test
    fun `bottom tab should contain CameraConfig`() {
        assertTrue(omasterBottomTabScreens.contains(OMasterScreen.CameraConfig))
    }

    @Test
    fun `bottom tab should contain AiFineTune`() {
        assertTrue(omasterBottomTabScreens.contains(OMasterScreen.AiFineTune))
    }

    @Test
    fun `bottom tab should contain Profile`() {
        assertTrue(omasterBottomTabScreens.contains(OMasterScreen.Profile))
    }

    @Test
    fun `bottom tab should not contain Settings`() {
        assertFalse(omasterBottomTabScreens.contains(OMasterScreen.Settings))
    }

    @Test
    fun `bottom tab should not contain Detail`() {
        assertFalse(omasterBottomTabScreens.contains(OMasterScreen.Detail))
    }

    @Test
    fun `bottom tab should not contain Watermark`() {
        assertFalse(omasterBottomTabScreens.contains(OMasterScreen.Watermark))
    }

    @Test
    fun `Home should be first bottom tab`() {
        assertEquals(OMasterScreen.Home, omasterBottomTabScreens[0])
    }

    // ==================== Screen (旧版) 测试 ====================

    @Test
    fun `legacy Screen Home should have correct route`() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun `legacy Screen Detail should create correct route`() {
        val route = Screen.Detail.createRoute("test-id")
        assertEquals("detail/test-id", route)
    }

    @Test
    fun `legacy Screen Settings should have correct route`() {
        assertEquals("settings", Screen.Settings.route)
    }

    // ==================== 图标测试 ====================

    @Test
    fun `all OMasterScreens should have selected icons`() {
        val screens = listOf(
            OMasterScreen.Home,
            OMasterScreen.Detail,
            OMasterScreen.SceneDetection,
            OMasterScreen.CameraConfig,
            OMasterScreen.AiFineTune,
            OMasterScreen.Profile,
            OMasterScreen.Settings,
            OMasterScreen.Watermark
        )
        screens.forEach { screen ->
            assertNotNull("Screen ${screen.route} should have selected icon", screen.selectedIcon)
        }
    }

    @Test
    fun `all OMasterScreens should have unselected icons`() {
        val screens = listOf(
            OMasterScreen.Home,
            OMasterScreen.Detail,
            OMasterScreen.SceneDetection,
            OMasterScreen.CameraConfig,
            OMasterScreen.AiFineTune,
            OMasterScreen.Profile,
            OMasterScreen.Settings,
            OMasterScreen.Watermark
        )
        screens.forEach { screen ->
            assertNotNull("Screen ${screen.route} should have unselected icon", screen.unselectedIcon)
        }
    }
}
