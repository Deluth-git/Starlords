package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.ai.LordAI;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogAddon_AICommand extends DialogAddon_Base{
    String order;
    @SneakyThrows
    public DialogAddon_AICommand(JSONObject json, String key){
        order = json.getString(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        /*          END_CURRENT_ACTIONS
                *          FOLLOW_PLAYER_FLEET
                *          FOLLOW_TARGET_LORD
                *          RAID_TARGET_MARKET: "marketID"
                *          UPGRADE:
         *          PATROL_TARGET_MARKET
                *          ORGANIZE_CAMPAIGN
                *          JOIN_CAMPAIGN*/
        switch (order){
            case "END_CURRENT_ACTIONS":
                END_CURRENT_ACTIONS(lord);
                break;
            case "FOLLOW_PLAYER_FLEET":
                FOLLOW_PLAYER_FLEET(lord);
                break;
            case "FOLLOW_TARGET_LORD":
                FOLLOW_TARGET_LORD(lord,targetLord);
                break;
            case "RAID_TARGET_MARKET":
                RAID_TARGET_MARKET(lord,targetMarket);
                break;
            case "UPGRADE":
                UPGRADE(lord);
                break;
            case "PATROL_TARGET_MARKET":
                PATROL_TARGET_MARKET(lord,targetMarket);
                break;
            case "ORGANIZE_CAMPAIGN":
                ORGANIZE_CAMPAIGN(lord);
                break;
            case "JOIN_CAMPAIGN":
                JOIN_CAMPAIGN(lord);
                break;
        }
    }
    private void END_CURRENT_ACTIONS(Lord lord){
        lord.setCurrAction(null);
        if (lord.getFleet() == null)return;
        lord.getFleet().clearAssignments();
    }
    private void FOLLOW_PLAYER_FLEET(Lord lord){
        LordAI.playerOrder(lord, LordAction.FOLLOW, Global.getSector().getPlayerFleet());
    }
    private void FOLLOW_TARGET_LORD(Lord lord,Lord targetLord){
        if (targetLord == null || targetLord.getFleet() == null) return;
        LordAI.playerOrder(lord, LordAction.FOLLOW, targetLord.getFleet());
    }
    private void RAID_TARGET_MARKET(Lord lord,MarketAPI market){
        if (market == null) return;
        SectorEntityToken raidTarget = market.getPrimaryEntity();
        LordAI.playerOrder(lord, LordAction.RAID_TRANSIT, raidTarget);

    }
    private void UPGRADE(Lord lord){
        LordAI.playerOrder(lord, LordAction.UPGRADE_FLEET_TRANSIT, null);
    }
    private void PATROL_TARGET_MARKET(Lord lord,MarketAPI market){
        if (market == null) return;
        SectorEntityToken patrolTarget = market.getPrimaryEntity();
        LordAI.playerOrder(lord, LordAction.PATROL_TRANSIT, patrolTarget);

    }
    private void ORGANIZE_CAMPAIGN(Lord lord){

    }
    private void JOIN_CAMPAIGN(Lord lord){

    }
}
