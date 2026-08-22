package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class DialogAddon_wedPlayerToLord extends DialogAddon_Base{
    boolean hadDate;
    public DialogAddon_wedPlayerToLord(boolean hadDate){
        this.hadDate=hadDate;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord){
        if (hadDate){
            unMarryLord(lord);
            unMarryLord(LordController.getPlayerLord());

            lord.setMarried(true);
            lord.setSpouse(LordController.getPlayerLord().getLordAPI().getId());
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
