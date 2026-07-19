import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../application/settings/provider_credential_store.dart';
import '../../../application/settings/settings_repository.dart';
import '../models/settings_models.dart';

class SettingsController extends ChangeNotifier {
  SettingsController({SettingsRepository? repository})
      : this.withCredentials(
          repository: repository,
          credentials: InMemoryProviderCredentialStore(),
        );

  SettingsController.withCredentials({
    SettingsRepository? repository,
    ProviderCredentialStore? credentials,
    ProviderConfigurationValidator validator =
        const ProviderConfigurationValidator(),
  })  : _repository = repository ?? InMemorySettingsRepository(),
        _credentials = credentials ?? InMemoryProviderCredentialStore(),
        _validator = validator {
    _state = _repository.load();
    unawaited(_refreshCredentialStatus());
  }

  final SettingsRepository _repository;
  final ProviderCredentialStore _credentials;
  final ProviderConfigurationValidator _validator;
  late SettingsState _state;
  bool _hasApiKey = false;
  bool _credentialBusy = false;
  String? _credentialError;
  bool _disposed = false;

  String get selectedProfileId => _state.selectedProfileId;
  bool get streamResponses => _state.streamResponses;
  bool get preferLocalModel => _state.preferLocalModel;
  String get localModelPath => _state.localModelPath;
  List<ProviderProfile> get profiles => _state.profiles;
  ProviderProfile get selectedProfile => profiles.firstWhere(
        (profile) => profile.id == selectedProfileId,
      );
  bool get hasApiKey => _hasApiKey;
  bool get credentialBusy => _credentialBusy;
  String? get credentialError => _credentialError;

  Future<void> selectProfile(String profileId) async {
    await _save(_state.copyWith(selectedProfileId: profileId));
    await _refreshCredentialStatus();
  }

  Future<void> setStreamResponses(bool value) {
    return _save(_state.copyWith(streamResponses: value));
  }

  Future<void> setPreferLocalModel(bool value) {
    return _save(_state.copyWith(preferLocalModel: value));
  }

  Future<void> setLocalModelPath(String path) {
    return _save(_state.copyWith(localModelPath: path.trim()));
  }

  Future<String?> updateSelectedProviderConfiguration({
    required String baseUrl,
    required String model,
  }) async {
    final profile = selectedProfile;
    if (!profile.editable || profile.kind != ProviderKind.remote) {
      return 'This provider configuration is managed by the app.';
    }
    final error = _validator.validate(baseUrl: baseUrl, model: model);
    if (error != null) {
      return error;
    }
    final updatedProfile = profile.copyWith(
      baseUrl: _validator.normalizeBaseUrl(baseUrl),
      model: model.trim(),
    );
    await _save(
      _state.copyWith(
        profiles: [
          for (final candidate in profiles)
            if (candidate.id == profile.id) updatedProfile else candidate,
        ],
      ),
    );
    return null;
  }

  Future<String?> saveSelectedApiKey(String apiKey) async {
    final profile = selectedProfile;
    if (!profile.requiresApiKey) {
      return 'The local provider does not use an API key.';
    }
    _credentialBusy = true;
    _credentialError = null;
    _notifyListeners();
    try {
      await _credentials.writeApiKey(profile.id, apiKey);
      _hasApiKey = apiKey.trim().isNotEmpty;
      return null;
    } on ProviderCredentialException catch (error) {
      _credentialError = error.message;
      return error.message;
    } finally {
      _credentialBusy = false;
      _notifyListeners();
    }
  }

  Future<void> _refreshCredentialStatus() async {
    final profile = selectedProfile;
    if (!profile.requiresApiKey) {
      _hasApiKey = false;
      _credentialError = null;
      _notifyListeners();
      return;
    }
    _credentialBusy = true;
    _credentialError = null;
    _notifyListeners();
    try {
      final apiKey = await _credentials.readApiKey(profile.id);
      _hasApiKey = apiKey?.trim().isNotEmpty ?? false;
    } on ProviderCredentialException catch (error) {
      _hasApiKey = false;
      _credentialError = error.message;
    } finally {
      _credentialBusy = false;
      _notifyListeners();
    }
  }

  Future<void> _save(SettingsState state) async {
    _state = state;
    _notifyListeners();
    await _repository.save(state);
  }

  void _notifyListeners() {
    if (!_disposed) {
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
