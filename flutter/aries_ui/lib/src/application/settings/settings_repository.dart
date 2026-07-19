import '../../features/settings/models/settings_models.dart';

class SettingsState {
  SettingsState({
    required this.selectedProfileId,
    required this.streamResponses,
    required this.preferLocalModel,
    required List<ProviderProfile> profiles,
  }) : profiles = List.unmodifiable(profiles);

  final String selectedProfileId;
  final bool streamResponses;
  final bool preferLocalModel;
  final List<ProviderProfile> profiles;

  SettingsState copyWith({
    String? selectedProfileId,
    bool? streamResponses,
    bool? preferLocalModel,
    List<ProviderProfile>? profiles,
  }) {
    return SettingsState(
      selectedProfileId: selectedProfileId ?? this.selectedProfileId,
      streamResponses: streamResponses ?? this.streamResponses,
      preferLocalModel: preferLocalModel ?? this.preferLocalModel,
      profiles: profiles ?? this.profiles,
    );
  }
}

abstract interface class SettingsRepository {
  SettingsState load();

  Future<void> save(SettingsState state);
}

class InMemorySettingsRepository implements SettingsRepository {
  SettingsState? _state;

  @override
  SettingsState load() => _state ??= SettingsState(
        selectedProfileId: 'default',
        streamResponses: true,
        preferLocalModel: false,
        profiles: const [
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
        ],
      );

  @override
  Future<void> save(SettingsState state) async {
    _state = state;
  }
}
