package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class DialogRule_marketFiefBelongsToPlayer extends DialogRule_Base {
    boolean data;
    @SneakyThrows
    public DialogRule_marketFiefBelongsToPlayer(JSONObject json, String key){
        data = json.getBoolean(key);
    }
    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        return FiefController.getOwner(market).equals(LordController.getPlayerLord()) == data;
    }
}
