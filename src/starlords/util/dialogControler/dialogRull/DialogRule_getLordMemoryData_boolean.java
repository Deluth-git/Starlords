package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import starlords.person.Lord;
import starlords.util.memoryUtils.DataHolder;

import static starlords.util.Constants.STARLORD_ADDITIONAL_MEMORY_KEY;

public class DialogRule_getLordMemoryData_boolean extends DialogRule_getDialogData_boolean{
    public DialogRule_getLordMemoryData_boolean(String key, boolean feast) {
        super(key, feast);
    }

    @Override
    protected boolean getBoolean(Lord lord) {
        DataHolder DATA_HOLDER = lord.getLordDataHolder();

        boolean out = false;
        out = DATA_HOLDER.getBoolean(this.key);
        return out;
    }
}
