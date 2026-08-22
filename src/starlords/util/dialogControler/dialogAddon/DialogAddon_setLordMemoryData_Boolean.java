package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.memoryUtils.DataHolder;

public class DialogAddon_setLordMemoryData_Boolean extends DialogAddon_setDialogData_Boolean{
    public DialogAddon_setLordMemoryData_Boolean(String key, JSONObject json) {
        super(key, json);
    }

    @Override
    protected void applyBoolean(boolean bol, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        DataHolder DATA_HOLDER = lord.getLordDataHolder();
        if (time != null) {
            DATA_HOLDER.setBoolean(key,bol,time.getValue(lord, targetLord, targetMarket));
            return;
        }
        DATA_HOLDER.setBoolean(key, bol);
    }
}
