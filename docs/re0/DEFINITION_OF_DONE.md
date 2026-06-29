# re0 Definition of Done

The rewrite is not complete until all items below are true.

## Build and test

- `./gradlew test` passes under the checked-in toolchain configuration.
- `./gradlew :app-re0:assembleDebug` passes.
- `flutter test` and `flutter analyze` pass.
- Pigeon generated Dart/Kotlin files are reproducible from `pigeons/capabilities.dart` via `./tools/generate_pigeon.sh`.

## Architecture

- Main UI is Flutter, not legacy Android UI.
- Android system features are accessed only through Capability APIs.
- Each platform capability has an isolated module.
- Each capability has health, errors, diagnostics, and self-test hooks.
- Each capability has a fake implementation for UI/domain tests.
- Diagnostics must read live native health through `CapabilityHostApi`; static fake capability lists do not satisfy the bridge milestone.

## Android capability parity

- Permissions: status + settings navigation.
- Shizuku: binder check, permission, typed shell execution, timeout, stderr/stdout.
- Accessibility: service host, event stream, node snapshots, gesture backend.
- Screen: screenshot, display metrics, UI tree normalization.
- Input: tap, swipe, type, key, backend selection.
- VirtualDisplay: start/stop/session, displayId, focus/IME policy, black-frame detection.
- Floating: overlay permission, service lifecycle, notification, content host.
- Background: durable task scheduling and diagnostics.
- Native runtime: model load/unload/generate and speech recognition wrappers.

## Manual device validation

On at least one real Android device:

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
