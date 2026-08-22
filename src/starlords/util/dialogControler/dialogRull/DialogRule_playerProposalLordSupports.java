package starlords.util.dialogControler.dialogRull;

import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogRule_playerProposalLordSupports extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_playerProposalLordSupports(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal proposal = PoliticsController.getProposal(LordController.getPlayerLord());
        if (proposal == null) return false;
        boolean supports = proposal.getSupporters().contains(lord.getLordAPI().getId());
        return isMarried == supports;
    }
}
