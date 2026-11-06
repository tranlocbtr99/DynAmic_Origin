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
