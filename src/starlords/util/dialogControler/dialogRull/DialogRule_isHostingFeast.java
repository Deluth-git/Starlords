package starlords.util.dialogControler.dialogRull;

import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogRule_isHostingFeast extends DialogRule_Base {
    boolean feast;
    public DialogRule_isHostingFeast(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        boolean check = (lord.getCurrAction() == LordAction.FEAST);
        if (!check || EventController.getCurrentFeast(lord.getLordAPI().getFaction()) == null || EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator() == null) return feast == check;
        return feast == EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator().equals(lord);
    }
}
