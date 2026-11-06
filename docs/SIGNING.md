# Signing identity
Application ID: vn.loctm.dynamicorigin
Certificate SHA-256: b4a978c13be9338b511aad1829ebbd92fc632ce1ff587662bc6abdfa968b828b
Private keystore and properties are backed up separately as DynAmic-Origin-signing-backup.zip, never in git.
For an update, recover that keystore, set an absolute storeFile path in ignored keystore.properties, increase versionCode, then assembleRelease. Debug CI APKs have a different signing identity and cannot update the release install.
