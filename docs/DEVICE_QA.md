# Device acceptance (not yet performed)
- Confirm no accessibility service declared and accessibility remains off.
- Grant overlays/listener; start from Activity; verify compact island at camera, calibrate offsets.
- Verify long press, source launch, action buttons, scroll, swipe collapse; status bar remains usable.
- Rotate and change display scaling; verify cutout, card clipping and keyboard behavior.
- Post regular and grouped messages; edit same notification; verify one stored entry per source key.
- Transfer OFF: source remains. Transfer ON: saved item survives cancellation; source actions may expire and app fallback must work.
- Overlay blocked/permission revoked/storage failure: never cancel new source notifications.
- Ongoing/call/alarm/media/FGS must not be transferred. Test call receive/end and downloads independently.
- Media controls only when source exposes a session; test pause/next and session end.
- Lock screen: no notification content visible. Unlock restores overlay. Screen off: no rendering loop.
- Reboot, package update, swipe away, battery saver, overnight idle; record actual recovery. Force stop requires user relaunch.
- Kill process; saved inbox survives but stale media/actions must not be restored as live objects.
- Install next release signed with same key and higher versionCode; settings persist.
- Check banking/protected screens hide overlay as expected; no attempt to bypass system restrictions.

## 0.2.0 checks
- Reply on a source free-form RemoteInput action; send only after tapping Send; test canceled token, source update while composing, lock/unlock, rotation and process death.
- Toggle an app off: no new capture/cancellation, stored entries hidden. Toggle back on: current source notifications replay without alert.
- Download updates change progress without repeatedly expanding; source stays alive.
- Two ongoing activities show distinct labels; tap secondary opens that source.
- Ordinary transfer cancellation no longer cancels the delayed collapse timer.

## 0.3.0 position and visual regression
- Idle for 2 minutes; camera center and top must not move.
- Expand/collapse 30 times, including while progress/media updates arrive; compare screen-coordinate center and top.
- Hide/show status bar through fullscreen app transitions; anchor must not follow local insets.
- Change keyboard, display density, rotation and light/dark mode; only display geometry changes should recapture camera bounds.
- Test manual horizontal offset and reset; test minimum/maximum widths.
- Check light/dark grouped settings, readable control rims, 44dp touch targets, reply keyboard and app-filter switches.
- Disable animations and verify size/switch transitions are immediate.

## 0.4.0
- Tap physical left/right on idle and expanded headers; no child label should steal right-half touches. Long press still opens notifications.
- Confirm left opens newest notification even if a different ongoing media card exists.
- Deny Usage Access: right tap shows grant guidance; left still works. Grant then return and tap right again.
- Open A, B, A: list order A then B, no duplicates; home/island excluded. Revoke access: no cached names shown.
- Expand one notification, scroll/load more, delete/reply; actions hidden in collapsed rows and scroll retained.
- A notification arriving while viewing recent apps is saved without interrupting that panel. After collapsing, subsequent notifications can open normally.

## 0.5.0 critical checks
- With another normal app foreground and safe mode ON: left/right taps and >500ms hold must work.
- Receive notification/progress updates while holding; long press must still open the inbox. Header touch must not steal taps from action rows.
- Compare safe mode with camera mode; camera mode may be intercepted by SystemUI and must not be reported as universally supported.
- Rotate in another app at 0/90/180/270 degrees; overlay must re-anchor based on current display geometry.
- Compact pill must show icon/title/count or media/download status, not just a single dot.
- Protected apps may hide overlays regardless of safe mode; no bypass should be attempted.

## 0.7.0
- Revoke/regrant listener access, sync, and compare connected state plus last received timestamp with a real message from another app.
- Test plain, big-text, MessagingStyle, line-array, custom and summary-only notifications; no callback crash for missing/null extras.
- Lock after private content is expanded: screen off hides island, screen on renders count-only before showing; no title/source/recent-app exposure.
- Transfer ON while locked must leave originals intact.
- Tap count/hold on keyguard; content and actions only after successful credential/biometric authentication. Cancel authentication and verify no private content.
- Compare overlay lock visibility on actual OriginOS; if hidden, public native summary remains subject to system lock-notification settings.
