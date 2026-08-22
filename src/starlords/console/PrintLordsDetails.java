package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.util.Utils;

public class PrintLordsDetails implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        Utils.printLordsDetails();

        Console.showMessage("Print full lord list details to log");
        return CommandResult.SUCCESS;
    }
}
