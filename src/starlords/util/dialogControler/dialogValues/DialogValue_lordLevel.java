package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_lordLevel extends DialogValue_base{
    public DialogValue_lordLevel(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        return lord.getLordAPI().getStats().getLevel();
    }

}
