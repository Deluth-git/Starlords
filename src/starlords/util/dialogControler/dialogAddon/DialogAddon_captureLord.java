package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogAddon_captureLord extends DialogAddon_Base{
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        Lord playerLord = LordController.getPlayerLord();
        for (String prisonerId : lord.getPrisoners()) {
            playerLord.addPrisoner(prisonerId);
            LordController.getLordById(prisonerId).setCaptor(playerLord.getLordAPI().getId());
        }
        lord.setCurrAction(LordAction.IMPRISONED);
        playerLord.addPrisoner(lord.getLordAPI().getId());
        lord.getPrisoners().clear();
        EventController.removeFromAllEvents(lord);
    }
}
