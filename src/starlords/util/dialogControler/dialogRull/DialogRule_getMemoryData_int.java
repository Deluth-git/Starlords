package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogRule_getMemoryData_int extends DialogRule_getDialogData_int{
    public DialogRule_getMemoryData_int(String key, JSONObject jsonObject) {
        super(key, jsonObject);
    }

    @Override
    protected int getValue(Lord lord,Lord targetLord, MarketAPI targetMarket){
        int out = 0;
        if(Global.getSector().getMemory().contains(key) && Global.getSector().getMemory().get(key) instanceof Integer) out = Global.getSector().getMemory().getInt(key);
        return out;
    }
}
