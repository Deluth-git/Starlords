package starlords.util.dialogControler.dialogRull.bases;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogRule_minmax extends DialogRule_Base {
    private int max = 2147483647;
    private int min = -2147483647;
    private DialogValuesList maxList;
    private DialogValuesList minList;
    @SneakyThrows
    public DialogRule_minmax(JSONObject jsonObject,String key){
        if (!(jsonObject.get(key) instanceof JSONObject)){
            int value = jsonObject.getInt(key);
            max=value;
            min=value;
            return;
        }
        JSONObject json2 = jsonObject.getJSONObject(key);
        if (json2.has("max")){
            maxList = new DialogValuesList(json2,"max");
        }
        if (json2.has("min")){
            minList = new DialogValuesList(json2,"min");
        }
        if ((!json2.has("min") && !json2.has("max"))){
            minList = new DialogValuesList(jsonObject,key);
            maxList = minList;
        }
    }
    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int rel = getValue(lord, targetLord,targetMarket);
        int max = this.max;
        if (maxList != null) max = maxList.getValue(lord, targetLord,targetMarket);
        int min = this.min;
        if (minList != null) min = minList.getValue(lord, targetLord,targetMarket);
        if (min <= rel && rel <= max) return true;
        return false;
    }

    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket){
        return 0;
    }
}
