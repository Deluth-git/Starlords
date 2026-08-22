package starlords.util.dialogControler.dialogRull;

import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogRule_isWeddingTarget extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_isWeddingTarget(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return false;
        if (currentFeast.getWeddingCeremonyTarget() == null) return false;
        return isMarried == currentFeast.getWeddingCeremonyTarget().equals(lord);
    }
}
