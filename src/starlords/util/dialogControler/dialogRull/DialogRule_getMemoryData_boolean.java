package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import starlords.person.Lord;

public class DialogRule_getMemoryData_boolean extends DialogRule_getDialogData_boolean{
    public DialogRule_getMemoryData_boolean(String key, boolean feast) {
        super(key, feast);
    }

    @Override
    protected boolean getBoolean(Lord lord) {
        boolean out = false;
        if(Global.getSector().getMemory().contains(key) && Global.getSector().getMemory().get(key) instanceof Boolean) out = Global.getSector().getMemory().getBoolean(key);
        return out;
    }
}
