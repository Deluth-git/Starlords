package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogRule_LordAndTargetAtSameFeast extends DialogRule_Base{
    boolean bol;
    @SneakyThrows
    public DialogRule_LordAndTargetAtSameFeast(JSONObject json, String key){
        bol = json.getBoolean(key);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        if (lord.getCurrAction() != LordAction.FEAST || targetLord.getCurrAction() != LordAction.FEAST) return false;
        return bol == EventController.getCurrentFeast(lord.getLordAPI().getFaction()).equals(EventController.getCurrentFeast(targetLord.getLordAPI().getFaction()));
    }
}
