# Claude Watch Buddy — Pixel Watch 3 port

A Wear OS port of
[vthinkxie/claude-desktop-buddy-esp32-s3-touch-amoled-1.8](https://github.com/vthinkxie/claude-desktop-buddy-esp32-s3-touch-amoled-1.8),
itself a port of
[anthropics/claude-desktop-buddy](https://github.com/anthropics/claude-desktop-buddy).

The watch becomes the "hardware buddy" device Claude for macOS / Windows pairs
with over BLE in developer mode. Instead of an ESP32 + AMOLED panel, you get a
Pixel Watch 3 on your wrist that shows session state, approval prompts, and the
Claude buddy character. Tap upper half of the screen to approve, lower half to
deny — same as the original firmware's touch bindings.

> **Target device**: Pixel Watch 3 (Wear OS 5 / Android 14). Should work on
> any Wear OS 4+ device with BLE peripheral support, which is most of them.

## Features

- BLE Nordic UART peripheral advertising as `Claude-XXXX`, same UUIDs as the
  ESP32 firmware — the Claude desktop Hardware Buddy window picks it up
  without any app-side changes.
- Full wire protocol implemented: heartbeat snapshots, turn events, permission
  prompts (approve / deny round-trip), status acks, time / owner / name /
  unpair commands, and folder-push acks.
- Seven-state buddy animation: `sleep`, `idle`, `busy`, `attention`,
  `celebrate` (on level-up every 50k tokens), `dizzy` (on shake), `heart`
  (after a fast approval).
- Three species you can cycle from the settings screen: blob, cat, robot.
- Foreground service keeps the GATT server alive when the watch screen is off.
- Approval screen auto-pops when a prompt arrives; auto-closes when it clears.

## Building

Requirements: **JDK 17+**, **Android SDK platform 34**, **build-tools 34.0.0**.

```bash
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Installing

Enable developer options + ADB debugging on the watch, then:

```bash
# pair via wi-fi if the watch isn't USB-tethered
adb pair <watch-ip>:<pairing-port>
adb connect <watch-ip>:<adb-port>

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

First launch prompts for `BLUETOOTH_CONNECT` + `BLUETOOTH_ADVERTISE` +
`POST_NOTIFICATIONS`. Grant them. A foreground notification appears while the
GATT server is running.

## Pairing with the Claude desktop

1. **Help → Troubleshooting → Enable Developer Mode** in Claude for macOS /
   Windows.
2. **Developer → Open Hardware Buddy…**
3. Click **Connect**; pick `Claude-XXXX` from the list.

macOS will prompt for Bluetooth permission on first use. Once bonded, the
bridge reconnects on its own whenever both sides are awake.

## Protocol parity with the ESP32 firmware

| Wire feature                  | Status |
| ----------------------------- | ------ |
| Nordic UART UUIDs + advertise | ✅     |
| Heartbeat snapshot parsing    | ✅     |
| Permission approve / deny     | ✅     |
| Status ack (battery / stats)  | ✅     |
| Time / owner / name / unpair  | ✅     |
| Folder push (char packs)      | ✅ acks + validates; bytes discarded (GIF playback TODO) |
| Turn events                   | ✅ parsed, not yet shown on screen |
| LE Secure Connections bonding | ⚠️ relies on Android's default bonding; no passkey display yet |

## Differences from the ESP32 original

- **No ASCII-art character rendering** — the 368×448 PSRAM canvas becomes a
  Compose `Canvas`. The seven animations are redrawn with geometric primitives.
- **No GIF character decoder yet** — pushed character packs are acked but the
  bytes are not rendered. Planned.
- **No physical button remap** — taps replace the ESP32's Key1/Key3 keys.
  Rotary crown navigation is a future improvement.
- **No passkey display** — LE Secure Connections bonding uses the OS's default
  pairing flow rather than the 6-digit passkey UI on the ESP32.

## Layout

```
app/src/main/
  AndroidManifest.xml
  java/rocks/claudiusthebot/watchbuddy/
    MainActivity.kt                — Compose entry + permission request
    ble/
      NusConstants.kt              — NUS UUIDs
      BuddyBleService.kt           — foreground GATT server + advertiser
    protocol/
      BuddyProtocol.kt             — line splitter + JSON dispatch
      Heartbeat.kt                 — heartbeat data class
    buddy/
      BuddyRenderer.kt             — per-state Compose canvas
    state/
      BuddyState.kt                — enums + UI state
      BuddyStore.kt                — StateFlow store + DataStore persistence
    ui/
      WatchBuddyApp.kt             — screen routing
      screens/
        NormalScreen.kt
        ApprovalScreen.kt
        InfoScreen.kt
        PetScreen.kt
        SettingsScreen.kt
  res/
    values/strings.xml colors.xml
    drawable/ic_launcher_{background,foreground}.xml
    mipmap-anydpi-v26/ic_launcher{,_round}.xml
```

## License

Same as the ESP32 firmware port (MIT), itself derived from
[anthropics/claude-desktop-buddy](https://github.com/anthropics/claude-desktop-buddy).
