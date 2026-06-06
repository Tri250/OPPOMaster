package com.omaster.app.domain.model

import java.time.LocalDate
import java.time.Month

/**
 * 节日数据模型
 */
data class Holiday(
    val id: String,
    val name: String,
    val greeting: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val theme: HolidayTheme,
    val presetIds: List<String> = emptyList(),
    val isActive: Boolean = true
)

/**
 * 节日主题
 */
data class HolidayTheme(
    val primaryColor: String,
    val secondaryColor: String,
    val backgroundGradient: List<String>,
    val accentColor: String,
    val icon: String
)

/**
 * 预定义节日列表
 */
object HolidayPresets {
    
    fun getCurrentYearHolidays(year: Int): List<Holiday> = listOf(
        // 春节
        Holiday(
            id = "spring_festival",
            name = "春节",
            greeting = "新春快乐，万事如意！",
            startDate = LocalDate.of(year, Month.FEBRUARY, 10),
            endDate = LocalDate.of(year, Month.FEBRUARY, 17),
            theme = HolidayTheme(
                primaryColor = "#D32F2F",
                secondaryColor = "#FFC107",
                backgroundGradient = listOf("#D32F2F", "#B71C1C"),
                accentColor = "#FFD700",
                icon = "🧧"
            ),
            presetIds = listOf("spring_red", "lantern_warm", "new_year_gold")
        ),
        
        // 元宵节
        Holiday(
            id = "lantern_festival",
            name = "元宵节",
            greeting = "月圆人团圆，元宵快乐！",
            startDate = LocalDate.of(year, Month.FEBRUARY, 24),
            endDate = LocalDate.of(year, Month.FEBRUARY, 24),
            theme = HolidayTheme(
                primaryColor = "#FF6F00",
                secondaryColor = "#FFD54F",
                backgroundGradient = listOf("#FF6F00", "#E65100"),
                accentColor = "#FFF176",
                icon = "🏮"
            ),
            presetIds = listOf("lantern_warm", "night_light")
        ),
        
        // 情人节
        Holiday(
            id = "valentine",
            name = "情人节",
            greeting = "愿爱如星光，永恒闪耀！",
            startDate = LocalDate.of(year, Month.FEBRUARY, 14),
            endDate = LocalDate.of(year, Month.FEBRUARY, 14),
            theme = HolidayTheme(
                primaryColor = "#E91E63",
                secondaryColor = "#F8BBD9",
                backgroundGradient = listOf("#E91E63", "#C2185B"),
                accentColor = "#FF80AB",
                icon = "💕"
            ),
            presetIds = listOf("romantic_pink", "rose_soft")
        ),
        
        // 清明节
        Holiday(
            id = "qingming",
            name = "清明节",
            greeting = "踏青寻春，缅怀先人",
            startDate = LocalDate.of(year, Month.APRIL, 4),
            endDate = LocalDate.of(year, Month.APRIL, 6),
            theme = HolidayTheme(
                primaryColor = "#4CAF50",
                secondaryColor = "#A5D6A7",
                backgroundGradient = listOf("#4CAF50", "#2E7D32"),
                accentColor = "#81C784",
                icon = "🌿"
            ),
            presetIds = listOf("spring_green", "fresh_natural")
        ),
        
        // 劳动节
        Holiday(
            id = "labor_day",
            name = "劳动节",
            greeting = "劳动最光荣，节日快乐！",
            startDate = LocalDate.of(year, Month.MAY, 1),
            endDate = LocalDate.of(year, Month.MAY, 5),
            theme = HolidayTheme(
                primaryColor = "#2196F3",
                secondaryColor = "#90CAF9",
                backgroundGradient = listOf("#2196F3", "#1565C0"),
                accentColor = "#64B5F6",
                icon = "🛠️"
            ),
            presetIds = listOf("worker_strong", "industrial_cool")
        ),
        
        // 端午节
        Holiday(
            id = "dragon_boat",
            name = "端午节",
            greeting = "粽香飘万里，端午安康！",
            startDate = LocalDate.of(year, Month.JUNE, 10),
            endDate = LocalDate.of(year, Month.JUNE, 10),
            theme = HolidayTheme(
                primaryColor = "#009688",
                secondaryColor = "#80CBC4",
                backgroundGradient = listOf("#009688", "#00695C"),
                accentColor = "#4DB6AC",
                icon = "🐲"
            ),
            presetIds = listOf("dragon_green", "rice_white")
        ),
        
        // 七夕节
        Holiday(
            id = "qixi",
            name = "七夕节",
            greeting = "鹊桥相会，情定今生！",
            startDate = LocalDate.of(year, Month.AUGUST, 10),
            endDate = LocalDate.of(year, Month.AUGUST, 10),
            theme = HolidayTheme(
                primaryColor = "#9C27B0",
                secondaryColor = "#E1BEE7",
                backgroundGradient = listOf("#9C27B0", "#7B1FA2"),
                accentColor = "#CE93D8",
                icon = "🌌"
            ),
            presetIds = listOf("starry_night", "romantic_purple")
        ),
        
        // 中秋节
        Holiday(
            id = "mid_autumn",
            name = "中秋节",
            greeting = "月圆人团圆，中秋快乐！",
            startDate = LocalDate.of(year, Month.SEPTEMBER, 17),
            endDate = LocalDate.of(year, Month.SEPTEMBER, 17),
            theme = HolidayTheme(
                primaryColor = "#FF9800",
                secondaryColor = "#FFE0B2",
                backgroundGradient = listOf("#FF9800", "#F57C00"),
                accentColor = "#FFCC80",
                icon = "🌕"
            ),
            presetIds = listOf("moon_warm", "night_gold")
        ),
        
        // 国庆节
        Holiday(
            id = "national_day",
            name = "国庆节",
            greeting = "祖国繁荣昌盛，国庆快乐！",
            startDate = LocalDate.of(year, Month.OCTOBER, 1),
            endDate = LocalDate.of(year, Month.OCTOBER, 7),
            theme = HolidayTheme(
                primaryColor = "#D32F2F",
                secondaryColor = "#FFEB3B",
                backgroundGradient = listOf("#D32F2F", "#B71C1C"),
                accentColor = "#FFC107",
                icon = "🇨🇳"
            ),
            presetIds = listOf("china_red", "celebration_gold")
        ),
        
        // 圣诞节
        Holiday(
            id = "christmas",
            name = "圣诞节",
            greeting = "圣诞快乐，平安喜乐！",
            startDate = LocalDate.of(year, Month.DECEMBER, 24),
            endDate = LocalDate.of(year, Month.DECEMBER, 25),
            theme = HolidayTheme(
                primaryColor = "#2E7D32",
                secondaryColor = "#EF5350",
                backgroundGradient = listOf("#1B5E20", "#2E7D32"),
                accentColor = "#FFFFFF",
                icon = "🎄"
            ),
            presetIds = listOf("christmas_green", "snow_white", "warm_fire")
        ),
        
        // 元旦
        Holiday(
            id = "new_year",
            name = "元旦",
            greeting = "新年快乐，万事如意！",
            startDate = LocalDate.of(year, Month.JANUARY, 1),
            endDate = LocalDate.of(year, Month.JANUARY, 3),
            theme = HolidayTheme(
                primaryColor = "#673AB7",
                secondaryColor = "#FFD700",
                backgroundGradient = listOf("#673AB7", "#512DA8"),
                accentColor = "#FFD700",
                icon = "🎆"
            ),
            presetIds = listOf("new_year_purple", "firework_colorful")
        )
    )
    
    /**
     * 获取当前节日
     */
    fun getCurrentHoliday(): Holiday? {
        val today = LocalDate.now()
        val year = today.year
        return getCurrentYearHolidays(year).find { holiday ->
            !today.isBefore(holiday.startDate) && !today.isAfter(holiday.endDate)
        }
    }
    
    /**
     * 获取即将到来的节日
     */
    fun getUpcomingHolidays(daysAhead: Int = 7): List<Holiday> {
        val today = LocalDate.now()
        val year = today.year
        return getCurrentYearHolidays(year).filter { holiday ->
            val daysUntil = holiday.startDate.toEpochDay() - today.toEpochDay()
            daysUntil in 0..daysAhead
        }
    }
}
