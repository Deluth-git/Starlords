package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogRule_playerFactionMarital extends DialogRule_Base {
    boolean feast;
    public DialogRule_playerFactionMarital(boolean feast){
        this.feast = feast;
    }

    @Override
    public boolean condition(Lord lord) {
        FactionAPI faction = Misc.getCommissionFaction();
        if (faction == null) faction = Global.getSector().getPlayerFaction();
        boolean sameFaction = faction.equals(lord.getFaction());
        if (sameFaction){
            sameFaction = LordController.getPlayerLord().isMarshal();
        }
        return feast == (sameFaction);
    }

}
