package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogAddon.DialogAddon_customList;

public class DialogValue_custom extends DialogValue_base  {
    public JSONObject json;
    public DialogValue_custom(String id) {
        DialogValue_customList.values.put(id, this);
    }
    @Deprecated
    @Override
    public int value(Lord lord, Lord targetLord) {
        return super.value(lord, targetLord);
    }
}
