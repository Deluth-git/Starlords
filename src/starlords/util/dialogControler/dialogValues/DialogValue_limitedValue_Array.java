package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.ArrayList;

public class DialogValue_limitedValue_Array extends DialogValue_base{
    ArrayList<DialogValue_limitedValue> values = new ArrayList<>();
    @SneakyThrows
    public DialogValue_limitedValue_Array(JSONObject json, String key) {
        super(json, key);
        JSONArray json2 = json.getJSONArray(key);
        for (int a = 0; a < json2.length(); a++){
            values.add(new DialogValue_limitedValue(json2.getJSONObject(a)));
        }
    }
    @Override
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int value = 0;
        for (DialogValue_limitedValue a : values){
            value += a.computeValue(lord, targetLord,targetMarket);
        }
        return value;
    }
}
