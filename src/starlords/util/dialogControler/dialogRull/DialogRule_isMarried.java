package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

public class DialogRule_isMarried extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_isMarried(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == lord.isMarried();
    }
}
