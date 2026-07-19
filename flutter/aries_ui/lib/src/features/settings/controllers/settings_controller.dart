import 'package:flutter/foundation.dart';

import '../../../application/settings/settings_repository.dart';
import '../models/settings_models.dart';

class SettingsController extends ChangeNotifier {
  SettingsController({SettingsRepository? repository})
      : _repository = repository ?? InMemorySettingsRepository() {
    _state = _repository.load();
  }

  final SettingsRepository _repository;
  late SettingsState _state;

  String get selectedProfileId => _state.selectedProfileId;
  bool get streamResponses => _state.streamResponses;
  bool get preferLocalModel => _state.preferLocalModel;
  List<ProviderProfile> get profiles => _state.profiles;

  Future<void> selectProfile(String profileId) {
    return _save(_state.copyWith(selectedProfileId: profileId));
  }

  Future<void> setStreamResponses(bool value) {
    return _save(_state.copyWith(streamResponses: value));
  }

  Future<void> setPreferLocalModel(bool value) {
    return _save(_state.copyWith(preferLocalModel: value));
  }

  Future<void> _save(SettingsState state) async {
    _state = state;
    notifyListeners();
    await _repository.save(state);
  }
}
