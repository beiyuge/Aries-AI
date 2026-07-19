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
- Flutter Chat, Automation, and Settings screens now have usable shared UI surfaces with split feature controllers, models, and widgets.
- Chat, Automation, and Settings state flows through shared Flutter application repositories with durable history and provider/runtime persistence.
- Cross-platform preference adapters now persist chat history, attachments, provider/runtime preferences, and the automation queue with versioned JSON and safe fallback for incompatible state.
- Chat attachment import now uses the host platform's native file selector behind a shared application contract, records MIME/size/source metadata, and enforces per-selection limits without platform checks in the UI.
- Chat now uses a real OpenAI-compatible streaming runtime. Provider endpoints/models are configurable, redirects are rejected, API keys live in platform secure storage, and native file paths are excluded from provider requests.
- Local model load/generate/unload is exposed through the same Pigeon schema. Settings can select and restore a model file, while Chat receives platform errors through shared typed events.
- Automation commands now flow through a shared planner/runtime/gateway into `AutomationHostApi`. Readiness, UI tree, screen capture, tap, swipe, text, and key operations dispatch to the registered native capabilities with typed success/failure results; Flutter never calls Android APIs directly.
- Android screen capture now has a real MediaProjection consent flow, foreground-service-backed session lifecycle, frame encoding, and explicit stop command. Screenshot and UI-tree results appear as typed task artifacts; image bytes remain in memory while durable state stores only safe metadata and text previews.
- Attachment content/multimodal adapters, a production local inference engine, speech UI, multi-step agent orchestration, richer Markdown, and real-device validation are still in progress.
- Non-Android platform backends are intentionally not implemented yet; their contracts must fit behind the same capability API.

## Verification

```bash
./tools/generate_pigeon.sh
flutter --directory flutter/aries_ui analyze
flutter --directory flutter/aries_ui test
./gradlew test :app-re0:assembleDebug -Pflutter.sdk=/path/to/flutter
```

See `docs/re0/ARCHITECTURE.md` and `docs/re0/DEFINITION_OF_DONE.md`.
