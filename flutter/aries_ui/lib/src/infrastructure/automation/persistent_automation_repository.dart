import '../../application/automation/automation_repository.dart';
import '../persistence/json_state_store.dart';
import '../persistence/persistence_keys.dart';
import '../persistence/string_store.dart';
import 'automation_state_codec.dart';

class PersistentAutomationRepository implements AutomationRepository {
  PersistentAutomationRepository({required StringStore store})
      : _stateStore = _createStateStore(store);

  final JsonStateStore<AutomationState> _stateStore;

  static JsonStateStore<AutomationState> _createStateStore(StringStore store) {
    final fallback = InMemoryAutomationRepository().load();
    return JsonStateStore(
      store: store,
      key: PersistenceKeys.automation,
      codec: AutomationStateCodec(fallback.capabilities),
      fallback: () => fallback,
    );
  }

  @override
  AutomationState load() => _stateStore.load();

  @override
  Future<void> save(AutomationState state) => _stateStore.save(state);
}
