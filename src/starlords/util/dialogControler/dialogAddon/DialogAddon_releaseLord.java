package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.person.Lord;
import starlords.plugins.PrisonerInteractionDialogPlugin;

public class DialogAddon_releaseLord extends DialogAddon_Base{
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        lord.setCaptor(null);
        if (!PrisonerInteractionDialogPlugin.getTargetLord().equals(lord)) return;
        if (PrisonerInteractionDialogPlugin.getUi() == null) return;
        PrisonerInteractionDialogPlugin.getUi().updateIntelList();
    }
}
