package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_playerLevel extends DialogValue_base{
    public DialogValue_playerLevel(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        return Global.getSector().getPlayerStats().getLevel();
    }

}
