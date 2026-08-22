package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.memoryUtils.DataHolder;

import static starlords.util.Constants.STARLORD_ADDITIONAL_MEMORY_KEY;

public class DialogRule_getLordMemoryData_int extends DialogRule_getDialogData_int{
    public DialogRule_getLordMemoryData_int(String key, JSONObject jsonObject) {
        super(key, jsonObject);
    }

    @Override
    protected int getValue(Lord lord,Lord targetLord, MarketAPI targetMarket){
        DataHolder DATA_HOLDER = lord.getLordDataHolder();
        int out = 0;
        out = DATA_HOLDER.getInteger(this.key);
        return out;
    }
}
