# LTMori 1.3.7

Development baseline is the signed 1.3.7 patch built from APK 1.3.2. Version code is 50. The earlier standalone and bridge projects are available in Git history.

The patch updates the welcome/privacy and About panels and adds in-app GitHub Releases downloads with APK validation and Android installer handoff. The base manifest and two UI hooks and shared layout dimensions are modified, and the new helpers live in a separate DEX. Island and listener implementations were preserved in the disassembly comparison.

Validated APK archive integrity, version, manifest, signature match, version comparison and APK asset selection. End-to-end installation from GitHub and UI/Island behavior on iQOO require device verification.
1.3.7 reduces the idle indicator diameter from 16 to 10 bitmap pixels and enables APK signing schemes v1, v2 and v3 with the existing key.
