import '../../features/settings/models/settings_models.dart';

class SettingsState {
  SettingsState({
    required this.selectedProfileId,
    required this.streamResponses,
    required this.preferLocalModel,
    required this.localModelPath,
    required List<ProviderProfile> profiles,
  }) : profiles = List.unmodifiable(profiles);

  final String selectedProfileId;
  final bool streamResponses;
  final bool preferLocalModel;
  final String localModelPath;
  final List<ProviderProfile> profiles;

  SettingsState copyWith({
    String? selectedProfileId,
    bool? streamResponses,
    bool? preferLocalModel,
    String? localModelPath,
    List<ProviderProfile>? profiles,
  }) {
    return SettingsState(
      selectedProfileId: selectedProfileId ?? this.selectedProfileId,
      streamResponses: streamResponses ?? this.streamResponses,
      preferLocalModel: preferLocalModel ?? this.preferLocalModel,
      localModelPath: localModelPath ?? this.localModelPath,
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
        localModelPath: '',
        profiles: const [
          ProviderProfile(
            id: 'default',
            name: 'Default',
            kind: ProviderKind.remote,
            baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
            model: 'glm-4-flash',
          ),
          ProviderProfile(
            id: 'local',
            name: 'Local',
            kind: ProviderKind.local,
            baseUrl: '',
            model: 'local.default',
          ),
          ProviderProfile(
            id: 'staging',
            name: 'Custom',
            kind: ProviderKind.remote,
            baseUrl: 'https://api.openai.com/v1',
            model: 'gpt-4.1-mini',
            editable: true,
          ),
        ],
      );

  @override
  Future<void> save(SettingsState state) async {
    _state = state;
  }
}

class ProviderConfigurationValidator {
  const ProviderConfigurationValidator();

  String? validate({required String baseUrl, required String model}) {
    if (model.trim().isEmpty) {
      return 'Model is required.';
    }
    final uri = Uri.tryParse(baseUrl.trim());
    if (uri == null || !uri.hasScheme || uri.host.isEmpty) {
      return 'Enter a valid provider URL.';
    }
    if (uri.hasQuery || uri.hasFragment || uri.userInfo.isNotEmpty) {
      return 'Provider URL cannot contain credentials, query, or fragment.';
    }
    if (uri.scheme == 'https') {
      return null;
    }
    if (uri.scheme == 'http' && _isDevelopmentHost(uri.host)) {
      return null;
    }
    return 'Provider URL must use HTTPS.';
  }

  String normalizeBaseUrl(String baseUrl) {
    return baseUrl.trim().replaceFirst(RegExp(r'/+$'), '');
  }

  bool _isDevelopmentHost(String host) {
    final normalized = host.toLowerCase();
    return normalized == 'localhost' ||
        normalized == '127.0.0.1' ||
        normalized == '::1' ||
        normalized == '10.0.2.2';
  }
}
