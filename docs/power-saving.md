# Android 功耗优化说明

本仓库是 Melodia 的 Android 专属功耗优化分支。目标不是与上游功能对等，而是在 Android 端减少后台唤醒、常驻渲染与无效网络请求；同时尽量保持与上游一致的代码结构，让后续 `git merge upstream/main` 的成本可控。

当前基线：上游 `v2.3.0`（2026-11-17）。

## 取舍原则

1. **只删主动实现，保留被动模型**：共享层的数据模型、设置项与插件 API 尽量与上游一致；Android 端不加载、不渲染对应功能即可，避免无谓的分叉。
2. **删除的功能**：桌面端、歌词运行时与全部歌词 UI、Karaoke、动态背景逐帧渲染、原生插件实现。
3. **保留的功能**：随上游合入的多账户、私信/消息页、一起听、私人FM、网易云外链与剪贴板识别、通知栏按钮自定义、界面精简开关、预见式返回、本地下载识别、音质徽标等。它们默认不产生额外后台开销，或只在用户主动进入时工作。

## 功能范围

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 桌面端（`desktopApp`）、桌面源集/测试、Windows 打包、官网 | 删除 | 仅保留 Android |
| 歌词运行时、全屏/小/悬浮/外部歌词、歌词设置、歌词分享 | 删除 | `LyricsResolver` 与 `PlayerViewModel` 歌词状态不存在 |
| Karaoke 逐字动画、播放器动态背景/mesh | 删除 | 播放器使用静态渐变背景 |
| 原生插件 API 与实现（`plugin-api`） | 删除 | `NativePluginHost` 接口保留，Android 注入为 `null` |
| 共享歌词模型 / `FloatingLyric*` 设置模型 / 插件歌词与桌面布局枚举 | 保留 | 纯类型与解析，不主动加载或渲染 |
| 分享卡片 | 保留 | 私信分享需要，仅按需渲染 |
| 插件 `backdrops` 状态 | 保留 | 只解析插件声明，Android 不绘制动态背景 |

## 功耗参数

| 项目 | 取值 |
| --- | --- |
| 播放进度轮询 | 仅播放时运行；前台 1s，后台 30s，暂停完全退出 |
| 播放状态落盘 | 前台 30s，后台 60s；暂停/seek/退出立即保存 |
| ExoPlayer 缓冲 | 30s–120s，目标上限 32MB |
| HTTP 超时 | 连接 15s，读取 30s |
| AudioTrack 缓冲 | PCM 1s，直通/硬件卸载 2s |
| 硬件音频解码 | 默认开启，不支持时自动回退软解 |
| 插件进度事件 | 5s |
| 播客进度采样 | 5s |
| 外部打断恢复轮询 | 3s |
| 更新检查 | 等界面可见后再检查 |

后台（`AppVisibility.foreground == false`）时不做下一首预取、相似漫游、响度分析、边听边存、封面预载等会唤醒网络/CPU 的工作；自然切歌由 ExoPlayer 的 `ENDED` 兜底。

## 验证

```bash
./gradlew :app:compileDebugKotlin \
  -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
./gradlew :app:testDebugUnitTest \
  -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
./gradlew :app:assembleDebug \
  -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
```

版本号没有默认值，缺失时 Gradle 会在配置阶段直接失败；`releaseVersionCode` 必须是正整数。

安装后可用以下命令抽查：

```bash
adb shell dumpsys batterystats --charged com.lin0721.linmusic
adb shell dumpsys power | grep -iE "wake|suspend"
adb shell dumpsys alarm | grep com.lin0721
```

重点确认：暂停时无进度轮询唤醒；后台播放约 2 次/分钟以内 CPU 唤醒；后台无预取/封面/边听边存请求；播放页无逐帧重绘。

## 跟进上游

```bash
git fetch upstream
git merge upstream/main
```

冲突处理约定：

- 上游新增的桌面/歌词/原生插件实现：保持删除；仅保留共享层被动类型与接口。
- 双方都改过的播放器/设置文件：优先取上游，再重新套用上表中的功耗参数。
- 合并后跑编译与单测；如上游新增了后台轮询或常驻任务，先评估是否纳入后台 gating，再决定是否保留。
