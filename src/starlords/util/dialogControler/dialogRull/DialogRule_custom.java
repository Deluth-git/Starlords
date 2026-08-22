package starlords.util.dialogControler.dialogRull;

import org.json.JSONObject;
import starlords.person.Lord;

public class DialogRule_custom extends DialogRule_Base {
    public JSONObject json;
    public DialogRule_custom(String id) {
        DialogRule_customList.rules.put(id, this);
    }
    @Deprecated
    @Override
    public boolean condition(Lord lord) {
        return super.condition(lord);
    }
    @Deprecated
    @Override
    public boolean condition(Lord lord, Lord targetLord) {
        return super.condition(lord, targetLord);
    }
}