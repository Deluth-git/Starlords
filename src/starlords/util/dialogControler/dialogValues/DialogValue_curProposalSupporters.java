package starlords.util.dialogControler.dialogValues;

import org.json.JSONObject;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogValue_curProposalSupporters extends DialogValue_base{
    public DialogValue_curProposalSupporters(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    public int value(Lord lord, Lord targetLord) {
        LawProposal proposal = PoliticsController.getCurrProposal(lord.getFaction());
        if (proposal == null) return 0;
        int rel = proposal.getSupporters().size();
        return rel;
    }

}
