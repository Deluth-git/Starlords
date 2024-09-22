package starlords.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import starlords.controllers.LordController;
import org.lwjgl.input.Keyboard;
import starlords.util.LordFleetFactory;
import starlords.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class TournamentDialogPlugin extends FleetInteractionDialogPluginImpl {

    public static enum TournamentOptionId {
        LIST_PARTICIPANTS,
        PLACE_WAGER,
        PROCESS_WAGER,
        CHOOSE_SHIP,
    }

    private ArrayList<PersonAPI> participants;
    private MarketAPI market;

    public TournamentDialogPlugin() {
        config = new FIDConfig();
        config.pullInEnemies = false;
        config.pullInAllies = false;
        config.pullInStations = false;
        config.showFleetAttitude = false;
        config.showCommLinkOption = false;
        config.showTransponderStatus = false;
        market = Global.getSector().getEconomy().getMarketsCopy().get(0);
        participants = new ArrayList<>();
        participants.add(Global.getSector().getPlayerPerson());
        for (int i = 0; i < 15; i++) {
            participants.add(LordController.getLordsList().get(i).getLordAPI());
        }
    }


    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        // create temporary fleets for next battle
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(
                Utils.getRecruitmentFaction().getId(), FleetTypes.TASK_FORCE,
                market);
        market.getContainingLocation().addEntity(fleet);
        fleet.setName("Test1");
        fleet.setHidden(false);
        fleet.setExpired(false);
        fleet.inflateIfNeeded();
        fleet.forceSync();
        fleet.setCommander(participants.get(0));
        fleet.setLocation(market.getPrimaryEntity().getLocation().x,
                market.getPrimaryEntity().getLocation().y);
        LordFleetFactory.addToLordFleet(new ShipRolePick("tesseract_Strike"), fleet, new Random());
        fleet.getFleetData().getMembersInPriorityOrder().get(0).setFlagship(true);
        fleet.getFleetData().getMembersInPriorityOrder().get(0).setCaptain(participants.get(0));
        Global.getSector().setPlayerFleet(fleet);
        CampaignFleetAPI fleet2 = FleetFactoryV3.createEmptyFleet(
                Utils.getRecruitmentFaction().getId(), FleetTypes.TASK_FORCE,
                market);
        fleet2.setAIMode(true);
        fleet2.setName("Test2");
        fleet2.setCommander(participants.get(1));
        fleet2.setLocation(market.getPrimaryEntity().getLocation().x,
                market.getPrimaryEntity().getLocation().y);
        LordFleetFactory.addToLordFleet(new ShipRolePick("tesseract_Strike"), fleet2, new Random());
        fleet2.getFleetData().getMembersInPriorityOrder().get(0).setFlagship(true);
        fleet2.getFleetData().getMembersInPriorityOrder().get(0).setCaptain(participants.get(1));
        playerFleet = fleet;
        otherFleet = fleet2;
        BattleAPI battle = Global.getFactory().createBattle(playerFleet, otherFleet);
        context.setBattle(battle);
        context.getBattle().genCombined();
        context.getBattle().takeSnapshots();
        visual.setVisualFade(0.25f, 0.25f);
        showFleetInfo();
        conversationDelegate = new RuleBasedInteractionDialogPluginImpl();
        conversationDelegate.setEmbeddedMode(true);
        conversationDelegate.init(dialog);
        conversationDelegate.fireBest("BeginFleetEncounter");
        optionSelected(null, OptionId.INIT);
    }

    @Override
    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;
        boolean doSuper = true;
        OptionId option = (OptionId) optionData;
        switch (option) {
            case INIT:
                //updateMainState(true);
                options.clearOptions();
                options.addOption("Proceed to the next round", OptionId.ENGAGE, getString("tooltipEngage"));
                options.addOption("Check the tournament roster", TournamentOptionId.LIST_PARTICIPANTS);
                options.addOption("Place a wager", TournamentOptionId.PLACE_WAGER);
                options.addOption("Pilot your own ship", TournamentOptionId.CHOOSE_SHIP);
                options.addOption("Withdraw from the tournament", OptionId.LEAVE, null);
                options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
                doSuper = false;
                break;
        }
        if (doSuper) {
            super.optionSelected(text, optionData);
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
        result.setBattle(context.getBattle());
        lastResult = result;
        if (!result.didPlayerWin()) {
            showFleetInfo();
            addText("You've been eliminated from the tournament.", Color.RED);
            options.clearOptions();
            options.addOption("Leave", OptionId.LEAVE, null);
            options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
        } else {
            showFleetInfo();
            addText("You advance to the next round!", Color.GREEN);
            optionSelected(null, OptionId.INIT);
        }
    }
}
