package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_optionOfPlayerProposal extends DialogRule_minmax {
    @SneakyThrows
    public DialogRule_optionOfPlayerProposal(JSONObject jsonObject,String key){
        super(jsonObject, key);
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        LawProposal proposal = PoliticsController.getProposal(LordController.getPlayerLord());
        if (proposal == null) return 0;
        int rel = PoliticsController.getApproval(lord, proposal, false).one;
        return rel;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal proposal = PoliticsController.getProposal(LordController.getPlayerLord());
        if (proposal == null) return false;
        return super.condition(lord);
    }
}
