package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogValue_playerFleetDP extends DialogValue_base{
    public DialogValue_playerFleetDP(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        return getFleet(lord, targetLord).getFleetPoints();
    }
    protected CampaignFleetAPI getFleet(Lord lord, Lord targetLord){
        return Global.getSector().getPlayerFleet();
    }
}
