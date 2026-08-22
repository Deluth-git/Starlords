package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.FactionAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.Utils;

public class DialogValue_playerCommissionedMarketAverageStability extends DialogValue_playerMarketAverageStability{
    public DialogValue_playerCommissionedMarketAverageStability(JSONObject json, String key) {
        super(json, key);
    }
    @Override
    protected FactionAPI getFactionID(Lord lord, Lord targetLord) {
        return Utils.getRecruitmentFaction();
    }
}
