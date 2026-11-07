# LTMori 1.1.2 — 2026-11-07

- Match the working sample card by using a local icon and app-owned PendingIntent for forwarded notifications. Forwarded text and percentage remain available; foreign action buttons are omitted in this compatibility path.
- The non-exported click activity opens the source notification while unlocked. A rejected card retries once with a plain sample-style card.
- Show the newest diagnostic entries first and distinguish rejection of a forwarded card from idle or sample cleanup. Device verification remains pending.

# LTMori 1.1.1 — 2026-11-07

- Complete the idle card handoff before forwarding a real notification; hold arriving updates by source key during the handoff.
- Show listener access and forwarding state. Log distinct local outcomes without source identity or text, and retry OEM-rejected cards once with a basic payload.
- Keep standalone group summaries and preserve source notifications. Reuse the LTMori signing key.
- Verify the handoff and app notification rendering on OriginOS after installing the update.

# LTMori 1.1.0 — 2026-11-07

- Native flavor keeps package com.autonavi.minimap and adopts the LTMori name.
- Opt-in idle card posts from the notification listener only while no forwarded card is active. It yields to incoming cards and returns after the last one ends. OriginOS must honor the persistent card flags on the handset.
- Native onboarding requires scrolling the privacy policy, confirming it was read and checking consent. Forwarding and idle are gated in the listener. Withdrawal clears cards and the diagnostic log. Private content is not requested on AOD or lockscreen. The native UI shows LocTM attribution, build version, voluntary support and policy controls.
- Settings motion uses the upstream preview as a reference for a dark capsule, a 700 ms pulse and short press transitions. The OS owns animation of actual SuperX cards.
- Release APKs are signed outside CI with the dedicated ltmori key. Retain the same key and package ID, and increment versionCode for every update. Keep signing credentials out of the repo. Replacing the older debug APK requires uninstalling it once.

# Native SuperX rebuild — 2026-11-07

- Native2 uses one shared sender for samples and forwarded notifications. Its independently implemented wire protocol registers the 15 observed scene names, posts a NAVIGATION base card on a new silent HIGH channel, uses SuperX click callbacks, and sends operation 2 before cancelling.
- Notifications have independent IDs and revision counters. The app displays text, source icon, progress when available and one eligible action. It skips system apps, group summaries and secret notifications, and supports exclusions. One-shot cards expire after eight seconds. Source notifications are preserved.
- Lockscreen is opt-in and forwards only app labels, with no action or private content on those cards. Foreground-only diagnostics remain a separate flavor.
- Physical OriginOS rendering, tap routing, AOD and lockscreen still require iQOO testing. Dedicated upstream templates for navigation, sports, OTP, payments and media, and the upstream background keep-alive mechanisms, are not included in this build.

# Native SuperX experiment — 2026-11-07

- The opt-in nativeSuperx variant uses the AMap application ID required by the tested Origin Isle approach. This variant conflicts with AMap and Origin Isle installations sharing that ID. The diagnostic variant retains its ID, and the existing DynAmic 0.8.2 APK can coexist.
- The notification listener sends one transient SuperX card with the source app label and original content intent while unlocked. It ignores its own events, group summaries and secret notifications, and clears its card when disabled or when its source is removed. It does not cancel source notifications. This uses the existing independent protocol implementation.
- Device verification still needs to cover scene acceptance, visibility next to the camera, tap routing, and cleanup. A posted notification or a reflective call returning without exception does not prove SystemUI rendered a card.
- Local Gradle validation was blocked by network access to the Gradle distribution. The Android checks workflow builds both variants.

# Current state — 2026-11-06
Version 0.8.2 / code 12; package vn.loctm.dynamicorigin. Initial independent implementation in previously empty repo.

## Implemented
- Removed the white grid glyph from the island’s right side at user request. Right-half recent-app gesture remains unchanged.
- User-requested transparent touch extension attached beneath the camera island; no second visible card, labels, background or shadow. Left opens latest notification, right recent apps, hold inbox. It hides while panels/reply/screen-off are active, with configurable extra width/height/gap and rotation-aware hit mapping. Camera island geometry remains independent.
- Explicit compact width (8–90%) with no 136/208dp or camera-padding floor; height 12–120dp, top 0–240dp, horizontal ±240dp; expanded width, radius, icon size and padding controls. Tap numeric values for validated direct input.
- User-started foreground service and configurable camera overlay; no accessibility declaration or network permission.
- Persistent root-owned header gesture detector; notification redraws no longer recreate gesture state, with queued notification refresh while a finger is down.
- Camera placement is the default again. One-time migration disables 0.5.0 automatic below-status-bar mode without changing user calibration. Below-status-bar placement is explicit opt-in only; system windows may still intercept camera-area touch.
- DisplayManager listener plus live Display metrics/cutout handles rotation while another app is foreground; a natural-orientation rectangle is now pinned to the handset through all four rotations. Landscape no longer selects a new top-center anchor.
- Source icon/title or live progress replaces the single-dot notification indicator.
- Compact persistent pill; expand by long press or new notification; physical left tap opens newest notification, right tap opens recent apps; swipe-up collapse; 220ms resize.
- Stable display-space camera anchor, independent from overlay-local insets. Fixed top, horizontal calibration/reset and size-only 280ms transitions; duplicate targets do not restart animations.
- iOS-inspired light/dark grouped settings, capsule navigation, custom switches, glass control rims, black expanded island and updated reply/app-filter screens. See docs/DESIGN_SPEC.md.
- Broader notification extraction: big text, plain text, lines, MessagingStyle bundles, generic fallback for custom notifications; standalone group summaries retained. Listener sync/rebind action, explicit all-type default filter and last-received diagnostics.
- Lock-screen island requested with anonymous count; content/recent apps/actions require keyguard authentication. Native public summary is fallback if OEM hides the overlay. No cancellation while locked and no automatic Activity launch.
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
- 64 transfer gating combinations (including locked state), 6 expansion/update scenarios 15,000 geometry frames and recent-app ordering/deduplication plus physical tap-boundary and explicit width regression tests and 48 four-rotation physical-rectangle round trips passed.
- Gradle assembleRelease, assembleDebug and lintDebug succeeded (Android SDK 36, JDK 21). 0 lint errors, 15 warnings (UI localization, gesture click delegation and intentional physical LEFT screen coordinates). Same release certificate verified against 0.1.0.
- No connected iQOO or KVM available. Software emulator boot was attempted; UI runtime validation is pending until Android finishes initialization. OEM camera placement and service survival remain device QA.

## Next implementation milestones
1. Device feedback: cutout/status-bar layering, gesture hit targets, battery restrictions, notification transfer.
2. Separate secondary activity bubble, richer navigation/media presentations and reply delivery feedback from sources.
3. Charging/earbuds integrations and stable release automation with private CI signing secrets.

## Limits
Cannot suppress every system banner before delivery, replace SystemUI, guarantee AOD or survive force-stop. Protected source notifications remain. Source actions can expire after cancellation. Device QA checklist in docs/DEVICE_QA.md.
