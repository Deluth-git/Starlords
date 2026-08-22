package starlords.util.dialogControler.dialogInsert;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;

public class DialogInsert_memory extends DialogInsert_Base{
    String key2;
    @SneakyThrows
    public DialogInsert_memory(JSONObject json){
        key = json.getString("replaced");
        key2 = json.getString("memory");
    }

    @Override
    public String getInsertedData(String line, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        String out = "";
        if(Global.getSector().getMemory().contains(key) && Global.getSector().getMemory().get(key) instanceof String) out = Global.getSector().getMemory().getString(key);
        return out;
    }
}
