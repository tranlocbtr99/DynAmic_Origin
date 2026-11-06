# DynAmic Origin maintenance
Read PROJECT_STATUS.md first, then only relevant source files and git diff.
App is standalone Android Java, framework APIs only. No AccessibilityService, root, AMap package spoof, copied Origin Isle source, analytics, cloud or AI APIs.
Keep notification ingestion, persistent inbox, overlay and recovery separate.
Never cancel protected/ongoing/media/call/alarm notifications. Transfer regular notifications only after durable save and an attached visible overlay; keep this opt-in.
Do not claim a notification listener intercepts before system delivery. Do not disable source app notification permission.
Preserve applicationId and signing key for upgrades. Never commit keystores, local.properties or passwords.
Validate changes with ./gradlew :app:assembleDebug :app:lintDebug. Real-device QA checklist is docs/DEVICE_QA.md. Never report hardware results without actual device evidence.
For each change update PROJECT_STATUS.md with behavior, verification and unresolved limitations. Avoid full-repo dumps, repeated builds and broad unrelated refactors.

Position regressions: read IslandGeometry.java and docs/DESIGN_SPEC.md before IslandService. Never feed overlay-local WindowInsets cutout rectangles back into screen coordinates.
UI changes: IOSStyle.java and IOSToggle.java own shared materials/controls. Keep the camera island opaque black; document Android rendering differences rather than promise an exact iOS clone.

Touch regressions: IslandTouchLayout owns header gestures for the lifetime of the overlay. Never re-create its detector during notification render. Safe interaction mode must stay below protected status-bar windows; do not promise touch interception at camera on all OEM firmware. Rotation uses DisplayManager events and current Display metrics, not only Service.onConfigurationChanged.

User requires camera placement. Never enable below-status-bar positioning automatically as a touch workaround. That mode is explicit opt-in only. Preserve the one-time 0.5.1 migration that reverses 0.5.0's default.

Calibration: do not reintroduce notification-dependent minimum widths (136/208dp) or automatic height growth that overrides the user's numeric dimensions. Respect explicit compact sizes; adapt/truncate content instead.

Physical position is locked across app rotation. PinnedFrame maps a natural-orientation rectangle into the display; do not restore the former landscape top-center fallback or calculate widths from rotated display width.

Lock-screen mode is anonymous/count-only; require KeyguardManager authentication before opening source intents, recent apps or content. Never cancel source notifications while locked. Never auto-launch an Activity to cover keyguard. Ingestion diagnostics must distinguish permission/binding failures from empty source content; do not claim OEM delivery without device evidence.

The separate control strip is intentionally in logical display coordinates below the status bar; do not merge its placement with the handset-fixed camera island. Keep it visible (not an invisible touch trap), non-modal, optional, and hidden while panels are expanded.
