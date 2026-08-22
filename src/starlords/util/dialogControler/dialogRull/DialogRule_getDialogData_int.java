package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_getDialogData_int extends DialogRule_minmax {
    String key;
    @SneakyThrows
    public DialogRule_getDialogData_int(String key,JSONObject jsonObject){
        super(jsonObject, key);
        this.key = key;
    }
    @Override
    protected int getValue(Lord lord,Lord targetLord, MarketAPI targetMarket){
        int rel = 0;
        rel = LordInteractionDialogPluginImpl.DATA_HOLDER.getInteger(key);
        return rel;
    }
}
