# Release 签名配置

仓库根目录内置 `melodia-release.jks`，**别名与密码均为 `android`**，作为 CI 和本地
默认的 release 签名：克隆仓库后直接 `./gradlew assembleRelease` 就是正式签名包，
不需要配置任何 secret、不需要 `local.properties`。

`app/build.gradle.kts` 的取值顺序（`signingValue()`）：

1. `local.properties`（本地）里的 `RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/
   `RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD`；
2. 同名环境变量（CI）；
3. 都没配 → 落到内置 `melodia-release.jks` + `android`/`android`/`android`/`android`。

只有**内置 keystore** 才会套用 `android` 这套固定别名/密码；自定义 keystore 必须四项
给全，缺任意一项视为未配置，`release` 退回调试签名，`assembleRelease` 仍会产出可安装包，
便于随时验证混淆后的行为。

## 本地打包

```bash
./gradlew assembleRelease
```

产出的包在 `app/build/outputs/apk/release/`，文件名形如 `Melodia-v2.1.1-rainac.1-release.apk`。
确认实际签名者：

```bash
keytool -printcert -jarfile app/build/outputs/apk/release/Melodia-v2.1.1-rainac.1-release.apk
# 或（Android SDK build-tools）
apksigner verify --print-certs app/build/outputs/apk/release/Melodia-v2.1.1-rainac.1-release.apk
```

内置证书主体是 `CN=Melodia, OU=Melodia, O=Melodia, L=Beijing, ST=Beijing, C=CN`，
指纹 `SHA256: 5C:52:18:91:02:D5:AC:7A:78:7E:CC:BE:9F:DC:FC:A2:7F:94:71:3B:09:25:09:EA:B2:CE:1E:1E:96:91:3B:AD`。

## CI 配置

`.github/workflows/release.yml` 只手动触发（Actions → Release → Run workflow，
push tag 触发已关闭）：填入形如 `2.1.1` 或 `2.1.1-powersaving.1` 的版本号后跑正式签名构建，
keystore 随 checkout 一起拿到，**无需配置任何 Repository secret**。

构建后有一步 `校验正式签名生效`，用 `apksigner verify --print-certs` 检查证书 DN，
一旦发现是 `Android Debug`（说明内置 keystore 没生效）就终止，不把假包发出去。

发布时在 Actions → Release → Run workflow 里填版本号运行即可；`git tag` 只作为记录，
不再是触发条件。

## 换签名 / 重新生成

```bash
keytool -genkeypair -v -keystore melodia-release.jks -keyalg RSA -keysize 2048 \
  -validity 36500 -alias android -storepass android -keypass android \
  -dname "CN=Melodia, OU=Melodia, O=Melodia, L=Beijing, ST=Beijing, C=CN"
```

## 注意

- 密码 `android` 与 keystore 一起提交，等于公开：**任何拿到仓库的人都能用这个身份签名**。
  当前按需求接受这一点；要换安全性时，把四项写进 `local.properties`/secrets 并从仓库里删掉 jks 即可，
  Gradle 侧的覆盖逻辑不用改。
- **换 keystore = 换签名**：新签名的包无法覆盖安装旧签名的包，已装用户必须卸载重装，
  应用数据会一并丢失；正式上架后换签名要格外谨慎。
- keystore 一旦被覆盖或删除且没有备份，后续签名指纹就变了，无法再向已装用户推送更新。
- 密码若要改成别的值，`app/build.gradle.kts` 里的 `bundledDefault` 和本文件需同步修改。
