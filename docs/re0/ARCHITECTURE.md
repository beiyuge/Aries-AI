# re0 Architecture

## Intent

re0 rewrites Aries AI from a clean branch as a cross-platform automation app. The previous Android app's large Activity/Service/Object implementation is not extended. Old code can be inspected for behavior only; new implementation must use explicit contracts, diagnostics, tests, and isolated modules.

Android is the first platform backend because it contains the current automation target. The architecture must not assume Android is the whole product: Flutter owns the shared app surface, `core/*` owns platform-neutral capability contracts, and each platform implements its own backend modules behind those contracts.

## Layers

```text
flutter/aries_ui
    Cross-platform UI, navigation, settings, chat, diagnostics
flutter/aries_ui/lib/src/application
    Shared application state and repository interfaces
flutter/aries_ui/lib/src/infrastructure
    Cross-platform repository adapters, codecs, and local persistence
pigeons/capabilities.dart
    Typed Flutter <-> host API surface
app-re0
    First Android host app and Android backend bridge registration
core/foundation
    Shared primitives
core/domain
    Cross-platform/domain model surface
core/capability-api
    Stable capability contracts and DTOs
core/capability-test
    Fakes and test helpers
platform/android/*
    First platform backend: one Android automation capability per module
platform/<future>/*
    Future desktop/mobile automation backends behind the same capability contracts
```

## Android capability modules

| Module | Responsibility |
| --- | --- |
| `platform/android/permissions` | Permission discovery, status, settings intents |
| `platform/android/shizuku` | Shizuku binder state, permission, shell execution |
| `platform/android/accessibility` | Accessibility service host, events, gestures, node snapshots |
| `platform/android/screen` | Screenshots, display metrics, UI tree normalization |
| `platform/android/input` | Tap, swipe, text, key injection and backend routing |
| `platform/android/virtualdisplay` | Virtual display sessions, focus policy, capture/preview |
| `platform/android/floating` | Overlay windows, foreground service, content host |
| `platform/android/background` | WorkManager and durable background tasks |
| `platform/android/native-runtime` | Local model, speech, native diagnostics |

## Flutter host integration

`flutter/aries_ui` is a Flutter module. `flutter pub get` generates its ignored `.android` metadata, the root `settings.gradle` loads the generated `include_flutter.groovy`, and `app-re0` depends on `project(':flutter')`. This keeps plugin discovery and `GeneratedPluginRegistrant` on Flutter's standard add-to-app path.

The Android federated implementation of `shared_preferences` is pinned to `2.4.21` until the Kotlin DSL extension accessor introduced in `2.4.22` works in this add-to-app build. The shared Dart interface remains on `shared_preferences 2.5.x`.

Chat attachment selection uses Flutter's federated `file_selector` API behind `ChatAttachmentPicker`; feature UI and controllers only receive platform-neutral metadata. `file_selector_android` is pinned to `0.5.2+4`, the last Groovy build before the same unresolved Kotlin DSL Flutter accessor appeared in `0.5.2+5`.

## Rules

1. Flutter does not call platform system APIs directly.
2. App/UI code does not call platform module internals directly; it goes through capability contracts.
3. Android-specific APIs stay under `app-re0` or `platform/android/*`.
4. New non-Android automation support must live under its own platform backend and reuse the capability API.
5. Every capability exposes `CapabilityHealth`.
6. Every real plugin must have a fake and tests before full implementation.
7. No new code may import old `app/` classes; the old `app/` tree is absent in this branch.
8. Shared application state is injected at the app composition root; feature screens must not create private persistence adapters.
9. Persisted state uses explicit schema versions and must recover safely when data is malformed or from an unsupported future schema.
10. Cross-platform device services such as file selection are injected behind application contracts; feature controllers do not import Flutter plugin packages.
