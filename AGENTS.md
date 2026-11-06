# DynAmic Origin maintenance
Read PROJECT_STATUS.md first, then only relevant source files and git diff.
App is standalone Android Java, framework APIs only. No AccessibilityService, root, AMap package spoof, copied Origin Isle source, analytics, cloud or AI APIs.
Keep notification ingestion, persistent inbox, overlay and recovery separate.
Never cancel protected/ongoing/media/call/alarm notifications. Transfer regular notifications only after durable save and an attached visible overlay; keep this opt-in.
Do not claim a notification listener intercepts before system delivery. Do not disable source app notification permission.
Preserve applicationId and signing key for upgrades. Never commit keystores, local.properties or passwords.
Validate changes with ./gradlew :app:assembleDebug :app:lintDebug. Real-device QA checklist is docs/DEVICE_QA.md. Never report hardware results without actual device evidence.
For each change update PROJECT_STATUS.md with behavior, verification and unresolved limitations. Avoid full-repo dumps, repeated builds and broad unrelated refactors.
