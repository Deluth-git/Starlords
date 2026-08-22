package starlords.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.controllers.LordController;
import starlords.controllers.RequestController;
import starlords.person.Lord;
import starlords.person.LordRequest;

public class AddLordRequest implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        if (argsArr.length != 2) {
            Console.showMessage("AddLordRequest requires 2 arguments.");
            return CommandResult.BAD_SYNTAX;
        }
        Lord lord = LordController.getLordById(argsArr[0]);
        if (lord == null || lord.isPlayer()) {
            Console.showMessage("Lord not found.");
            return CommandResult.ERROR;
        }

        String request_type = String.valueOf(argsArr[1]);;
        switch (request_type){
            case LordRequest.PRISON_BREAK:
                RequestController.addRequest(new LordRequest(LordRequest.PRISON_BREAK, lord));
                Console.showMessage("Added " + request_type + " request to " + lord.getLordAPI().getNameString());
                return CommandResult.SUCCESS;
            case LordRequest.FIEF_FOR_DEFECTION:
                RequestController.addRequest(new LordRequest(LordRequest.FIEF_FOR_DEFECTION, lord));
                Console.showMessage("Added " + request_type + " request to " + lord.getLordAPI().getNameString());
                return CommandResult.SUCCESS;
            default:
                Console.showMessage("Incorrect request type");
                return CommandResult.ERROR;
        }


    }
}
