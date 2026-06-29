# re0 Architecture

## Intent

re0 rewrites Aries AI from a clean branch. The previous app's large Activity/Service/Object implementation is not extended. Old code can be inspected for behavior only; new implementation must use explicit contracts, diagnostics, tests, and isolated modules.

## Layers

```text
flutter/aries_ui
    UI, navigation, settings, chat, diagnostics
pigeons/capabilities.dart
    Typed Flutter <-> Android API surface
app-re0
    Android host app and plugin bridge registration
core/foundation
    Shared primitives
core/domain
    Cross-platform/domain model surface
core/capability-api
    Stable capability contracts and DTOs
core/capability-test
    Fakes and test helpers
platform/android/*
    One Android system capability per module
```

## Capability modules

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

1. Flutter does not call Android system APIs directly.
2. App/UI code does not call platform module internals directly; it goes through capability contracts.
3. Every capability exposes `CapabilityHealth`.
4. Every real plugin must have a fake and tests before full implementation.
5. No new code may import old `app/` classes; the old `app/` tree is absent in this branch.
