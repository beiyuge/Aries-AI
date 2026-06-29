import 'package:flutter/foundation.dart';

import '../models/settings_models.dart';

class SettingsController extends ChangeNotifier {
  String _selectedProfileId = 'default';
  bool _streamResponses = true;
  bool _preferLocalModel = false;

  String get selectedProfileId => _selectedProfileId;
  bool get streamResponses => _streamResponses;
  bool get preferLocalModel => _preferLocalModel;

  List<ProviderProfile> get profiles => const [
        ProviderProfile(
          id: 'default',
          name: 'Default',
          endpointLabel: 'System provider',
        ),
        ProviderProfile(
          id: 'local',
          name: 'Local',
          endpointLabel: 'native runtime',
        ),
        ProviderProfile(
          id: 'staging',
          name: 'Staging',
          endpointLabel: 'remote gateway',
        ),
      ];

  void selectProfile(String profileId) {
    _selectedProfileId = profileId;
    notifyListeners();
  }

  void setStreamResponses(bool value) {
    _streamResponses = value;
    notifyListeners();
  }

  void setPreferLocalModel(bool value) {
    _preferLocalModel = value;
    notifyListeners();
  }
}
