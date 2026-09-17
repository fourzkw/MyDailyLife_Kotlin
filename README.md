# MyDailyLife（Kotlin）

日程 + 课表 + 统计的 Android 日常工具，使用 **Kotlin / Jetpack Compose / Material3** 实现。  
由 UniApp 版思路重写；界面配色见根目录 [`DESIGN.md`](DESIGN.md)（柔和珊瑚浅色风格）。

**当前版本：`1.10.2`（`versionCode` 18）**

面向协作者与 AI 的工程约定见 [`AGENTS.md`](AGENTS.md)。

---

## 功能概览

| 模块 | 能力 |
|------|------|
| **日程** | 周条 / 下拉月历、待办与已完成、课程同步显示、综合 / 时间 / 紧急度排序、Once·Daily·Weekly 提醒 |
| **课表** | 教学周滑动、长按选格加课、详情编辑与删除、清空课表、Excel·CSV·xlsx / ICS 订阅 / 教务选校导入 |
| **统计** | 聚合概览 |
| **设置** | 通知权限与精确闹钟引导、默认优先级、标签、应用内检查更新 |

---

## 环境与构建

- **JDK 21**（Windows 上请确认 `JAVA_HOME`）
- Android Studio 或命令行 Gradle
- `minSdk` 26 / `targetSdk` 36

```powershell
# Debug
.\gradlew.bat :app:assembleDebug

# Release（需本地签名，见下）
.\gradlew.bat :app:assembleRelease
```

产物路径：

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

### Release 签名（本地，勿提交）

1. 生成本地 keystore（只做一次，妥善备份）。
2. 在仓库根目录创建 **`keystore.properties`**（已在 `.gitignore`）：

```properties
storeFile=D:/keys/mydailylife-release.jks
storePassword=你的密码
keyAlias=mdl
keyPassword=你的密码
```

无该文件仍可编 Debug；Release 与覆盖安装 / 自更新需要签名一致。

---

## 应用内更新

清单文件：[`update/version.json`](update/version.json)  
公开读取示例：

```text
https://raw.githubusercontent.com/fourzkw/MyDailyLife_Kotlin/master/update/version.json
```

发版时建议按序：

1. 递增 `app/build.gradle.kts` 的 `versionCode` / `versionName`
2. 在下方 **版本更新说明** 追加本版条目
3. `assembleRelease`，将 APK 命名为 `MyDailyLife-x.y.z.apk`
4. 创建 GitHub Release（如 `v1.10.2`）并上传 APK
5. 更新 `update/version.json` 的 `versionCode`、`versionName`、`apkUrl`、`changelog` 后推送

> 仓库名或用户名若不同，请同步改 `version.json`、构建里的 `UPDATE_MANIFEST_URL` 与本文链接。

---

## 版本更新说明

发版或合并用户可见改动时，请在本节 **顶部** 追加条目（新 → 旧）。  
应用内更新弹窗文案以 `update/version.json` 的 `changelog` 为准，可与本节对应版本摘要保持一致。

### 1.10.2（code 18）

- 统一各页顶部高度：Tab 页共用 `ScreenTopPadding` + `SectionHeader`；全屏页共用 `MdlTopAppBar`（避免嵌套 Scaffold 重复叠 status bar）

### 1.10.1（code 17）

- 去掉设置中的「自动更新课表」及进入课表时的自动拉 ICS；订阅仍可在课表内手动「刷新订阅」

### 1.10.0（code 16）

- 当天课程同步到日程列表（「课表」标签，只读）
- 日程排序：综合 / 时间 / 紧急度（记住选择）
- 课表时间栏字号再缩小

### 1.9.1（code 15）

- 长按草稿「+」可删除；去掉空课表提示
- 网格更紧凑；课程块显示标题 + 教师 + 地点

### 1.9.0（code 14）

- 空格长按拖选加课（可合并相邻格）；支持每周 / 仅本周
- 课程详情编辑与删除（本节 / 所有同时段）
- 课表可清空（保留学期起始日）

### 1.8.0（code 13）

- ICS 订阅：链接 / 文本 / `.ics` 文件；可刷新订阅
- 支持直接导入 `.xlsx`（另支持 CSV / TSV；旧 `.xls` 仍不支持）

### 1.7.0（code 11）

- 教务导入先经「选择学校」页（拼音首字母分组 + 搜索；当前接入重庆大学）
- 学期「第 N 周」按含当月 1 号的日历周计算（如 2026 年九月第二周周一为 9 月 7 日）

### 1.6.0（code 10）

- 设置页检查更新：拉取 `version.json` 并下载安装 APK
- 去掉首次启动默认示例日程与课表

### 1.5.1（code 8）

- Release 签名配置（`keystore.properties`）
- 启动图标与课表网格样式同步

### 1.5.0（code 7）

- 教学周过滤、课程详情、学期起点设置
- 教务 WebView 导入（重庆大学）与 Excel / CSV 导入

### 1.1.0（code 2）

- 通知管理页
- 提醒能力完善（循环、起止提醒、提前期等；此前若干改动同属提醒体系）

### 1.0（code 1）

- 初版：日程（周 / 月）、课表、统计与设置的 Compose 实现

---

## 不要提交

- `keystore.properties`、`*.jks` / `*.keystore`
- `local.properties`、`*.apk`、构建产物、`.idea` 本地状态
- 含个人课表的抓取文件（见 `.gitignore`）

---

## 许可证

[MIT](LICENSE) © 2026 fourzkw
