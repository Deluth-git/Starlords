package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.ai.utils.TargetUtils;
import starlords.person.Lord;

public class DialogRule_isValidMarket extends DialogRule_Base {
    boolean bol;
    @SneakyThrows
    public DialogRule_isValidMarket(JSONObject json, String key){
        bol = json.getBoolean(key);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        if (market == null) return false;
        return TargetUtils.isValidMarket(market);
    }
}
