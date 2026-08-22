package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.HashMap;
import java.util.Iterator;

public class DialogValue_customList extends DialogValue_base {
    public static HashMap<String, DialogValue_custom> values = new HashMap<>();
    public HashMap<String, JSONObject> data = new HashMap<>();
    @SneakyThrows
    public DialogValue_customList(JSONObject json, String key){
        super(json, key);
        json = json.getJSONObject(key);
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            data.put(key2,json.getJSONObject(key2));
        }
    }

    @Override
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int value = 0;
        for (String key : data.keySet()){
            DialogValue_custom item = values.get(key);
            item.json = data.get(key);
            value += item.computeValue(lord, targetLord, targetMarket);
            item.json = null;
        }
        return value;
    }
}
