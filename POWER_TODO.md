# Melodia 功耗优化 TODO（临时）

> 基线提交：`0b1fce6c feat(android): 移除歌词/Karaoke/动态背景并收敛后台播放唤醒`
>
> 范围：只保留 Android 端；桌面端代码、构建、测试与 workflow 已移除。

## 本次已完成

- Android 歌词运行时与 UI 基本移除：
  - 歌词拉取、全屏歌词、小歌词、歌词卡、悬浮歌词、外部歌词（Lyricon / SuperLyric / LyricInfo / FluidCloud / 蓝牙歌词）、歌词分享、歌词设置、本地标签歌词、下载内嵌歌词。
  - 删除对应 AIDL、Lyricon 依赖、悬浮窗权限和设置入口。
- Karaoke 逐帧渲染移除（Android）。
- 播放器动态背景改为静态渐变：移除 mesh 游走、逐帧重绘和全屏 blur。
- 播放进度轮询：
  - 只在真正播放时运行；暂停完全退出循环。
  - 前台 1 秒，后台 30 秒。
  - 全屏歌词专用的 50ms 轮询已删除。
- 播放状态落盘：
  - 前台 30 秒、后台 60 秒；暂停/seek/退出仍即时保存。
- 后台网络/CPU 收敛：
  - 后台停止预取下一首、相似漫游、响度分析、边听边存、封面预载。
  - 移除蓝牙 ACL 常驻接收器。
  - 更新检查延后到界面可见。
  - 插件进度事件 1s → 5s；播客进度采样 1s → 5s；外部打断恢复兜底轮询 1s → 3s。

### 本轮新增：后台播放的主功耗点

- ExoPlayer 单次缓冲拉长：`min 30s / max 120s`，单次网络加载尽量覆盖更长时间，减少反复拉起射频；32MB 字节上限避免高解析曲目占用过多内存。
- HTTP 超时放宽：连接 15s、读取 30s，减少弱网下频繁重连。
- 音频输出缓冲拉长：PCM 1s，直通/硬件卸载 2s，减少 AudioTrack 唤醒频率。
- 硬件音频解码默认开启；不支持时由 ExoPlayer 自动回退软件解码；与交叉淡化/独占输出的互斥逻辑保持不变。

### 本轮进一步清理

- 播放页彻底移除歌词参数、歌词 state 读取与空回调：`FullPlayerScreen`、`FullPlayerWideLayout`、`FullPlayerPlaybackSection`、`FullPlayerInfoSection`、`FullPlayerLayoutParts`、`FullPlayerSheets`。
- 迷你播放器与更多菜单移除小歌词开关、当前歌词索引与绑定中的 `hasLyrics` 计算。
- 卡片编辑器与首屏卡片列表过滤旧配置里的歌词卡，恢复默认也不会再带回。
- 插件布局中的 `MINI_LYRIC` / `LYRICS_CARD` / `LYRICS_VIEW` 与歌词 HostAction 宿主空占位已删除，统一落到 `else -> Unit`。
- 播放器静态背景删除 `BackdropMode` / `animateMesh` 参数及动态背景残留色板字段。
- 删除 Android 已无调用的歌词/分享死入口：播放页分享按钮、分享菜单项、全屏歌词按钮。
- `SettingsPreferences` 删除悬浮歌词、小歌词、外部歌词源等已无调用的设置键与 Flow。
- 清理删除功能留下的空目录与过期注释/设置文案。

### 桌面端移除

- 删除 `desktopApp` 模块及其界面、播放器、平台实现、原生打包脚本、图标与 ProGuard 配置。
- 删除 `shared/src/desktopMain` 与 `shared/src/desktopTest`，共享层只保留 `commonMain` + `androidMain`。
- 删除桌面端专用的 `plugin-sample` 原生插件示例模块，以及仅它和桌面端使用的 `plugin-api` 原生插件 API 模块。
- 从 `settings.gradle.kts`、`shared/build.gradle.kts`、`gradle.properties` 与版本目录移除桌面目标、依赖、JNA/JAudiotagger/GraalJS/libmpv 参数与 Kotlin JVM 插件。
- CI/Release workflow、PR 模板、Issue 模板与 README/RELEASE_SIGNING 文档改为 Android-only。
- 删除整个 `site` 官网源码、桌面截图与 Pages workflow；插件/皮肤文档移除桌面入口、Windows 下载和原生插件说明。
- 清理 `.gitignore` 中桌面动态库条目。

## 下一步（按优先级）

### P0：先保证可编译、行为可回归

- [x] 本地执行 `./gradlew :app:compileDebugKotlin :shared:compileKotlinMetadata`，构建成功。
  - 本机通过腾讯镜像安装 Android SDK 36/36.1、build-tools 36.0.0、NDK 28.2.13676358、CMake 3.22.1，并用 Temurin JDK 21。
  - Gradle 依赖通过 `/tmp/mirrors.gradle` 临时切换到阿里云 Maven 镜像。
- [x] `./gradlew :app:testDebugUnitTest` 通过。
- [x] 清理 `FullPlayerScreen` 里遗留的空参数/回调：
  - `onLyricsFullScreenChange`
  - `onLyricsControlsVisibilityChange`
  - `MelodiaApp` 的 `isLyricsFullScreen` / `isLyricsControlsVisible` 状态
  - `MelodiaOverlays` / `PlayerDockPanel` 的透传参数
- [x] Android 端不再读取 `PlayerSongDetailState` 的歌词字段（`lyrics` / `lyricsSource` / `isLyricsLoading`）；
  - `PlayerViewModel` 的 `showMiniLyric` / `toggleMiniLyric` 兼容 stub 已删除；`currentLyricIndex` / `activeLyricIndices` 暂无 Android 调用。
  - 同步删除 `VisibleInfoCards` / `FullPlayerInfoSection` / `MelodiaBottomBar` 中 Android 端的歌词分支与 state 读取。
- [x] 检查 `CapsuleSlider` 替换后的三个设置页 UI（播放、外观、本地音乐）行为是否与原来的滑块一致。
- [x] 跑一遍 Android 单元测试；已通过。
- [x] 清理后重新执行 `:app:compileDebugKotlin`、`:app:testDebugUnitTest` 通过；桌面端已移除，相关构建与测试不再执行。

### P1：继续降低后台播放唤醒

- [ ] 评估把 `PlaybackState` 从 `playback_prefs` 拆到独立小 DataStore：
  - 目前 30/60 秒保存仍会重写包含整份播放队列 JSON 的 `playback_prefs`，存在写放大。
  - 目标：进度单独存小文件，队列只在队列变更时写。
- [ ] `MediaSessionService.loadCoverBitmap` 在后台仍会按曲目请求封面；可考虑后台跳过网络封面，直接用缓存或占位图。
- [ ] `MediaSessionService.checkAndFetchLikedStatus` 首次播放会拉喜欢列表；评估是否改为按需/前台拉取。
- [ ] `LeapmotorMediaClaim` 车机心跳仍是 1 秒一次；评估放宽到 2–5 秒，并且 `!selfOnTop && !playing` 时完全停掉循环。
- [ ] `LxPluginManager.warmUp` / `PluginRepository.warmUp` 仍在进程启动时执行；评估延后到界面可见或首次播放。
- [ ] `CrossfadePlayer` 始终创建两个 ExoPlayer；评估交叉淡化关闭时懒创建 secondary。
- [ ] 验证硬件音频解码默认开启后的兼容性：部分机型 offload 有 bug，必要时按设备/格式回退。
- [ ] 评估 120s 缓冲在弱网/高解析下的内存与起播延迟，必要时按网络类型动态调整。
- [ ] `MediaSessionService` 暂停后的前台超时使用 Media3 默认值；如需更快释放进程，可显式设置更短超时。
- [ ] 给 `AppVisibility` 增加单元测试/日志，确认多 Activity、透明 Activity、分屏切换时不会误判前台。

### P2：桌面兼容遗留清理（已完成）

- [x] 删除 `LyricsResolver`、`AmllLyricsClient`、`QqLyricsClient`、`QqLyricsSource`、`LyricsSourceState`、TTML/QRC/LRC parser。
- [x] 删除 `LyricLine` / `LyricSource` / `LyricProgressTracker` / `LyricTimeline` 模型。
- [x] 删除 `PlaybackRepository.getLyrics/getRawLyrics` 与 `PlaybackApi` 的歌词请求/响应。
- [x] 删除 `SettingsPreferences` 中全部歌词/Karaoke 设置键与 Flow，包括下载内嵌歌词开关。
- [x] 删除 `FloatingLyricStyle` / `FloatingLyricBounds` / `LyricResize` 与歌词分享卡片模型。
- [x] 删除插件 `lyricsSource`、脚本 `melodia.register.lyricsSource`、`song.lyric` 网易接口与相关扩展点。
- [x] 删除 `PlayerLayout` 中 `MINI_LYRIC` / `LYRICS_CARD` / `LYRICS_VIEW`、歌词页区域与歌词 HostAction。
- [x] 删除 `PlayerSongDetailState` 歌词字段、`currentLyricIndex` / `activeLyricIndices` stub 与 `FullPlayerCard.LYRICS`。
- [x] 删除桌面布局区域、桌面 HostAction、`PluginPlatform.DESKTOP`、`NativePluginHost` 与原生插件设置。
- [x] 删除原生插件格式、JAR/DLL 包校验、原生动作与 Compose 页面扩展。
- [x] 删除插件动态背景（`backdrops` / `BackdropExtension` / 逐帧渲染设置）。

## 验证方法

```bash
# 先编译
./gradlew :app:compileDebugKotlin :shared:compileKotlinMetadata

# 安装后重点看
adb shell dumpsys batterystats --charged com.lin0721.linmusic
adb shell dumpsys power | grep -iE "wake|suspend"
adb shell dumpsys alarm | grep com.lin0721
```

再用 Perfetto / Android Studio Energy Profiler / Macrobenchmark `PowerMetric` 对比：

- 暂停时是否还有进度轮询唤醒（预期：没有）。
- 后台播放时 CPU 唤醒次数（预期：约 2 次/分钟以内）。
- 后台播放时网络请求（预期：只有流媒体本身，没有预取/封面/边听边存）。
- 全屏播放页/播放器背景是否还有逐帧重绘。
