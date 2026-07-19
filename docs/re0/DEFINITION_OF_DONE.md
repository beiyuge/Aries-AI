# re0 Definition of Done

The rewrite is not complete until all items below are true.

## Current verified evidence

- Flutter analysis and 35 shared/unit/widget tests pass for persisted app state, native file selection, remote streaming, secure credential routing, local-model routing, and settings recovery.
- Android `LocalModelHostApi` load/generate/unload has a registry-backed host adapter and JVM tests.
- `app-re0` assembles with Flutter plugin registration for preferences, file selection, and secure storage.
- Emulator validation is useful during development but does not replace the real-device checklist below.

## Build and test

- `./gradlew test` passes under the checked-in toolchain configuration.
- `./gradlew :app-re0:assembleDebug` passes.
- `flutter test` and `flutter analyze` pass.
- Pigeon generated Dart/Kotlin files are reproducible from `pigeons/capabilities.dart` via `./tools/generate_pigeon.sh`.

## Architecture

- Main UI is Flutter and remains cross-platform.
- Platform system features are accessed only through Capability APIs.
- Android is only the first implemented backend; the app architecture must allow future platform backends without rewriting shared UI/domain code.
- Each platform capability backend has an isolated module.
- Each capability has health, errors, diagnostics, and self-test hooks.
- Each capability has a fake implementation for UI/domain tests.
- Diagnostics must read live native health through `CapabilityHostApi`; static fake capability lists do not satisfy the bridge milestone.

## Android-first capability parity

- Permissions: status + settings navigation.
- Shizuku: binder check, permission, typed shell execution, timeout, stderr/stdout.
- Accessibility: service host, event stream, node snapshots, gesture backend.
- Screen: screenshot, display metrics, UI tree normalization.
- Input: tap, swipe, type, key, backend selection.
- VirtualDisplay: start/stop/session, displayId, focus/IME policy, black-frame detection.
- Floating: overlay permission, service lifecycle, notification, content host.
- Background: durable task scheduling and diagnostics.
- Native runtime: model load/unload/generate and speech recognition wrappers.

The current file-backed local-model backend validates bridge behavior but is not a production inference engine. This item remains open until a real engine is integrated and validated with an actual model.

## Future platform readiness

- Shared Flutter UI must not contain Android-only assumptions.
- Capability IDs and DTOs must remain platform-neutral unless a feature is explicitly Android-only.
- New platform backends must be able to report unsupported capabilities without breaking shared Diagnostics.
- Documentation must distinguish product-level cross-platform behavior from Android backend behavior.

## Manual device validation

On at least one real Android device for the Android backend:

1. Install `app-re0`.
2. Open Diagnostics.
3. Resolve required permissions.
4. Verify Shizuku status.
5. Dump UI tree.
6. Capture screenshot.
7. Inject tap/type.
8. Start/stop virtual display.
9. Open/close floating window.
10. Run one local speech/model self-test when model files are available.
