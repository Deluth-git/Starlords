package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;

public class DialogValue_lordsInFeast extends DialogValue_base{
    public DialogValue_lordsInFeast(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return 0;
        int rel = currentFeast.getParticipants().size();
        return rel;
    }

}
