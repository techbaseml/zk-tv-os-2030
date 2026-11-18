# ZK TV OS 2030

Native Kotlin TV launcher by **ZK LABS** · **by TechBaseML**.

**Status: 0.1.0 device-test, not a completed or device-certified release.**
Android 9 (API 28) minimum. Built specifically for Sanjay's Fire TV AFTSS / sheldon.

## Included

- OLED black, cyan/violet accents, lightweight gradient glass-style panels.
- Native remote/D-pad focus, short optional focus animations, landscape UI.
- Real launchable app discovery (Leanback and standard launcher activities).
- Installed YouTube, Netflix and JioHotstar shortcuts; an honest unavailable state if missing.
- Favorites: hold OK on a card to pin, remove or move to first.
- App library, refresh, live minute clock/date, 12/24-hour settings.
- Aurora, Midnight, Violet sky and downsampled custom JPEG wallpapers.
- Validated JSON settings export/restore, including favorites and theme.
- Original Amazon Home shortcut and BACK confirmation.
- Offline operation; no network permission, advertising, telemetry or background service.

## HOME limitation

This build is opened from **Your Apps & Channels**. It deliberately does not register as a replacement HOME activity, disable Amazon packages or install an accessibility redirector.

Single HOME continues to open Amazon Home. Triple HOME interception is **not implemented**. Android handles `KEYCODE_HOME` in the framework; an ordinary Activity cannot receive it. No reliable override has been established for firmware PS7717.5746N. Do not assume Fire OS 8 workarounds apply to Fire OS 7.

Optional weather is not included. Gradient panels approximate frosted glass without live blur. No zero-lag or full firmware-backup claims are made.

## Build

JDK 17, Android SDK platform 35, build tools 35.0.0, Gradle wrapper 8.9,
Android Gradle plugin 8.7.3, Kotlin 2.0.21. The runtime target remains Android 9.

```sh
export ANDROID_HOME=/path/to/android-sdk
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.
GitHub Actions runs the same tasks and uploads the APK plus lint report.

Dependency/tool versions are pinned; signed APKs are not promised byte-identical across machines.
Debug signing keys are machine-specific. A fresh GitHub runner may produce a different signing identity.
Use the same build machine/key for in-place updates. Do not uninstall to work around a signature mismatch without exporting settings first. A private stable release key should be provisioned before production distribution; no signing secret belongs in this repository.

## Install and test

Read [device workflow](docs/DEVICE_WORKFLOW.md) and [test report](docs/TEST_REPORT.md).
Install only after Sanjay approves the concrete APK. The cloud has no direct Fire TV connection;
local Termux performs ADB operations. No root, firmware changes, package disabling or port forwarding.

## Sources

- https://developer.android.com/reference/android/view/KeyEvent#KEYCODE_HOME
- https://developer.amazon.com/docs/fire-tv/design-and-user-experience-guidelines.html
- https://developer.amazon.com/docs/fire-tv/connecting-adb-to-device.html
- https://developer.android.com/build/releases/agp-8-7-0-release-notes
