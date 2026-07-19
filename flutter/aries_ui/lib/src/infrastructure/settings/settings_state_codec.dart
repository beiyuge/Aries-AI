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
      'localModelPath': value.localModelPath,
      'providerOverrides': [
        for (final profile in value.profiles)
          if (profile.editable)
            {
              'id': profile.id,
              'baseUrl': profile.baseUrl,
              'model': profile.model,
            },
      ],
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
    final overrides = value['providerOverrides'] == null
        ? const <Object?>[]
        : JsonValue.list(value['providerOverrides'], 'providerOverrides');
    final profiles = [
      for (final profile in _profiles) _applyOverride(profile, overrides),
    ];
    return SettingsState(
      selectedProfileId: selectedProfileId,
      streamResponses:
          JsonValue.boolean(value['streamResponses'], 'streamResponses'),
      preferLocalModel:
          JsonValue.boolean(value['preferLocalModel'], 'preferLocalModel'),
      localModelPath: value['localModelPath'] == null
          ? ''
          : JsonValue.string(value['localModelPath'], 'localModelPath'),
      profiles: profiles,
    );
  }

  ProviderProfile _applyOverride(
    ProviderProfile profile,
    List<Object?> overrides,
  ) {
    if (!profile.editable) {
      return profile;
    }
    for (final item in overrides) {
      final override = JsonValue.map(item, 'providerOverride');
      if (override['id'] == profile.id) {
        return profile.copyWith(
          baseUrl: JsonValue.string(
            override['baseUrl'],
            'providerOverride.baseUrl',
          ),
          model: JsonValue.string(
            override['model'],
            'providerOverride.model',
          ),
        );
      }
    }
    return profile;
  }
}
