package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogAddon_wedPlayerToWeddingTarget extends DialogAddon_Base{
    boolean hadDate;
    public DialogAddon_wedPlayerToWeddingTarget(boolean hadDate){
        this.hadDate=hadDate;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord){
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return;
        if (currentFeast.getWeddingCeremonyTarget() == null) return;
        lord = currentFeast.getWeddingCeremonyTarget();
        if (hadDate){
            unMarryLord(lord);
            unMarryLord(LordController.getPlayerLord());

            lord.setMarried(true);
            lord.setSpouse(Global.getSector().getPlayerPerson().getId());
            LordController.getPlayerLord().setMarried(true);
            LordController.getPlayerLord().setSpouse(lord.getLordAPI().getId());
        }else if (LordController.getPlayerLord().getSpouse().equals(lord.getLordAPI().getId())){
            unMarryLord(lord);
        }
    }

    private void unMarryLord(Lord lord){
        if (lord.getSpouse() != null){
            Lord lord2 = LordController.getLordById(lord.getSpouse());
            lord2.setSpouse(null);
            lord2.setMarried(false);
            lord.setSpouse(null);
            lord.setMarried(false);
        }
    }
}
