package plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.SectorAPI;
import controllers.*;
import ai.LordAI;
import listeners.BattleListener;
import listeners.MarketStateChangeListener;
import listeners.MarketStateChangeNexListener;
import listeners.MonthlyUpkeepListener;
import org.apache.log4j.Logger;
import ui.CouncilIntelPlugin;
import ui.LawsIntelPlugin;

import java.util.HashMap;

import static util.Constants.LORD_TABLE_KEY;


public class LordsModPlugin extends BaseModPlugin {

    public static Logger log = Global.getLogger(LordsModPlugin.class);


    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        // creates table for lords in persistent data
        if (!sector.getPersistentData().containsKey(LORD_TABLE_KEY)) {
            sector.getPersistentData().put(LORD_TABLE_KEY, new HashMap<String, HashMap<String, Object>>());
        }

        if (!newGame) {
            LordController.parseLordTemplates();
            LordController.loadLords();
            log.info(LordController.getLordsList().size() + " Lords found");
        }

        FiefController.getInstance(true);
        EventController.getInstance(true);
        LawsIntelPlugin.getInstance(true);
        CouncilIntelPlugin.getInstance(true);
        QuestController.getInstance(true);
        RelationController.getInstance(true);
        PoliticsController.getInstance(true);

        if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
            sector.getListenerManager().addListener(new MarketStateChangeNexListener(), true);
        }
        sector.registerPlugin(new LordsCampaignPlugin());
    }


    @Override
    public void onNewGameAfterTimePass() {
        SectorAPI sector = Global.getSector();
        // creates table for lords in persistent data
        if (!sector.getPersistentData().containsKey(LORD_TABLE_KEY)) {
            sector.getPersistentData().put(LORD_TABLE_KEY, new HashMap<String, HashMap<String, Object>>());
        }

        // TODO reset instances when multiple saves are loaded
        LordController.parseLordTemplates();
        LordController.createAllLords();

        // these classes and their fields persist between save loads
        // Lords don't, so make sure they dont have lord variables
        sector.addListener(new MonthlyUpkeepListener(true));
        sector.addListener(new BattleListener(true));
        sector.getListenerManager().addListener(new MarketStateChangeListener(), false);
        sector.addScript(new LordAI());
    }
}