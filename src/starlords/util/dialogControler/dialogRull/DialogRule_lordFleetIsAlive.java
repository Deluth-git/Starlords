package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

public class DialogRule_lordFleetIsAlive extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_lordFleetIsAlive(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == (lord.getFleet() != null && lord.getFleet().isAlive());
    }
}
