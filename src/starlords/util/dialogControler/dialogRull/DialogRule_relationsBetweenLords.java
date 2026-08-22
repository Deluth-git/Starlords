package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.RelationController;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_relationsBetweenLords  extends DialogRule_minmax {
    @SneakyThrows
    public DialogRule_relationsBetweenLords(JSONObject jsonObject,String key){
        super(jsonObject, key);
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (targetLord == null) return 0;
        int rel = (RelationController.getRelation(lord,targetLord));
        return rel;
    }

    @Override
    public boolean condition(Lord lord,Lord targetLord, MarketAPI targetMarket) {
        if (targetLord == null) return false;
        return super.condition(lord, targetLord,targetMarket);
    }
}
