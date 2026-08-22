package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.util.Utils;

public class PrintLordsPrisonerProblems implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        Utils.printLordsWithPrisonerProblems(false, true);

        Console.showMessage("Printed prisoner issues to log");
        return CommandResult.SUCCESS;
    }
}
