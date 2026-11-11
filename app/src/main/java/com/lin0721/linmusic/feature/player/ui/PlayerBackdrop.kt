package com.lin0721.linmusic.feature.player.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.skin.CoverGroup
import com.lin0721.linmusic.core.ui.skin.isCoverBackdrop
import com.lin0721.linmusic.core.ui.theme.smoothVerticalGradient
import com.lin0721.linmusic.core.ui.theme.themedCoverBase
import kotlin.random.Random

// 播放器背景为静态单色渐变：不做逐帧重绘和全屏 blur，避免持续 GPU/CPU 唤醒。
private const val COLLAPSED_GRADIENT_END_DP = 1100f
private const val COLLAPSED_GRADIENT_STEPS = 64
private const val COLLAPSED_FADE_END_FRACTION = 0.8f
private const val NOISE_TILE_PX = 128
private const val NOISE_MAX_ALPHA = 4
private const val NOISE_MASK_SOLID_FRACTION = 0.9f
private const val NOISE_SEED = 20261005

private fun createNoiseBrush(): ShaderBrush {
    val random = Random(NOISE_SEED)
    val pixels = IntArray(NOISE_TILE_PX * NOISE_TILE_PX) {
        (random.nextInt(NOISE_MAX_ALPHA + 1) shl 24) or 0x00FFFFFF
    }
    val bitmap = Bitmap.createBitmap(pixels, NOISE_TILE_PX, NOISE_TILE_PX, Bitmap.Config.ARGB_8888)
    return ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

// 全屏播放器背景：静态单色平滑渐隐，不随播放状态动起来。
// 背景绘制和 content 拆成两个子 Box，content 保持清晰。
@Composable
fun PlayerBackdrop(
    base: Color,
    modifier: Modifier = Modifier,
    translationYProvider: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit = {}
) {
    // 透出皮肤背景图时由外层整页画背景，这里不再叠加封面色渐变
    if (isCoverBackdrop(CoverGroup.PLAYER)) {
        Box(modifier, content = content)
        return
    }

    val density = LocalDensity.current
    val gradientEndY = with(density) { COLLAPSED_GRADIENT_END_DP.dp.toPx() }

    // base 本身已偏深且明度有上限，深色下直接铺底；浅色下取同色相浅调，两者都渐隐到页面底
    val fillColor = themedCoverBase(base)
    val endColor = Color.Transparent
    val gradientBrush = remember(fillColor, endColor, gradientEndY) {
        Brush.smoothVerticalGradient(
            from = fillColor,
            to = endColor,
            startY = 0f,
            endY = gradientEndY,
            fadeEndFraction = COLLAPSED_FADE_END_FRACTION,
            steps = COLLAPSED_GRADIENT_STEPS
        )
    }
    // 开启抖动打散 8-bit 色阶，消除大面积暗色渐变的条纹
    val ditherPaint = remember { Paint().apply { asFrameworkPaint().isDither = true } }
    val noiseBrush = remember { createNoiseBrush() }
    val noiseMaskBrush = remember(gradientEndY) {
        Brush.verticalGradient(
            0f to Color.Black,
            NOISE_MASK_SOLID_FRACTION to Color.Black,
            1f to Color.Transparent,
            startY = 0f,
            endY = gradientEndY * COLLAPSED_FADE_END_FRACTION
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1200.dp)
            .graphicsLayer { translationY = translationYProvider() }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                // 独立离屏层，渐变与噪声的合成结果缓存，不逐帧重绘
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        gradientBrush.applyTo(size, ditherPaint, 1f)
                        canvas.drawRect(0f, 0f, size.width, size.height, ditherPaint)
                    }
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                        drawRect(brush = noiseBrush)
                        drawRect(brush = noiseMaskBrush, blendMode = BlendMode.DstIn)
                        canvas.restore()
                    }
                }
        )
        content()
    }
}
