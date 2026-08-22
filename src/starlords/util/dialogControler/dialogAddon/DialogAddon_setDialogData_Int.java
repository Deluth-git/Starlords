package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_changeValue;
import starlords.util.dialogControler.dialogValues.DialogValue_base;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogAddon_setDialogData_Int extends DialogAddon_changeValue {
    protected DialogValuesList time = null;
    protected String key;
    @SneakyThrows
    public DialogAddon_setDialogData_Int(String key, JSONObject json){
        this.key=key;
        if (json.get(key) instanceof JSONObject && json.getJSONObject(key).has("time")){
            json = json.getJSONObject(key);
            time = new DialogValuesList(json,"time");
            this.init(json,"data");
            return;
        }
        this.init(json, key);
    }

    @Override
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return LordInteractionDialogPluginImpl.DATA_HOLDER.getInteger(key);
    }
    protected void change(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){
        int data = LordInteractionDialogPluginImpl.DATA_HOLDER.getInteger(key);
        data = data+value;
        if (time != null) {
            LordInteractionDialogPluginImpl.DATA_HOLDER.setInteger(key, data + value,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        LordInteractionDialogPluginImpl.DATA_HOLDER.setInteger(key, data + value);
    }
    @Override
    protected void decreaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        change(value, textPanel, options, dialog, lord, targetLord, targetMarket);
    }

    @Override
    protected void increaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        change(value, textPanel, options, dialog, lord, targetLord, targetMarket);
    }
}
