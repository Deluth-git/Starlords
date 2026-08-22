package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.util.Utils;

public class PrintLordsRequests implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        Utils.printRequests();

        Console.showMessage("Print existing lord requests to log");
        return CommandResult.SUCCESS;
    }
}
