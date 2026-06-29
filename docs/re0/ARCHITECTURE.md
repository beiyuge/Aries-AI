# re0 Architecture

## Intent

re0 rewrites Aries AI from a clean branch as a cross-platform automation app. The previous Android app's large Activity/Service/Object implementation is not extended. Old code can be inspected for behavior only; new implementation must use explicit contracts, diagnostics, tests, and isolated modules.

Android is the first platform backend because it contains the current automation target. The architecture must not assume Android is the whole product: Flutter owns the shared app surface, `core/*` owns platform-neutral capability contracts, and each platform implements its own backend modules behind those contracts.

## Layers

```text
flutter/aries_ui
    Cross-platform UI, navigation, settings, chat, diagnostics
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

## Rules

1. Flutter does not call platform system APIs directly.
2. App/UI code does not call platform module internals directly; it goes through capability contracts.
3. Android-specific APIs stay under `app-re0` or `platform/android/*`.
4. New non-Android automation support must live under its own platform backend and reuse the capability API.
5. Every capability exposes `CapabilityHealth`.
6. Every real plugin must have a fake and tests before full implementation.
7. No new code may import old `app/` classes; the old `app/` tree is absent in this branch.
