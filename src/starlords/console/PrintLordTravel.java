package starlords.console;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.Utils;

public class PrintLordTravel implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        Lord lord = LordController.getLordByFirstName(argsArr[0]);
        if (lord == null) {
            lord = LordController.getLordOrPlayerById(argsArr[0]);
        }
        if (lord == null) {
            lord = LordController.getPlayerLord();
            Console.showMessage("No Lord provided. Using player");
        }
        SectorEntityToken token;

        if (lord.isPlayer()) {
             token = lord.getFleet().getInteractionTarget();
        }
        else {
             token = lord.getTarget();
        }

        if (token == null) {
            Console.showMessage("Lord has no current target.");
            return CommandResult.ERROR;
        }

        float distanceLY = Misc.getDistanceLY(lord.getFleet(),token);
        float speed = lord.getFleet().getTravelSpeed();

        Console.showMessage("Lord " + lord.getLordAPI().getNameString()
                + "traveling to " + token.getName() + " "
                + "distance: " + distanceLY + " "
                + "speed: " + speed + " "
                + "time at current speed: " + (distanceLY / speed) * 200 + " days "
                + "time at average speed: " + Utils.getTravelTime(lord.getFleet(),token) + " days ");
        return CommandResult.SUCCESS;
    }
}
