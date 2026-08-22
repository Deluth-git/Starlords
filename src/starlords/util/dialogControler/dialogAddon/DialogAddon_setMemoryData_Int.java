package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogAddon_setMemoryData_Int extends DialogAddon_setDialogData_Int{
    public DialogAddon_setMemoryData_Int(String key, JSONObject json) {
        super(key, json);
    }

    @Override
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (!Global.getSector().getMemory().contains(key)) return 0;
        return Global.getSector().getMemory().getInt(key);
    }

    @Override
    protected void change(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (Global.getSector().getMemory().contains(key)) value += Global.getSector().getMemory().getInt(key);
        if (time != null) {
            Global.getSector().getMemory().set(key, value,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        Global.getSector().getMemory().set(key, value);
    }
}
