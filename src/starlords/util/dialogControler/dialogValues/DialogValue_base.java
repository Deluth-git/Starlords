package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_base {
    int base = 0;
    double multi = 1;
    public DialogValue_base(){

    }
    @SneakyThrows
    public DialogValue_base(JSONObject json){
        if (json.has("multi")) multi = json.getDouble("multi");
        if (json.has("base")) base = json.getInt("base");
    }
    @SneakyThrows
    public DialogValue_base(JSONObject json, String key){
        if (json.get(key) instanceof JSONObject){
            JSONObject json2 = json.getJSONObject(key);
            if (json2.has("multi")) multi = json2.getDouble("multi");
            if (json2.has("base")) base = json2.getInt("base");
            return;
        }
        if (json.get(key) instanceof JSONArray) return;
        multi = json.getDouble(key);
    }
    public int computeValue(Lord lord, Lord targetLord, MarketAPI targetMarket){
        return (int) ((base+value(lord, targetLord,targetMarket))*multi);
    }
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket){
        return value(lord, targetLord);
    }
    public int value(Lord lord, Lord targetLord){
        return 0;
    }
}
