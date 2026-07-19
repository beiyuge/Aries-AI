class ProviderCredentialException implements Exception {
  const ProviderCredentialException(this.message);

  final String message;

  @override
  String toString() => message;
}

abstract interface class ProviderCredentialStore {
  Future<String?> readApiKey(String profileId);

  Future<void> writeApiKey(String profileId, String? apiKey);
}

class InMemoryProviderCredentialStore implements ProviderCredentialStore {
  final Map<String, String> _apiKeys = {};

  @override
  Future<String?> readApiKey(String profileId) async => _apiKeys[profileId];

  @override
  Future<void> writeApiKey(String profileId, String? apiKey) async {
    final normalized = apiKey?.trim();
    if (normalized == null || normalized.isEmpty) {
      _apiKeys.remove(profileId);
    } else {
      _apiKeys[profileId] = normalized;
    }
  }
}
