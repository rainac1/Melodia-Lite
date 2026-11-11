package com.lin0721.linmusic.feature.player.ui

import android.media.AudioDeviceInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.uiTween
import com.lin0721.linmusic.feature.podcast.ui.PodcastPlayerState
import com.lin0721.linmusic.core.player.PlayMode
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// 播放器主区之间的留白项个数，与下方 fillGapItem 调用次数保持一致
const val FullPlayerFillGapCount = 4

// 撑满一屏时参与高度计算的主区项，需与下方 item key 保持一致
val FullPlayerPlaybackItemKeys = listOf("cover", "song_info", "progress", "controls", "actions")

// 播放器主区：封面、歌名歌手、进度条、播放控制、快捷操作
// fillGap 非空时在各区块间插入等高留白，并在末尾补 fillTail 高度，使主区恰好铺满首屏、后续项留在屏外
fun LazyListScope.fullPlayerPlaybackSection(
    songState: PlayerSongDetailState,
    coverUrl: String,
    previousCoverUrl: String?,
    nextCoverUrl: String?,
    onSwipeToPrevious: () -> Unit,
    onCancelSwipe: () -> Boolean,
    currentKey: Any,
    previousKey: Any? = null,
    nextKey: Any? = null,
    // 封面额外水平内缩，面板模式下由 FullPlayerScreen 按可用高度算出
    coverExtraInset: Dp = 0.dp,
    onToggleSidebarFullscreen: (() -> Unit)? = null,
    isSidebarFullscreen: Boolean = false,
    title: String,
    artist: String,
    playContext: String?,
    podcast: PodcastPlayerState = PodcastPlayerState(),
    onToggleSubscribe: () -> Unit = {},
    // 大播放按钮专用：弱网缓冲期间也要立刻显示"暂停中"，不等音频真正流出
    playWhenReady: Boolean,
    currentPositionProvider: () -> Long,
    duration: Long,
    playMode: PlayMode,
    onClose: () -> Unit,
    onMoreClick: () -> Unit,
    onToggleLike: () -> Unit,
    onArtistClick: () -> Unit,
    onAlbumClick: (() -> Unit)? = null,
    onPlaylistClick: (() -> Unit)? = null,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onDisableRoaming: () -> Unit,
    onDisableIntelligence: () -> Unit,
    onOutputDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    connectedDevice: AudioDeviceInfo? = null,
    qualityLabel: String? = null,
    baseCoverPadding: Dp = MelodiaSpacing.lg,
    itemGap: Dp = 0.dp,
    fillGap: Dp? = null,
    fillTail: Dp = 0.dp
) {
    fun fillGapItem(index: Int) {
        val gapHeight = fillGap ?: (if (itemGap > 0.dp) itemGap else null) ?: return
        item(key = "$FullPlayerFillGapKeyPrefix$index") {
            Spacer(modifier = Modifier.height(gapHeight))
        }
    }

    item(key = "cover") {
        FullPlayerItemEnterAnimation {
            FullPlayerCoverArt(
                coverUrl = coverUrl,
                playContext = playContext,
                podcastRadioName = podcast.radioName,
                onClose = onClose,
                onMoreClick = onMoreClick,
                previousCoverUrl = previousCoverUrl,
                nextCoverUrl = nextCoverUrl,
                onSwipeToPrevious = onSwipeToPrevious,
                onSwipeToNext = onPlayNext,
                currentKey = currentKey,
                previousKey = previousKey,
                nextKey = nextKey,
                coverExtraInset = coverExtraInset,
                baseCoverPadding = baseCoverPadding,
                onToggleSidebarFullscreen = onToggleSidebarFullscreen,
                isSidebarFullscreen = isSidebarFullscreen,
                onPlaylistClick = onPlaylistClick,
                onCancelSwipe = onCancelSwipe
            )
        }
    }

    fillGapItem(1)

    item(key = "song_info") {
        SongInfo(
            title = title,
            artist = artist,
            isLiked = songState.isLiked,
            onToggleLike = onToggleLike,
            onArtistClick = onArtistClick,
            onAlbumClick = onAlbumClick,
            podcast = podcast,
            onToggleSubscribe = onToggleSubscribe
        )
    }


    item(key = "progress") {
        // 播放器尚未拿到时长时退回歌曲详情里的时长
        val displayDuration = if (duration > 0L) duration else (songState.songDetail?.dt ?: 0L)
        ProgressSection(
            currentPositionProvider = currentPositionProvider,
            duration = displayDuration,
            onSeek = onSeek,
            chorusStartMs = songState.chorusStartMs
        )
    }

    fillGapItem(3)

    item(key = "controls") {
        PlaybackControls(
            isPlaying = playWhenReady,
            onTogglePlay = onTogglePlay,
            onPlayNext = onPlayNext,
            onPlayPrevious = onPlayPrevious,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            playMode = playMode,
            isRoaming = playContext == "similar_roaming",
            onDisableRoaming = onDisableRoaming,
            isIntelligence = playContext == "intelligence",
            onDisableIntelligence = onDisableIntelligence,
            showModeButtons = !podcast.isPodcast
        )
    }

    fillGapItem(4)

    item(key = "actions") {
        ActionButtons(
            onOutputDeviceClick = onOutputDeviceClick,
            onQueueClick = onQueueClick,
            connectedDevice = connectedDevice,
            qualityLabel = qualityLabel
        )
    }

    if (fillGap != null) {
        item(key = "${FullPlayerFillGapKeyPrefix}tail") {
            Spacer(modifier = Modifier.height(fillTail))
        }
    }
}
