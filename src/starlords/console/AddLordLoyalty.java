package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.controllers.LordController;
import starlords.controllers.RelationController;
import starlords.person.Lord;

public class AddLordLoyalty implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        if (argsArr.length != 2) {
            Console.showMessage("AddLordLoyalty requires 2 arguments.");
            return CommandResult.BAD_SYNTAX;
        }
        Lord lord = LordController.getLordByFirstName(argsArr[0]);
        if (lord == null) {
            lord = LordController.getLordById(argsArr[0]);
        }
        if (lord == null || lord.isPlayer()) {
            Console.showMessage("Lord not found.");
            return CommandResult.ERROR;
        }

        int amount;
        try {
            amount = Integer.parseInt(argsArr[1]);
        } catch (Exception e) {
            Console.showMessage("Could not parse loyalty to add.");
            return CommandResult.ERROR;
        }

        RelationController.modifyLoyalty(lord, amount);
        Console.showMessage("Faction loyalty of " + lord.getLordAPI().getNameString()
                + " is now " + RelationController.getLoyalty(lord));
        return CommandResult.SUCCESS;
    }
}
