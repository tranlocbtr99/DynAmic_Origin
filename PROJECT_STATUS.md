# Current state — 2026-11-06
Version 0.1.0 / code 1; package vn.loctm.dynamicorigin. Initial independent implementation in previously empty repo.

## Implemented
- User-started foreground service and configurable camera overlay; no accessibility declaration or network permission.
- Compact persistent pill; expand by long press or new notification; tap newest entry; swipe-up collapse; 220ms resize.
- Cutout-aware alignment plus manual width/height/top sliders.
- Notification listener; grouped-summary suppression; upsert by source key; bounded durable local history.
- Source open/actions; session media previous/play-pause/next. Replies open source app.
- Optional source cancellation gated by durable save, new event, visible attached island and protected-item exclusion.
- Lock-screen privacy; explicit stop; boot/update recovery attempt and connection diagnostics.
- Stable release signing key backed up separately; private material ignored by git.
- Small CI workflow, cache, cancellation of obsolete jobs, 7-day debug artifacts; maintenance map.

## Validation
- Android SDK 36 Java compilation succeeded independently.
- 32 transfer gating combinations passed.
- Gradle assembleRelease, assembleDebug and lintDebug succeeded (Android SDK 36, JDK 21). One lint warning: gesture detector delegates click to performClick; lint does not follow this callback. No lint errors.
- No emulator/KVM or connected iQOO available: camera placement, input, source actions, OEM service survival and notification cancellation behavior remain device QA.

## Next implementation milestones
1. Device feedback: cutout/status-bar layering, gesture hit targets, battery restrictions, notification transfer.
2. Two-activity layout, per-app filters, richer download/navigation/media presentations, native quick reply.
3. Charging/earbuds integrations and stable release automation with private CI signing secrets.

## Limits
Cannot suppress every system banner before delivery, replace SystemUI, guarantee AOD or survive force-stop. Protected source notifications remain. Source actions can expire after cancellation. Device QA checklist in docs/DEVICE_QA.md.
