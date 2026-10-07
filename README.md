# Liturgy of the Hours (Android)

![Liturgy icon](branding/liturgy-icon-vibrant.svg)

> A [Mobitecture](https://github.com/mohuddle) app · *apps, architected.*

Native Kotlin port of the [Omarchy Liturgy of the Hours](https://github.com/mohuddle/omarchy-liturgy-of-the-hours) bar widget.

Package: `io.github.mohuddle.hours`

This is the phone app. The desktop plugin stays in its own repository. The two do not share a process or settings file.

**Progress: 8 of 10 v1 tasks.** Next is Task 9 (Notifications, receiver, permission CTAs). Full board: **[TASKS.md](TASKS.md)**. Design: **[DESIGN.md](DESIGN.md)**.

| Status | Task |
|---|---|
| Done | 1. Gradle scaffold and bundled assets |
| Done | 2. Domain: hours, schedule, verses, cache |
| Done | 3. Domain: liturgical day and office assembly |
| Done | 4. Persistence (DataStore + asset JSON) |
| Done | 5. Hours screen |
| Done | 6. Office screen and navigation |
| Done | 7. Settings screen |
| Done | 8. Alarm scheduling |
| Next | 9. Notifications, receiver, permission CTAs |
| Remaining | 10. Device pass |

The debug APK opens on the Hours screen: six hours, the current hour in accent, and today’s BSB verse. The bell opens the featured hour’s office; tapping a row opens that hour. Gear opens Settings (reminders, enable, and time per hour). There is no Play listing.

---

## Overview

The Omarchy widget lists Lauds, Prime, Terce, Sext, None, and Vespers, highlights the current hour, shows one Berean Standard Bible passage per local calendar day, and rings a chapel bell at each enabled time. Tapping the bell opens a brief office: the liturgical day, a chapter, a short respond, a collect, and a memorial collect.

This repository is that same office on Android: Compose screens, `AlarmManager` bells, and the same bundled JSON. No network. Sideload a debug APK from a local build.

## Key features (v1 target)

1. **Canonical hours** — Lauds, Prime, Terce, Sext, None, Vespers, with enable and time per hour.
2. **Daily Scripture** — one curated BSB passage per local calendar day, sequential, bundled in `app/src/main/assets/verses.json`.
3. **The Office** — liturgical day (1662/1928/REC), chapter, short respond, collect, memorial collect from `app/src/main/assets/office.json`.
4. **Church-bell reminders** — exact `AlarmManager` clocks; a high-priority notification that stays until you tap it or open the app; tap opens that hour’s office.
5. **Offline** — no runtime network request, no accounts, no analytics.

Not in v1: Compline, Matins, lectionary lessons, TTS, confessions, a home-screen widget, Play Store.

### Hours and default times

| Hour | Latin | Traditional | Default |
|---|---|---|---|
| Morning Prayer | Laudes | Dawn | 06:00 |
| Prime | Prima | First Hour | 07:00 |
| Terce | Tertia | Third Hour | 09:00 |
| Sext | Sexta | Sixth Hour | 12:00 |
| None | Nona | Ninth Hour | 15:00 |
| Evening Prayer | Vesperae | Sunset | 18:00 |

Prime is 07:00 by default so it does not collide with Morning Prayer.

## How to build and install

There is no Play listing. Install from a build of this repository.

1. JDK 21 and an Android SDK with platform 36.
2. Clone this repository.
3. Point Gradle at the SDK (`sdk.dir` in `local.properties`, or `ANDROID_HOME`).
4. Build, test, and install:

```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`assembleDebug` succeeds (`minSdk 29`, `targetSdk 36`). `OfficeModelTest` 8/8. `HoursStoreTest` 6/6. `OfficeViewTest` 4/4. `AlarmSchedulerTest` 5/5 (six clocks, Terce off → five, evening after 18:00 tomorrow, boot/time/timezone). Manifest has `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED`. Hour fire still does not post a notification (Task 9).

## Technical specs

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| minSdk | 29 (Android 10) |
| compileSdk / targetSdk | 36 |
| Application id | `io.github.mohuddle.hours` |
| Domain spec | plugin `Model.js` + `tests/model.test.js` |
| Domain | `OfficeModel.kt` (hours, schedule, verses, cache, liturgical day, office) |
| Persistence | `HoursStore.kt` (DataStore preferences + `verses.json` / `office.json`) |
| Hours / Office / Settings | `HoursApp.kt` NavHost: `hours`, `office/{hourId}`, `settings`. `SettingsScreen.kt` reminders, enable, 24h time picker. |
| Alarms | `AlarmScheduler.kt` `setAlarmClock` per enabled hour. Reschedule on launch, settings, boot, time/timezone. |
| Assets | `verses.json`, `office.json`, `church_bell.ogg` |

## Privacy

The app does not need the internet. Scripture and office texts ship in the APK. Hour settings and the daily verse are stored in app-private DataStore. There is no account, no crash reporter, and no analytics.

## Security

Sideload only in v1. The scaffold declares no `INTERNET` permission. Exact clocks use `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED`. `POST_NOTIFICATIONS` and `VIBRATE` land with Task 9. Exact-alarm and notification access fail closed: Settings will send you to the system pages rather than falling back to inexact alarms.

## License

MIT. See [LICENSE](LICENSE). BSB text is CC0; collects and memorials are 1662/1928 public-domain texts. See [NOTICE.md](NOTICE.md).

---
Made by [Mobitecture](https://github.com/mohuddle) · apps, architected.
