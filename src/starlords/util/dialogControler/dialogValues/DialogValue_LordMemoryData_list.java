package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogValue_LordMemoryData_list extends DialogValue_base{
    ArrayList<DialogValue_base> values = new ArrayList<>();
    @SneakyThrows
    public DialogValue_LordMemoryData_list(JSONObject json, String key) {
        super(json, key);
        JSONObject json2 = json.getJSONObject(key);
        for (Iterator it2 = json2.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            //if (!(json2.get(key2) instanceof JSONObject)) continue;
            values.add(new DialogValue_LordMemoryData(json2,key2));
        }
    }

    @Override
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int totalValue = 0;
        for (DialogValue_base a : values){
            totalValue+= a.computeValue(lord,targetLord,targetMarket);
        }
        return totalValue;
    }

}
