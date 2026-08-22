package starlords.util.dialogControler.dialogRull;

import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogRule_firstMeeting extends DialogRule_Base {
    boolean feast;
    public DialogRule_firstMeeting(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {

        return feast == !lord.isKnownToPlayer();
    }
}
