package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.util.Utils;

public class PrintLordsAlignments implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {


        Utils.printLordsAlignments();
        Console.showMessage("Print full lord list alignments to log");
        return CommandResult.SUCCESS;



    }
}
