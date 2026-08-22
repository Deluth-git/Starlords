package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_marketFactionRelationToPlayer extends DialogRule_minmax {
    public DialogRule_marketFactionRelationToPlayer(JSONObject jsonObject, String key) {
        super(jsonObject, key);
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        FactionAPI faction = Misc.getCommissionFaction();
        return (int) faction.getRelationship(targetMarket.getFaction().getId());
    }
}
