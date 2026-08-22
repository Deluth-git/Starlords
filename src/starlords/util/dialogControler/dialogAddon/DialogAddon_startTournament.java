package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.plugins.TournamentDialogPlugin;

public class DialogAddon_startTournament extends DialogAddon_Base {
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord) {
        dialog.dismiss();
        // cant open new dialogue immediately
        Global.getSector().addTransientScript(new EveryFrameScript() {
            boolean isDone;
            @Override
            public boolean isDone() {
                return isDone;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }

            @Override
            public void advance(float amount) {
                if (!isDone && Global.getSector().getCampaignUI().showInteractionDialog(
                        new TournamentDialogPlugin(EventController.getCurrentFeast(lord.getFaction())), null)) {
                    isDone = true;
                }
            }
        });
    }
}
