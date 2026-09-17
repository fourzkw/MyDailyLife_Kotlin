# MyDailyLife（Kotlin）

日程 + 课表 + 统计的 Android 日常工具，Jetpack Compose / Material3 实现。  
由 UniApp 版 [MyDailyLife](https://github.com/fourzkw) 思路重写（若仓库地址不同，请自行替换链接）。

**当前版本：** 1.6.0（`versionCode` 10）

## 功能概览

- **日程**：周条 / 月历、待办与已完成分组、长按完成、提醒（Once / Daily / Weekly）
- **课表**：教学周滑动、格子详情、Excel/CSV / 教务 WebView 导入（持续完善）
- **统计**：聚合概览
- **设置**：通知、默认优先级、标签等

界面配色参考根目录 `DESIGN.md` 中的柔和珊瑚浅色风格（灵感来自公开设计分析，非官方品牌素材）。

## 环境要求

- JDK 21（`JAVA_HOME` 指向 JDK 21）
- Android Studio 或命令行 Gradle
- `minSdk` 26 / `targetSdk` 36

## 构建

```powershell
# Debug
.\gradlew.bat :app:assembleDebug

# Release（需本地签名配置，见下）
.\gradlew.bat :app:assembleRelease
```

产物：

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

### Release 签名（本地，勿提交）

1. 生成 keystore（只做一次，妥善备份）：

```powershell
keytool -genkeypair -v `
  -keystore D:\keys\mydailylife-release.jks `
  -storetype JKS `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias mdl `
  -dname "CN=YourName, O=Personal, C=CN" `
  -storepass "你的密码" `
  -keypass "你的密码"
```

2. 在仓库根目录创建 **`keystore.properties`**（已在 `.gitignore` 中）：

```properties
storeFile=D:/keys/mydailylife-release.jks
storePassword=你的密码
keyAlias=mdl
keyPassword=你的密码
```

没有该文件时仍可编译 Debug；Release 需签名后才能稳定用于覆盖安装与自更新。

## 应用内更新清单

仓库内维护：

```text
update/version.json
```

公开读取示例（分支名按实际修改）：

```text
https://raw.githubusercontent.com/fourzkw/MyDailyLife_Kotlin/master/update/version.json
```

发版流程简述：

1. 递增 `app/build.gradle.kts` 中的 `versionCode` / `versionName`
2. `.\gradlew.bat :app:assembleRelease`，将 APK 改名为 `MyDailyLife-x.y.z.apk`
3. 创建 GitHub Release（tag 如 `v1.5.2`），上传 APK
4. 更新 `update/version.json` 中的 `versionCode`、`versionName`、`apkUrl`、`changelog` 并推送

`apkUrl` 指向对应 Release 附件，例如：

```text
https://github.com/fourzkw/MyDailyLife_Kotlin/releases/download/v1.5.2/MyDailyLife-1.5.2.apk
```

> 若你的 GitHub 用户名或仓库名不同，请同步修改 `update/version.json` 与本文中的链接。

## 不要提交

- `keystore.properties`、`*.jks` / `*.keystore`
- `local.properties`、`*.apk`、构建产物
- 含个人课表的抓取文件（见 `.gitignore`）

## 许可证

[MIT](LICENSE) © 2026 fourzkw

## 更多约定

面向协作者与 AI 的工程说明见 [AGENTS.md](AGENTS.md)。
