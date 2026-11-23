package com.example.game

import androidx.compose.ui.graphics.Color

enum class BubbleColor(
    val id: Int,
    val label: String,
    val highlightColor: Color,
    val baseColor: Color,
    val deepColor: Color,
    val rimColor: Color
) {
    RED(
        id = 0,
        label = "Ruby Red",
        highlightColor = Color(0xFFFF8A9B),
        baseColor = Color(0xFFFF2442),
        deepColor = Color(0xFFB3001B),
        rimColor = Color(0xFF700010)
    ),
    YELLOW(
        id = 1,
        label = "Sun Gold",
        highlightColor = Color(0xFFFFF59D),
        baseColor = Color(0xFFFFCA18),
        deepColor = Color(0xFFE07A00),
        rimColor = Color(0xFF8C4600)
    ),
    GREEN(
        id = 2,
        label = "Emerald Lime",
        highlightColor = Color(0xFFA6FF7B),
        baseColor = Color(0xFF2CD622),
        deepColor = Color(0xFF0E8012),
        rimColor = Color(0xFF064C09)
    ),
    BLUE(
        id = 3,
        label = "Sky Sapphire",
        highlightColor = Color(0xFF9CE3FF),
        baseColor = Color(0xFF1D9BF0),
        deepColor = Color(0xFF0C4DC2),
        rimColor = Color(0xFF06296E)
    ),
    PURPLE(
        id = 4,
        label = "Electric Violet",
        highlightColor = Color(0xFFF3A6FF),
        baseColor = Color(0xFFBF24F2),
        deepColor = Color(0xFF6B0BA8),
        rimColor = Color(0xFF3E0563)
    ),
    ORANGE(
        id = 5,
        label = "Tangerine Burst",
        highlightColor = Color(0xFFFFC078),
        baseColor = Color(0xFFFF6D1A),
        deepColor = Color(0xFFC23900),
        rimColor = Color(0xFF752000)
    ),
    RAINBOW(
        id = 6,
        label = "Rainbow Wildcard",
        highlightColor = Color(0xFFFFFFFF),
        baseColor = Color(0xFFFFD54F),
        deepColor = Color(0xFFEC4899),
        rimColor = Color(0xFF4F46E5)
    );

    companion object {
        val standardColors: List<BubbleColor> = listOf(RED, YELLOW, GREEN, BLUE, PURPLE, ORANGE)
    }
}
