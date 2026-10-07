# Tasks

v1 progress for the Android Liturgy of the Hours. Product decisions: [DESIGN.md](DESIGN.md). Home page: [README.md](README.md).

Desktop plugin: [omarchy-liturgy-of-the-hours](https://github.com/mohuddle/omarchy-liturgy-of-the-hours).

**6 of 10 done. Next: Task 7.**

## Board

| # | Task | Status | Notes |
|---|---|---|---|
| 1 | Gradle scaffold and assets | Done 2026-10-05 | `assembleDebug` OK. Package `io.github.mohuddle.hours`, minSdk 29. Device launch not verified. |
| 2 | Domain: hours, schedule, verses, cache | Done 2026-10-05 | `OfficeModelTest` 7/7. No liturgical calendar. |
| 3 | Domain: liturgical day and office assembly | Done 2026-10-06 | `OfficeModelTest` 8/8. Easter 2026-04-05. None on 2026-08-21 assembles four sections. |
| 4 | Persistence (DataStore + asset JSON) | Done 2026-10-06 | `HoursStoreTest` 4/4. Defaults, one verse per day, `lastNotified`, corrupt JSON. |
| 5 | Hours screen | Done 2026-10-06 | Six defaults, Genesis 1:1 (BSB), Terce accent at 09:05 on an API 35 emulator. Bell and gear log only. |
| 6 | Office screen and navigation | Done 2026-10-07 | Bell opens featured hour; Sext row opens Sext. Four sections on API 35 emulator (today). Settings is a stub. |
| 7 | Settings screen | Remaining | |
| 8 | Alarm scheduling | Remaining | |
| 9 | Notifications, receiver, permission CTAs | Remaining | |
| 10 | Device pass (emulator / G5) | Remaining | |

### Later (not v1)

- [ ] Glance home-screen widget
- [ ] Unrestricted-battery prompt if G5 testing misses bells
- [ ] Compline
- [ ] Play Store / F-Droid

After a task’s “Done when” is true, mark it on this board, refresh the README progress table, and push.

---

## File map

Landed through Task 6: domain, `HoursStore`, Hours screen, Office screen, and `NavHost` (`hours` / `office/{hourId}` / settings stub). Settings fields, alarms, and notifications are still planned.

```
app/src/main/java/io/github/mohuddle/hours/
  domain/OfficeModel.kt          # hours, schedule, verses, cache, liturgical day, office (Tasks 2–3)
  data/HoursStore.kt             # DataStore + asset JSON (Task 4)
  notify/AlarmScheduler.kt
  notify/HourReceiver.kt
  notify/BootReceiver.kt
  notify/TimeChangeReceiver.kt
  notify/HourNotifications.kt
  ui/HoursApp.kt                 # NavHost: hours | office/{hourId} | settings
  ui/HoursScreen.kt
  ui/OfficeScreen.kt
  ui/SettingsScreen.kt
  ui/JerusalemCross.kt
  ui/theme/Theme.kt
app/src/main/assets/verses.json
app/src/main/assets/office.json
app/src/main/res/raw/church_bell.ogg
app/src/test/java/.../OfficeModelTest.kt
app/src/test/java/.../AlarmSchedulerTest.kt
app/src/test/java/.../HourReceiverTest.kt
```

---

## Task 1: Gradle scaffold and assets ✅

**Files:** Android Studio / Gradle app module, `settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, empty `MainActivity`, `NOTICE.md`, `LICENSE`, `.gitignore`, copy plugin `data/verses.json`, `data/office.json`, `data/church-bell.ogg`, `icon.png`.

**Produces:** installable debug APK with package `io.github.mohuddle.hours`, `minSdk 29`, Compose + Material 3, an empty activity titled “Liturgy of the Hours”.

**Done when:**

- `./gradlew assembleDebug` succeeds.
- App launches to a blank (or “Liturgy of the Hours”) screen on emulator API 29 or 35.
- Assets are in the tree (not fetched at runtime).
- `NOTICE.md` matches the plugin’s BSB / 1662 notices.

**Stop.** Do not port `Model.js`.

---

## Task 2: Domain — hours, schedule, verses, cache ✅

**Files:** Create `domain/OfficeModel.kt`, `app/src/test/.../OfficeModelTest.kt`.

**Produces:** Kotlin functions matching `Model.js` for `HOURS`, `isoDate`, `parseHm`, `minutesOfDay`, `formatUntil`, `nextPosition`, `boolSetting`, `resolvedHours`, `enabledHours`, `scheduleState`, `featuredHour`, `heroMeta`, `isCurrentWindow`, `dueNotifications`, `verseFromCatalog`, `parseCache`, `serializeCache`, `notificationTitle`, `notificationBody`, `plainText`, `hourById`. No Android imports.

**Done when:** JUnit ports every assertion in plugin `tests/model.test.js` **through** the cache / after-evening / verse-position block (before `easterDate`). Run:

```
./gradlew :app:testDebugUnitTest --tests io.github.mohuddle.hours.domain.OfficeModelTest
```

All of those tests pass. Liturgical calendar tests are still absent (Task 3).

**Stop.** Do not build UI.

---

## Task 3: Domain — liturgical day and office assembly ✅

**Files:** Extend `OfficeModel.kt` and `OfficeModelTest.kt`. Test resources may load `office.json` from `src/test/resources` or `assets`.

**Produces:** `easterDate`, `liturgicalDay`, `buildOffice`, `officeNotificationTitle`, `officeNotificationBody`.

**Done when:** Remaining `model.test.js` assertions pass, including:

- Easter 2026-04-05
- Friday 2026-08-21 spoken: `Friday in the week following the Eleventh Sunday after Trinity`
- Trinity Sunday 2026-05-31, Easter Day 2026-04-05, First Sunday in Advent 2026-11-29
- `buildOffice` for None on 2026-08-21: chapter `1 Corinthians 6:20`, memorial `cross`, four sections

Full `OfficeModelTest` green. Domain is complete.

**Stop.** Do not add DataStore.

---

## Task 4: Persistence ✅

**Files:** Create `data/HoursStore.kt` (DataStore preferences + read `verses.json` / `office.json` from assets). Tests with a fake/in-memory DataStore or a JVM-friendly wrapper.

**Consumes:** `OfficeModel` parse/serialize and verse rotation.

**Produces:** `HoursStore` that loads settings (plugin defaults), advances the verse once per local calendar day (sequential), persists `lastNotified`, returns today’s verse and the office book. Invalid JSON → error strings from DESIGN.md, not a crash.

**Done when:** unit tests cover: first launch defaults; verse does not advance twice on the same date; verse advances on a new date; `lastNotified` round-trip; corrupt JSON does not throw to the UI layer.

**Stop.** Do not draw Hours yet.

---

## Task 5: Hours screen ✅

**Files:** Create `ui/theme/Theme.kt`, `ui/JerusalemCross.kt`, `ui/HoursScreen.kt`, wire `MainActivity` to Hours with fake or real `HoursStore`.

**Consumes:** schedule + verse from domain/store.

**Produces:** Hours UI per DESIGN.md (list, current accent, verse, bell and gear buttons). Buttons may be no-ops or logs if navigation is not in this task; prefer stub callbacks `onOpenOffice(hourId)`, `onOpenSettings()`.

**Done when:** emulator shows six hours with defaults, a BSB verse, accent on the current hour when the clock is in that window. Disabled styling can wait for Settings (Task 7) if store enable flags are already respected.

**Stop.** Do not build Office layout.

---

## Task 6: Office screen and navigation ✅

**Files:** Create `ui/OfficeScreen.kt`, `ui/HoursApp.kt` (`NavHost` destinations `hours`, `office/{hourId}`, `settings` stub).

**Consumes:** `OfficeModel.buildOffice`, store office book.

**Produces:** Office UI per DESIGN.md. Bell and row tap navigate to that hour. Back returns to Hours. Missing book → “The Office is not ready yet.”

**Done when:** from Hours, bell opens featured hour’s office; tapping Sext opens Sext; heading and four sections render for a known fixture date if you freeze the clock in a debug build, or for today if the book loaded.

**Stop.** Do not build Settings fields.

---

## Task 7: Settings screen

**Files:** Create `ui/SettingsScreen.kt`; persist through `HoursStore`.

**Produces:** reminders toggle; six enable + time pickers; defaults as DESIGN.md; save reschedules **if** `AlarmScheduler` exists, otherwise a `var onScheduleChanged: () -> Unit` stub. Caption about BSB and bells. Gear on Hours opens this screen.

**Done when:** changing Prime to 07:30 and leaving the screen, killing the process, and reopening shows 07:30. Disabling an hour mutes it on Hours.

**Stop.** Do not implement AlarmManager yet.

---

## Task 8: Alarm scheduling

**Files:** Create `notify/AlarmScheduler.kt`, `notify/BootReceiver.kt`, `notify/TimeChangeReceiver.kt`. Test `AlarmSchedulerTest` against a fake `AlarmClock` wrapper (no real AlarmManager in unit tests).

**Consumes:** enabled hours + times from store.

**Produces:** `setAlarmClock` for each enabled hour’s next occurrence; cancel+reschedule all six on `reschedule(now)`; `BOOT_COMPLETED` / `TIME_SET` / `TIMEZONE_CHANGED` call `reschedule`. Manifest receivers. PendingIntent identity keyed by `hourId`.

**Done when:** unit tests: six enabled → six clocks; disable Terce → five; after an 18:00 fire, evening is scheduled tomorrow; reboot path invokes reschedule. `SCHEDULE_EXACT_ALARM` declared. No notifications posted yet (Task 9).

**Stop.** Do not implement the notification UI.

---

## Task 9: Notifications, receiver, permission CTAs

**Files:** Create `notify/HourReceiver.kt`, `notify/HourNotifications.kt`. Test `HourReceiverTest` with a fake poster and fake store.

**Consumes:** Task 8 intents (`hourId`, `date`). Domain `notificationTitle` / `notificationBody`. Channel sound `R.raw.church_bell`.

**Produces:** fire path from DESIGN.md (drop stale/already-notified/disabled; post ongoing HIGH notification; write `lastNotified`; schedule tomorrow). Tap opens `office/{hourId}`. Opening Hours cancels all six ids. Settings shows “Allow notifications” and “Allow exact alarms” when those are missing (fail closed; no inexact fallback).

**Done when:** unit tests for drop/stale/happy path; on emulator, setting an hour two minutes ahead posts a notification with the bell; tap opens that Office; returning to Hours clears the toast.

**Stop.** Do not start a G5 soak unless this session is Task 10.

---

## Task 10: Device pass

**Files:** none required except bugfixes. Manual checklist.

**Done when:** all of the following have been run on emulator (API 29 **and** 35) or on the G5 if it is Android 10+:

1. Set Prime two minutes ahead → bell + notification → tap opens that Office.
2. Open Hours → notification gone.
3. Reboot before the next hour → it still fires.
4. Deny notifications → Settings CTA → grant → next hour works.
5. Disable Terce → it does not fire.
6. Airplane mode the whole time (offline).

Record results (device, Android version, pass/fail) at the bottom of this file. Fix only failures found in this pass.

---

## Device pass log

_(Task 10 fills this in.)_

## Session prompt

One task per working session. Native, no subagents.

> Do only Task N in TASKS.md. Follow DESIGN.md. Stop when that task’s Done when is true. Do not start the next task. No subagents.
