package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordEvent;

public class DialogAddon_startWedding extends DialogAddon_Base{
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord) {
        LordEvent a = EventController.getCurrentFeast(lord.getLordAPI().getFaction());
        if (a == null) return;
        a.setWeddingCeremonyTarget(lord);
    }
}
