package starlords.util.dialogControler.dialogRull;

import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogRule_playerFactionHasActiveConsole extends DialogRule_Base {
    boolean feast;
    public DialogRule_playerFactionHasActiveConsole(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal currentProposal = PoliticsController.getProposal(lord);
        return feast == (currentProposal != null);
    }
}
