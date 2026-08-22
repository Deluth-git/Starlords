package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.HashMap;
import java.util.Iterator;

public class DialogAddon_customList extends DialogAddon_Base{
    public static HashMap<String,DialogAddon_custom> addons = new HashMap<>();
    public HashMap<String, JSONObject> data = new HashMap<>();
    @SneakyThrows
    public DialogAddon_customList(JSONObject json, String key){
        json = json.getJSONObject(key);
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            data.put(key2,json.getJSONObject(key2));
        }
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        for (String key : data.keySet()){
            DialogAddon_custom item = addons.get(key);
            item.json = data.get(key);
            item.apply(textPanel, options, dialog, lord, targetLord, targetMarket);
            item.json = null;
        }
    }
}
