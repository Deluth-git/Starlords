package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_lordsInFeast extends DialogRule_minmax {
    @SneakyThrows
    public DialogRule_lordsInFeast(JSONObject jsonObject,String key){
        super(jsonObject, key);
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return 0;
        int rel = currentFeast.getParticipants().size();
        return rel;
    }

    @Override
    public boolean condition(Lord lord) {
        boolean isFeast = lord.getCurrAction() == LordAction.FEAST;
        LordEvent currentFeast = isFeast ? EventController.getCurrentFeast(lord.getLordAPI().getFaction()) : null;
        if (currentFeast == null) return false;
        return super.condition(lord);
    }
}
