package starlords.util.dialogControler.dialogRull;

import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogRule_curProposalPlayerSupports extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_curProposalPlayerSupports(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal proposal = PoliticsController.getCurrProposal(lord.getFaction());
        if (proposal == null) return false;
        return isMarried == proposal.isPlayerSupports();
    }
}
