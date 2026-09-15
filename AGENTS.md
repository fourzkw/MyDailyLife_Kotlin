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

## Build

```bat
gradlew.bat :app:assembleDebug
```

## Not done / optional later

ICS import/export, reminders, settings persistence, fuller course editing parity with UniApp.
