package starlords.util.dialogControler.dialogRull;

import starlords.controllers.EventController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogRule_lordProposalExists extends DialogRule_Base {
    boolean feast;
    public DialogRule_lordProposalExists(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal currentProposal = PoliticsController.getProposal(lord);
        return feast == (currentProposal != null);
    }
}
