# MotoIntercom

Android app for Rider-Pillion intercom using a single phone.
- Rider: Wired Headset
- Pillion: Bluetooth Headset (e.g., OPPO Enco Buds 2)

## Features
- **Foreground Service**: Keeps audio running in background.
- **Audio Routing**:
  - **Rider Speaking**: Wired Mic -> Bluetooth Output.
  - **Pillion Speaking**: Bluetooth Mic -> Wired Output.
- **Floating Overlay**: Switch active mic or stop service from any screen.
- **MIUI Support**: Helper to open Autostart settings.

## Prerequisites
- Android SDK (configured in `local.properties`)
- Gradle (install manually or use wrapper if available)
- ADB for installation

## Build & Run
1.  Open terminal in this directory.
2.  Run `gradle assembleDebug` (assuming gradle is in PATH).
3.  Install APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
4.  Launch "MotoIntercom".

## Usage
1.  **Permissions**: Grant Audio, Bluetooth, and Overlay permissions when prompted.
2.  **MIUI Users**: Click "MIUI Settings" and enable "Autostart" for MotoIntercom to prevent background killing.
3.  **Start**: Click "Start Intercom".
4.  **Overlay**: Use the floating bubble to "Switch Mic" (toggle between Rider and Pillion input) or "Stop".

## Troubleshooting
- **No Audio**: Ensure Bluetooth headset is connected *before* starting the service.
- **Background Kill**: On Xiaomi/Redmi devices, ensure "Battery Saver" is set to "No Restrictions" and Autostart is enabled.
