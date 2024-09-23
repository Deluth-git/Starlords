package starlords.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.LordController;
import org.lwjgl.input.Keyboard;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.util.LordFleetFactory;
import starlords.util.Utils;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TournamentDialogPlugin extends FleetInteractionDialogPluginImpl {

    public static enum TournamentOptionId {
        LIST_PARTICIPANTS,
        PLACE_WAGER,
        PROCESS_WAGER1,
        PROCESS_WAGER2,
        PROCESS_WAGER3,
        PROCESS_WAGER4,
        CHOOSE_SHIP,
        UNCHOOSE_SHIP,
    }

    // Total possible winnings that should be expected from a wager, by round
    private static float[] wagerRatio = new float[]{2.5f, 2, 1.8f, 1.5f};

    private LordEvent feast;
    private boolean placedWager;
    private int wager;
    private int winnings;
    private FleetMemberAPI playerShip;
    private CampaignFleetAPI origPlayerFleet;
    private ArrayList<PersonAPI> participants;
    private MarketAPI market;
    private ShipAPI.HullSize currRoundHullSize;
    private ArrayList<String> currRoundOptions;
    private String shipCategory;

    public TournamentDialogPlugin(LordEvent feast) {
        if (feast == null) {
            // for testing only
            MarketAPI market = Misc.findNearestLocalMarket(Global.getSector().getPlayerFleet(), 1e5f, null);
            feast = new LordEvent(LordEvent.FEAST, LordController.getLordsList().get(0), market.getPrimaryEntity());
            for (int i = 0; i < 7; i++) {
                feast.getParticipants().add(LordController.getLordsList().get(1 + i));
            }
        }

        this.feast = feast;
        market = feast.getTarget().getMarket();
        config = new FIDConfig();
        config.pullInEnemies = false;
        config.pullInAllies = false;
        config.pullInStations = false;
        config.showFleetAttitude = false;
        config.showCommLinkOption = false;
        config.showTransponderStatus = false;
        origPlayerFleet = Global.getSector().getPlayerFleet();
        participants = new ArrayList<>();
        currRoundOptions = new ArrayList<>();
        participants.add(feast.getOriginator().getLordAPI());
        for (Lord participant : feast.getParticipants()) {
            if (participants.size() < 15) participants.add(participant.getLordAPI());
        }
        populateParticipants();
        setShipCategory();
    }


    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setInteractionTarget(feast.getOriginator().getFleet());
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        conversationDelegate = new RuleBasedInteractionDialogPluginImpl();
        conversationDelegate.setEmbeddedMode(true);
        conversationDelegate.init(dialog);
        Global.getSector().setPaused(true);
        advanceTournamentStage(false);
    }

    public void advanceTournamentStage(boolean eliminate) {
        // clean up old fleets
        if (playerFleet != null && playerFleet != origPlayerFleet) playerFleet.despawn();
        if (otherFleet != null) otherFleet.despawn();
        // eliminate loser contestants
        // if the player's team lost this method won't be called, so assume player team won
        // TODO make advancement based on kills
        if (eliminate) {
            int toRemove = 1 + participants.size() / 2;
            for (int i = 0; i < toRemove; i++) {
                participants.remove(participants.size() - 1);
            }
            Collections.shuffle(participants);
            addText("Gained " + wager + " credits from wagers.", Color.GREEN);
            winnings += wager;
            placedWager = false;
        }

        // create temporary fleets for next battle
        updateHullSize();
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(
                Utils.getRecruitmentFaction().getId(), FleetTypes.TASK_FORCE,
                market);
        market.getContainingLocation().addEntity(fleet);
        fleet.setName("Test1");
        fleet.setHidden(false);
        fleet.setExpired(false);
        fleet.setCommander(Global.getSector().getPlayerPerson());
        fleet.setLocation(market.getPrimaryEntity().getLocation().x,
                market.getPrimaryEntity().getLocation().y);
        addContestant(fleet, Global.getSector().getPlayerPerson(), chooseHull(Global.getSector().getPlayerPerson()));
        for (int i = 0; i < participants.size() / 2; i++) {
            addContestant(fleet, participants.get(i), chooseHull(participants.get(i)));
        }
        Global.getSector().setPlayerFleet(fleet);
        fleet.setAIMode(true);
        fleet.inflateIfNeeded();
        fleet.getFleetData().setSyncNeeded();
        fleet.forceSync();
        fleet.getCargo().addCrew((int) fleet.getFleetData().getMinCrew());

        CampaignFleetAPI fleet2 = FleetFactoryV3.createEmptyFleet(
                Utils.getRecruitmentFaction().getId(), FleetTypes.TASK_FORCE,
                market);
        fleet2.setAIMode(true);
        fleet2.setName("Test2");
        fleet2.setCommander(participants.get(1));
        fleet2.setLocation(market.getPrimaryEntity().getLocation().x,
                market.getPrimaryEntity().getLocation().y);
        for (int i = participants.size() / 2; i < participants.size(); i++) {
            addContestant(fleet2, participants.get(i), chooseHull(participants.get(i)));
        }
        fleet2.forceSync();
        playerFleet = fleet;
        otherFleet = fleet2;
        BattleAPI battle = Global.getFactory().createBattle(playerFleet, otherFleet);
        context.setBattle(battle);
        context.getBattle().genCombined();
        context.getBattle().takeSnapshots();
        visual.setVisualFade(0.25f, 0.25f);
        showFleetInfo();
        conversationDelegate.fireBest("BeginFleetEncounter");
        optionSelected(null, OptionId.INIT);
    }

    private void addContestant(CampaignFleetAPI fleet, PersonAPI person, String variantId) {
        if (person.isPlayer() && playerShip != null) {
            fleet.getFleetData().addFleetMember(playerShip);
        } else {
            LordFleetFactory.addToLordFleet(new ShipRolePick(variantId), fleet, new Random());
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder()) {
            if (member.getCaptain().getNameString().isEmpty()) {
                member.getRepairTracker().setCR(1f);
                member.setCaptain(person);
            }
        }
        fleet.getFleetData().ensureHasFlagship();

    }

    private String chooseHull(PersonAPI person) {
        long seed = Global.getSector().getClock().getTimestamp() * person.getId().hashCode();
        return currRoundOptions.get(new Random(seed).nextInt(currRoundOptions.size()));
    }

    private void updateHullSize() {
        if (playerShip != null) {
            currRoundHullSize = playerShip.getHullSpec().getHullSize();
        } else {
            long seed = Global.getSector().getClock().getTimestamp() * market.getId().hashCode() * participants.size();
            int choice = new Random(seed).nextInt(3);
            switch (choice) {
                case 0:
                    currRoundHullSize = ShipAPI.HullSize.CRUISER;
                    break;
                case 1:
                    currRoundHullSize = ShipAPI.HullSize.DESTROYER;
                    break;
                default:
                    currRoundHullSize = ShipAPI.HullSize.FRIGATE;
                    break;
            }
        }

        currRoundOptions.clear();
        for (String variantId : Global.getSettings().getAllVariantIds()) {
            ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
            if (variant.getHullSize() != currRoundHullSize) continue;
            if (variant.hasHullMod(HullMods.CIVGRADE)) continue;
            if (variant.getWeaponGroups().isEmpty()) continue;
            if (variant.getHullSpec().getManufacturer().equals(shipCategory)) currRoundOptions.add(variantId);
        }
        if (currRoundOptions.isEmpty()) {
            // backup: guess we're doing pirates
            for (String variantId : Global.getSettings().getAllVariantIds()) {
                ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
                if (variant.getHullSize() != currRoundHullSize) continue;
                if (variant.hasHullMod(HullMods.CIVGRADE)) continue;
                if (variant.getWeaponGroups().isEmpty()) continue;
                if (variant.getHullSpec().getManufacturer().equals("Pirate")) currRoundOptions.add(variantId);
            }
        }
    }

    @Override
    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;
        boolean doSuper = true;
        if (optionData instanceof OptionId) {
            OptionId option = (OptionId) optionData;
            switch (option) {
                case INIT:
                    addText("You've reached the Round of " + (participants.size() + 1));
                    options.clearOptions();
                    options.addOption("Proceed to the next round", OptionId.ENGAGE, getString("tooltipEngage"));
                    options.addOption("Check the tournament roster", TournamentOptionId.LIST_PARTICIPANTS);
                    options.addOption("Place a wager", TournamentOptionId.PLACE_WAGER);
                    if (playerShip == null) {
                        options.addOption("Pilot your own ship", TournamentOptionId.CHOOSE_SHIP);
                    } else {
                        options.addOption("Pilot a standardized ship", TournamentOptionId.UNCHOOSE_SHIP);
                    }
                    options.addOption("Withdraw from the tournament", OptionId.LEAVE, null);
                    options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
                    if (placedWager) options.setEnabled(TournamentOptionId.PLACE_WAGER, false);
                    doSuper = false;
                    break;
                case LEAVE:
                    if (playerFleet != null && playerFleet != origPlayerFleet) playerFleet.despawn();
                    if (otherFleet != null) otherFleet.despawn();
                    Global.getSector().setPlayerFleet(origPlayerFleet);
                    origPlayerFleet.getCargo().getCredits().add(winnings);
                    break;
            }
        } else {
            TournamentOptionId option = (TournamentOptionId) optionData;
            doSuper = false;
            int roundsLeft;
            float baseWinPerRound;
            switch (option) {
                case LIST_PARTICIPANTS:
                    addText("Tournament Roster:");
                    addText(Global.getSector().getPlayerPerson().getNameString(),
                            Utils.getRecruitmentFaction().getBaseUIColor());
                    for (int i = 0; i < participants.size(); i++) {
                        addText(participants.get(i).getNameString(), participants.get(i).getFaction().getBaseUIColor());
                    }
                    break;
                case PLACE_WAGER:
                    addText("Choose the amount you'd like to wager. " +
                            "You will pay this amount upfront and receive winnings for each round you win." +
                            "You can wager once per round and winnings stack.");
                    int creditsLeft = (int) origPlayerFleet.getCargo().getCredits().get() + winnings;
                    roundsLeft = (int) Math.round(Math.log(participants.size() + 1) / Math.log(2));
                    baseWinPerRound = wagerRatio[4 - roundsLeft] / roundsLeft;
                    // 2.5, 2, 1.8, 1.5
                    options.clearOptions();
                    options.addOption("2000 Credits", TournamentOptionId.PROCESS_WAGER1);
                    if (creditsLeft < 2000) options.setEnabled(TournamentOptionId.PROCESS_WAGER1, false);
                    options.setTooltip(TournamentOptionId.PROCESS_WAGER1,
                            "Gain " + ((int) (baseWinPerRound * 2000)) + " credits per win");
                    options.addOption("5000 Credits", TournamentOptionId.PROCESS_WAGER2);
                    if (creditsLeft < 5000) options.setEnabled(TournamentOptionId.PROCESS_WAGER2, false);
                    options.setTooltip(TournamentOptionId.PROCESS_WAGER2,
                            "Gain " + ((int) (baseWinPerRound * 5000)) + " credits per win");
                    options.addOption("10000 Credits", TournamentOptionId.PROCESS_WAGER3);
                    if (creditsLeft < 10000) options.setEnabled(TournamentOptionId.PROCESS_WAGER3, false);
                    options.setTooltip(TournamentOptionId.PROCESS_WAGER3,
                            "Gain " + ((int) (baseWinPerRound * 10000)) + " credits per win");
                    options.addOption("20000 Credits", TournamentOptionId.PROCESS_WAGER4);
                    if (creditsLeft < 20000) options.setEnabled(TournamentOptionId.PROCESS_WAGER4, false);
                    options.setTooltip(TournamentOptionId.PROCESS_WAGER4,
                            "Gain " + ((int) (baseWinPerRound * 20000)) + " credits per win");
                    options.addOption("Never mind", OptionId.INIT);
                    break;
                case PROCESS_WAGER1:
                case PROCESS_WAGER2:
                case PROCESS_WAGER3:
                case PROCESS_WAGER4:
                    int credits = Integer.parseInt(text.split(" ")[0]);
                    roundsLeft = (int) Math.round(Math.log(participants.size() + 1) / Math.log(2));
                    baseWinPerRound = wagerRatio[4 - roundsLeft] / roundsLeft;
                    wager += baseWinPerRound * credits;
                    winnings -= credits;
                    placedWager = true;
                    addText("Wagered " + credits + " credits.", Color.RED);
                    optionSelected(null, OptionId.INIT);
                    break;
                case CHOOSE_SHIP:
                    List<FleetMemberAPI> members = origPlayerFleet.getFleetData().getMembersListCopy();
                    List<FleetMemberAPI> toRemove = new ArrayList<>();
                    for (FleetMemberAPI member : members) {
                        if (member.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) toRemove.add(member);
                    }
                    members.removeAll(toRemove);
                    if (members.isEmpty()) return;
                    dialog.showFleetMemberPickerDialog("Select craft to pilot", "Ok", "Cancel",
                            4, 8, 58f, false, false, members,
                            new FleetMemberPickerListener() {
                                public void pickedFleetMembers(List<FleetMemberAPI> members) {
                                    if (members != null && !members.isEmpty()) {
                                        playerShip = members.get(0);
                                        advanceTournamentStage(false);
                                    }
                                }
                                public void cancelledFleetMemberPicking() {

                                }
                            });
                    break;
                case UNCHOOSE_SHIP:
                    playerShip = null;
                    advanceTournamentStage(false);
                    break;
            }
        }
        if (doSuper) {
            super.optionSelected(text, optionData);
        }

        if (options.hasOption(OptionId.SELECT_FLAGSHIP)) {
            options.setEnabled(OptionId.SELECT_FLAGSHIP, false);
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
        result.setBattle(context.getBattle());
        lastResult = result;
        if (!result.didPlayerWin()) {
            showFleetInfo();
            addText("You've been eliminated from the tournament.", Color.RED);
            // TODO calculate winner
            options.clearOptions();
            options.addOption("Leave", OptionId.LEAVE, null);
            options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
        } else if (participants.size() == 1) {
            showFleetInfo();
            addText("The crowd erupts in cheers! You have won the tournament!", Color.GREEN);
            options.clearOptions();
            options.addOption("Leave", OptionId.LEAVE, null);
            options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
        } else {
            showFleetInfo();
            addText("You advance to the next round!", Color.GREEN);
            advanceTournamentStage(true);
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
        if (optionData instanceof TournamentOptionId) return;
        super.optionMousedOver(optionText, optionData);
    }

    private void populateParticipants() {
        List<OfficerDataAPI> playerOfficers = origPlayerFleet.getFleetData().getOfficersCopy();
        Collections.shuffle(playerOfficers);
        List<PersonAPI> marketPeople = market.getPeopleCopy();
        Collections.shuffle(marketPeople);
        boolean memed = false;
        while (participants.size() < 15) {
            ArrayList<Integer> options = new ArrayList<>();
            ArrayList<Integer> weights = new ArrayList<>();

            // 0 - player officers
            if (!playerOfficers.isEmpty() && playerOfficers.get(0).getPerson().isPlayer()) {
                playerOfficers.remove(0);
            }
            if (!playerOfficers.isEmpty()) {
                options.add(0);
                weights.add(50);
            }

            // 1 - Some meme characters
            if (!memed) {
                options.add(1);
                weights.add(5);
            }

            // 2 - pather and pirate characters
            options.add(2);
            weights.add(10);

            // 3 - station characters
            while (!marketPeople.isEmpty() && participants.contains(marketPeople.get(0))) {
                marketPeople.remove(0);
            }
            if (!marketPeople.isEmpty()) {
                options.add(3);
                weights.add(50);
            }

            // 4 - Random faction character
            options.add(4);
            weights.add(25);
            int option = Utils.weightedSample(options, weights, Utils.rand);
            PersonAPI person;
            switch (option) {
                case 0:
                    participants.add(playerOfficers.remove(0).getPerson());
                    break;
                case 1:
                    memed = true;
                    person = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson();
                    person.setPersonality(Personalities.RECKLESS);
                    person.setGender(FullName.Gender.MALE);
                    person.getStats().setLevel(15);
                    person.setName(new FullName("John", "Starsector", FullName.Gender.MALE));
                    participants.add(person);
                    break;
                case 2:
                    if (Utils.rand.nextBoolean()) {
                        person = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson();
                    } else {
                        person = Global.getSector().getFaction(Factions.LUDDIC_PATH).createRandomPerson();
                    }
                    person.getStats().setLevel(6);
                    participants.add(person);
                    break;
                case 3:
                    participants.add(marketPeople.remove(0));
                    break;
                case 4:
                    person = market.getFaction().createRandomPerson();
                    person.getStats().setLevel(5);
                    participants.add(person);
                    break;
            }
        }
        Collections.shuffle(participants);
    }

    private void setShipCategory() {
        switch (market.getFaction().getId()) {
            case Factions.HEGEMONY:
                shipCategory = "Low Tech";
                break;
            case Factions.TRITACHYON:
                shipCategory = "High Tech";
                break;
            case Factions.LIONS_GUARD:
            case Factions.DIKTAT:
                shipCategory = "Lion's Guard";
                break;
            case Factions.LUDDIC_CHURCH:
                shipCategory = "Luddic Church";
                break;
            case Factions.LUDDIC_PATH:
                shipCategory = "Luddic Path";
                break;
            case Factions.PERSEAN:
                shipCategory = "Midline";
                break;
            case Factions.REMNANTS:
                shipCategory = "Remnant";
                break;
            case Factions.PIRATES:
            default:
                shipCategory = "Pirate";
                break;
        }
    }
}
