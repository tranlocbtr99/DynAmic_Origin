# LTM Origin Bridge — cross-platform island research (2026-11-07)

## Scope
The target renderer remains **vivo OriginOS 6 SystemUI** on iQOO Neo 9, with no overlay or root. Other Android island systems are architecture references, not wire-compatible adapters.

## Sources reviewed
1. [OriginOS Toolkit protocol](https://github.com/cameleonnbss/originos-toolkit/blob/main/docs/ORIGINISLAND.md): complete nested SuperX bundles, scene TRAIN, graceful unmount.
2. [Origin Isle protocol constants](https://github.com/fvhde/origin-isle/blob/main/app/src/main/java/com/originisle/android/island/OriginIslandConstants.kt): much larger vocabulary including forceShow, priority, permanent, islandNotify, clickResp, showTime, custom template, keepDuration, and lockscreen-related AOD adapter names. **Field presence does not establish permission or functionality.** Reference only; do not copy restricted project source.
3. [Origin Isle builder](https://github.com/fvhde/origin-isle/blob/main/app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt): hidden scene-registration method and per-scene permission investigation. Scene registration can fail due to hidden API, OEM checks or unsupported firmware.
4. [Origin Ghost Player](https://github.com/fvhde/origin-ghost-player): independent investigation of SuperX scene grants and native media.
5. [Android official notifications](https://developer.android.com/develop/ui/views/notifications): Android notification lifecycle and permissions.
6. [Android notification listener](https://developer.android.com/reference/android/service/notification/NotificationListenerService): event intake with user grant.
7. [Android MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager): playback state and control.
8. [Android Live Updates](https://developer.android.com/develop/ui/views/notifications/live-update): platform update patterns; NOT a Vivo SuperX protocol.
9. [Shizuku](https://github.com/RikkaApps/Shizuku): optional privilege bridge, not equivalent to root or privileged SystemUI access.

## Workstreams and verification gates
- P1 native handshake: show/update/end a single SuperX notification, log registration outcome, verify visually on actual firmware.
- P2 event engine: source permission, filtering, deduplication, priorities, no personal data logging, expiry.
- P3 persistent idle: investigate documented SuperX permanent/forceShow flags **only after P1**; no periodic fake notification spam.
- P4 lockscreen, rotation and interaction: verify separately. Rendering position and gesture handling belong to SystemUI.
- P5 optional Shizuku: use only when a concrete permission gap is identified and device policy permits.
- P6 energy, crash and lifecycle tests.

## Test design
Keep each protocol variable isolated (scene, template, flags) and record (a) API call result, (b) scene-registration result, (c) native island visible yes/no, (d) shade visible yes/no, (e) update/unmount behavior. Never treat notification posting as proof of island rendering. Avoid bulk source-notification forwarding until native handshake succeeds.

## Copyright / licenses
Use public protocol facts as reference; do not transplant Origin Isle source code without an applicable license or permission.
