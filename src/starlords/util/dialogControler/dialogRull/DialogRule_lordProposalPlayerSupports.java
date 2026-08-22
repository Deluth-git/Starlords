package starlords.util.dialogControler.dialogRull;

import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogRule_lordProposalPlayerSupports extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_lordProposalPlayerSupports(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal proposal = PoliticsController.getProposal(lord);
        if (proposal == null) return false;
        return isMarried == proposal.isPlayerSupports();
    }
}
