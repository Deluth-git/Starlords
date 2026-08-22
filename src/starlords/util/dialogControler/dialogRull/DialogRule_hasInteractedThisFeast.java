package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogRule_hasInteractedThisFeast extends DialogRule_Base{
    boolean bol;
    @SneakyThrows
    public DialogRule_hasInteractedThisFeast(JSONObject json, String key){
        bol = json.getBoolean(key);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        return bol == lord.isFeastInteracted();
    }
}
