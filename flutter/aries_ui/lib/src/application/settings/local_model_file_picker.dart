class PickedLocalModelFile {
  const PickedLocalModelFile({
    required this.name,
    required this.path,
    required this.byteLength,
  });

  final String name;
  final String path;
  final int byteLength;
}

class LocalModelFilePickerException implements Exception {
  const LocalModelFilePickerException(this.message);

  final String message;

  @override
  String toString() => message;
}

abstract interface class LocalModelFilePicker {
  Future<PickedLocalModelFile?> pick();
}
