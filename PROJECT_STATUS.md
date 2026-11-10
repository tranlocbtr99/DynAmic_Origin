# LTMori 3.0.0-beta

Development baseline is the signed 3.0.0-beta patch built from APK 2.0.3. Version code is 60. The earlier standalone, bridge and 2.0.x projects are available in Git history.

The patch updates the welcome/privacy, About panels, adds in-app GitHub Releases downloads with APK validation, Android installer handoff, Island state detection and beta channel support. The base manifest and two UI hooks and shared layout dimensions are modified, and the new helpers live in a separate DEX. Island and listener implementations were preserved in the disassembly comparison.

Validated APK archive integrity, version, manifest, signature match, version comparison and APK asset selection. End-to-end installation from GitHub and UI/Island behavior on iQOO require device verification.
1.3.7 reduces the idle indicator diameter from 16 to 10 bitmap pixels and enables APK signing schemes v1, v2 and v3 with the existing key.
2.0.1 adds periodic release checks, release notes and confirmation for notification mode changes.
3.0.0-beta adds beta channel support, Island state detection (charging, music, headset, unlock), and research foundation for widget expansion.

The 2.0.3 base is retained intact: message expansion on new notifications (`ltm_expand_message` → `island.superx.forceShowCard`), notification mode confirmation, compact notification layout, and the IdleDot template (`oi_template=4`, "Đang chờ"). Rebuild verification showed `classes2.dex` is identical to the 2.0.3 source; only version metadata and `classes3.dex` helpers differ.
