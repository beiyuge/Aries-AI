# Legacy Audit

The re0 branch intentionally removes legacy source from the working tree.

Legacy reference sources live only in Git history / previous branches. They can be read to understand expected behavior, but code must not be copied blindly into re0.

## Known legacy systems to reference, not extend

| Legacy area | New destination |
| --- | --- |
| `MainActivity.kt` giant UI/activity logic | Flutter `flutter/aries_ui` + `app-re0` host |
| `FloatingChatService.kt` | `platform/android/floating` |
| `PhoneAgentAccessibilityService.kt` | `platform/android/accessibility` |
| `VirtualDisplayController.kt` and `vdiso/*` | `platform/android/virtualdisplay` |
| `ShizukuBridge.kt` | `platform/android/shizuku` |
| Screenshot/cache/input helpers | `platform/android/screen` + `platform/android/input` |
| MNN/sherpa wrappers | `platform/android/native-runtime` |

## Migration policy

- Recreate behavior from contracts and tests.
- Add diagnostics before complex side effects.
- Prefer small backend-specific implementations behind a stable capability interface.
- Delete legacy references from docs once a new module has validated parity.
