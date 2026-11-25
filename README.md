# Sleep Timer — Android

A minimal, glass-style countdown timer (2–60 minutes) that runs as a foreground service. On completion it requests a cleanup sequence through Android Accessibility: attempt to clear the Recent Apps overview, open Quick Settings and attempt to toggle Bluetooth, then lock the device using Device Admin.

## Limitations (Android/OxygenOS)
Android does not allow ordinary apps to force-stop other apps. Clearing Recent Apps is an accessibility-based UI attempt, and app background services may continue. Quick Settings tile labels and layout vary between manufacturers/Android versions, so Bluetooth automation is best-effort. Device Admin must be granted for the screen-lock action.

## Build
Open in Android Studio (Jellyfish or newer) and build `app` > `assembleDebug`. Or use the included GitHub Actions workflow to build the APK and download it from Actions artifacts.

## Setup on device
1. Install the APK.
2. Open Sleep Timer and grant Notifications.
3. Settings → Accessibility → Downloaded apps/services → Sleep Timer → enable.
4. Tap Screen lock in the app and activate device-admin permission.
5. On OxygenOS, set Sleep Timer to unrestricted battery usage / allow background activity for reliability.

## Source
Kotlin + Jetpack Compose. No analytics or network permissions.