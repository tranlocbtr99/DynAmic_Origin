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
