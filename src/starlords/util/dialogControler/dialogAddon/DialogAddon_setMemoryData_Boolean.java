package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogAddon_setMemoryData_Boolean extends DialogAddon_setDialogData_Boolean{
    public DialogAddon_setMemoryData_Boolean(String key, JSONObject json) {
        super(key, json);
    }

    @Override
    protected void applyBoolean(boolean bol, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (time != null) {
            Global.getSector().getMemory().set(key, bol,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        Global.getSector().getMemory().set(key, bol);
    }
}
