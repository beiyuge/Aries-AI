class LocalModelGatewayException implements Exception {
  const LocalModelGatewayException({
    required this.code,
    required this.message,
    required this.recoverable,
  });

  final String code;
  final String message;
  final bool recoverable;

  @override
  String toString() => '$code: $message';
}

abstract interface class LocalModelGateway {
  Future<void> load({required String modelId, required String path});

  Future<String> generate({required String modelId, required String prompt});

  Future<void> unload(String modelId);
}
