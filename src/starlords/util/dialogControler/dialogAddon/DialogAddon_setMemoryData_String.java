package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONException;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_setString;

public class DialogAddon_setMemoryData_String extends  DialogAddon_setDialogData_String{
    public DialogAddon_setMemoryData_String(String key, JSONObject json) throws JSONException {
        super(key, json);
    }
    @Override
    protected void applyString(String string, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (time != null) {
            Global.getSector().getMemory().set(key, string,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        Global.getSector().getMemory().set(key, string);
    }
}
