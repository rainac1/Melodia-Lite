# Melodia 功耗优化 TODO（临时）

> 当前提交：`0b1fce6c feat(android): 移除歌词/Karaoke/动态背景并收敛后台播放唤醒`
>
> 范围：Android 端已处理；桌面端按你的要求保持原样。

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

## 下一步（按优先级）

### P0：先保证可编译、行为可回归

- [ ] 本地执行 `./gradlew :app:compileDebugKotlin :shared:compileKotlinMetadata`（当前环境 Gradle 发行包下载超时，未能本地编译）。
- [ ] 清理 `FullPlayerScreen` 里遗留的空参数/回调：
  - `onLyricsFullScreenChange`
  - `onLyricsControlsVisibilityChange`
  - `MelodiaApp` 的 `isLyricsFullScreen` / `isLyricsControlsVisible` 状态
  - `MelodiaOverlays` / `PlayerDockPanel` 的透传参数
- [ ] 清理 `PlayerSongDetailState` 的歌词兼容字段：
  - `lyrics`、`lyricsSource`、`isLyricsLoading`
  - `PlayerViewModel` 的 `currentLyricIndex` / `activeLyricIndices` / `showMiniLyric` 兼容 stub
  - 同步删除 `VisibleInfoCards` / `FullPlayerInfoSection` / `MelodiaBottomBar` 中最后的歌词分支
- [ ] 检查 `CapsuleSlider` 替换后的三个设置页 UI（播放、外观、本地音乐）行为是否与原来的滑块一致。
- [ ] 跑一遍 Android 单元测试；歌词相关测试已删，剩余测试如引用旧字段需要修。

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

### P2：彻底清理歌词底层残留（可选，取决于是否保留插件/分享兼容）

- [ ] `LyricsResolver`、`AmllLyricsClient`、`QqLyricsClient`、`QqLyricsSource`、`LyricsSourceState`、TTML/QRC/LRC parser。
- [ ] `LyricLine` / `LyricSource` / `LyricProgressTracker` / `LyricTimeline` 模型。
- [ ] `PlaybackRepository.getLyrics/getRawLyrics` 与 `PlaybackApi` 的歌词请求/响应。
- [ ] `SettingsPreferences` 中所有 `*lyric*` / `*karaoke*` 设置键与 Flow。
- [ ] `FloatingLyricStyle` / `FloatingLyricBounds` / `LyricResize` 与对应偏好。
- [ ] `Shared` 的歌词分享模型、卡片、`ShareCardKind.LYRIC`；若保留歌曲分享需先拆分。
- [ ] 插件 API 的 `lyricsSource` / `MelodiaPlugin.lyric` / `NeteaseLyricLine`：
  - 删除会破坏第三方插件兼容；如果不再需要外部插件歌词，可一并删。
  - 当前为了“对外部代码影响最小”暂时保留。
- [ ] 清理 `PlayerLayout` 中 `MINI_LYRIC` / `LYRICS_CARD` / `LYRICS_VIEW` / 歌词 HostAction 的宿主实现（当前是空占位）。

### 桌面端（暂不处理）

- 桌面歌词窗口、全屏歌词、Karaoke、动态插件背景均未改动。
- 如果后续也要砍，建议单独提交，避免和 Android 改动混在一起。

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
