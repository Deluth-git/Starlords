package starlords.util.dialogControler.dialogRull;

import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.person.Lord;

public class DialogRule_curProposalLordSupports extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_curProposalLordSupports(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        LawProposal proposal = PoliticsController.getCurrProposal(lord.getFaction());
        if (proposal == null) return false;
        boolean supports = proposal.getSupporters().contains(lord.getLordAPI().getId());
        return isMarried == supports;
    }}
