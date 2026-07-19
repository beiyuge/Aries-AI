import 'package:shared_preferences/shared_preferences.dart';

import 'string_store.dart';

class SharedPreferencesStringStore implements StringStore {
  SharedPreferencesStringStore(this._preferences);

  final SharedPreferencesWithCache _preferences;
  Future<void> _pendingWrite = Future.value();

  @override
  String? read(String key) => _preferences.getString(key);

  @override
  Future<void> write(String key, String value) {
    final nextWrite = _pendingWrite.then(
      (_) => _preferences.setString(key, value),
      onError: (Object _, StackTrace __) => _preferences.setString(key, value),
    );
    _pendingWrite = nextWrite;
    return nextWrite;
  }
}
