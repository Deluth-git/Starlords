package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_MemoryData extends DialogValue_base{
    String key;
    public DialogValue_MemoryData(JSONObject json, String key) {
        super(json, key);
        this.key = key;

    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        int out = 0;
        if(Global.getSector().getMemory().contains(key) && Global.getSector().getMemory().get(key) instanceof Integer) out = Global.getSector().getMemory().getInt(key);
        return out;
    }

}
