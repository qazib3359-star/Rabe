package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.BubbleColor
import com.example.ui.theme.GoldStarDark
import com.example.ui.theme.GoldStarLight
import com.example.ui.theme.GoldStarMid
import com.example.ui.theme.RibbonRedDark
import com.example.ui.theme.RibbonRedLight
import com.example.ui.theme.RibbonRedMid

/**
 * Draws a rich, glossy 3D glass bubble directly on Compose Canvas with zero bitmap overhead.
 */
fun DrawScope.drawGlossyBubble(
    center: Offset,
    radius: Float,
    color: BubbleColor,
    alpha: Float = 1f
) {
    if (radius <= 0.5f || alpha <= 0.01f) return

    // Soft drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.24f * alpha),
        radius = radius * 0.96f,
        center = Offset(center.x + radius * 0.06f, center.y + radius * 0.10f)
    )

    if (color == BubbleColor.RAINBOW) {
        val sweep = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF2442),
                Color(0xFFFFCA18),
                Color(0xFF2CD622),
                Color(0xFF1D9BF0),
                Color(0xFFBF24F2),
                Color(0xFFFF2442)
            ),
            center = center
        )
        drawCircle(brush = sweep, radius = radius * 0.95f, center = center, alpha = alpha)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.72f * alpha),
                    Color.Transparent
                ),
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
                radius = radius * 0.85f
            ),
            radius = radius * 0.95f,
            center = center
        )
    } else {
        // Main 3D spherical radial gradient
        val sphereBrush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to color.highlightColor.copy(alpha = alpha),
                0.38f to color.baseColor.copy(alpha = alpha),
                0.82f to color.deepColor.copy(alpha = alpha),
                1.0f to color.rimColor.copy(alpha = alpha)
            ),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
            radius = radius * 1.25f
        )
        drawCircle(brush = sphereBrush, radius = radius * 0.95f, center = center)
    }

    // Glossy specular upper-left highlight oval
    drawOval(
        color = Color.White.copy(alpha = 0.75f * alpha),
        topLeft = Offset(center.x - radius * 0.52f, center.y - radius * 0.62f),
        size = Size(radius * 0.44f, radius * 0.28f)
    )

    // Tiny secondary sparkle dot
    drawCircle(
        color = Color.White.copy(alpha = 0.60f * alpha),
        radius = radius * 0.11f,
        center = Offset(center.x - radius * 0.18f, center.y - radius * 0.62f)
    )

    // Bottom internal light bounce crescent
    drawArc(
        color = Color.White.copy(alpha = 0.30f * alpha),
        startAngle = 25f,
        sweepAngle = 125f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.72f, center.y - radius * 0.72f),
        size = Size(radius * 1.44f, radius * 1.44f),
        style = Stroke(width = radius * 0.10f, cap = StrokeCap.Round)
    )

    // Crisp outer rim
    drawCircle(
        color = color.rimColor.copy(alpha = 0.55f * alpha),
        radius = radius * 0.94f,
        center = center,
        style = Stroke(width = (radius * 0.07f).coerceAtLeast(1f))
    )
}

/**
 * Draws the ornate purple-and-gold bubble launcher cannon matching the Bubble Shooter Pro design.
 */
fun DrawScope.drawBubbleCannon(
    center: Offset,
    aimAngleDeg: Float,
    loadedBubble: BubbleColor,
    bubbleRadius: Float
) {
    val pedestalW = bubbleRadius * 3.9f
    val pedestalH = bubbleRadius * 1.35f

    // Pedestal shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.32f),
        topLeft = Offset(center.x - pedestalW * 0.55f, center.y + bubbleRadius * 0.75f),
        size = Size(pedestalW * 1.1f, pedestalH * 0.75f)
    )

    // Purple base tier
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF7C3AED), Color(0xFF3B0764)),
            startY = center.y + bubbleRadius * 0.45f,
            endY = center.y + bubbleRadius * 1.55f
        ),
        topLeft = Offset(center.x - pedestalW * 0.5f, center.y + bubbleRadius * 0.45f),
        size = Size(pedestalW, pedestalH * 0.85f)
    )

    // Gold base trim ring
    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(GoldStarDark, GoldStarLight, GoldStarMid, GoldStarDark),
            startX = center.x - pedestalW * 0.42f,
            endX = center.x + pedestalW * 0.42f
        ),
        topLeft = Offset(center.x - pedestalW * 0.42f, center.y + bubbleRadius * 0.30f),
        size = Size(pedestalW * 0.84f, pedestalH * 0.65f)
    )

    // Rotating cannon nozzle
    rotate(degrees = aimAngleDeg + 90f, pivot = center) {
        val barrelTopW = bubbleRadius * 1.15f
        val barrelBotW = bubbleRadius * 2.35f
        val barrelTopY = center.y - bubbleRadius * 1.75f
        val barrelBotY = center.y + bubbleRadius * 0.65f

        val barrelPath = Path().apply {
            moveTo(center.x - barrelTopW * 0.5f, barrelTopY)
            lineTo(center.x + barrelTopW * 0.5f, barrelTopY)
            lineTo(center.x + barrelBotW * 0.5f, barrelBotY)
            lineTo(center.x - barrelBotW * 0.5f, barrelBotY)
            close()
        }

        drawPath(
            path = barrelPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF4C1D95), Color(0xFF9333EA), Color(0xFF6D28D9), Color(0xFF3B0764)),
                startX = center.x - barrelBotW * 0.5f,
                endX = center.x + barrelBotW * 0.5f
            )
        )

        // Gold nozzle lip
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(GoldStarDark, GoldStarLight, GoldStarMid, GoldStarDark),
                startX = center.x - barrelTopW * 0.62f,
                endX = center.x + barrelTopW * 0.62f
            ),
            topLeft = Offset(center.x - barrelTopW * 0.62f, barrelTopY - bubbleRadius * 0.14f),
            size = Size(barrelTopW * 1.24f, bubbleRadius * 0.34f),
            cornerRadius = CornerRadius(bubbleRadius * 0.18f, bubbleRadius * 0.18f)
        )

        // Gold cradle ring around bubble chamber
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(GoldStarLight, GoldStarMid, GoldStarDark),
                start = Offset(center.x - bubbleRadius, center.y - bubbleRadius),
                end = Offset(center.x + bubbleRadius, center.y + bubbleRadius)
            ),
            radius = bubbleRadius * 1.24f,
            center = center,
            style = Stroke(width = bubbleRadius * 0.24f)
        )
    }

    // Loaded bubble sitting inside the cannon cradle
    drawGlossyBubble(
        center = center,
        radius = bubbleRadius * 0.96f,
        color = loadedBubble
    )
}

/**
 * Fairytale Castle & Meadow Canvas backdrop for the Home & Auth screens.
 * Pure vector/Canvas graphics = crisp at any resolution and ultra-low RAM!
 */
@Composable
fun CastleMeadowBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Vibrant sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF0A4DC2),
                    0.32f to Color(0xFF2896FF),
                    0.62f to Color(0xFF8BE2FF),
                    1.0f to Color(0xFF4CC441)
                )
            )
        )

        // Fluffy sunlit clouds
        fun drawCloud(cx: Float, cy: Float, scale: Float, alpha: Float) {
            val c = Color.White.copy(alpha = alpha)
            drawCircle(c, radius = 36f * scale, center = Offset(cx, cy))
            drawCircle(c, radius = 48f * scale, center = Offset(cx + 38f * scale, cy - 10f * scale))
            drawCircle(c, radius = 34f * scale, center = Offset(cx + 76f * scale, cy + 2f * scale))
            drawCircle(c, radius = 30f * scale, center = Offset(cx - 28f * scale, cy + 8f * scale))
        }
        drawCloud(w * 0.15f, h * 0.28f, w / 420f, 0.72f)
        drawCloud(w * 0.78f, h * 0.34f, w / 460f, 0.65f)
        drawCloud(w * 0.45f, h * 0.46f, w / 380f, 0.55f)

        // Distant fairytale castle silhouette on left hill
        val castleBaseY = h * 0.55f
        val castleColor = Color(0xFFD6E4FF)
        val castleRoof = Color(0xFFE11D48)
        val castleShadow = Color(0xFFA5B4FC)

        // Castle towers
        val towers = listOf(
            Triple(w * 0.08f, w * 0.07f, h * 0.11f),
            Triple(w * 0.17f, w * 0.09f, h * 0.16f),
            Triple(w * 0.28f, w * 0.07f, h * 0.12f)
        )
        for ((tx, tw, th) in towers) {
            drawRect(
                color = castleColor,
                topLeft = Offset(tx, castleBaseY - th),
                size = Size(tw, th)
            )
            drawRect(
                color = castleShadow,
                topLeft = Offset(tx + tw * 0.65f, castleBaseY - th),
                size = Size(tw * 0.35f, th)
            )
            val roofPath = Path().apply {
                moveTo(tx - tw * 0.15f, castleBaseY - th)
                lineTo(tx + tw * 0.5f, castleBaseY - th - tw * 0.95f)
                lineTo(tx + tw * 1.15f, castleBaseY - th)
                close()
            }
            drawPath(roofPath, color = castleRoof)
        }
        // Castle keep wall
        drawRect(
            color = castleColor,
            topLeft = Offset(w * 0.09f, castleBaseY - h * 0.075f),
            size = Size(w * 0.25f, h * 0.075f)
        )

        // Distant blue mountains on right
        val mountainPath = Path().apply {
            moveTo(w * 0.42f, h * 0.64f)
            lineTo(w * 0.72f, h * 0.44f)
            lineTo(w * 1.05f, h * 0.62f)
            lineTo(w * 1.05f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.72f)
            close()
        }
        drawPath(
            mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF7CB7FF), Color(0xFF3E82DB)),
                startY = h * 0.44f,
                endY = h * 0.68f
            )
        )

        // Rolling green meadow hills
        val backHill = Path().apply {
            moveTo(0f, h * 0.58f)
            quadraticTo(w * 0.28f, h * 0.50f, w * 0.58f, h * 0.60f)
            quadraticTo(w * 0.85f, h * 0.66f, w, h * 0.56f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            backHill,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF69DA45), Color(0xFF289F22)),
                startY = h * 0.52f,
                endY = h
            )
        )

        val frontHill = Path().apply {
            moveTo(0f, h * 0.74f)
            quadraticTo(w * 0.45f, h * 0.66f, w, h * 0.78f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            frontHill,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF56C932), Color(0xFF1A7A1D)),
                startY = h * 0.68f,
                endY = h
            )
        )

        // Floating decorative glossy bubbles around the sky
        drawGlossyBubble(Offset(w * 0.14f, h * 0.16f), w * 0.068f, BubbleColor.BLUE, 0.92f)
        drawGlossyBubble(Offset(w * 0.34f, h * 0.10f), w * 0.072f, BubbleColor.PURPLE, 0.90f)
        drawGlossyBubble(Offset(w * 0.07f, h * 0.25f), w * 0.056f, BubbleColor.ORANGE, 0.92f)
        drawGlossyBubble(Offset(w * 0.08f, h * 0.36f), w * 0.048f, BubbleColor.GREEN, 0.92f)
        drawGlossyBubble(Offset(w * 0.88f, h * 0.38f), w * 0.045f, BubbleColor.BLUE, 0.88f)
        drawGlossyBubble(Offset(w * 0.66f, h * 0.43f), w * 0.046f, BubbleColor.YELLOW, 0.92f)
        drawGlossyBubble(Offset(w * 0.87f, h * 0.44f), w * 0.052f, BubbleColor.PURPLE, 0.90f)
    }
}

/**
 * Scenic World Backdrop for Gameplay Screen (adapts palette across all 5 worlds!).
 */
@Composable
fun WorldGameplayBackground(
    worldIndex: Int,
    modifier: Modifier = Modifier
) {
    val (skyTop, skyMid, skyLow, mountainColor, waterTop, waterBot, grassTop, grassBot) = when (worldIndex % 5) {
        0 -> WorldPalette(
            Color(0xFF0A4EC4), Color(0xFF2196F3), Color(0xFF8BE2FF),
            Color(0xFF4F87D8), Color(0xFF38BDF8), Color(0xFF0284C7),
            Color(0xFF57CC36), Color(0xFF1F7A22)
        )
        1 -> WorldPalette(
            Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF7DD3FC),
            Color(0xFF335C99), Color(0xFF22D3EE), Color(0xFF0369A1),
            Color(0xFF34D399), Color(0xFF065F46)
        )
        2 -> WorldPalette(
            Color(0xFF581C87), Color(0xFFDB2777), Color(0xFFFBBF24),
            Color(0xFF9D174D), Color(0xFFFB923C), Color(0xFF9A3412),
            Color(0xFFD97706), Color(0xFF78350F)
        )
        3 -> WorldPalette(
            Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF2DD4BF),
            Color(0xFF1E293B), Color(0xFF0EA5E9), Color(0xFF1E1B4B),
            Color(0xFF0F766E), Color(0xFF064E3B)
        )
        else -> WorldPalette(
            Color(0xFF2E1065), Color(0xFF6D28D9), Color(0xFFC084FC),
            Color(0xFF4C1D95), Color(0xFF818CF8), Color(0xFF312E81),
            Color(0xFF4338CA), Color(0xFF1E1B4B)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to skyTop,
                    0.45f to skyMid,
                    0.74f to skyLow,
                    1.0f to grassBot
                )
            )
        )

        // Alpine mountains silhouette
        val mt = Path().apply {
            moveTo(0f, h * 0.72f)
            lineTo(w * 0.16f, h * 0.62f)
            lineTo(w * 0.34f, h * 0.71f)
            lineTo(w * 0.64f, h * 0.66f)
            lineTo(w * 0.85f, h * 0.55f)
            lineTo(w, h * 0.66f)
            lineTo(w, h * 0.78f)
            lineTo(0f, h * 0.78f)
            close()
        }
        drawPath(mt, color = mountainColor.copy(alpha = 0.75f))

        // Snow cap on right peak
        val snow = Path().apply {
            moveTo(w * 0.85f, h * 0.55f)
            lineTo(w * 0.79f, h * 0.59f)
            lineTo(w * 0.84f, h * 0.585f)
            lineTo(w * 0.90f, h * 0.595f)
            close()
        }
        drawPath(snow, color = Color.White.copy(alpha = 0.75f))

        // Serene alpine lake
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(waterTop, waterBot),
                startY = h * 0.70f,
                endY = h * 0.84f
            ),
            topLeft = Offset(0f, h * 0.70f),
            size = Size(w, h * 0.15f)
        )

        // Foreground grassy banks & stone shooter platform
        val bank = Path().apply {
            moveTo(0f, h * 0.78f)
            quadraticTo(w * 0.5f, h * 0.84f, w, h * 0.77f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            bank,
            brush = Brush.verticalGradient(
                colors = listOf(grassTop, grassBot),
                startY = h * 0.77f,
                endY = h
            )
        )

        // Stone deck at very bottom under the cannon
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF433878).copy(alpha = 0.78f), Color(0xFF1E1B4B).copy(alpha = 0.92f)),
                startY = h * 0.85f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.85f),
            size = Size(w, h * 0.15f)
        )
    }
}

private data class WorldPalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyLow: Color,
    val mountain: Color,
    val waterTop: Color,
    val waterBot: Color,
    val grassTop: Color,
    val grassBot: Color
)

/**
 * Stylized stacked "Bubble Shooter Pro" arcade logo with red ribbon banner.
 */
@Composable
fun BubbleShooterLogoCrest(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Bubble" in sunny gold with deep royal outline illusion
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Bubble",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF3B0764),
                modifier = Modifier.offset(x = 0.dp, y = 4.dp)
            )
            Text(
                text = "Bubble",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF59D), GoldStarMid, Color(0xFFFF8C00))
                    )
                )
            )
        }

        // "Shooter" in bright crisp white-lilac with purple shadow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(y = (-10).dp)
        ) {
            Text(
                text = "Shooter",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF3B0764),
                modifier = Modifier.offset(x = 0.dp, y = 4.dp)
            )
            Text(
                text = "Shooter",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFE9D5FF))
                    )
                )
            )
        }

        // Red ribbon banner "Pro"
        Box(
            modifier = Modifier
                .offset(y = (-8).dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RibbonRedLight, RibbonRedMid, RibbonRedDark)
                    )
                )
                .border(2.dp, Color(0xFFFFB4B4), RoundedCornerShape(18.dp))
                .padding(horizontal = 34.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
        }
    }
}

/**
 * Tactile 3D Glossy Pill Game Button with top specular highlight, bottom bevel, and press animation.
 */
@Composable
fun GlossyGameButton(
    text: String,
    icon: ImageVector,
    topColor: Color,
    midColor: Color,
    bottomColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "btnScale")

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .shadow(10.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(bottomColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(bottom = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(topColor, midColor)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Top glossy shine bar
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp, start = 18.dp, end = 18.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.30f))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            }
        }
    }
}
