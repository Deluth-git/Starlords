package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.HashMap;
import java.util.Iterator;

public class DialogRule_customList extends DialogRule_Base {
    public static HashMap<String, DialogRule_custom> rules = new HashMap<>();
    public HashMap<String, JSONObject> data = new HashMap<>();
    @SneakyThrows
    public DialogRule_customList(JSONObject json, String key){
        json = json.getJSONObject(key);
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            data.put(key2,json.getJSONObject(key2));
        }
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        for (String key : data.keySet()){
            DialogRule_custom item = rules.get(key);
            item.json = data.get(key);
            boolean temp = item.condition(lord, targetLord, market);
            item.json = null;
            if (!temp)return false;
        }
        return true;
    }
}
