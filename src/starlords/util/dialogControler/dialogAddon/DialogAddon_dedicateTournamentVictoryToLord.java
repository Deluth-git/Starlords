package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;


public class DialogAddon_dedicateTournamentVictoryToLord extends DialogAddon_Base{
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord) {
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return;
        if (currentFeast.getTournamentWinner() == null) return;
        if (!currentFeast.getTournamentWinner().isPlayer()) return;
        currentFeast.setVictoryDedicated(true);
        // other courted lords get jealous
        /*
        for (Lord lord2: LordController.getLordsList()) {
            if (lord2.isCourted() && !lord2.equals(currentFeast)) {
                int decrease = 1;
                if (currentFeast.getOriginator().equals(lord2) || currentFeast.getParticipants().contains(lord2)) {
                    decrease = 2;
                }
                new DialogAddon_repDecrease(decrease,decrease).apply(textPanel,options,dialog,lord2);
            }
        }*/
    }
}
