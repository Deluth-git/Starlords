package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;
import starlords.person.Lord;

public class DialogRule_lordHasLiege extends DialogRule_Base {
    boolean isMarried;
    public DialogRule_lordHasLiege(boolean isMarried){
        this.isMarried = isMarried;
    }

    @Override
    public boolean condition(Lord lord) {
        return isMarried == (lord.getLiegeName() != null);
    }
}
