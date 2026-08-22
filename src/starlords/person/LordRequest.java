package starlords.person;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.Getter;
import lombok.Setter;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.ui.RequestIntelPlugin;
import starlords.util.DefectionUtils;

@Getter
public class LordRequest {

    //Request type for when the lord is imprisoned and reaches out to the player to spring him out in exchange for joining him
    public static final String PRISON_BREAK = "prison_break";
    public static final String FIEF_FOR_DEFECTION = "fief_for_defection";
    //TODO: These requests add a new dimension to the mod, making Lords more proactive with regards to the player.
    // They could: challenge the player to a duel, offer to trade ships, offer to defeat common enemy, etc.

    private Lord originator;
    @Setter
    private boolean alive;
    @Setter
    private boolean playerAgreed;
    @Setter
    private MarketAPI fief;
    private final String type;
    private RequestIntelPlugin intelPlugin;

    public LordRequest(String type, Lord origin) {
        originator = origin;
        this.type = type;
        this.playerAgreed = false;
        alive = true;
        intelPlugin =  new RequestIntelPlugin(this);
        Global.getSector().getIntelManager().addIntel(intelPlugin);
    }

    public String getRequestCapitalized(){
        switch(type) {
            case PRISON_BREAK:
                return "Prison Break";
            case FIEF_FOR_DEFECTION:
                return "Fief for Defection";

        }
        return null;
    }

    public String getRequest() {
        switch(type) {
            case PRISON_BREAK:
                return LordRequest.PRISON_BREAK;
            case FIEF_FOR_DEFECTION:
                return LordRequest.FIEF_FOR_DEFECTION;
        }
        return null;
    }

    public void setAlive(boolean life){
        this.alive = life;
        if (life == false) {
            intelPlugin.endImmediately();
            this.intelPlugin = null;
            switch (this.getType()){
                case LordRequest.FIEF_FOR_DEFECTION:
                    if (fief == null) {
                        DefectionUtils.performDefection(this.getOriginator());
                    }
                    else {
                        DefectionUtils.performDefection(this.getOriginator(), LordController.getPlayerLord().getFaction(), true);
                        FiefController.playerTransferFief(this.getOriginator(), fief);
                    }
                    break;
            }
        }


    }

    public boolean hasPlayerAgreed(){
        return this.playerAgreed;
    }

	public void updateReferences() {
        if (originator != null) {
            originator = LordController.getLordOrPlayerById(originator.getLordAPI().getId());
        }
    }

}
