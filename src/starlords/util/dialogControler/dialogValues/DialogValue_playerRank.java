package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class DialogValue_playerRank extends DialogValue_base{
    public DialogValue_playerRank(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        int rel = LordController.getPlayerLord().getRanking();
        return rel;
    }

}
