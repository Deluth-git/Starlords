package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_playerHasCommodity extends DialogRule_minmax {
    String item;
    @SneakyThrows
    public DialogRule_playerHasCommodity(JSONObject jsonObject,String key){
        super(jsonObject, key);
        item = key;
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int rel = (int) Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(item);
        return rel;
    }
}
