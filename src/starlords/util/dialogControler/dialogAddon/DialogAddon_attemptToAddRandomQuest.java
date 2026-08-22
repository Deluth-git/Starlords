package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.impl.campaign.rulecmd.BeginMission;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.LordController;
import starlords.controllers.QuestController;
import starlords.person.Lord;
import starlords.ui.MissionPreviewIntelPlugin;
import starlords.util.dialogControler.DialogSet;

import java.util.ArrayList;
import java.util.HashMap;

import static com.fs.starfarer.api.impl.campaign.rulecmd.BeginMission.TEMP_MISSION_KEY;

public class DialogAddon_attemptToAddRandomQuest extends DialogAddon_Base{
    public DialogAddon_attemptToAddRandomQuest(){

    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord) {
        boolean questGiven = false;
        CampaignFleetAPI lordFleet = (CampaignFleetAPI) dialog.getInteractionTarget();

        //lordsReference = new HashMap<>();
        targetLord = LordController.getLordById(lordFleet.getCommander().getId());
        if (!QuestController.isQuestGiven(targetLord)) {
            MarketAPI tmp = lordFleet.getMarket();
            MarketAPI tmp2 = targetLord.getLordAPI().getMarket();
            SectorEntityToken marketEntity = targetLord.getClosestBase();
            MarketAPI newMarket;
            if (marketEntity != null) {
                newMarket = marketEntity.getMarket();
            } else {  // last resort, just make it some market for now
                newMarket = Global.getSector().getEconomy().getMarketsCopy().get(0);
            }
            lordFleet.setMarket(newMarket);
            targetLord.getLordAPI().setMarket(newMarket);
            ArrayList<Misc.Token> params = new ArrayList<>();
            params.add(new Misc.Token(QuestController.getQuestId(targetLord), Misc.TokenType.LITERAL));
            params.add(new Misc.Token("false", Misc.TokenType.LITERAL));
            new BeginMission().execute("", dialog, params, new HashMap<String, MemoryAPI>());
            BaseHubMission mission = (BaseHubMission) Global.getSector().getMemoryWithoutUpdate().get(TEMP_MISSION_KEY);
            if (mission != null && !(mission instanceof BaseCustomBounty)) {  // TODO bounties dont seem to work
                MissionPreviewIntelPlugin intel = new MissionPreviewIntelPlugin(mission);
                Global.getSector().getIntelManager().addIntel(intel);
                questGiven = true;
                DialogSet.addParaWithInserts("quest_available",lord,targetLord,null,textPanel,options,dialog);
                DialogSet.addParaWithInserts("addedRandomIntel",lord,targetLord,null,textPanel,options,dialog);
            }
            QuestController.setQuestGiven(targetLord, true);
            lordFleet.setMarket(tmp);
            targetLord.getLordAPI().setMarket(tmp2);
        }

        if (!questGiven) {
            DialogSet.addParaWithInserts("no_quest_available",lord,targetLord,null,textPanel,options,dialog);
        }
    }
}
