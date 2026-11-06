# iOS-inspired design and stable camera anchor — 0.3.0

## Cause and correction
0.2.0 read cutout bounding rectangles from the island view's WindowInsets, then treated their window-local x/bottom as display coordinates. Resizing and repositioning the overlay changes those local rectangles, feeding changes back into its position. This code path could oscillate.
0.3.0 captures maximum display metrics/cutout once per display geometry. It never measures the camera from the overlay. A screen-space anchor is retained until width, height, density or rotation changes. Window placement uses TOP|LEFT with fit-insets disabled. Every resize frame computes left = center - width/2 from the same anchor and fixed top, rather than accumulating offsets. Manual x/y changes are explicit settings. Duplicate target-size updates do not restart an in-flight size animation.

## Materials and hierarchy
The camera island stays opaque black. Content is separated using dark grouped cards, readable text and source icons; controls use specular rims, rounded capsules and subtle pressed transitions. App settings use grouped content, a floating capsule navigation bar, iOS-proportioned switches and light/dark palettes. Liquid Glass is concentrated in control surfaces, not applied indiscriminately to every content card.
The Android framework cannot render Apple's proprietary Liquid Glass refraction or San Francisco system integration identically. Glass control drawables approximate tint/specular cues. Reply and confirmation windows request native cross-window background blur where Android enables it, with a more opaque fallback. No screen capture is used. Font is Android sans-serif, with adjusted weight/spacing; no Apple font is bundled.

## Motion
Size transition: 280ms cubic curve, no positional spring/overshoot. The camera center and top remain fixed throughout. Android animation-disable settings are honored for island resize and switches. Empty/default island remains quiet without an infinite animation loop.

## References consulted 2026-11-06
- https://developer.apple.com/design/human-interface-guidelines/materials
- https://developer.apple.com/design/human-interface-guidelines/live-activities
- https://developer.apple.com/documentation/TechnologyOverviews/adopting-liquid-glass
- https://developer.android.com/develop/ui/views/layout/display-cutout
- https://developer.android.com/reference/android/view/WindowMetrics
- https://source.android.com/docs/core/display/window-blurs

## Regression evidence
Pure geometry test checks 15,000 frames across five screen widths and three explicit horizontal offsets, preserving center within 0.5px and retaining on-screen bounds. Device/emulator evidence is recorded in PROJECT_STATUS.md; these mathematical checks alone do not prove OEM compositor behavior.

## 0.5.0 corrections from device feedback
A screen-space anchor alone did not solve touch routing: Android's status bar is a higher window than TYPE_APPLICATION_OVERLAY. Safe interaction mode deliberately places the actual island beneath it. Exact camera placement remains available as an explicit less reliable option. A persistent IslandTouchLayout intercepts the header only; child label redraws cannot reset GestureDetector's long-press timer. Card controls below the header retain their own touch routing.
Rotation now uses DisplayManager.DisplayListener and the current Display's real metrics/cutout. Configuration callbacks remain supplementary. It never feeds overlay-local cutout insets into placement. Landscape uses top center and a horizontal layout.

## 0.6.0 explicit sizing
User feedback revealed a compact width floor overrode the percentage slider. IslandSizing now computes requested display percentage capped only by available screen space. No content/state-driven width floor remains. Height is explicitly user-controlled as well; camera-covering automatic height growth is removed. Geometry and material controls offer validated numeric entry as well as sliders.
