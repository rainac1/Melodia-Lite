# Android 功耗优化说明

本仓库是 Melodia 的 **Android 专属功耗优化分支**。项目目标并非与上游保持功能对等，而是在 Android 端尽可能减少后台唤醒、常驻渲染与无效网络请求；同时保持共享层与核心目录结构贴近上游，使后续 `git merge upstream/main` 的维护成本可控。

当前上游基线：`v2.3.0`（2026-11-17）。

---

## 取舍原则

1. **只删主动实现，保留被动模型**：共享层的数据模型、设置项声明与插件结构尽量与上游保持一致，Android 端不加载、不渲染对应特性，避免无谓的跨层分叉。
2. **明确移除的功能**：桌面端模块与桌面测试、歌词运行时与全部歌词 UI、Karaoke 逐字动画、播放器动态背景逐帧渲染、原生插件实现。
3. **随上游保留的功能**：多账户切换、私信/消息页、一起听、私人 FM、网易云外链与剪贴板识别、通知栏按钮自定义、界面精简开关、预见式返回、本地下载识别、音质徽标、按需渲染的分享卡片等。这些能力默认不产生额外常驻后台开销，或仅在用户主动进入对应页面时工作。

---

## 功能范围对照

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 桌面端（`desktopApp`）、桌面源集/测试、Windows 打包与官网 | 已移除 | 仅保留 Android 端（`:app`、`:shared`、`:baselineprofile`） |
| 歌词运行时、全屏/迷你/悬浮/外部歌词、歌词设置、播放器歌词分享入口 | 已移除 | `LyricsResolver` 与 `PlayerViewModel` 歌词状态已移除 |
| Karaoke 逐字动画、播放器动态背景 / Mesh 动画 | 已移除 | 播放器统一使用静态渐变背景 |
| 原生插件实现（`plugin-api`） | 已移除 | 保留 `NativePluginHost` 接口定义，Android 端注入为 `null` |
| 共享歌词模型、`FloatingLyric*` 设置项、插件歌词与桌面布局枚举 | 保留（被动） | 仅保留类型定义与配置解析，不主动加载或渲染 |
| 分享卡片（`TrackShareCardSheet` / `ShareCard`） | 保留（按需） | 供曲目详情、歌单、歌手、搜索、本地音乐及私信分享使用，仅按需渲染 |
| 插件 `backdrops` 状态 | 保留（被动） | 仅解析插件声明，Android 端不绘制动态背景 |

---

## 功耗参数与后台策略

| 项目 | 当前取值 | 说明 |
| --- | --- | --- |
| 播放进度轮询（`PlaybackProgressTracker`） | 仅播放时运行；前台 `1s`，后台 `30s` | 暂停时完全退出轮询循环，避免每秒唤醒 |
| 播放状态落盘（`PlayerManager`） | 前台 `30s`，后台 `60s` | 暂停、Seek 与退出时仍会立即保存 |
| ExoPlayer 缓冲（`buildLoadControl`） | `30s–120s`，目标上限 `32MB` | 起播 `2.5s` / 重缓冲 `5s`，降低后台播放时反复拉起射频与解码器的频率 |
| 播放数据源 HTTP 超时（`MelodiaPlaybackService`） | 连接 `15s`，读取 `30s` | 放宽超时以减少弱网下频繁重连导致的射频唤醒 |
| AudioTrack 缓冲（`MelodiaRenderersFactory`） | PCM `1s`，直通 / 硬件卸载 `2s` | 拉长音频输出层缓冲，降低输出线程唤醒频次 |
| 硬件音频解码 / Audio Offload | 默认开启 | 交给 DSP 硬件解码；设备不支持、或开启交叉淡化 / USB 独占输出时自动回退或互斥 |
| 插件进度事件（`PluginEventDispatcher`） | `5s` | 降低插件进度广播频率 |
| 播客进度采样（`PodcastProgressTracker`） | `5s` | 降低播客进度记录与持久化频率 |
| 外部打断恢复兜底轮询（`ExternalInterruptionResumeController`） | `3s` | 主要依赖系统 `AudioPlaybackCallback`，定时轮询仅作兜底 |
| 应用更新检查（`MelodiaApplication`） | 前台可见后延迟 `3s` 检查 | 等待 `AppVisibility.foreground == true`，避免后台被媒体键或蓝牙拉起时触发网络唤醒 |
| 蓝牙常驻广播 | 已移除 | 移除原版 `BluetoothReceiver` 常驻 ACL 广播接收器 |

### 后台可见性收敛（`AppVisibility.foreground == false`）

当应用退到后台时，`PlayerManager` 的进度轮询回调会跳过以下可能唤醒网络或 CPU 的周期性任务：

- 下一首播放链接预取（`maybePrefetchNextTrackUrl`）
- 相似漫游续播与私人 FM 预取调度（`roaming.onProgressTick` / `personalFm.onProgressTick`）
- 自动交叉淡化预调度（`maybeScheduleAutoCrossfade`）
- 响度分析与预测量（`maybeMeasureCurrentLoudness` / `maybePremeasureNextLoudness`）
- 边听边存进度检查（`maybeCheckStreamCacheProgress`）

后台自然切歌由 ExoPlayer 的 `Player.STATE_ENDED` 回调直接兜底，不依赖进度轮询驱动。

---

## 构建与功耗验证

### 编译与单元测试

版本号不设默认值，缺失 `releaseVersionName` 或 `releaseVersionCode` 时 Gradle 会在配置阶段直接失败；`releaseVersionCode` 必须为正整数：

```bash
./gradlew :app:compileDebugKotlin \
  -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001

./gradlew :app:testDebugUnitTest \
  -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001

./gradlew :app:assembleDebug \
  -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
```

### 真机功耗抽查

安装后可通过以下命令观察后台唤醒与锁屏表现：

```bash
adb shell dumpsys batterystats --charged com.lin0721.linmusic
adb shell dumpsys power | grep -iE "wake|suspend"
adb shell dumpsys alarm | grep com.lin0721
```

建议重点核查：

- 暂停状态下无进度轮询造成的周期性唤醒
- 后台持续播放时 CPU 唤醒控制在约 2 次/分钟以内
- 后台播放期间无预取、响度分析或边听边存触发的额外网络请求
- 播放页处于静态渲染状态，无逐帧重绘开销

---

## 跟进上游维护指南

拉取并合入上游更新：

```bash
git fetch upstream
git merge upstream/main
```

### 冲突处理约定

- **桌面端 / 歌词 / 原生插件实现**：保持删除；仅在共享层保留不影响运行时与编译的被动类型及接口。
- **播放器 / 设置等双方均修改的文件**：优先采用上游实现，再对照上表重新套用本分支的功耗参数与后台 gating 逻辑。
- **新增后台任务核查**：合入后先完整运行编译与单元测试；若上游新增了后台轮询、预加载或常驻监听器，先评估其唤醒开销与前台 gating 必要性，再决定是否保留。
