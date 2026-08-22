package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;

public class DialogValue_limitedValue extends DialogValue_base{
    private int max = 2147483647;
    private int min = -2147483647;
    private DialogValuesList maxList;
    private DialogValuesList minList;
    DialogValuesList value;
    public DialogValue_limitedValue(JSONObject json) {
        super(json);
        if (json.has("value"))value = new DialogValuesList(json,"value");
        if (json.has("max")){
            maxList = new DialogValuesList(json,"max");
        }
        if (json.has("min")){
            minList = new DialogValuesList(json,"min");
        }
        if ((!json.has("min") && !json.has("max"))){
            minList = new DialogValuesList(json,"max");
            maxList = minList;
        }
    }
    @Override
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int value = 0;
        if (this.value != null)value = this.value.getValue(lord, targetLord,targetMarket);
        int max = this.max;
        if (maxList != null) max = maxList.getValue(lord, targetLord,targetMarket);
        int min = this.min;
        if (minList != null) min = minList.getValue(lord, targetLord,targetMarket);
        value = Math.max(min,value);
        value = Math.min(max,value);
        return value;
    }
}
