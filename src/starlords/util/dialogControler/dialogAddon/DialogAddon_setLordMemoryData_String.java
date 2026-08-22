package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONException;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.memoryUtils.DataHolder;

public class DialogAddon_setLordMemoryData_String extends DialogAddon_setDialogData_String{
    public DialogAddon_setLordMemoryData_String(String key, JSONObject json) throws JSONException {
        super(key, json);
    }

    @Override
    protected void applyString(String string, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        DataHolder DATA_HOLDER = lord.getLordDataHolder();
        if (time != null) {
            DATA_HOLDER.setString(key,string,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        DATA_HOLDER.setString(key,string);
    }
}
