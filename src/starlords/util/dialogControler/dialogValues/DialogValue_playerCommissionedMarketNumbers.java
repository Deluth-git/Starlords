package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.FactionAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.Utils;

public class DialogValue_playerCommissionedMarketNumbers extends DialogValue_playerMarketNumbers{
    public DialogValue_playerCommissionedMarketNumbers(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    protected FactionAPI getFactionID(Lord lord, Lord targetLord) {
        return Utils.getRecruitmentFaction();
    }
}
