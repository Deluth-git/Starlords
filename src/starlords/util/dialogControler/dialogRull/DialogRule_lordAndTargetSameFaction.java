package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

public class DialogRule_lordAndTargetSameFaction extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_lordAndTargetSameFaction(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord,Lord targetLord) {
        if (targetLord == null) return false;
        boolean bool = lord.getFaction().equals(targetLord.getFaction());
        return isMarried == bool;
    }
}
