# Current state — 2026-11-06
Version 0.2.0 / code 2; package vn.loctm.dynamicorigin. Initial independent implementation in previously empty repo.

## Implemented
- User-started foreground service and configurable camera overlay; no accessibility declaration or network permission.
- Compact persistent pill; expand by long press or new notification; tap newest entry; swipe-up collapse; 220ms resize.
- Cutout-aware alignment plus manual width/height/top sliders.
- Notification listener; grouped-summary suppression; upsert by source key; bounded durable local history.
- Source open/actions; session media previous/play-pause/next. Quick reply uses a private input Activity and the source RemoteInput; only sends after explicit user submission.
- Optional source cancellation gated by durable save, new event, visible attached island and protected-item exclusion.
- Per-app filtering with search, direct source notification settings, adjustable expansion duration.
- Two active activity labels around camera, progress bars from source extras; progress/media updates no longer repeatedly expand the inbox.
- Lock-screen privacy; explicit stop; boot/update recovery attempt and connection diagnostics.
- Stable release signing key backed up separately; private material ignored by git.
- Small CI workflow, cache, cancellation of obsolete jobs, 7-day debug artifacts; maintenance map.

## Validation
- Android SDK 36 Java compilation succeeded independently.
- 32 transfer gating combinations and 6 expansion/update scenarios passed.
- Gradle assembleRelease, assembleDebug and lintDebug succeeded (Android SDK 36, JDK 21). 0 lint errors, 5 warnings (gesture click delegation and Vietnamese UI localization). Same release certificate verified against 0.1.0.
- No emulator/KVM or connected iQOO available: camera placement, input, source actions, OEM service survival and notification cancellation behavior remain device QA.

## Next implementation milestones
1. Device feedback: cutout/status-bar layering, gesture hit targets, battery restrictions, notification transfer.
2. Separate secondary activity bubble, richer navigation/media presentations and reply delivery feedback from sources.
3. Charging/earbuds integrations and stable release automation with private CI signing secrets.

## Limits
Cannot suppress every system banner before delivery, replace SystemUI, guarantee AOD or survive force-stop. Protected source notifications remain. Source actions can expire after cancellation. Device QA checklist in docs/DEVICE_QA.md.
