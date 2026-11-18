# Test report — ZK TV OS 2030

Date: 2026-11-18. Target: Amazon AFTSS/sheldon, Android 9/API 28, PS7717.5746N.

| Test | Status | Evidence / next step |
|---|---|---|
| Authorized local ADB | PASS, user supplied | `adb devices -l` shows device |
| Device version | PASS, user supplied | Android 9, SDK 28, PS7717.5746N |
| Original HOME resolution | PASS, user supplied | Amazon HomeActivity_vNext |
| Original Home recovery before install | PASS, user observed | TV returned from Settings to Amazon Home |
| Build / static lint | PENDING | Update after actual build |
| Installation | NOT RUN | Requires approval and local ADB |
| Startup/rendering | NOT RUN | Check clipping, text, icons on the actual TV |
| D-pad and Back/Menu | NOT RUN | Reach every card and setting without touch |
| App shortcuts | NOT RUN | Test installed YouTube, Netflix, JioHotstar and other apps |
| Missing-app state | NOT RUN | Must explain absence without launching the wrong app |
| Favorites/reordering | NOT RUN | Add/remove/move first, leave and return |
| Settings persistence | NOT RUN | Force-stop/reopen and reboot TV |
| Clock/date | NOT RUN | Observe minute change and 12/24-hour toggle |
| Wallpapers | NOT RUN | Three built-ins; custom JPEG; corrupt/oversized image errors |
| Export/restore | NOT RUN | Export, pull, change settings, push, restore; verify equality |
| Invalid backup | NOT RUN | Invalid schema/JSON must leave settings unchanged |
| Amazon Home after install | NOT RUN | Button and physical HOME must return to Amazon |
| Uninstall recovery | NOT RUN | Export/pull first, uninstall only ZK, verify Home |
| Single HOME → ZK | NOT IMPLEMENTED | Safe automatic override not established for this firmware |
| Triple HOME → Amazon | NOT IMPLEMENTED | HOME not delivered to ordinary Activities |
| Optional weather | OMITTED | Offline build has no network permission |
| Memory/startup/frame/CPU | NOT MEASURED | Commands in DEVICE_WORKFLOW.md |
| Long-session stability | NOT RUN | At least 30 minutes of app switching/navigation |

This is a device-test build. It must not be described as fully working or complete
until the supported rows pass on the actual device. Build/lint success alone is
not proof of correct TV rendering, remote navigation, or performance.
