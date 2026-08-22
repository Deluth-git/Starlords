package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_lordFleetDP extends DialogValue_playerFleetDP{
    public DialogValue_lordFleetDP(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    protected CampaignFleetAPI getFleet(Lord lord, Lord targetLord) {
        return lord.getFleet();
    }
}
