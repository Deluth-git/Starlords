package starlords.util.dialogControler.dialogRull;

import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogRule_isSwayed extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_isSwayed(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == lord.isSwayed();
    }
}
