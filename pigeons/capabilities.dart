import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'flutter/aries_ui/lib/src/generated/capabilities.g.dart',
    dartPackageName: 'aries_ui',
    kotlinOut:
        'app-re0/src/main/java/com/ai/phoneagent/re0/generated/Capabilities.g.kt',
    kotlinOptions: KotlinOptions(package: 'com.ai.phoneagent.re0.generated'),
  ),
)
class CapabilityHealthDto {
  CapabilityHealthDto({
    required this.id,
    required this.available,
    required this.supported,
    required this.state,
    required this.missingRequirements,
    required this.diagnostics,
    this.lastErrorCode,
    this.lastErrorMessage,
  });

  String id;
  bool available;
  bool supported;
  String state;
  List<String> missingRequirements;
  List<String> diagnostics;
  String? lastErrorCode;
  String? lastErrorMessage;
}

class AutomationResultDto {
  AutomationResultDto({
    required this.success,
    required this.summary,
    required this.recoverable,
    this.text,
    this.bytes,
    this.mimeType,
    this.errorCode,
    this.errorMessage,
  });

  bool success;
  String summary;
  bool recoverable;
  String? text;
  Uint8List? bytes;
  String? mimeType;
  String? errorCode;
  String? errorMessage;
}

@HostApi()
abstract class CapabilityHostApi {
  List<String> listCapabilities();
  CapabilityHealthDto getCapabilityHealth(String id);
  String runCapabilitySelfTest(String id);
  void openCapabilitySettings(String id);
}

@HostApi()
abstract class LocalModelHostApi {
  @async
  void loadLocalModel(String modelId, String path);

  @async
  String generateLocalModel(String modelId, String prompt);

  @async
  void unloadLocalModel(String modelId);
}

@HostApi()
abstract class AutomationHostApi {
  @async
  AutomationResultDto checkReadiness();

  @async
  AutomationResultDto dumpUiTree(String detail);

  @async
  AutomationResultDto captureScreen();

  @async
  AutomationResultDto tap(int x, int y);

  @async
  AutomationResultDto swipe(
    int fromX,
    int fromY,
    int toX,
    int toY,
    int durationMs,
  );

  @async
  AutomationResultDto typeText(String text);

  @async
  AutomationResultDto pressKey(int keyCode);
}
