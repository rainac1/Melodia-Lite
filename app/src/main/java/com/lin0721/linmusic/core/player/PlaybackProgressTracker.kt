package com.lin0721.linmusic.core.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// 进度轮询：只在真正播放时读控制器；退到后台后把轮询降到 30 秒一次。
class PlaybackProgressTracker(
    private val scope: CoroutineScope,
    private val controller: MediaControllerHolder
) {

    companion object {
        private const val FOREGROUND_INTERVAL_MS = 1_000L
        private const val BACKGROUND_INTERVAL_MS = 30_000L
    }

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // 保留旧接口：前台轮询间隔仍可由上层设置，但不再允许低于 1 秒。
    private val _updateInterval = MutableStateFlow(FOREGROUND_INTERVAL_MS)
    val updateInterval: StateFlow<Long> = _updateInterval.asStateFlow()

    private var job: Job? = null

    fun setUpdateInterval(intervalMs: Long) {
        _updateInterval.value = intervalMs.coerceAtLeast(FOREGROUND_INTERVAL_MS)
    }

    fun setPosition(positionMs: Long) {
        _currentPosition.value = positionMs
    }

    fun setDuration(durationMs: Long) {
        _duration.value = durationMs
    }

    // 从控制器同步最新时长，控制器未就绪时保留已有有效时长
    fun updateDurationFromController() {
        val dur = controller.duration
        if (dur > 0L) {
            _duration.value = dur
        }
    }

    // 立即重置当前进度与时长，避免上一首歌曲的数据在加载新歌期间残留导致进度条闪烁；断点续播可保留已有时长
    fun resetTo(startPosition: Long, preserveDuration: Boolean = false) {
        _currentPosition.value = startPosition
        if (!preserveDuration) {
            _duration.value = 0L
        }
    }

    fun start(
        isPlaying: StateFlow<Boolean>,
        isForeground: StateFlow<Boolean>,
        onTick: suspend (positionMs: Long, foreground: Boolean) -> Unit
    ) {
        job?.cancel()
        job = scope.launch {
            combine(isPlaying, isForeground) { playing, foreground -> playing to foreground }
                .distinctUntilChanged()
                .collectLatest { (playing, foreground) ->
                    // 暂停时完全退出循环，不再每秒唤醒一次。
                    if (!playing) return@collectLatest
                    while (true) {
                        val isReady = controller.playbackState == Player.STATE_READY
                        if (isReady) {
                            val curPos = controller.currentPosition
                            _currentPosition.value = curPos
                            onTick(curPos, foreground)
                        }
                        delay(if (foreground) _updateInterval.value else BACKGROUND_INTERVAL_MS)
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
