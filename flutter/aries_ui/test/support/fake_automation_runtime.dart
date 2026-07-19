import 'package:aries_ui/src/application/automation/automation_command.dart';
import 'package:aries_ui/src/application/automation/automation_runtime.dart';

class FakeAutomationRuntime implements AutomationRuntime {
  FakeAutomationRuntime(this.result);

  FakeAutomationRuntime.success(String summary)
      : result = AutomationExecutionResult(
          success: true,
          summary: summary,
          recoverable: false,
        );

  final AutomationExecutionResult result;
  final List<AutomationCommand> commands = [];

  @override
  Future<AutomationExecutionResult> execute(AutomationCommand command) async {
    commands.add(command);
    return result;
  }
}
