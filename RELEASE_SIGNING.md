# Release 签名与发版配置

仓库根目录内置了默认的签名密钥库 `melodia-release.jks`（**别名与密码均为 `android`**），供本地与 CI 构建开箱即用：克隆仓库后无需配置任何 Repository Secret 或 `local.properties`，只要显式传入版本号参数即可直接打出正式签名 APK。

## 签名解析优先级

`app/build.gradle.kts` 中的 `signingValue()` 按以下顺序读取签名配置：

1. **本地覆盖**：项目根目录 `local.properties` 中的 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`；
2. **环境变量覆盖（CI）**：上述四个同名的环境变量；
3. **内置默认兜底**：以上均未配置时，自动使用根目录的 `melodia-release.jks`，且仅在此情况下套用固定别名与密码 `android`。

> **自定义 keystore 约束**：若配置了自定义 keystore，上述四项必须全部给全。缺少任意一项都会视为未配置，使 `release` 构建退回 Debug 签名（`assembleRelease` 仍会产出可安装包，便于本地验证混淆后的运行行为）。

---

## 本地打包与验签

### 必填版本参数

版本号不设默认值：`releaseVersionName` 与 `releaseVersionCode` 必须通过 `-P` 显式传入；缺失或 `releaseVersionCode` 不是正整数时，Gradle 会在配置阶段直接失败，避免误用默认版本号发版。

```bash
./gradlew :app:assembleRelease \
  -PreleaseVersionName=2.3.0-powersaving.1 \
  -PreleaseVersionCode=2030001
```

产物输出至 `app/build/outputs/apk/release/`，文件名格式为 `Melodia-v{releaseVersionName}-release.apk`（例如 `Melodia-v2.3.0-powersaving.1-release.apk`）。

### 校验实际签名者

```bash
keytool -printcert -jarfile app/build/outputs/apk/release/Melodia-v2.3.0-powersaving.1-release.apk
# 或使用 Android SDK build-tools 中的 apksigner：
apksigner verify --print-certs app/build/outputs/apk/release/Melodia-v2.3.0-powersaving.1-release.apk
```

内置证书固定信息如下：

- **Subject DN**：`CN=Melodia, OU=Melodia, O=Melodia, L=Beijing, ST=Beijing, C=CN`
- **SHA-256 指纹**：`5C:52:18:91:02:D5:AC:7A:78:7E:CC:BE:9F:DC:FC:A2:7F:94:71:3B:09:25:09:EA:B2:CE:1E:1E:96:91:3B:AD`

---

## CI 手动打包流程

`.github/workflows/release.yml` 已关闭 `push tag` 自动触发，仅支持在 GitHub 页面中手动运行（**Actions → Release → Run workflow**）：

- **输入版本号**：填入不带 `v` 前缀的版本号，支持两种格式：
  - 正式版：`MAJOR.MINOR.PATCH`（如 `2.3.0`），对应 `versionCode` 末两位固定为 `99`；
  - 预发布 / 衍生版：`MAJOR.MINOR.PATCH-kind.N`（如 `2.3.0-powersaving.1`），`kind` 须以小写字母开头，`N` 取值 `0~98`（写入 `versionCode` 末两位，避免与正式版 `99` 冲突）。
- **签名与校验**：Runner 在 checkout 时直接获取内置 `melodia-release.jks`，**无需配置任何 Repository Secret**。构建完成后会执行「校验正式签名生效」步骤，通过 `apksigner verify --print-certs` 检查证书 DN；一旦检测到 `Android Debug`，流水线会立即终止。
- **产物输出**：签名 APK 作为 Workflow Artifact（名称为 `android`，保留 `7` 天）上传，**不会自动创建 GitHub Release**；`git tag` 仅作版本记录用途。

> 另外，`.github/workflows/ci.yml` 也已关闭 `push` 触发，仅在向 `main` 提交 PR 或手动运行时执行单元测试与 Release（R8）构建验证。

---

## 重新生成签名密钥库

如需生成新的内置 keystore（替换现有 `melodia-release.jks`）：

```bash
keytool -genkeypair -v -keystore melodia-release.jks -keyalg RSA -keysize 2048 \
  -validity 36500 -alias android -storepass android -keypass android \
  -dname "CN=Melodia, OU=Melodia, O=Melodia, L=Beijing, ST=Beijing, C=CN"
```

---

## 安全与升级注意事项

- **默认密码公开风险**：由于 `melodia-release.jks` 与默认密码 `android` 均随仓库提交，**任何获取仓库代码的人都可以使用同一身份签名**。若需收紧签名权限，请将四项配置移入 `local.properties` 或 CI Secrets，并从仓库中删除 `melodia-release.jks`（`app/build.gradle.kts` 中的覆盖逻辑无需改动）。
- **更换签名不可覆盖安装**：更换 keystore 会导致签名指纹变化，新包无法在已安装旧签名包的设备上直接覆盖升级，用户必须卸载重装（本地应用数据会一并清除）。
- **务必备份 keystore**：一旦现有 keystore 被覆盖或删除且无备份，后续构建将无法再向存量设备提供覆盖升级。
- **修改默认密码**：若保留内置 keystore 但修改别名或密码，需同步更新 `app/build.gradle.kts` 中的 `bundledDefault` 与本文档说明。
