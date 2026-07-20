# re0 Roadmap

## Milestone A: Cross-platform shell

- Gradle clean-room workspace.
- `app-re0` builds as the first Android host.
- Platform-neutral Capability API TDD tests pass.
- Flutter cross-platform app shell exists.

## Milestone B: Android typed bridge

- Pigeon code generation installed.
- `CapabilityHostApi` bridged to `AndroidCapabilityRegistry`.
- `AutomationHostApi` bridged to UI tree, screen capture, and input capabilities.
- Diagnostics screen reads real native health.

## Milestone C: Base Android capabilities

- permissions
- Shizuku shell
- screen capture / UI tree
- input injection

The first shared Automation command vocabulary now executes these registered capabilities and records native results. Android MediaProjection consent, foreground session lifecycle, PNG/JPEG capture, explicit stop, and screenshot/UI-tree artifact presentation are complete. Multi-step planning, retries, dedicated artifact storage, and agent-driven orchestration remain in progress.

## Milestone D: Complex Android capabilities

- Accessibility service
- VirtualDisplay: typed start/launch/capture/stop lifecycle, frame ownership, and black-frame guard complete for the public `DisplayManager` backend; trusted cross-UID launch and display-specific input remain open
- Floating window
- Background tasks

## Milestone E: Native runtime and UI parity

- speech recognition
- local model wrapper: typed host bridge and model-file lifecycle complete; production inference engine pending
- chat UI parity: real remote/local runtime routing complete; richer rendering and interaction parity pending
- settings/provider parity: editable endpoints and secure API keys complete; broader provider options pending
- history/attachments/Markdown parity: durable history and native selection complete; attachment content and richer Markdown pending

## Milestone F: Additional platform backends

- Identify desktop/mobile automation surfaces outside Android.
- Add backend modules under `platform/<name>/*`.
- Keep Flutter UI and domain logic shared.
- Keep platform-specific permission/settings/debug flows behind capability contracts.
