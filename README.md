# DynAmic Origin
Android camera island for iQOO and other Android 14+ devices. Independent implementation using Android public APIs, with no accessibility permission, root, package spoof or network permission.

## First setup
1. Install the signed APK; grant display-over-other-apps and notification listener access.
2. Allow runtime notifications, then press **Bật đảo** while the app is visible.
3. Tune width, height, horizontal alignment and top offset to surround the camera. The overlay is below critical system windows; some firmware may prevent camera-area touch or display.
4. On OriginOS enable unrestricted battery, auto-start and associated startup as supported by the installed firmware.
5. Long press for the inbox; tap the physical left half to open the newest notification, and the right half for recently used apps. Swipe up to collapse.

Transfer of ordinary notifications is optional. Capture is persisted before requesting cancellation. Ongoing, foreground service, call, alarm, media and core system notifications are retained. This cannot prevent an initial system banner. Keep source notification permissions enabled; turn off source channel banners manually if desired. Source PendingIntents may expire after cancellation: opening the app is the fallback, quick reply is available when the source exposes a mutable free-form RemoteInput action. Reply tokens may expire after transfer; failures are shown without claiming delivery.

## Implemented
Persistent configurable overlay with stable screen anchor; iOS-inspired light/dark controls; compact/expanded inbox; bounded on-device history; source actions and quick reply; per-app filters; progress bars; two compact activity labels; media transport buttons; screen-lock privacy; foreground runtime; boot/update recovery attempt; diagnostics; opt-in ordinary notification transfer.

## Not yet implemented / not guaranteed
Separate two-bubble display, full media metadata cards, advanced navigation parsing, charging/earbuds cards, iPhone system integrations, AOD, universal heads-up suppression or full SystemUI replacement. Lock-screen private content is intentionally hidden; 0.7.0 requests an anonymous island/count on the lock screen. No claim of device validation until real testing.

## Build
JDK 17+, Android SDK platform 36; `./gradlew :app:assembleDebug :app:lintDebug`.
For signed builds create gitignored `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`, then `./gradlew :app:assembleRelease`. Absolute storeFile paths are supported. Do not lose the signing key.

CI runs once per push/PR, cancels superseded jobs, caches Gradle and uploads a debug APK. Debug signing is for development; install the release APK for a stable update identity.

## Position fix in 0.3.0
Camera bounds now come from display WindowMetrics once per screen geometry, never from the resized overlay. The center and top stay fixed while the window changes size. Use **Đặt lại vị trí** once if old calibration is off, then fine-tune horizontal/top offsets. This fixes a feedback path in 0.2.0 that could move the island repeatedly.

The UI follows Apple material hierarchy and iOS geometry, but Android cannot reproduce proprietary iOS rendering 100%. Camera island stays black; glass is concentrated in controls. See docs/DESIGN_SPEC.md for design sources and limitations.

## 0.4.0 compact inbox and recent apps
Notifications default to short rows with one-line title/body; tap to reveal actions. First 8 entries are shown, with load more. Right-half tap opens a custom recent-app list, not Android Overview. Enable Usage Access from the app or the prompt inside the island. It reads the past 24h of foreground-resume events only on demand, filters to launchable apps, deduplicates, and keeps up to 12 entries in memory. No usage history is written to disk or sent online. Notification functionality does not require Usage Access.

## 0.5.0 touch and rotation fix
The header's gesture owner now survives notification/card redraws, preserving long press. Incoming notification refreshes are queued during a touch. Display changes are observed directly, using live display metrics instead of relying solely on service configuration callbacks. Compact UI shows the source icon/title or active progress.
**Tương tác ổn định** defaults on and places the interactive island just below the status bar so ordinary overlay touches can reach it in other apps. Turn this off only if your firmware allows touch in the camera/status-bar region. Ordinary application overlays cannot override the system status bar or apps that hide overlays. In landscape the island uses the top center; it is not a camera-following vertical iPhone island. No accessibility or root is added.

## 0.5.1 camera-position correction
The default is camera placement. Upgrading automatically reverses 0.5.0's below-status-bar default once, preserving manual size/offset calibration and all notification data. The optional **Đặt dưới thanh trạng thái** switch must be enabled explicitly. No automatic fallback moves the island away from the camera. Camera-area touch remains subject to Android SystemUI restrictions; this position correction does not claim to override them.

## 0.6.0 calibration
Compact width now honors the chosen percentage regardless of notification presence. Removed hardcoded 136/208dp and camera-width floors; compact height no longer silently grows to cover cutout bounds. Width 8–90%, height 12–120dp, top 0–240dp, horizontal -240–240dp, expanded width 40–100%, corner radius 0–60dp, icons 12–32dp, side padding 0–24dp. Tap a numeric value to enter an integer; invalid/out-of-range values are rejected. Screen-edge bounds remain enforced. Small dimensions can truncate labels or expose part of the camera; manual sizes are respected. Existing calibration and camera mode remain unchanged.

## 0.6.1 fixed physical position
The island is now pinned in natural handset coordinates, including its width/height. A transformed outer window maps this same rectangle into the app's rotated display coordinates; its content is counter-rotated so another app's orientation does not choose a new location. No global orientation lock is imposed. Camera x is captured once per natural display width and normalized if startup happens in landscape. Existing manual calibration remains unchanged. This supersedes 0.5.x landscape top-center behavior.

## 0.7.0 notification ingestion and lock screen
Use **Đồng bộ lại thông báo** to replay active notifications or request listener reconnection. Connection and last received app are shown in settings. Extraction covers big/plain text, line arrays, message bundles and custom notifications without text. Group summaries are skipped only when a matching child notification exists. Keep source app notifications enabled.
The app no longer hides its island merely because keyguard is locked. It requests lock-screen visibility and displays anonymous counts only; app launching, recent apps and notification content require keyguard authentication initiated by an explicit tap. Incoming notifications remain in the system while locked. A public, content-free foreground summary provides a fallback because OriginOS may still hide application overlays on keyguard. No full-screen activity is launched automatically, no AOD guarantee, no root/accessibility or keyguard bypass.

## 0.8.0 separate touch controls
A visible control strip is positioned beneath the status bar independently of the camera island. Left tap opens the latest notification, right tap opens recent apps, long press opens the inbox. Its gesture detector is persistent. It hides while the island panel is expanded, composing a reply or the screen is off. Under **Thanh điều khiển**, toggle it or adjust width (100–280dp), height (28–56dp) and status-bar gap (0–80dp). The strip follows the logical status bar on rotation; the camera island remains handset-fixed. This does not elevate any app window above SystemUI or bypass lock-screen restrictions.
