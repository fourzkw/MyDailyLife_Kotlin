# AGENTS.md — MyDailyLife_Kotlin

Kotlin/Compose rewrite of UniApp `MyDailyLife`. UI follows root `DESIGN.md` (Airbnb-inspired soft palette).

## Stack

- Kotlin, Jetpack Compose, Material3
- Navigation Compose; ViewModels + `StateFlow`
- Persistence: `ScheduleRepository` / `SettingsRepository` (JSON on device)
- JDK 21 for builds (`JAVA_HOME` may need setting on Windows)

## Layout

```
app/src/main/java/com/mydailylife/schedule/
  data/           # models, ScheduleRepository, ScheduleQuery, time format
  ui/
    components/   # ScheduleCard, swipe helpers, shared chrome
    navigation/   # Routes, MdlNavHost
    screens/      # schedule, courses, statistics, settings, create, …
    theme/        # Color, Type, Shape, Theme
```

Reference UniApp behavior in sibling repo `d:\_Project\MyDailyLife` when porting UX.

## Tabs & routes

Bottom tabs: **日程** / **课表** / **统计** / **设置**.

- Schedule merges former 事项 + 日历: week strip by default; pull down on strip → month; swipe up on month → collapse; horizontal swipe changes day.
- Daily list: pending items first; **已完成 · N** expandable section at bottom (same `ScheduleCard`).
- `ScheduleCard`: short tap → edit; double-tap → delete confirm; hold ~1s (pending) / ~0.5s (completed) with green progress → toggle completed.
- Courses: swipe weeks; Statistics: aggregates; Create: add/edit schedule.

Legacy screens (`calendar`, `items`, `completed`) may still exist but are not primary tabs.

## Conventions

- Match existing Compose patterns and soft theme tokens in `ui/theme` (avoid inventing a new palette).
- Prefer small, focused diffs; do not refactor unrelated modules.
- Reuse `ScheduleQuery` for filtering/sorting; don’t duplicate day/completed logic in UI.
- Gestures: `horizontalSwipe` / `verticalSwipe` in `ui/components/Swipe.kt`.
- Do not commit secrets, `local.properties`, or build outputs (see `.gitignore`).

## Git commits

Use [Conventional Commits](https://www.conventionalcommits.org/). Keep `type`/`scope` in English; write **summary and body in Chinese**.

```
<type>(optional-scope): <中文摘要>
```

- **Summary**: ≤72 chars; 简要说明为什么；句末不加句号
- **Body** (optional): blank line after summary; wrap ~72; 非显而易见时补充动机（中文）
- **One concern per commit**; do not mix unrelated changes

| type | use for |
|------|---------|
| `feat` | new user-facing capability |
| `fix` | bug fix |
| `refactor` | code change with no behavior change |
| `style` | UI/theme-only polish (not CSS lint) |
| `docs` | AGENTS.md, DESIGN notes, comments-only docs |
| `chore` | tooling, gitignore, non-code housekeeping |
| `build` | Gradle/deps/AGP |
| `test` | tests only |
| `perf` | performance |

**Scopes** (optional): `schedule`, `courses`, `stats`, `settings`, `create`, `nav`, `data`, `theme`, `ui`

**Examples**

```
feat(schedule): 列表底部展开已完成分组
fix(schedule): 长按取消时不触发点击编辑
style(theme): 浅色画布上弱化珊瑚主色
docs: 补充 AGENTS.md 供 AI 对齐约定
chore: 忽略 .idea 与构建产物
build: 升级 Compose BOM
```

When the agent creates a commit, follow this format unless the user specifies another message.

## Build

```bat
gradlew.bat :app:assembleDebug
```

## Versioning

Single source of truth: `app/build.gradle.kts` → `defaultConfig`.

| Field | Meaning |
|-------|---------|
| `versionName` | User-facing `MAJOR.MINOR.PATCH` (shown in 设置) |
| `versionCode` | Integer; must increase on every shipped build |

**When to bump** (do it in the same change set that ships the work):

- **PATCH** (`1.1.0` → `1.1.1`): bugfix, copy/UI polish, no new capability
- **MINOR** (`1.1.0` → `1.2.0`): new user-facing feature (reminders, screens, gestures…)
- **MAJOR** (`1.x` → `2.0.0`): breaking storage/API or large UX reset

Also bump `versionCode` by **+1** whenever `versionName` changes.

Settings 「版本」 reads `BuildConfig.VERSION_NAME` — never hardcode the string in UI.

**Current:** `1.10.2` / `versionCode` 18 — 统一各页顶部高度（Tab SectionHeader + 全屏 MdlTopAppBar）。

用户可见的版本更新说明维护在 `README.md` →「版本更新说明」；发版时同步更新该节与 `update/version.json` 的 `changelog`。

## Not done / optional later

ICS course **export**, Excel `.xls`（旧二进制）解析,
上课提醒.

**Courses:** grid + `CourseStore` (`courses.json`); teaching week swipe filters
`teachingWeeks`; tap block for detail/edit/delete; long-press drag to add;
import Excel/CSV/xlsx / 教务选校再 WebView / ICS 订阅;
term start editable (default autumn = Sept week-2 Monday, spring = Feb week-2 Monday).

**Reminders:** `ReminderScheduler` + `AlarmManager` for Once / Daily / Weekly;
start and end can fire separately; per-item lead minutes on create.
Notification channel; settings toggle requests `POST_NOTIFICATIONS` and can guide
exact-alarm / app notification settings. BootReceiver restarts process so alarms
are re-registered. After a recurring fire, the receiver re-schedules the next occurrence.

Settings prefs persist via `SettingsRepository` → `settings.json`.
