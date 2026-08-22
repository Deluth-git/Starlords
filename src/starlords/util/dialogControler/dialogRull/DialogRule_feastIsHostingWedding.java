package starlords.util.dialogControler.dialogRull;

import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogRule_feastIsHostingWedding extends DialogRule_Base {
    boolean feast;
    public DialogRule_feastIsHostingWedding(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return false;
        boolean is = currentFeast.getWeddingCeremonyTarget() != null && !currentFeast.getWeddingCeremonyTarget().isMarried();
        return feast == is;
    }

}
