package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogRule_getMemoryData_string extends DialogRule_getDialogData_string{
    public DialogRule_getMemoryData_string(String key, JSONObject json) {
        super(key, json);
    }

    @Override
    protected String getString(Lord lord) {
        String out = "";
        if(Global.getSector().getMemory().contains(key) && Global.getSector().getMemory().get(key) instanceof String) out = Global.getSector().getMemory().getString(key);
        return out;
    }
}
