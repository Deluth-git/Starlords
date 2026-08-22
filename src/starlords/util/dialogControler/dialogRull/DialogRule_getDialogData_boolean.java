package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;

public class DialogRule_getDialogData_boolean extends DialogRule_Base {
    boolean feast;
    String key;
    public DialogRule_getDialogData_boolean(String key,boolean feast){
        this.key = key;
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        return feast == getBoolean(lord);
    }
    protected boolean getBoolean(Lord lord){
        boolean data = false;
        data = LordInteractionDialogPluginImpl.DATA_HOLDER.getBoolean(key);
        return data;
    }
}
