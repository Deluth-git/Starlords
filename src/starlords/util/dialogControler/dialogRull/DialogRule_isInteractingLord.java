package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

public class DialogRule_isInteractingLord extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_isInteractingLord(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord) {
        if (targetLord == null) return false;
        boolean bool = lord.equals(targetLord);
        return isMarried == bool;
    }
}
