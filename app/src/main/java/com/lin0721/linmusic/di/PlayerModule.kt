package com.lin0721.linmusic.di

import com.lin0721.linmusic.core.offline.CachedAudioIndex
import com.lin0721.linmusic.core.player.AndroidCachedAudioIndex
import com.lin0721.linmusic.core.player.PlaybackController
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.ExternalInterruptionResumeController
import com.lin0721.linmusic.feature.podcast.data.PodcastProgressTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playerModule = module {
    single { ExternalInterruptionResumeController(androidContext(), get()) }
    single {
        PlayerManager(androidContext(), get(), get(), get(), get(), get(), get(), get(), get(), get()).also { manager ->
            // 随播放器一同创建，进程内常驻记录播客收听进度
            PodcastProgressTracker(manager, get()).start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        }
    }
    single<PlaybackController> { get<PlayerManager>() }
    // 离线时的可播放判定：已下载 + 播放缓存已完整
    single<CachedAudioIndex> { AndroidCachedAudioIndex(androidContext(), get(), get()) }
}

