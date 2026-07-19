enum ProviderKind { remote, local }

class ProviderProfile {
  const ProviderProfile({
    required this.id,
    required this.name,
    required this.kind,
    required this.baseUrl,
    required this.model,
    this.editable = false,
  });

  final String id;
  final String name;
  final ProviderKind kind;
  final String baseUrl;
  final String model;
  final bool editable;

  bool get requiresApiKey => kind == ProviderKind.remote;

  String get endpointLabel => switch (kind) {
        ProviderKind.local => 'native runtime',
        ProviderKind.remote => '$model · $baseUrl',
      };

  ProviderProfile copyWith({String? baseUrl, String? model}) {
    return ProviderProfile(
      id: id,
      name: name,
      kind: kind,
      baseUrl: baseUrl ?? this.baseUrl,
      model: model ?? this.model,
      editable: editable,
    );
  }
}
