# LTM Origin Bridge — Phase 1 (experimental)

Target: iQOO Neo 9 V2338A, OriginOS 6. Native Origin Island only; no root.

## Preservation strategy
- Work only on `feature/ltm-origin-bridge-phase1`.
- Do not modify existing `IslandService`, `NotificationBridge`, overlay geometry, touch handlers, signing config or package ID.
- Do not switch off the existing overlay until native rendering is confirmed on a real device.
- The new `OriginIslandProbe` is currently an **unwired diagnostic helper**, not a working user-facing feature.

## Current implementation
`OriginIslandProbe.showProbe(context)` posts a normal notification with two experimental OEM extras (`notification.superx.operation`, `notification.superx.showNotify`). `clearProbe(context)` cancels it. This is intentionally a minimal smoke test. It does **not** claim that these extras alone trigger the native island. OEM capsule/island/template structures still need verification before adding them.

## Phase 1 next steps
1. Compare the complete private extras schema and license of publicly documented OriginOS implementations.
2. Add a settings-only explicit test/clear control, never an automatic background trigger.
3. Build debug APK and run static checks.
4. On device, grant POST_NOTIFICATIONS; invoke show and verify whether native Origin Island appears (not merely notification shade).
5. Check update/remove behavior, lock screen, portrait/landscape, and reboot. Record firmware build and screenshots.
6. If the OEM ignores payloads, mark as unsupported; do not simulate success with the overlay.

## Safety
Do not read or forward third-party notifications during Phase 1. Do not request Shizuku or privileged permissions without a demonstrated need. Do not publish a release or claim device validation before an actual install and test.
