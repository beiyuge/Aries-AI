import '../../application/settings/settings_repository.dart';
import '../persistence/json_state_store.dart';
import '../persistence/persistence_keys.dart';
import '../persistence/string_store.dart';
import 'settings_state_codec.dart';

class PersistentSettingsRepository implements SettingsRepository {
  PersistentSettingsRepository({required StringStore store})
      : _stateStore = _createStateStore(store);

  final JsonStateStore<SettingsState> _stateStore;

  static JsonStateStore<SettingsState> _createStateStore(StringStore store) {
    final fallback = InMemorySettingsRepository().load();
    return JsonStateStore(
      store: store,
      key: PersistenceKeys.settings,
      codec: SettingsStateCodec(fallback.profiles),
      fallback: () => fallback,
    );
  }

  @override
  SettingsState load() => _stateStore.load();

  @override
  Future<void> save(SettingsState state) => _stateStore.save(state);
}
