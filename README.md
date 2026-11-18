<div align="center">
  <img src="docs/images/cover.png" alt="Melodia" />

参考 Spotify UI 构建的现代化、轻量级第三方网易云音乐客户端。

> **Fork 说明**
>
> 本仓库（`rainac1/Melodia-Lite`）是 Melodia 的 **Android 功耗优化激进精简版**。项目目标并非与上游保持功能对等，而是在 Android 端尽可能减少后台唤醒、常驻渲染与无效网络请求，因此移除了部分平台支持与高频渲染特性：
>
> - **仅保留 Android 端**：删除桌面端模块、桌面源集与测试、Windows 打包及官网站点
> - **移除歌词系统**：删除歌词运行时与 UI、Karaoke 逐帧渲染、悬浮歌词、外部歌词源、播放器歌词分享入口与歌词设置
> - **静态播放页**：删除播放器动态背景与逐帧 Mesh 动画，改为静态渐变背景
> - **精简原生插件**：移除 `plugin-api` 原生插件实现（共享层保留 `NativePluginHost` 接口与被动配置模型，Android 端注入为 `null`）
> - **收敛后台调度**：后台停止下一首预取、相似漫游、响度分析与边听边存调度，降低进度轮询与状态落盘频率，并拉长播放器与 AudioTrack 缓冲
>
> 为控制后续跟进上游的维护成本，共享层与代码结构尽量与上游保持一致，仅移除上述功能在 Android 端的主动实现（详细策略与参数见 [功耗说明](docs/power-saving.md)）。当前已合入上游 **v2.3.0**：多账户、私信/消息页、一起听、私人 FM、外链识别、通知栏按钮自定义、界面精简开关、预见式返回、按需渲染的歌曲/私信分享卡片等能力均已同步。
>
> 这些取舍会导致部分功能缺失或与上游行为不同，不适合需要桌面端、歌词展示或原生插件扩展的场景。完整功能请以上游原仓库为准。

[![GitHub Release](https://img.shields.io/github/v/release/rinchao0721/Melodia?style=flat-square&color=blue)](https://github.com/rinchao0721/Melodia/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green?style=flat-square)](https://github.com/rinchao0721/Melodia/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple?style=flat-square)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

[插件格式](docs/plugin-format.md) · [皮肤格式](docs/skin-format.md) · [插件目录](docs/plugin-catalog.md) · [功耗说明](docs/power-saving.md)
</div>

---

## 截图预览

### 手机

<div align="center">
  <img src="docs/screenshots/home.webp" width="19%" alt="首页" />
  <img src="docs/screenshots/library.webp" width="19%" alt="音乐库" />
  <img src="docs/screenshots/player.webp" width="19%" alt="播放器" />
  <img src="docs/screenshots/profile.webp" width="19%" alt="个人主页" />
</div>

### 平板

<div align="center">
  <img src="docs/screenshots/tablet-portrait-home.webp" width="32%" alt="平板竖屏首页" />
  <img src="docs/screenshots/tablet-portrait-panel.webp" width="32%" alt="平板竖屏播放侧栏" />
</div>
<div align="center">
  <img src="docs/screenshots/tablet-landscape-panel.webp" width="32%" alt="平板横屏播放侧栏" />
  <img src="docs/screenshots/tablet-fullscreen-info.webp" width="32%" alt="平板全屏播放页歌曲信息" />
</div>

---

## 下载与反馈

- **本 Fork 构建**：可在仓库的 GitHub Actions 手动运行 `Release` 工作流产出签名 APK，或按下方说明本地编译（详见 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)）
- **上游原版安装包**：前往上游 [GitHub Releases](https://github.com/rinchao0721/Melodia/releases) 获取（以下为上游社区与反馈渠道）
- **上游蓝奏云**：https://wware.lanzn.com/b01gic8ioj 密码:cem2
- **上游缺陷与建议**：[GitHub Issues](https://github.com/rinchao0721/Melodia/issues)
- **上游交流 QQ 群**：`331832966`
- **上游 Telegram**：https://t.me/+OZRD58lnMy85NjE0

---

## 插件

- **插件仓库**：[rinchao0721/melodia-plugins](https://github.com/rinchao0721/melodia-plugins)
- **在线浏览与下载**：<https://rinchao0721.github.io/melodia-plugins/>
- **插件规范**：[docs/plugin-format.md](docs/plugin-format.md)；目录格式见 [docs/plugin-catalog.md](docs/plugin-catalog.md)

---
## 项目架构

项目由 Android 应用、共享层与 Baseline Profile 三个 Gradle 模块组成，网易云接口、加密、数据仓储与大部分 ViewModel 只在 `shared` 中维护一份：

| 模块 | 类型 | 职责 |
|---|---|---|
| `shared` | Kotlin Multiplatform（Android） | 数据模型、网络与原生加密、各业务域 `data` / `domain`、跨平台 ViewModel 与展示逻辑、播放器抽象 `PlaybackController` |
| `app` | Android 应用 | Jetpack Compose 界面、Media3 播放、下载、本地音乐、识曲等 Android 专属能力 |
| `baselineprofile` | Android Baseline Profile | 启动与常用路径的 profile 生成 |

代码按**业务域 (Feature-Driven)** 组织：`core` 承载全局共享基础设施与通用能力，`feature` 下每个业务域自持 `data` / `domain` / `ui` 结构。依赖方向单向收敛——`feature` 依赖 `core`，`core` 不反向依赖 `feature`，各业务域之间解耦无循环依赖。

```text
shared/src/
├── commonMain/kotlin/com/lin0721/linmusic/
│   ├── core/                    # api、auth、network、model、preferences、player 抽象、format 格式化等共享基础能力
│   ├── feature/                 # 各业务域的 data / domain、跨平台 ViewModel（ui）与展示逻辑（presentation）
│   └── di/                      # 网络与仓储的 Koin 模块
└── androidMain/                 # Context 相关实现（DataStore 落盘、网络状态、音乐库偏好等）

app/src/main/java/com/lin0721/linmusic/
├── MelodiaApplication.kt        # 应用程序入口：Koin 依赖注入初始化与 Coil 预热
├── MainActivity.kt              # 单 Activity 架构：系统栏适配、主题/缩放、外链与剪贴板识别、通知权限请求
├── MelodiaApp.kt                # 顶层组合：导航骨架编排与播放器图层联动
├── MelodiaNavHost.kt            # 核心路由分发与平滑转场动画配置
├── MelodiaOverlays.kt           # 全屏播放器、迷你条/底栏、下载横幅与全局浮层
├── MelodiaNavigationState.kt    # 自定义导航回退栈管理与参数上下文恢复
├── MelodiaPlayerSheetState.kt   # 全屏播放器展开/收起手势状态机
├── MelodiaSidebarState.kt       # 侧边栏抽屉滑动状态机
├── core/                        # Android 专属基础能力（Media3 播放引擎、下载、Material 3 主题与组件、应用内更新等）
├── di/                          # Android 侧 Koin 模块（Local, Player, Download, Update, ViewModel 等）
└── feature/                     # 各业务域的 Compose 界面与 Android 专属 ViewModel（本地音乐、云盘、识曲等）
```

---


## 统一错误处理

Repository 边界统一产出 Kotlin `Result<T>`，异常类型抽象为领域错误模型 `AppError`（包括网络中断、风控拦截、鉴权过期、数据解析失败及业务错误码）。UI 表现层针对不同错误语义分流呈现对应的重试引导或提示。

---

## 构建与开发

### 环境要求

- **JDK**：Java 21（推荐 Eclipse Temurin 21）
- **Gradle**：9.3+（项目内置 Gradle Wrapper 9.3.1）
- **Android Gradle Plugin (AGP)**：9.1.1+
- **Android SDK**：Compile / Target SDK 36，Min SDK 26 (Android 8.0+)
- **IDE**：Android Studio Ladybug (2024.2.1) 或更高版本

### 常用命令

> 版本号没有默认值：`releaseVersionName` 与 `releaseVersionCode` 必须通过 `-P` 显式传入，
> 缺失时 Gradle 会在配置阶段直接失败。

- **Android 调试构建**：
  ```bash
  ./gradlew :app:assembleDebug -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
  ```
- **运行单元测试**：
  ```bash
  ./gradlew :app:testDebugUnitTest -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
  ```
- **构建 Android Release 安装包**：
  ```bash
  ./gradlew :app:assembleRelease -PreleaseVersionName=2.3.0-powersaving.1 -PreleaseVersionCode=2030001
  ```

### 混淆与签名说明

- **代码裁剪与混淆**：`release` 构建已开启 R8 压缩与混淆保护。项目对 Retrofit 接口与 `@Serializable` 数据传输类配置了显式 Keep 规则，确保混淆后的运行安全。
- **签名机制**：仓库根目录内置 `melodia-release.jks`（别名与密码均为 `android`）作为本地与 CI 默认的 Release 签名，开箱即用；可通过 `local.properties` 或 CI 环境变量覆盖（自定义 keystore 需补全四项配置，缺项时退回 Debug 签名）。完整签名与覆盖规则请参阅 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)。
- **CI 与发布流水线**：已关闭 `push` 自动触发。`CI` 工作流仅在向 `main` 提交 PR 或手动触发时运行 Android 单元测试与 Release（R8）构建验证；`Release` 工作流仅支持手动触发（传入版本号后打出正式签名 APK 并作为 Workflow Artifact 保留，不会自动创建 GitHub Release），详见 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)。

---

## 非常感谢

- [NeteaseCloudMusicApi](https://github.com/binaryify/NeteaseCloudMusicApi) - 网易云音乐 API 分析与实现参考
- [api-enhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced) - 增强接口与加密逻辑参考
- [node-vibrant](https://github.com/Vibrant-Colors/node-vibrant) - 取色算法参考
- [SPlayer](https://github.com/SPlayer-Dev/SPlayer) - 现代流媒体架构设计启发
- [Spotify](https://spotify.com) - 优秀的移动端流媒体交互范式与 UI/UX 体验灵感
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) - 共享层的 Compose 依赖
- [TagLib](https://github.com/Kyant0/taglib) - 本地音乐标签读写

---

## 免责声明

1. 本项目为个人技术交流与 Android 现代原生架构学习研究所用。
2. 本项目不包含任何商业目的，不提供任何形式的商业变现或增值服务。
3. 软件中调用的所有音频及元数据均来源于公开网络接口，版权归属于原权利人所有。请在下载安装后 24 小时内删除，严禁用于任何商业用途。
