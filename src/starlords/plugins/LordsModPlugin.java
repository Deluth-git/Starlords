package starlords.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.thoughtworks.xstream.XStream;
import starlords.ai.LordStrategicModule;
import starlords.controllers.*;
import starlords.ai.LordAI;
import starlords.faction.LawProposal;
import starlords.faction.Lawset;
import starlords.listeners.BattleListener;
import starlords.listeners.MarketStateChangeListener;
import starlords.listeners.MarketStateChangeNexListener;
import starlords.listeners.MonthlyUpkeepListener;
import org.apache.log4j.Logger;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.scripts.ActionCompleteScript;
import starlords.ui.*;
import starlords.util.Utils;

import java.util.HashMap;

import static starlords.util.Constants.LORD_TABLE_KEY;


public class LordsModPlugin extends BaseModPlugin {

    public static Logger log = Global.getLogger(LordsModPlugin.class);

    @Override
    public void configureXStream(XStream x) {
        // for backwards compatibility
        x.alias("ai.LordAI", LordAI.class);
        x.alias("ai.LordStrategicModule", LordStrategicModule.class);
        x.alias("faction.Lawset", Lawset.class);
        x.alias("faction.LawProposal", LawProposal.class);
        x.alias("scripts.ActionCompleteScript", ActionCompleteScript.class);
        x.alias("person.Lord", Lord.class);
        x.alias("person.LordEvent", LordEvent.class);
        x.alias("controllers.EventController", EventController.class);
        x.alias("controllers.FiefController", FiefController.class);
        x.alias("controllers.LordController", LordController.class);
        x.alias("controllers.PoliticsController", PoliticsController.class);
        x.alias("controllers.QuestController", QuestController.class);
        x.alias("controllers.RelationController", RelationController.class);
        x.alias("listeners.BattleListener", BattleListener.class);
        x.alias("listeners.MarketStateChangeListener", MarketStateChangeListener.class);
        x.alias("listeners.MonthlyUpkeepListener", MonthlyUpkeepListener.class);
        x.alias("ui.CouncilIntelPlugin", CouncilIntelPlugin.class);
        x.alias("ui.EventIntelPlugin", EventIntelPlugin.class);
        x.alias("ui.HostileEventIntelPlugin", HostileEventIntelPlugin.class);
        x.alias("ui.LawsIntelPlugin", LawsIntelPlugin.class);
        x.alias("ui.LordsIntelPlugin", LordsIntelPlugin.class);
        x.alias("ui.MissionPreviewIntelPlugin", MissionPreviewIntelPlugin.class);
        x.alias("ui.PrisonerIntelPlugin", PrisonerIntelPlugin.class);
        x.alias("ui.ProposalIntelPlugin", ProposalIntelPlugin.class);
    }

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

        if (Utils.nexEnabled()) {
            sector.getListenerManager().addListener(new MarketStateChangeNexListener(), true);
        }
        sector.registerPlugin(new LordsCampaignPlugin());
    }


    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        if (wasEnabledBefore) return;
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