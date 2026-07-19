abstract interface class StringStore {
  String? read(String key);

  Future<void> write(String key, String value);
}

class InMemoryStringStore implements StringStore {
  InMemoryStringStore([Map<String, String>? values]) : _values = {...?values};

  final Map<String, String> _values;

  @override
  String? read(String key) => _values[key];

  @override
  Future<void> write(String key, String value) async {
    _values[key] = value;
  }
}
