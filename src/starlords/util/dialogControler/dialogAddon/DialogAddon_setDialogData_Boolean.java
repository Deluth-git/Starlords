package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_setBoolean;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogAddon_setDialogData_Boolean extends DialogAddon_setBoolean {
    DialogValuesList time = null;
    String key;
    @SneakyThrows
    public DialogAddon_setDialogData_Boolean(String key, JSONObject json) {
        this.key = key;
        if (json.get(key) instanceof JSONObject && json.has("time")){
            json = json.getJSONObject(key);
            time = new DialogValuesList(json,"time");
            init(json,"data");
            return;
        }
        init(json, key);
    }

    @Override
    protected void applyBoolean(boolean bol, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (time != null) {
            LordInteractionDialogPluginImpl.DATA_HOLDER.setBoolean(key, bol,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        LordInteractionDialogPluginImpl.DATA_HOLDER.setBoolean(key, bol);
    }
}
