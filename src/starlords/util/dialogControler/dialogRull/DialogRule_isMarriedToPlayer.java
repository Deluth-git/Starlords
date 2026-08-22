package starlords.util.dialogControler.dialogRull;

import starlords.controllers.LordController;
import starlords.person.Lord;

public class DialogRule_isMarriedToPlayer extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_isMarriedToPlayer(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == LordController.getPlayerLord().getLordAPI().getId().equals(lord.getSpouse());
    }
}
