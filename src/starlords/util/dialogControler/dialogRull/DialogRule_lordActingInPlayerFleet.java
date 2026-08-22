package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogRule_lordActingInPlayerFleet extends DialogRule_Base {
    boolean feast;
    public DialogRule_lordActingInPlayerFleet(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        return feast == (lord.getCurrAction() == LordAction.COMPANION);
    }
}
