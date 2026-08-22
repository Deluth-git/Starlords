package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_lordWealth extends DialogValue_base{
    public DialogValue_lordWealth(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        return (int) lord.getWealth();
    }

}
