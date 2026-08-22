package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.FactionAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_lordMarketNumbers extends DialogValue_playerMarketNumbers{
    public DialogValue_lordMarketNumbers(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    protected FactionAPI getFactionID(Lord lord, Lord targetLord) {
        return lord.getFaction();
    }
}
