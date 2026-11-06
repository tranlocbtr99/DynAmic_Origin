# DynAmic Origin
Android camera island for iQOO and other Android 14+ devices. Independent implementation using Android public APIs, with no accessibility permission, root, package spoof or network permission.

## First setup
1. Install the signed APK; grant display-over-other-apps and notification listener access.
2. Allow runtime notifications, then press **Bật đảo** while the app is visible.
3. Tune width, height and top offset to surround the camera. The overlay is below critical system windows; some firmware may prevent camera-area touch or display.
4. On OriginOS enable unrestricted battery, auto-start and associated startup as supported by the installed firmware.
5. Long press the island for the inbox, tap for the newest item, swipe up to collapse.

Transfer of ordinary notifications is optional. Capture is persisted before requesting cancellation. Ongoing, foreground service, call, alarm, media and core system notifications are retained. This cannot prevent an initial system banner. Keep source notification permissions enabled; turn off source channel banners manually if desired. Source PendingIntents may expire after cancellation: opening the app is the fallback, quick reply is available when the source exposes a mutable free-form RemoteInput action. Reply tokens may expire after transfer; failures are shown without claiming delivery.

## Implemented
Persistent configurable overlay; compact/expanded inbox; bounded on-device history; source actions and quick reply; per-app filters; progress bars; two compact activity labels; media transport buttons; screen-lock privacy; foreground runtime; boot/update recovery attempt; diagnostics; opt-in ordinary notification transfer.

## Not yet implemented / not guaranteed
Separate two-bubble display, full media metadata cards, advanced navigation parsing, charging/earbuds cards, iPhone system integrations, AOD, universal heads-up suppression or full SystemUI replacement. Lock-screen content is intentionally hidden. No claim of device validation until real testing.

## Build
JDK 17+, Android SDK platform 36; `./gradlew :app:assembleDebug :app:lintDebug`.
For signed builds create gitignored `keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`, then `./gradlew :app:assembleRelease`. Absolute storeFile paths are supported. Do not lose the signing key.

CI runs once per push/PR, cancels superseded jobs, caches Gradle and uploads a debug APK. Debug signing is for development; install the release APK for a stable update identity.
