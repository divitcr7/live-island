# Live Island

An open-source island for Android that stays on screen around your camera, runs several things at once, and collects nothing.

- **Always there.** At rest it shows what you pick (next event, battery, date, network speed, a countdown, a pinned note) or shrinks to a ring around the lens.
- **Four themes**, each fully customisable: **Signal**, **Coded**, **Instrument**, **Clay**. Change the colour, how progress shows on the edge (line, ticks, underline, fill), the corners, the body, the typeface and the motion.
- **Fits any camera.** Reads the display cutout and grows from a left, centre or right camera. You can drag it into place and resize it.
- **Zero data.** The app does not request internet access. No account, analytics or crash reporting.

## What it does

| | |
|---|---|
| Music | Any player through media sessions: art, waveform, seek bar, play/pause/skip |
| Calls | Answer or decline from the island, then a call timer |
| One-tap OTP | Login codes from SMS with a Copy button |
| Timers | From your clock app, or started on the island ("timer 10") |
| Navigation | Next turn and arrival from Maps, Waze, OsmAnd, Organic Maps |
| Live updates | Deliveries, rides, downloads, Android 16 promoted notifications |
| Message peeks | Short pop-ups with quick reply |
| Charging | Percentage, time to full, and real watts and temperature if you want them |
| Camera & mic watch | Which app is using them, by name |
| Earbuds & watch | Battery when they connect |
| Satellites | Several activities at once; swipe between them |
| Command bar | Hold the island: `timer 10`, `focus 25`, `note door 4471`, `torch`, `wifi`, app names |
| Shelf | Share or drag things onto the island; drag them out later |
| Pinned notes, countdowns, focus sessions | |
| Automation | Tasker or any script can post to the island (see below) |

### Gestures

| Gesture | Does |
|---|---|
| Tap | Open / close |
| Swipe ← → | Switch between running activities |
| Swipe ↓ | Open the full card |
| Swipe ↑ | Shrink one step (never fully hides). On a pop-up, dismiss it |
| Hold | Command bar |
| Double-tap | Main action (play/pause, answer, copy) |

## Build

Requirements: JDK 17+, Android SDK 35.

```bash
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # unit tests (OTP parsing, commands)
./gradlew :app:recordPaparazziDebug   # renders every screen and island state to app/src/test/snapshots
```

After installing: open the app and turn on the two switches it asks for. On Android 13+ a sideloaded app's accessibility switch is greyed out until you open **App info → ⋮ → Allow restricted settings**. The app links you straight there.

## Project layout

```
app/src/main/java/app/liveisland/
├── core/      Settings (what the user chose), SettingsStore (DataStore), models
├── engine/    IslandEngine (what's on the island, priorities, focus), Timers, Commands, Otp
├── island/    The island renderer, shared by the overlay and every preview
│   ├── IslandStyle.kt   themes, palettes, fonts, motion
│   ├── Surface.kt       body + progress rim (line, ticks, underline, fill)
│   ├── Views.kt         compact / expanded / pop-up layouts per activity
│   └── Island.kt        sizing, transitions, satellites, gestures, command bar, shelf
├── sources/   Media sessions, notifications, battery, camera/mic, Bluetooth, calendar, automation
├── service/   Accessibility overlay window, notification listener, cutout detection
├── shelf/     Shelf storage and the share target
└── ui/        Settings app in its own design language ("Paper"), with live previews
```

The island composable that draws the real overlay is the same one used in every settings preview, so what you see while choosing is exactly what you get.

## Automation

Turn on **Automation** in the app, then:

```bash
am broadcast -a app.liveisland.SHOW --es id build --es title "Build #42" --es text "Running tests" --ei progress 60 --ei seconds 120
am broadcast -a app.liveisland.DISMISS --es id build
```

Extras: `id`, `title`, `text`, `source`, `progress` (0–100), `countdown` (seconds), `seconds` (auto-remove).

## Permissions

| Permission | Used for |
|---|---|
| Accessibility | Drawing over the status bar (the only window type that can), and knowing the open app so it can hide where you asked. It doesn't read screen content. |
| Notification access | Music, calls, timers, navigation, live updates, codes. Processed in memory only. |
| Calendar | Next event at rest |
| Phone calls | Answer / decline |
| Nearby devices | Earbud and watch names and battery |
| Do Not Disturb | Focus sessions, only if you allow it |

Not supported: drawing on the screen-off always-on display (Android doesn't allow it), or reading the clipboard in the background (blocked since Android 10; the shelf uses share and drag-and-drop instead).

## Fonts

Geist and Geist Mono (Vercel), Figtree and Martian Mono are bundled under the SIL Open Font License; see `licenses/`.
