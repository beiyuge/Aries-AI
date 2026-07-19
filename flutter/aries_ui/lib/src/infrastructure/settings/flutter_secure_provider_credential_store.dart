import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../application/settings/provider_credential_store.dart';

class FlutterSecureProviderCredentialStore implements ProviderCredentialStore {
  FlutterSecureProviderCredentialStore({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  @override
  Future<String?> readApiKey(String profileId) async {
    try {
      return await _storage.read(key: _key(profileId));
    } on PlatformException catch (error) {
      throw ProviderCredentialException(
        error.message ?? 'Secure credential storage could not be read.',
      );
    }
  }

  @override
  Future<void> writeApiKey(String profileId, String? apiKey) async {
    try {
      final normalized = apiKey?.trim();
      if (normalized == null || normalized.isEmpty) {
        await _storage.delete(key: _key(profileId));
      } else {
        await _storage.write(key: _key(profileId), value: normalized);
      }
    } on PlatformException catch (error) {
      throw ProviderCredentialException(
        error.message ?? 'Secure credential storage could not be updated.',
      );
    }
  }

  String _key(String profileId) => 'aries.provider.$profileId.api-key';
}
