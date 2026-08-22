package starlords.util.dialogControler.dialogInsert;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.HashMap;
import java.util.Iterator;

public class DialogInsert_customList extends DialogInsert_Base{
    public static HashMap<String, DialogInsert_custom> inserts = new HashMap<>();
    public HashMap<String, JSONObject> data = new HashMap<>();
    @SneakyThrows
    public DialogInsert_customList(JSONObject json){
        key = json.getString("replaced");
        json = json.getJSONObject("customDialogInsert");
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            data.put(key2,json.getJSONObject(key2));
        }
    }

    @Override
    public String getInsertedData(String line, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        StringBuilder out = new StringBuilder();
        for (String key : data.keySet()){
            DialogInsert_custom item = inserts.get(key);
            item.json = data.get(key);
            out.append(item.getInsertedData(line, lord, targetLord, targetMarket));
            item.json = null;
        }
        return out.toString();
    }
}
