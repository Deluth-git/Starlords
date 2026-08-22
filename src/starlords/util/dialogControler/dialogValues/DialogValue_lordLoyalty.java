package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.controllers.RelationController;
import starlords.person.Lord;

public class DialogValue_lordLoyalty extends DialogValue_base{
    public DialogValue_lordLoyalty(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        int rel = RelationController.getLoyalty(lord);
        return rel;
    }

}
