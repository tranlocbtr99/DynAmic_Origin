# LTM Origin Bridge — OriginOS 6 protocol investigation

## Device finding
On iQOO Neo 9 V2338A / OriginOS 6, alpha2 generated only an Android shade notification. The two extras `notification.superx.operation` and `notification.superx.showNotify` were insufficient.

## Public protocol reference
- https://github.com/cameleonnbss/originos-toolkit/blob/main/docs/ORIGINISLAND.md
- https://github.com/cameleonnbss/originos-toolkit/blob/main/app/src/main/kotlin/dev/cameleonnbss/originostoolkit/core/OriginIslandTemplates.kt
- https://github.com/cameleonnbss/originos-toolkit/blob/main/app/src/main/kotlin/dev/cameleonnbss/originostoolkit/service/OriginIslandSender.kt

The implementation is independently authored based on documented field names and behavior. It does not import or copy Origin Isle source code.

## Phase 1 diagnostic v2
- `notification.superx.scene = TRAIN`
- Nested `baseInfos`, `capsule`, `infos`, `shortInfos`, `island`
- `island.superx.leftTemplate = 1`, `rightTemplate = 6`
- Shared resource icon across nested bundles
- Best-effort reflective call to `setSuperXInfosSceneList`; log `SCENE_REGISTERED` or `SCENE_UNAVAILABLE_...`
- Send and update use one notification ID
- Unmount writes `operation=2` before cancellation
- Diagnostic log stores event names and exception class only; no private notification text

## Acceptance
Build passing is not OEM success. Verify visually on device: native pill appears, updates in place, and disappears on cancel. If only shade notification appears, capture anonymized diagnostics and firmware information. No overlay, root or accessibility permissions are used.
