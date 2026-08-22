package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.ai.utils.TargetUtils;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.util.Utils;

public class DialogRule_marketIsValidTarget extends DialogRule_Base{
    boolean bol;
    @SneakyThrows
    public DialogRule_marketIsValidTarget(JSONObject json, String key){
        bol = json.getBoolean(key);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        LordEvent campaign = EventController.getCurrentCampaign(lord.getFaction());
        if (campaign != null && campaign.isAlive() && campaign.getOriginator().equals(lord)){
            return TargetUtils.canBeAttackedByCampaign(lord,market);
        }
        return TargetUtils.canBeAttackedByLord(lord,market);
    }
}
