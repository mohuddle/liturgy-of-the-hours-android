# Liturgy of the Hours — Android v1

Faithful native port of the Omarchy plugin [`io.github.mohuddle.liturgy-of-the-hours`](https://github.com/mohuddle/omarchy-liturgy-of-the-hours) (v1.4.0). Not a merge with daily-office.

## Product

- Six hours: Lauds, Prime, Terce, Sext, None, Vespers.
- Church bell at each enabled hour.
- Bell (and tapping a row) opens the brief office: chapter, short respond, collect, memorial.
- Daily BSB verse on the home screen.
- Settings: reminders on/off, enable and time per hour.

Out of v1: Compline, Matins, lectionary, TTS, confessions, Play Store, desktop sync, home-screen widget.

## Constraints

- Package: `io.github.mohuddle.hours`
- App name: Liturgy of the Hours
- Kotlin, Jetpack Compose, Material 3
- `minSdk 29` (Android 10), `targetSdk` current
- Test device: LG G5 (h830) only if Lineage is Android 10+ (Lineage 22.2 / Android 15 is fine)
- Offline only. No network, no accounts, no analytics
- License: MIT app code; BSB CC0; 1662/1928 collects public domain. Carry `NOTICE.md` from the plugin
- Sideload APK
- Plugin `Model.js` + `tests/model.test.js` are the behavioral spec for domain code

## Screens

**Hours (start destination).** Jerusalem Cross (accent while an hour is in its 20-minute current window). Title. Subtitle = featured hour meta (`Terce · Third Hour · 09:00`). Bell → Office for featured hour (current, else next). Gear → Settings. Six hours (short name + HH:MM); current bold + accent; disabled muted; row tap → Office for that hour. Today’s Scripture (reference, text, BSB). Opening Hours dismisses all hour notifications.

**Office.** Liturgical heading (+ feast). Hour name in accent. Sections: The Chapter, The Short Respond, Collect, Memorial Collect. Same assembly as `Model.buildOffice`. Back → Hours.

**Settings.** Reminders on/off. Per hour: enable + time picker. Defaults: 06:00, 07:00, 09:00, 12:00, 15:00, 18:00. Permission CTAs if notifications or exact alarms are denied. Saving reschedules immediately. Verse order stays sequential; no picker in v1.

## Bells

`AlarmManager.setAlarmClock` per enabled hour for its next occurrence (today or tomorrow). After fire, schedule tomorrow. Reschedule on app start, settings change, `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`.

Notification channel `hours`: HIGH, `church_bell` sound. Ongoing. Title `{name} — {traditional}`. Body: hymn, invitatory, today’s Scripture reference. Tap → Office for that hour. Extras carry `hourId` + intended `date`; drop stale date or already-notified. Opening the app cancels all six ids.

Permissions: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`. No `USE_EXACT_ALARM`. No battery-optimization prompt in v1.

Desktop’s 5-minute poll grace is not used. The alarm is the event.

## Data

DataStore: `notificationsEnabled`, `{id}Enabled` / `{id}Time` for the six ids, `versePosition`, `cachedDate`, `verseReference`, `verseText`, `lastNotified`.

Assets copied from the plugin: `verses.json`, `office.json`, `church-bell.ogg`, icons. Invalid JSON shows an error string; alarms still fire from the `HOURS` table.

## Non-goals

Quickshell on Android. TWA of daily-office. Sync with `~/.local/state/omarchy/settings/liturgy-of-the-hours.json`. Glance widget.
