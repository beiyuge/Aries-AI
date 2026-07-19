import 'dart:convert';

import 'string_store.dart';

abstract interface class JsonStateCodec<T> {
  Map<String, Object?> encode(T value);

  T decode(Map<String, Object?> value);
}

class JsonStateStore<T> {
  JsonStateStore({
    required StringStore store,
    required String key,
    required JsonStateCodec<T> codec,
    required T Function() fallback,
    int schemaVersion = 1,
  })  : _store = store,
        _key = key,
        _codec = codec,
        _fallback = fallback,
        _schemaVersion = schemaVersion;

  final StringStore _store;
  final String _key;
  final JsonStateCodec<T> _codec;
  final T Function() _fallback;
  final int _schemaVersion;

  T load() {
    final encoded = _store.read(_key);
    if (encoded == null) {
      return _fallback();
    }
    try {
      final root = JsonValue.map(jsonDecode(encoded), 'root');
      final schemaVersion = JsonValue.integer(
        root['schemaVersion'],
        'schemaVersion',
      );
      if (schemaVersion != _schemaVersion) {
        return _fallback();
      }
      return _codec.decode(JsonValue.map(root['data'], 'data'));
    } on FormatException {
      return _fallback();
    }
  }

  Future<void> save(T value) {
    return _store.write(
      _key,
      jsonEncode({
        'schemaVersion': _schemaVersion,
        'data': _codec.encode(value),
      }),
    );
  }
}

class JsonValue {
  const JsonValue._();

  static Map<String, Object?> map(Object? value, String field) {
    if (value is! Map) {
      throw FormatException('$field must be an object');
    }
    final result = <String, Object?>{};
    for (final entry in value.entries) {
      final key = entry.key;
      if (key is! String) {
        throw FormatException('$field contains a non-string key');
      }
      result[key] = entry.value;
    }
    return result;
  }

  static List<Object?> list(Object? value, String field) {
    if (value is! List) {
      throw FormatException('$field must be a list');
    }
    return List<Object?>.from(value);
  }

  static String string(Object? value, String field) {
    if (value is! String) {
      throw FormatException('$field must be a string');
    }
    return value;
  }

  static int integer(Object? value, String field) {
    if (value is! int) {
      throw FormatException('$field must be an integer');
    }
    return value;
  }

  static bool boolean(Object? value, String field) {
    if (value is! bool) {
      throw FormatException('$field must be a boolean');
    }
    return value;
  }
}
