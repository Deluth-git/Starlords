package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;

import static starlords.ai.LordAI.BUSY_REASON;

public class DialogAddon_setInPlayerFleetTrue extends DialogAddon_Base{
    public DialogAddon_setInPlayerFleetTrue(){
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord){
        if (lord.getCurrAction() != LordAction.COMPANION) {
            EventController.removeFromAllEvents(lord);
            lord.setCurrAction(LordAction.COMPANION);
            lord.getFleet().setVelocity(0, 0);
            lord.getFleet().fadeOutIndicator();
            lord.getFleet().setHidden(true);
            lord.getFleet().clearAssignments();
            lord.setOldFleet(lord.getFleet());
            MemoryAPI mem = lord.getFleet().getMemoryWithoutUpdate();
            Misc.setFlagWithReason(mem,
                    MemFlags.FLEET_BUSY, BUSY_REASON, true, 1e7f);
            Misc.setFlagWithReason(mem,
                    MemFlags.FLEET_IGNORES_OTHER_FLEETS, BUSY_REASON, true, 1e7f);
            Global.getSector().getPlayerFleet().getFleetData().addOfficer(lord.getLordAPI());
            Misc.setMercenary(lord.getLordAPI(), true);
            Misc.setMercHiredNow(lord.getLordAPI());
            lord.getLordAPI().setFaction(lord.getOldFleet().getFaction().getId());
        }
    }
}
