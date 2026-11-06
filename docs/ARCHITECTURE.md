# Decisions
- Public WindowManager overlay is the primary renderer; SuperX is not a dependency.
- Foreground service specialUse anchors the user-enabled overlay, with required notification.
- Listener receives already-posted notifications; it is not a pre-delivery firewall.
- Durable bounded local inbox (80 entries, title <=300 chars/body <=4000) before cancellation. No network permission and backup disabled.
- Transfer is opt-in; protect flags/category/session/system packages. Preserve original notifications for live activities.
- History retains text only across process death; PendingIntents/actions/tokens remain in memory only.
- UI hides when keyguard locked. No accessibility, screenshot scraping, usage access or permission escalation.
- Boot/update recovery best effort; Android/OEM may block start. Store diagnostic and require visible user launch when necessary.
- Stable release signing identity is separate from disposable debug builds. CI never uploads keys.

## Primary references consulted 2026-11-06
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/security/fraud-prevention/activities
- https://developer.apple.com/design/human-interface-guidelines/live-activities
The third-party Origin Isle implementation was studied for feasibility; no source copied.
