import '../../application/settings/settings_repository.dart';
import '../../features/settings/models/settings_models.dart';
import '../persistence/json_state_store.dart';

class SettingsStateCodec implements JsonStateCodec<SettingsState> {
  SettingsStateCodec(List<ProviderProfile> profiles)
      : _profiles = List.unmodifiable(profiles) {
    if (_profiles.isEmpty) {
      throw ArgumentError.value(profiles, 'profiles', 'must not be empty');
    }
  }

  final List<ProviderProfile> _profiles;

  @override
  Map<String, Object?> encode(SettingsState value) {
    return {
      'selectedProfileId': value.selectedProfileId,
      'streamResponses': value.streamResponses,
      'preferLocalModel': value.preferLocalModel,
    };
  }

  @override
  SettingsState decode(Map<String, Object?> value) {
    final storedProfileId =
        JsonValue.string(value['selectedProfileId'], 'selectedProfileId');
    final selectedProfileId = _profiles.any(
      (profile) => profile.id == storedProfileId,
    )
        ? storedProfileId
        : _profiles.first.id;
    return SettingsState(
      selectedProfileId: selectedProfileId,
      streamResponses:
          JsonValue.boolean(value['streamResponses'], 'streamResponses'),
      preferLocalModel:
          JsonValue.boolean(value['preferLocalModel'], 'preferLocalModel'),
      profiles: _profiles,
    );
  }
}
