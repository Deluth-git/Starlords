package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.person.Lord;

public class DialogAddon_setCourted extends DialogAddon_Base{
    boolean hadDate;
    public DialogAddon_setCourted(boolean hadDate){
        this.hadDate=hadDate;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord){
        lord.setCourted(hadDate);
    }
}
