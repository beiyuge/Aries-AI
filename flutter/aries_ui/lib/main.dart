import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'src/app.dart';
import 'src/application/application_repositories.dart';
import 'src/infrastructure/application_repository_factory.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final repositories = await _createRepositories();
  runApp(AriesRe0App(repositories: repositories));
}

Future<ApplicationRepositories> _createRepositories() async {
  try {
    return await ApplicationRepositoryFactory.createPersistent();
  } on MissingPluginException catch (error, stackTrace) {
    _reportPersistenceBootstrapError(error, stackTrace);
  } on PlatformException catch (error, stackTrace) {
    _reportPersistenceBootstrapError(error, stackTrace);
  }
  return ApplicationRepositories.inMemory();
}

void _reportPersistenceBootstrapError(Object error, StackTrace stackTrace) {
  FlutterError.reportError(
    FlutterErrorDetails(
      exception: error,
      stack: stackTrace,
      library: 'Aries application persistence',
      context: ErrorDescription('while bootstrapping local repositories'),
    ),
  );
}
