package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogValue_optionOfPlayerProposal extends DialogValue_base{
    public DialogValue_optionOfPlayerProposal(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        LawProposal proposal = PoliticsController.getProposal(LordController.getPlayerLord());
        if (proposal == null) return 0;
        int rel = PoliticsController.getApproval(lord, proposal, false).one;
        return rel;
    }
}
