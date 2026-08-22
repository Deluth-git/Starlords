package starlords.console;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.ai.LordAI;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class RespawnLord implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        if (argsArr.length != 1) {
            Console.showMessage("RespawnLord requires 1 argument.");
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

        if (lord.getFleet() != null) {
            for (FleetMemberAPI toDestroy : lord.getFleet().getMembersWithFightersCopy()) {
                if (!toDestroy.isFighterWing()) lord.getFleet().removeFleetMemberWithDestructionFlash(toDestroy);
            }
        }
        LordAI.progressAssignment(lord);

        Console.showMessage("Respawning lord " + lord.getLordAPI().getNameString() + " .");
        return CommandResult.SUCCESS;
    }
}
