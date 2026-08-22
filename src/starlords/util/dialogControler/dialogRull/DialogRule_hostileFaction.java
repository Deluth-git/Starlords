package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;
import starlords.person.Lord;

public class DialogRule_hostileFaction extends DialogRule_Base {
    boolean hostile;
    public DialogRule_hostileFaction(boolean hostile){
        this.hostile = hostile;
    }

    @Override
    public boolean condition(Lord lord) {
        FactionAPI faction = Misc.getCommissionFaction();
        if (faction == null) faction = Global.getSector().getPlayerFaction();
        boolean hostile = lord.getFaction().isHostileTo(faction);
        return this.hostile == hostile;
    }
}
