package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogRule_playerSubject extends DialogRule_Base {
    boolean feast;
    public DialogRule_playerSubject(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        FactionAPI faction = Misc.getCommissionFaction();
        if (faction == null) faction = Global.getSector().getPlayerFaction();
        boolean sameFaction = faction.equals(lord.getFaction());

        return feast == (lord.getFaction().isPlayerFaction() && sameFaction);
    }

}
