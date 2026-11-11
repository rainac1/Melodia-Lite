<div align="center">
  <img src="docs/images/cover.png" alt="Melodia" />

参考 Spotify UI 构建的现代化、轻量级第三方网易云音乐客户端。

> **Fork 说明**
>
> 本仓库是 Melodia 的 **Android 功耗优化激进修改版**。目标不是与上游保持功能对等，而是在 Android 端尽可能减少后台唤醒、常驻渲染和无效网络请求，因此删除了大量上游功能与平台支持：
>
> - 只保留 Android 端，删除桌面端模块、桌面源集、桌面测试、Windows 打包与官网
> - 删除歌词运行时与 UI、Karaoke 逐帧渲染、悬浮歌词、外部歌词源、歌词分享与歌词设置
> - 删除播放器动态背景与逐帧 mesh 动画，改为静态渐变
> - 删除播放器分享卡片、原生插件 API 与桌面插件布局
> - 后台停止预取、漫游、响度分析、边听边存与封面预载，降低进度轮询与状态落盘频率
>
> 这些改动会带来功能缺失和与上游行为不一致，不适合需要桌面端、歌词或完整插件能力的场景。上游项目请以原仓库为准。

[![GitHub Release](https://img.shields.io/github/v/release/rinchao0721/Melodia?style=flat-square&color=blue)](https://github.com/rinchao0721/Melodia/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green?style=flat-square)](https://github.com/rinchao0721/Melodia/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple?style=flat-square)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

[插件格式](docs/plugin-format.md) · [皮肤格式](docs/skin-format.md) · [插件目录](docs/plugin-catalog.md)
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

- **安装包下载**：前往 [GitHub Releases](https://github.com/rinchao0721/Melodia/releases) 获取最新预编译 APK
- **蓝奏云**：https://wware.lanzn.com/b01gic8ioj 密码:cem2
- **缺陷与建议**：[GitHub Issues](https://github.com/rinchao0721/Melodia/issues)
- **交流 QQ 群也可以下载**：`331832966`
- **或者tg**：https://t.me/+OZRD58lnMy85NjE0

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
├── MainActivity.kt              # 单 Activity 架构：系统窗口适应与悬浮窗权限引导
├── MelodiaApp.kt                # 顶层组合：导航骨架编排与播放器图层联动
├── MelodiaNavHost.kt            # 核心路由分发与平滑转场动画配置
├── MelodiaOverlays.kt           # 全屏播放器底栏、底部弹窗与全局反馈浮层
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

- **Android 调试构建**：
  ```bash
  ./gradlew :app:assembleDebug
  ```
- **运行单元测试**：
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- **构建 Android Release 安装包**：
  ```bash
  ./gradlew :app:assembleRelease
  ```

### 混淆与签名说明

- **代码裁剪与混淆**：`release` 构建已开启 R8 压缩与混淆保护。项目对 Retrofit 接口与 `@Serializable` 数据传输类配置了显式 Keep 规则，确保混淆后的运行安全。
- **签名机制**：签名材料从版本控制外部注入（读取 `local.properties` 或 CI 环境变量）。若未配置正式签名，将自动回退为 Debug 签名，确保本地可编译出可运行的 APK。完整签名与自动化发版配置请参阅 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)。
- **CI 流水线**：向 `main` 分支推送或提交 PR 时自动运行 Android 单元测试与 Release 构建验证；推送版本 Tag 时打 Android 正式签名 APK 并发布到 GitHub Release，详见 [RELEASE_SIGNING.md](RELEASE_SIGNING.md)。

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
