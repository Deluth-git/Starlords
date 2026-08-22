package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_playerWealth extends DialogValue_base{
    public DialogValue_playerWealth(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        int playerCredits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        return playerCredits;
    }

}
