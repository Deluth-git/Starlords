package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogAddon_setPledgedAgainst_PlayerProposal extends DialogAddon_Base{
    boolean hadDate;
    public DialogAddon_setPledgedAgainst_PlayerProposal(boolean hadDate){
        this.hadDate=hadDate;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord){
        LawProposal proposal = PoliticsController.getProposal(LordController.getPlayerLord());
        if (hadDate) proposal.getPledgedFor().remove(lord.getLordAPI().getId());
        if (hadDate && !proposal.getPledgedAgainst().contains(lord.getLordAPI().getId())){
            proposal.getPledgedAgainst().add(lord.getLordAPI().getId());
            PoliticsController.updateProposal(proposal);
            return;
        }
        if (!hadDate){
            proposal.getPledgedAgainst().remove(lord.getLordAPI().getId());
        }
        PoliticsController.updateProposal(proposal);
    }

}
