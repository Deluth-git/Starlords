package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_marketFactionRelationToTargetLord extends DialogRule_minmax {
    public DialogRule_marketFactionRelationToTargetLord(JSONObject jsonObject, String key) {
        super(jsonObject, key);
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return (int) (lord.getFaction().getRelationship(targetMarket.getFaction().getId())*100);
    }
}
