package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class AddLordRelation implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        if (argsArr.length != 2) {
            Console.showMessage("AddLordRelation requires 2 arguments.");
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
            Console.showMessage("Could not parse relations to add.");
            return CommandResult.ERROR;
        }

        lord.getLordAPI().getRelToPlayer().adjustRelationship(
                (float) amount / 100, null);
        Console.showMessage("Player relation with " + lord.getLordAPI().getNameString()
                + " is now " + lord.getLordAPI().getRelToPlayer().getRepInt());
        return CommandResult.SUCCESS;
    }
}
