package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.util.Utils;

public class FixLordsPrisonerProblems implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        Utils.printLordsWithPrisonerProblems(true, false);

        Console.showMessage("Fixed prisoner problems");
        return CommandResult.SUCCESS;
    }
}
