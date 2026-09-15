# AGENTS.md — MyDailyLife_Kotlin

Kotlin/Compose rewrite of UniApp `MyDailyLife`. UI follows root `DESIGN.md` (Airbnb-inspired soft palette).

## Stack

- Kotlin, Jetpack Compose, Material3
- Navigation Compose; ViewModels + `StateFlow`
- Persistence: `ScheduleRepository` (JSON on device)
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
- `ScheduleCard`: short tap → edit; hold ~1.5s with green progress → toggle completed; release mid-hold cancels (no edit).
- Courses: swipe weeks; Statistics: aggregates; Create: add/edit schedule.

Legacy screens (`calendar`, `items`, `completed`) may still exist but are not primary tabs.

## Conventions

- Match existing Compose patterns and soft theme tokens in `ui/theme` (avoid inventing a new palette).
- Prefer small, focused diffs; do not refactor unrelated modules.
- Reuse `ScheduleQuery` for filtering/sorting; don’t duplicate day/completed logic in UI.
- Gestures: `horizontalSwipe` / `verticalSwipe` in `ui/components/Swipe.kt`.
- Do not commit secrets, `local.properties`, or build outputs (see `.gitignore`).

## Git commits

Use [Conventional Commits](https://www.conventionalcommits.org/), English, imperative mood:

```
<type>(optional-scope): <summary>
```

- **Summary**: ≤72 chars; say *why* briefly; no trailing period
- **Body** (optional): blank line after summary; wrap ~72; explain motivation when non-obvious
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
feat(schedule): expand completed section at list bottom
fix(schedule): ignore tap when long-press is cancelled
style(theme): soften coral primary for light canvas
docs: add AGENTS.md for AI project context
chore: ignore .idea and build outputs
build: bump Compose BOM
```

When the agent creates a commit, follow this format unless the user specifies another message.

## Build

```bat
gradlew.bat :app:assembleDebug
```

## Not done / optional later

ICS import/export, reminders, settings persistence, fuller course editing parity with UniApp.
