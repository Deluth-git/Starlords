package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONException;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_setString;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogAddon_setDialogData_String extends DialogAddon_setString {
    DialogValuesList time = null;
    String key;
    public DialogAddon_setDialogData_String(String key, JSONObject json) throws JSONException {
        this.key=key;
        if (json.get(key) instanceof JSONObject && json.getJSONObject(key).has("time")){
            json = json.getJSONObject(key);
            time = new DialogValuesList(json, "time");
            init(json,"data");
        }
        init(json, key);
    }

    @Override
    protected void applyString(String string, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (time != null) {
            LordInteractionDialogPluginImpl.DATA_HOLDER.setString(key, string,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        LordInteractionDialogPluginImpl.DATA_HOLDER.setString(key, string);
    }
}