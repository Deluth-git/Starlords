package starlords.util.dialogControler.dialogInsert;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;

public class DialogInsert_dialogData extends DialogInsert_Base{
    String key2;
    @SneakyThrows
    public DialogInsert_dialogData(JSONObject json){
        key = json.getString("replaced");
        key2 = json.getString("DialogData");
    }

    @Override
    public String getInsertedData(String line, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return LordInteractionDialogPluginImpl.DATA_HOLDER.getString(key2);
    }
}
