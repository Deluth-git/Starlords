package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogRule_marketPlayerFaction extends DialogRule_Base{
    boolean data;
    @SneakyThrows
    public DialogRule_marketPlayerFaction(JSONObject json, String key){
        data = json.getBoolean(key);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        FactionAPI faction = Misc.getCommissionFaction();
        return market.getFaction().equals(faction) == data;
    }
}
