package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

private val LeftColumnMaxWidth = 420.dp
private val LeftColumnStartPadding = 48.dp
private val ColumnGap = 56.dp
private val RightColumnEndPadding = 24.dp
private const val DEFAULT_LEFT_FRACTION = 0.4f

// 平板横屏全屏播放器：左栏封面与播放控件固定不动；右栏只承载信息卡片。
// 歌词已整体移除，保留原参数签名以减小对调用方的改动。
@Composable
fun FullPlayerWideLayout(
    sourceBar: @Composable (Modifier) -> Unit,
    cover: @Composable (Modifier) -> Unit,
    playbackControls: @Composable ColumnScope.() -> Unit,
    lyrics: List<LyricLine>,
    isLyricsLoading: Boolean,
    currentLyricIndex: Int,
    activeLyricIndices: Set<Int> = emptySet(),
    highlightColor: Color,
    currentPositionProvider: () -> Long,
    isPlaying: Boolean,
    onLyricClick: (LyricLine) -> Unit,
    onSeek: (Long) -> Unit = {},
    onLyricsVisibleChange: (Boolean) -> Unit,
    infoCards: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
    // 插件布局：替换左栏的封面与播放控件，leftFraction 为左栏宽度占比
    pluginLeftColumn: (@Composable () -> Unit)? = null,
    leftFraction: Float? = null
) {
    val left = leftFraction ?: DEFAULT_LEFT_FRACTION
    Column(modifier = modifier.fillMaxSize()) {
        sourceBar(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MelodiaSpacing.lg)
                .padding(top = MelodiaSpacing.md)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = MelodiaSpacing.sm, bottom = MelodiaSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .weight(left)
                    .fillMaxHeight()
                    .padding(start = LeftColumnStartPadding),
                contentAlignment = Alignment.Center
            ) {
                if (pluginLeftColumn != null) {
                    Box(modifier = Modifier.widthIn(max = LeftColumnMaxWidth).fillMaxSize()) {
                        pluginLeftColumn()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = LeftColumnMaxWidth)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 封面取栏宽与扣掉控件后剩余高度中较小者，控件区始终完整可见
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            cover(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
                        }
                        Spacer(modifier = Modifier.height(MelodiaSpacing.md))
                        playbackControls()
                    }
                }
            }
            Spacer(modifier = Modifier.width(ColumnGap))
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .weight(1f - left)
                    .fillMaxHeight()
                    .padding(end = RightColumnEndPadding)
            ) {
                infoCards()
                item(key = "wide_bottom_spacer") {
                    Spacer(modifier = Modifier.height(MelodiaSpacing.lg))
                }
            }
        }
    }
}
