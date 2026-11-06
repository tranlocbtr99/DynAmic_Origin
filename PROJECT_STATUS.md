# Current state — 2026-11-06
Version 0.5.0 / code 5; package vn.loctm.dynamicorigin. Initial independent implementation in previously empty repo.

## Implemented
- User-started foreground service and configurable camera overlay; no accessibility declaration or network permission.
- Persistent root-owned header gesture detector; notification redraws no longer recreate gesture state, with queued notification refresh while a finger is down.
- Safe interaction mode defaults ON: island sits immediately below status bar. Camera-area mode remains optional but system windows may intercept touch.
- DisplayManager listener plus live Display metrics/cutout handles rotation while another app is foreground; landscape uses top center.
- Source icon/title or live progress replaces the single-dot notification indicator.
- Compact persistent pill; expand by long press or new notification; physical left tap opens newest notification, right tap opens recent apps; swipe-up collapse; 220ms resize.
- Stable display-space camera anchor, independent from overlay-local insets. Fixed top, horizontal calibration/reset and size-only 280ms transitions; duplicate targets do not restart animations.
- iOS-inspired light/dark grouped settings, capsule navigation, custom switches, glass control rims, black expanded island and updated reply/app-filter screens. See docs/DESIGN_SPEC.md.
- Notification listener; grouped-summary suppression; upsert by source key; bounded durable local history.
- Source open/actions; session media previous/play-pause/next. Quick reply uses a private input Activity and the source RemoteInput; only sends after explicit user submission.
- Optional source cancellation gated by durable save, new event, visible attached island and protected-item exclusion.
- Per-app filtering with search, direct source notification settings, adjustable expansion duration.
- Compact notification summary rows (8 initially, expand one for actions, load more, preserved scroll).
- Optional Usage Access for recent foreground apps (24h, up to 12 unique launchable apps, excludes home/self/system); queried on demand off main thread, no persisted usage history.
- Live activity labels around camera, progress bars from source extras; progress/media updates no longer repeatedly expand the inbox.
- Lock-screen privacy; explicit stop; boot/update recovery attempt and connection diagnostics.
- Stable release signing key backed up separately; private material ignored by git.
- Small CI workflow, cache, cancellation of obsolete jobs, 7-day debug artifacts; maintenance map.

## Validation
- Android SDK 36 Java compilation succeeded independently.
- 32 transfer gating combinations, 6 expansion/update scenarios 15,000 geometry frames and recent-app ordering/deduplication plus physical tap-boundary tests passed.
- Gradle assembleRelease, assembleDebug and lintDebug succeeded (Android SDK 36, JDK 21). 0 lint errors, 12 warnings (UI localization, gesture click delegation and intentional physical LEFT screen coordinates). Same release certificate verified against 0.1.0.
- No connected iQOO or KVM available. Software emulator boot was attempted; UI runtime validation is pending until Android finishes initialization. OEM camera placement and service survival remain device QA.

## Next implementation milestones
1. Device feedback: cutout/status-bar layering, gesture hit targets, battery restrictions, notification transfer.
2. Separate secondary activity bubble, richer navigation/media presentations and reply delivery feedback from sources.
3. Charging/earbuds integrations and stable release automation with private CI signing secrets.

## Limits
Cannot suppress every system banner before delivery, replace SystemUI, guarantee AOD or survive force-stop. Protected source notifications remain. Source actions can expire after cancellation. Device QA checklist in docs/DEVICE_QA.md.
