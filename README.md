# Eink Launcher

A minimalist, strictly black-and-white Android home-screen launcher, built with Kotlin +
Jetpack Compose. It translates the attached React/JSX prototype into a native app: a clock
+ widget home screen with a small pinned-app grid, and a swipe-left Notes screen with a
single rich note.

- **Package / applicationId:** `com.giles.einklauncher`
- **UI:** 100% Jetpack Compose (no XML layouts, no Views)
- **Min SDK:** 26 (Android 8.0) · **Target/Compile SDK:** 35
- **Persistence:** DataStore (Preferences) for settings + pinned grid; Room for the note
- **Design system:** pure black and white only — hierarchy from size/weight, selected
  states invert (solid fill), no greys, no accent colours, no ripples

## Building

This project uses the Gradle wrapper (Gradle 8.11.1) and the Android Gradle Plugin 8.7.3,
so it needs a JDK 17+ and an Android SDK with platform 35 and build-tools 35.

```bash
# Point Gradle at your SDK (or set sdk.dir in local.properties, or open in Android Studio)
export ANDROID_HOME=/path/to/Android/sdk

./gradlew assembleDebug        # build the APK
./gradlew installDebug         # install on a connected device/emulator
```

Then, on the device, set it as the default launcher: open the app once and tap **Set as
default launcher** in Settings (long-press the widget or empty home space), or use the
system Home-app setting.

> **Note on this environment:** the CI/web environment this was authored in blocks
> `dl.google.com` and Google's Maven repository at the network-policy level, so the Android
> SDK could not be installed and the project could **not be compiled here**. The code is
> written to build under a standard Android toolchain (Android Studio / a CI runner with SDK
> access). Please run `./gradlew assembleDebug` in such an environment to produce the APK.

## What's implemented (by build milestone)

1. **Scaffold + launcher role** — Compose project, `HOME`/`LAUNCHER` intent filters,
   `singleTask` + `excludeFromRecents`, `RoleManager.ROLE_HOME` request (API 29+) with a
   pre-Q home-settings fallback, and non-destructive back handling.
2. **App grid** — bottom-aligned 4-col grid (1–2 rows), outline tiles.
3. **App querying + edit flow** — real installed-app enumeration, tap-to-launch, long-press
   remove/add, picker bottom sheet, DataStore persistence, sensible first-run defaults.
4. **Widget** — live clock + date, next alarm, next calendar event (+ expandable schedule),
   Open-Meteo weather (+ expandable 7-day forecast columns).
5. **Notes** — `HorizontalPager` (2 pages), single continuous note with mixed line types and
   per-line character formatting, inline toolbar, overflow fade, Room persistence.
6. **Settings** — bottom sheet: appearance / clock style / weather location / app count /
   notification filter, each wired to real behaviour.
7. **Notification listener** — `NotificationListenerService` that suppresses notifications
   from non-pinned packages when the filter is on.

## Android system integrations

| Concern | Where | Notes |
|---|---|---|
| Enumerate apps without `QUERY_ALL_PACKAGES` | `AndroidManifest.xml` `<queries>` + `InstalledAppsRepository` | Uses the MAIN/LAUNCHER intent query, Play-compliant |
| Default launcher | `DefaultLauncherHelper`, `MainActivity` | `ROLE_HOME` on API 29+, `ACTION_HOME_SETTINGS` fallback |
| Calendar | `CalendarRepository` | `READ_CALENDAR` runtime permission with rationale dialog; hides the row if denied |
| Location (weather) | `LocationProvider` | `ACCESS_COARSE_LOCATION`, only requested when no manual location is set |
| Next alarm | `AlarmRepository` | `AlarmManager.getNextAlarmClock()`, no permission; only reflects alarms set via AlarmManager |
| Weather | `WeatherRepository`, `OpenMeteoApi` | Open-Meteo (no API key), HTTPS + Retrofit; manual location wins over device location |
| Offline handling | `NetworkStatus` | Connectivity check; weather row degrades gracefully |
| Notification filter | `LauncherNotificationListenerService`, `NotificationAccess` | `BIND_NOTIFICATION_LISTENER_SERVICE`; enabled via the special notification-access settings deep link |

### Notification-access & Play Store

The notification filter needs `NotificationListenerService`, whose access is a **special
access** the user grants in system settings (`ACTION_NOTIFICATION_LISTENER_SETTINGS`) — never
a runtime dialog. On the Play Store this is a policy-sensitive permission: apps must have a
core feature that genuinely uses notification data and complete the Play Console declaration.
"Notification management/filtering" is an accepted use, but access that looks incidental can
be rejected. That's why the toggle is opt-in and does nothing until the user explicitly
grants access.

## App icons

Grid and picker tiles render each app's **real launcher icon put through a monochrome
filter** (`ui/icons/AppIcon.kt`): the icon is desaturated to greyscale with a slight
contrast boost, and in dark mode its luminance is inverted so it reads as light marks on
black. If an icon can't be loaded, the tile falls back to the app's monogram. The home grid
is label-less, matching the prototype — the icons carry recognition; the picker keeps labels.
Bitmaps are rasterised once off the main thread and cached per (app, theme).

## Project layout

```
app/src/main/java/com/giles/einklauncher/
  MainActivity.kt, EinkLauncherApp.kt, DefaultLauncherHelper.kt
  data/            DataStore, settings, apps, notes (Room), widget (alarm/calendar/weather)
  notifications/   NotificationListenerService + access helper
  ui/              LauncherRoot, LauncherViewModel, theme/, components/, home/, notes/, settings/
```
