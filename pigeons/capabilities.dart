import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(PigeonOptions(
  dartOut: 'flutter/aries_ui/lib/src/generated/capabilities.g.dart',
  dartPackageName: 'aries_ui',
  kotlinOut: 'app-re0/src/main/java/com/ai/phoneagent/re0/generated/Capabilities.g.kt',
  kotlinOptions: KotlinOptions(package: 'com.ai.phoneagent.re0.generated'),
))
class CapabilityHealthDto {
  CapabilityHealthDto({
    required this.id,
    required this.available,
    required this.state,
    required this.missingRequirements,
    this.lastErrorCode,
    this.lastErrorMessage,
  });

  String id;
  bool available;
  String state;
  List<String> missingRequirements;
  String? lastErrorCode;
  String? lastErrorMessage;
}

@HostApi()
abstract class CapabilityHostApi {
  List<String> listCapabilities();
  CapabilityHealthDto getCapabilityHealth(String id);
  String runCapabilitySelfTest(String id);
  void openCapabilitySettings(String id);
}
