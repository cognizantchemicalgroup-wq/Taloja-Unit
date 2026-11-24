# 52 Weeks: Workout Countdown

A personal 52-week training tracker. The challenge runs from **Thursday 19 Nov 2026 to Wednesday 18 Nov 2027** (364 days). It comes in two versions:

| Version | Where it runs | Where data is saved |
|---|---|---|
| Cloud page | claude.ai artifact (private to the owner) | Artifact cloud database, owner-only |
| Android app | Your phone (APK) | On the phone (WebView localStorage), with backup/restore |

The two versions don't sync with each other. Use **Export backup / Import** to move data across.

---

## Features

- **Countdown:** current week (1–52), day of 364, days left, progress bar.
- **Stats:** workout days, days off, current streak, workouts this week (out of 7).
- **Log a workout:** date, type, minutes, feeling (Easy / Good / Hard / Max effort), notes.
- **Workout types:** Strength, Cardio, Run, HIIT, Yoga, Walk, Cycling, Sports, plus **+ Custom** types that are saved for next time.
- **Day off:** switch the form to *Day off* and pick a reason (Rest, Sick, Travel, Busy, Injury). Days off keep your streak alive; only an unlogged day breaks it.
- **Tap a date** in the Attendance calendar to log that day. Saving shows a confirmation pop-up.
- **Clear rules:** a day off can't be saved on a day that already has a workout; a workout saved on a day off replaces it; future dates are blocked.
- **Remove a custom type:** select it, then tap *Remove “…”*.
- **Attendance:** big attendance % (present ÷ days so far), a present / day off / missed bar, and a month calendar with large date boxes (‹ › to change month, Nov 2026 – Nov 2027).
- **Year calendar:** one row per week (W1–W52), read left to right Thursday → Wednesday. Blue = workout, orange = day off, pink = missed, outlined = upcoming, thick border = today. It opens scrolled to the current week.
- **Recent list:** last 40 entries, newest first, with a two-tap delete (Delete → Confirm?).
- **Day / Night theme:** a switch at the top (Auto, Day, Night). Auto follows the phone setting; your choice is remembered. In the app the status bar changes colour too.
- **Daily reminder (app only):** a notification at a time you choose (default 8 PM) that fires only if today has no workout or day off logged. Set it under *Reminder & widget*; *Send a test reminder* shows one immediately. Survives phone restarts.
- **Home-screen widget (app only):** this week's 7 days as dots (Thu→Wed), today's status, streak, attendance and days left. Tap it to open the app.
- **Backup (app only):** Export opens Android's share sheet (WhatsApp, Drive, Gmail…). Import merges a pasted backup without duplicating entries.

---

## Repository layout

```
CODEBASE.md                         ← this file
web/
  52-weeks-cloud.html               ← cloud version (published as a claude.ai artifact)
android-app/
  settings.gradle, build.gradle     ← Gradle project (Android Gradle Plugin 8.7.3)
  gradle.properties
  gradlew, gradle/wrapper/          ← Gradle 8.14.3 wrapper
  app/
    build.gradle                    ← app id com.taloja.fiftytwoweeks, minSdk 24, targetSdk 34
    src/main/
      AndroidManifest.xml
      java/com/taloja/fiftytwoweeks/
        MainActivity.java           ← full-screen WebView + JS bridge (share, theme bars, sync, reminder)
        Store.java                  ← native copy of logged dates; works out week, streak, attendance
        ReminderReceiver.java       ← daily alarm → "You haven't logged today" notification
        BootReceiver.java           ← re-books the reminder after restart / update
        WeekWidget.java             ← home-screen widget
      res/layout/widget.xml, res/xml/widget_info.xml  ← widget layout and settings
      assets/index.html             ← the tracker UI (offline version)
      res/                          ← app name, themes (light/dark), launcher icon (dumbbell)
.github/workflows/android-apk.yml   ← builds the APK on GitHub
```

---

## How it works

### The UI (`assets/index.html` and `web/52-weeks-cloud.html`)
One self-contained HTML file with inline CSS and JavaScript, and no frameworks.

Key constants and functions:

| Name | What it does |
|---|---|
| `START` | `new Date(2026, 10, 19)`, day 1 of the challenge |
| `TOTAL` | `364` days |
| `DEFAULT_TYPES` | built-in workout types |
| `OFF_REASONS` | reasons for a day off |
| `renderCountdown()` | week number, stats, streak and the 364-cell grid |
| `renderChips()` / `renderOffChips()` | workout-type and day-off-reason buttons |
| `renderLog()` | recent entries list |
| `setMode("workout" \| "off")` | switches the form between Workout and Day off |

**Entry format** (one per logged item; a day can have several workouts):
```json
{ "id": "abc123", "date": "2026-11-21", "kind": "workout",
  "type": "Strength", "minutes": 45, "feel": "Good",
  "note": "Chest 3 variation", "createdAt": "2026-11-21T14:20:11.368Z" }
```
A day off uses `"kind": "off"`, `"type": "Day off"`, `"reason": "Rest"`. Entries without `kind` count as workouts.

**Streak rule:** count back from today (or yesterday, if today isn't logged yet) while each day has a workout or a day off.

### Cloud version storage
Uses the claude.ai artifact `db` capability. The access rule `{path: "", read: "owner", write: "owner"}` means only the owner can read or write. Collections:
- `workouts/<id>`: entries
- `config/types`: `{ list: [...custom types] }`

### Android version storage
`index.html` swaps the cloud database for a small local shim (`db` object) backed by `localStorage`:
- `w52.entries`: the array of entries (seeded with your first entries from 19–22 Nov on first launch)
- `w52.types`: custom workout types

Whenever the log changes, the page calls `Android.syncState()` with the workout and day-off dates. `Store.java` saves them in SharedPreferences so the widget and reminder work even when the app is closed.

`MainActivity.java` loads `file:///android_asset/index.html` in a WebView with JavaScript and DOM storage enabled. It exposes `window.Android.shareText(text)` so the page's **Export backup** button can open the Android share sheet. Links to other sites open in the phone's browser.

> ⚠️ Uninstalling the app or using *Clear storage* in Android settings deletes the log. Export a backup first.

---

## Getting the APK

1. The workflow **Build Android APK** runs on GitHub on every push that changes `android-app/`. You can also start it by hand from the **Actions** tab with *Run workflow*.
2. Open the finished run → **Artifacts** → download **52-weeks-apk** (a zip containing `52-weeks.apk`).
3. On the phone, open the APK. Allow *Install unknown apps* for your browser or file manager when Android asks.

The APK is signed with a debug key, which is fine for personal use. Publishing on the Play Store would need your own release key.

### Building locally (optional)
Install Android Studio (or the Android SDK with platform 34) and JDK 17, then:
```bash
cd android-app
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

---

## Changing things

| To change… | Edit |
|---|---|
| Start date / length | `START` and `TOTAL` in `index.html` (and the header text "19 Nov 2026 → 18 Nov 2027") |
| Built-in workout types | `DEFAULT_TYPES` |
| Day-off reasons | `OFF_REASONS` |
| Colours | the `--accent`, `--done`, `--off` tokens at the top of the `<style>` (light and dark blocks) |
| App name | `android-app/app/src/main/res/values/strings.xml` |
| App icon | `res/drawable/ic_launcher_foreground.xml` and `icon_bg` in `res/values/colors.xml` |
| Version shown on the phone | `versionCode` / `versionName` in `android-app/app/build.gradle` |
