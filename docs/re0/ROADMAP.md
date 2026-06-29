# re0 Roadmap

## Milestone A: Cross-platform shell

- Gradle clean-room workspace.
- `app-re0` builds as the first Android host.
- Platform-neutral Capability API TDD tests pass.
- Flutter cross-platform app shell exists.

## Milestone B: Android typed bridge

- Pigeon code generation installed.
- `CapabilityHostApi` bridged to `AndroidCapabilityRegistry`.
- Diagnostics screen reads real native health.

## Milestone C: Base Android capabilities

- permissions
- Shizuku shell
- screen capture / UI tree
- input injection

## Milestone D: Complex Android capabilities

- Accessibility service
- VirtualDisplay
- Floating window
- Background tasks

## Milestone E: Native runtime and UI parity

- speech recognition
- local model wrapper
- chat UI parity
- settings/provider parity
- history/attachments/Markdown parity

## Milestone F: Additional platform backends

- Identify desktop/mobile automation surfaces outside Android.
- Add backend modules under `platform/<name>/*`.
- Keep Flutter UI and domain logic shared.
- Keep platform-specific permission/settings/debug flows behind capability contracts.
