# Device workflow

## Verified pre-development observations (2026-11-18)

Sanjay ran commands locally and supplied the outputs:
- Authorized ADB device: AFTSS, product/device sheldon.
- Android release 9, SDK 28, build PS7717.5746N.
- HOME resolves to `com.amazon.tv.launcher/.ui.HomeActivity_vNext`.
- Explicitly starting that activity brought Amazon Home to the foreground;
  Sanjay confirmed the TV left Settings and returned to Home.

This verifies the original launcher recovery command **before installation** only.
It is not a full firmware backup or a post-install rollback test.

## Connect locally

Phone/computer and TV must share a trusted LAN. Enable ADB Debugging in
Settings > My Fire TV > Developer Options (reveal by selecting the device name
seven times in About if needed). Use the IP displayed in About > Network.
Do not expose ADB on the public internet.

```sh
pkg install android-tools
adb connect TV_LOCAL_IP:5555
# Accept this connection's authorization prompt on the TV.
adb devices -l
```

Replace `TV_LOCAL_IP` below with the current TV address. `device` is required;
stop on unauthorized/offline/refused/timeout.

## Record the configuration before installation

```sh
adb -s TV_LOCAL_IP:5555 shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME > original-home.txt
adb -s TV_LOCAL_IP:5555 shell getprop ro.build.display.id > original-build.txt
```

## Install only after approval

Download `ZK_TV_OS_2030_device_test.apk` to the phone's Downloads folder.
Grant Termux storage access once with `termux-setup-storage` if required.

```sh
adb -s TV_LOCAL_IP:5555 install -r "$HOME/storage/downloads/ZK_TV_OS_2030_device_test.apk"
adb -s TV_LOCAL_IP:5555 shell am start -W -n com.zklabs.tvos/.MainActivity
```

Do not proceed after an installation error. In Fire TV's Apps list, pin/move ZK TV OS
near the front for easy access. HOME remains Amazon Home.

## Backup and restore

Select Settings > Export settings on the TV, then save the export to your phone:

```sh
adb -s TV_LOCAL_IP:5555 pull /sdcard/Android/data/com.zklabs.tvos/files/zk-settings.json ./zk-settings.json
```

Restore: after installing and opening the app once, push the file and choose Settings > Restore settings:

```sh
adb -s TV_LOCAL_IP:5555 push ./zk-settings.json /sdcard/Android/data/com.zklabs.tvos/files/zk-settings.json
```

Custom wallpaper (JPEG under 20 MB):

```sh
adb -s TV_LOCAL_IP:5555 push ./wallpaper.jpg /sdcard/Android/data/com.zklabs.tvos/files/wallpaper.jpg
```

Then choose Settings > Wallpaper > Custom image. Back up wallpaper.jpg separately.
Uninstallation removes the app's preferences and its external app-files directory.
No storage permission or unreliable Fire TV document picker is required.

## Recovery / rollback

1. Press remote HOME, or use ZK's Amazon Home shortcut.
2. If needed, run the already-verified original launcher command:

```sh
adb -s TV_LOCAL_IP:5555 shell am start -n com.amazon.tv.launcher/.ui.HomeActivity_vNext
```

3. Export and pull settings before uninstalling if they must be retained.
4. Only to remove ZK after deciding to roll back:

```sh
adb -s TV_LOCAL_IP:5555 uninstall com.zklabs.tvos
```

5. Verify Amazon Home and existing apps still work. No Amazon package is modified
or disabled by this project, so none needs re-enabling.
6. Disable ADB debugging when testing is finished if it is no longer needed.

## Measurements (collect; do not invent)

```sh
adb -s TV_LOCAL_IP:5555 shell am force-stop com.zklabs.tvos
adb -s TV_LOCAL_IP:5555 shell am start -W -n com.zklabs.tvos/.MainActivity
adb -s TV_LOCAL_IP:5555 shell dumpsys meminfo com.zklabs.tvos
adb -s TV_LOCAL_IP:5555 shell dumpsys gfxinfo com.zklabs.tvos reset
# Navigate with the remote for one minute, then:
adb -s TV_LOCAL_IP:5555 shell dumpsys gfxinfo com.zklabs.tvos
adb -s TV_LOCAL_IP:5555 shell dumpsys cpuinfo
```

Record three cold starts, PSS memory, frame/jank statistics and idle CPU. Check idle
again after several minutes. Reports can include other application/process names;
share only relevant sections. Test correctness before optimizing performance.
