package starlords.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.ai.LordAI;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class WarpLord implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        if (argsArr.length != 1) {
            Console.showMessage("WarpLord requires 1 argument.");
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
        if (lord.getFleet() == null || !lord.getFleet().isAlive()) {
            Console.showMessage("Lord currently has no fleet.");
            return CommandResult.ERROR;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        SectorEntityToken token = playerFleet.getContainingLocation().createToken(
                playerFleet.getLocation().x, playerFleet.getLocation().y);
        JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(token, null);
        Global.getSector().doHyperspaceTransition(lord.getFleet(), null, dest);
        EventController.removeFromAllEvents(lord);
        lord.setCurrAction(null);
        lord.getFleet().getAI().clearAssignments();

        //MemoryAPI mem = lord.getFleet().getMemory();
        //Misc.setFlagWithReason(mem,MemFlags.FLEET_BUSY, LordAI.BUSY_REASON,true,1f);

        Console.showMessage("Warped lord " + lord.getLordAPI().getNameString() + " to player.");
        return CommandResult.SUCCESS;
    }
}
