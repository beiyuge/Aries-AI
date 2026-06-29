# Aries AI re0

`re0` is a clean-room rewrite branch for Aries AI as a cross-platform automation app.

The old Android implementation has been removed from this branch and may only be used as reference from Git history or another branch. New product code must enter through the re0 architecture:

```text
Flutter app
  -> Pigeon typed bridge
  -> Kotlin Capability API
  -> Platform Capability Backends
       -> Android backend first
       -> desktop/mobile backends later
```

## Current status

- Clean Gradle workspace with `app-re0` as the first Android host for the cross-platform Flutter app.
- Kotlin `core:capability-api` defines platform-neutral capability contracts; `platform:android:capability-runtime` is the first backend runtime.
- Android platform plugin modules are present as clean-room backend modules and are kept split by capability.
- Flutter UI shell is wired into the Android host through Flutter add-to-app.
- Pigeon generated Dart/Kotlin files are present and `CapabilityHostApi` is registered in the Android host.
- Diagnostics reads native capability health from `AndroidCapabilityRegistry`.
- Android Diagnostics now reads live health for permissions, Shizuku shell, accessibility, screen capture, UI tree, input injection, virtual display, floating window, background tasks, native runtime, speech recognition, and local model wrapper capabilities.
- Chat, Automation, Settings, History, attachments, Markdown parity, and full real-device validation are still in progress.
- Non-Android platform backends are intentionally not implemented yet; their contracts must fit behind the same capability API.

## Verification

```bash
./tools/generate_pigeon.sh
flutter --directory flutter/aries_ui analyze
flutter --directory flutter/aries_ui test
./gradlew test :app-re0:assembleDebug -Pflutter.sdk=/path/to/flutter
```

See `docs/re0/ARCHITECTURE.md` and `docs/re0/DEFINITION_OF_DONE.md`.
