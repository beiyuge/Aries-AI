import '../../application/chat/chat_repository.dart';
import '../persistence/json_state_store.dart';
import '../persistence/persistence_keys.dart';
import '../persistence/string_store.dart';
import 'chat_state_codec.dart';

class PersistentChatRepository implements ChatRepository {
  PersistentChatRepository({
    required StringStore store,
    DateTime Function()? clock,
  }) : _stateStore = JsonStateStore(
          store: store,
          key: PersistenceKeys.chat,
          codec: const ChatStateCodec(),
          fallback: InMemoryChatRepository(clock: clock).load,
        );

  final JsonStateStore<ChatState> _stateStore;

  @override
  ChatState load() => _stateStore.load();

  @override
  Future<void> save(ChatState state) => _stateStore.save(state);
}
