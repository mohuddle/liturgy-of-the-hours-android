# Liturgy of the Hours (Android)

Native Kotlin port of the [Omarchy Liturgy of the Hours](https://github.com/mohuddle/omarchy-liturgy-of-the-hours) bar widget. Offline little hours, a brief office, a daily Berean Standard Bible verse, and a church bell at each enabled hour.

Package: `io.github.mohuddle.hours`

This is the phone app. The desktop plugin stays in its own repository. The two do not share a process or settings file.

**Status:** in progress. Task 1 (Gradle scaffold + bundled assets) is done. The app is not a usable office yet. See [TASKS.md](TASKS.md) for what is finished and what remains, and [DESIGN.md](DESIGN.md) for the product.

---

## Overview

The widget on Omarchy lists Lauds, Prime, Terce, Sext, None, and Vespers, highlights the current hour, shows one BSB passage per local calendar day, and rings a chapel bell at each enabled time. Tapping the bell opens a brief office: the liturgical day, a chapter, a short respond, a collect, and a memorial collect.

This repository is that same office on Android: Compose screens, `AlarmManager` bells, and the same bundled JSON. No network. No Play listing yet. Sideload a debug APK from a local build.

## Key features (v1 target)

1. **Canonical hours** — Lauds, Prime, Terce, Sext, None, Vespers, with enable and time per hour. Defaults: 06:00, 07:00, 09:00, 12:00, 15:00, 18:00.
2. **Daily Scripture** — one curated BSB passage per local calendar day, sequential, bundled in `app/src/main/assets/verses.json`.
3. **The Office** — liturgical day (1662/1928/REC), chapter, short respond, collect, memorial collect from `app/src/main/assets/office.json`.
4. **Church-bell reminders** — exact `AlarmManager` clocks; a high-priority notification that stays until you tap it or open the app; tap opens that hour’s office.
5. **Offline** — no runtime network request, no accounts, no analytics.

Not in v1: Compline, Matins, lectionary lessons, TTS, confessions, a home-screen widget, Play Store.

## How to build and install

There is no Play listing. Install from a build of this repository.

1. JDK 21 and an Android SDK with platform 36.
2. Clone this repository.
3. Point Gradle at the SDK (`sdk.dir` in `local.properties`, or `ANDROID_HOME`).
4. Build and install:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is a titled empty screen until Tasks 5–9 land. `assembleDebug` on 2026-10-05 succeeded with `minSdk 29` and `targetSdk 36`.

Pace: one task from [TASKS.md](TASKS.md) per working session. After a task’s “Done when” is true, update this README’s status line, check the box in TASKS.md, and push.

## Technical specs

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| minSdk | 29 (Android 10) |
| compileSdk / targetSdk | 36 |
| Application id | `io.github.mohuddle.hours` |
| Domain spec | plugin `Model.js` + `tests/model.test.js` |
| Assets | `verses.json`, `office.json`, `church_bell.ogg` |

## Privacy

The app does not need the internet. Scripture and office texts ship in the APK. Settings stay in app-private DataStore. There is no account, no crash reporter, and no analytics.

## Security

Sideload only in v1. The scaffold declares no `INTERNET` permission. Hour reminders will use `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, and `VIBRATE` when those tasks land. Exact-alarm and notification access fail closed: Settings will send you to the system pages rather than falling back to inexact alarms.

## License

MIT. See [LICENSE](LICENSE). BSB text is CC0; collects and memorials are 1662/1928 public-domain texts. See [NOTICE.md](NOTICE.md).
