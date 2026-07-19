import 'package:flutter/services.dart';

import '../../application/chat/local_model_gateway.dart';
import '../../generated/capabilities.g.dart';

class PigeonLocalModelGateway implements LocalModelGateway {
  PigeonLocalModelGateway({LocalModelHostApi? hostApi})
      : _hostApi = hostApi ?? LocalModelHostApi();

  final LocalModelHostApi _hostApi;

  @override
  Future<void> load({required String modelId, required String path}) {
    return _translate(() => _hostApi.loadLocalModel(modelId, path));
  }

  @override
  Future<String> generate({
    required String modelId,
    required String prompt,
  }) {
    return _translate(() => _hostApi.generateLocalModel(modelId, prompt));
  }

  @override
  Future<void> unload(String modelId) {
    return _translate(() => _hostApi.unloadLocalModel(modelId));
  }

  Future<T> _translate<T>(Future<T> Function() operation) async {
    try {
      return await operation();
    } on PlatformException catch (error) {
      final details = error.details;
      final recoverable =
          details is Map ? details['recoverable'] as bool? ?? true : true;
      throw LocalModelGatewayException(
        code: error.code,
        message: error.message ?? 'The local model operation failed.',
        recoverable: recoverable,
      );
    }
  }
}
