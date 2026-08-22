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
import starlords.generator.NewGameLordPicker;
import starlords.listeners.BattleListener;
import starlords.listeners.MarketStateChangeListener;
import starlords.listeners.MarketStateChangeNexListener;
import starlords.listeners.MonthlyUpkeepListener;
import org.apache.log4j.Logger;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.scripts.ActionCompleteScript;
import starlords.ui.*;
import starlords.util.Constants;
import starlords.util.NexerlinUtilitys;
import starlords.util.Utils;
import starlords.util.factionUtils.FactionTemplateController;

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
        x.alias("controllers.RequestController", RequestController.class);
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
        x.alias("ui.RequestIntelPlugin", RequestIntelPlugin.class);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        // creates table for lords in persistent data
        if (!sector.getPersistentData().containsKey(LORD_TABLE_KEY)) {
            sector.getPersistentData().put(LORD_TABLE_KEY, new HashMap<String, HashMap<String, Object>>());
            log.info("reset presistant data...");
        }

        if (!newGame) {
            LordController.parseLordTemplates();
            LordController.loadLords();
            log.info("loaded saved starlords...");
            log.info(LordController.getLordsList().size() + " Lords found");
            if (LordController.getPlayerLord() == null){

            }
        }

        StoredSettings.attemptEnableLunalib();
        StoredSettings.getSettings();
        log.info("got settings...");
        //Instantiate Controllers
        FiefController.getInstance(true);
        log.info("set fief controller...");
        EventController.getInstance(true);
        log.info("set event controller..");
        RequestController.getInstance(true);
        log.info("set request controller..");
        QuestController.getInstance(true);
        log.info("set quest controller...");
        RelationController.getInstance(true);
        log.info("set relation controller...");
        PoliticsController.getInstance(true);
        log.info("set politics controller...");
        LifeAndDeathController.getInstance(true);
        log.info("set life and death controller...");

        //Instantiate Static Intel Plugins
        LawsIntelPlugin.getInstance(true);
        log.info("set Law Plugin...");
        CouncilIntelPlugin.getInstance(true);
        log.info("set council plugin...");

        if (Utils.nexEnabled()) {
            sector.getListenerManager().addListener(new MarketStateChangeNexListener(), true);
        }
        sector.registerPlugin(new LordsCampaignPlugin());
        log.info("set capmain plugin...");

        if (newGame && Constants.ENABLE_NEW_LORDS_ON_GAME_START){
            NewGameLordPicker.instance.addAll();
        }
        NewGameLordPicker.instance = null;
        /*/String[] factionsTemp = {
                "HIVER",
                "pirates",
                "tritachyon",
                "remnant"
        };
        for (String a : factionsTemp){
            log.info("can the "+a+" go to go and fight ?"+Utils.canBeAttacked(Global.getSector().getFaction(a)));
            log.info("can the "+a+" try to make peace ?"+Utils.canHaveRelations(Global.getSector().getFaction(a)));
            log.info("is the "+a+" a minor faction? "+Utils.isMinorFaction(Global.getSector().getFaction(a)));
            //log.info("is the "+a+" playable? "+ NexConfig.getFactionConfig(a).playableFaction);
        }/**/
        LordController.loadFleetSMods();
        log.info("loaded fleet Smods....");
        LordController.fixAllLordsPartnerStatus();
        log.info("repaired partnear status...");
        LordController.logAllLords();
        log.info("loged all lords...");
        LordMemoryController.load();
        log.info("prepared memory controler....");
        LordController.updateFactionsWithLords();
        log.info("updated lord factions...");

        if (Utils.nexEnabled()) NexerlinUtilitys.calculateInvasionsEnabled();
        log.info("starlords loading completed...");
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

    @Override
    public void beforeGameSave() {
        LordController.logAllLords();
        LordController.saveUnusualLords();
        LordController.saveLordData();
    }
}