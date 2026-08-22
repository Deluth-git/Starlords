package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.memoryUtils.DataHolder;

import static starlords.util.Constants.STARLORD_ADDITIONAL_MEMORY_KEY;

public class DialogValue_LordMemoryData extends DialogValue_base{
    String key;
    public DialogValue_LordMemoryData(JSONObject json, String key) {
        super(json, key);
        this.key=key;
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        DataHolder DATA_HOLDER = lord.getLordDataHolder();
        return DATA_HOLDER.getInteger(this.key);
    }

}
