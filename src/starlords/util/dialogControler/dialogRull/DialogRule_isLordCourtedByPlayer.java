package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

public class DialogRule_isLordCourtedByPlayer  extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_isLordCourtedByPlayer(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == lord.isCourted();
    }
}
