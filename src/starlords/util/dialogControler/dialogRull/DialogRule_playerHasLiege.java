package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.LordController;
import starlords.person.Lord;

public class DialogRule_playerHasLiege extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_playerHasLiege(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == (LordController.getPlayerLord().getLiegeName() != null);
    }
}
