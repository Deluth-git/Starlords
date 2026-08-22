package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import starlords.person.Lord;

public class DialogRule_willEngage extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_willEngage(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        boolean willEngage = false;
        if (lord.getFaction().isHostileTo(Global.getSector().getPlayerFleet().getFaction())) {
            if (lord.getFleet() != null) {
                SectorEntityToken target = ((ModularFleetAIAPI) lord.getFleet().getAI()).getTacticalModule().getTarget();
                if (lord.getFleet().isHostileTo(Global.getSector().getPlayerFleet()) && Global.getSector().getPlayerFleet().equals(target)) {
                    willEngage = true;
                }
            }
        }
        return isMarried == willEngage;
    }
}
