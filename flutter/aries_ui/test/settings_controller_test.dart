import 'package:aries_ui/src/application/settings/provider_credential_store.dart';
import 'package:aries_ui/src/application/settings/settings_repository.dart';
import 'package:aries_ui/src/features/settings/controllers/settings_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('validates and normalizes editable provider configuration', () async {
    final repository = InMemorySettingsRepository();
    final controller = SettingsController.withCredentials(
      repository: repository,
      credentials: InMemoryProviderCredentialStore(),
    );
    await controller.selectProfile('staging');

    final error = await controller.updateSelectedProviderConfiguration(
      baseUrl: 'https://example.test/v1///',
      model: ' custom-model ',
    );

    expect(error, isNull);
    expect(controller.selectedProfile.baseUrl, 'https://example.test/v1');
    expect(controller.selectedProfile.model, 'custom-model');
    controller.dispose();
  });

  test('rejects public HTTP but permits Android emulator localhost', () {
    const validator = ProviderConfigurationValidator();

    expect(
      validator.validate(baseUrl: 'http://example.test/v1', model: 'model'),
      'Provider URL must use HTTPS.',
    );
    expect(
      validator.validate(baseUrl: 'http://10.0.2.2:8080/v1', model: 'model'),
      isNull,
    );
  });

  test('saves and clears the selected provider API key', () async {
    final credentials = InMemoryProviderCredentialStore();
    final controller = SettingsController.withCredentials(
      repository: InMemorySettingsRepository(),
      credentials: credentials,
    );

    expect(await controller.saveSelectedApiKey('  private-key  '), isNull);
    expect(controller.hasApiKey, isTrue);
    expect(await credentials.readApiKey('default'), 'private-key');

    expect(await controller.saveSelectedApiKey(''), isNull);
    expect(controller.hasApiKey, isFalse);
    expect(await credentials.readApiKey('default'), isNull);
    controller.dispose();
  });
}
