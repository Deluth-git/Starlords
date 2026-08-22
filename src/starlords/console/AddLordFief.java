package starlords.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.controllers.RelationController;
import starlords.person.Lord;
import starlords.util.Utils;

public class AddLordFief implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        String[] argsArr = args.split(" ");
        if (argsArr.length != 2) {
            Console.showMessage("AddLordFief requires 2 arguments.");
            return CommandResult.BAD_SYNTAX;
        }
        Lord lord = LordController.getLordByFirstName(argsArr[0]);
        if (lord == null) {
            lord = LordController.getLordOrPlayerById(argsArr[0]);
        }
        if (lord == null) {
            Console.showMessage("Lord not found.");
            return CommandResult.ERROR;
        }

        MarketAPI market = Global.getSector().getEconomy().getMarket(argsArr[1]);
        if (market == null) {
            Console.showMessage("Invalid market id, trying name.");
        }

        market = Utils.getFactionMarket(argsArr[1], lord.getFaction().getId());
        if (market == null) {
            Console.showMessage("Invalid market id or name.");
            return CommandResult.ERROR;
        }

        if (!market.getFaction().equals(lord.getFaction())) {
            Console.showMessage("Cannot grant market to a lord of a different faction.");
            return CommandResult.ERROR;
        }
        FiefController.setOwner(market, lord.getLordAPI().getId());
        Console.showMessage("Changed owner of " + market.getName() + " to " + lord.getLordAPI().getNameString());
        return CommandResult.SUCCESS;
    }
}
