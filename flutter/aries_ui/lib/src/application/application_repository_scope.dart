import 'package:flutter/widgets.dart';

import 'application_repositories.dart';

class ApplicationRepositoryScope extends InheritedWidget {
  const ApplicationRepositoryScope({
    required this.repositories,
    required super.child,
    super.key,
  });

  final ApplicationRepositories repositories;

  static ApplicationRepositories of(BuildContext context) {
    final scope = context
        .dependOnInheritedWidgetOfExactType<ApplicationRepositoryScope>();
    assert(scope != null, 'ApplicationRepositoryScope is missing');
    return scope!.repositories;
  }

  @override
  bool updateShouldNotify(ApplicationRepositoryScope oldWidget) {
    return repositories != oldWidget.repositories;
  }
}
