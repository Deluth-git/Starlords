package starlords.util.dialogControler.dialogRull;

import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogRule_playerProposalExists extends DialogRule_Base {
    boolean feast;
    public DialogRule_playerProposalExists(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal currentProposal = PoliticsController.getProposal(LordController.getPlayerLord());
        return feast == (currentProposal != null);
    }
}
