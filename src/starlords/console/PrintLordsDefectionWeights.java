package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.DefectionUtils;
import starlords.util.Utils;

public class PrintLordsDefectionWeights implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        for (Lord lord : LordController.getLordsList())
            DefectionUtils.getLordPreferredFaction(lord,true);

        Console.showMessage("Print full lord list defection weights to log");
        return CommandResult.SUCCESS;

    }
}
