# Aries AI re0

`re0` is a clean-room rewrite branch for Aries AI.

The old Android implementation has been removed from this branch and may only be used as reference from Git history or another branch. New code must enter through the re0 architecture:

```text
Flutter UI
  -> Pigeon typed bridge
  -> Kotlin Capability API
  -> Android Capability Plugins
```

## Current status

- Clean Gradle workspace with `app-re0` host app.
- Kotlin `core:capability-api` and `platform:android:capability-runtime` implemented with TDD tests.
- Android platform plugin modules are present as empty clean-room modules.
- Flutter UI shell is wired into `app-re0` through Flutter add-to-app.
- Pigeon generated Dart/Kotlin files are present and `CapabilityHostApi` is registered in the Android host.
- Diagnostics reads native capability health from `AndroidCapabilityRegistry`.
- Permissions capability reports the first permission requirement catalog; the rest of the system capabilities are explicit `Unavailable` placeholders until their plugins are implemented.

## Verification

```bash
./tools/generate_pigeon.sh
flutter --directory flutter/aries_ui analyze
flutter --directory flutter/aries_ui test
./gradlew test :app-re0:assembleDebug -Pflutter.sdk=/path/to/flutter
```

See `docs/re0/ARCHITECTURE.md` and `docs/re0/DEFINITION_OF_DONE.md`.
