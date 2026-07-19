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

  void selectProfile(String profileId) {
    _save(_state.copyWith(selectedProfileId: profileId));
  }

  void setStreamResponses(bool value) {
    _save(_state.copyWith(streamResponses: value));
  }

  void setPreferLocalModel(bool value) {
    _save(_state.copyWith(preferLocalModel: value));
  }

  void _save(SettingsState state) {
    _state = state;
    _repository.save(state);
    notifyListeners();
  }
}
