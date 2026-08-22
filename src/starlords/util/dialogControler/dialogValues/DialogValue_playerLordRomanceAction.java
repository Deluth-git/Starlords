package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_playerLordRomanceAction extends DialogValue_base{
    public DialogValue_playerLordRomanceAction(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        int rel = lord.getRomanticActions();
        return rel;
    }

}
