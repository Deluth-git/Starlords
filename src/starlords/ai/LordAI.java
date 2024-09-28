package starlords.ai;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAIFlags;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import starlords.controllers.*;
import exerelin.campaign.intel.groundbattle.GBUtils;
import exerelin.campaign.intel.groundbattle.GroundBattleIntel;
import exerelin.campaign.intel.groundbattle.GroundUnit;
import exerelin.campaign.intel.groundbattle.GroundUnitDef;
import starlords.faction.Lawset;
import org.apache.log4j.Logger;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;
import starlords.person.LordPersonality;
import starlords.plugins.TournamentDialogPlugin;
import starlords.scripts.ActionCompleteScript;
import starlords.ui.HostileEventIntelPlugin;
import starlords.util.LordFleetFactory;
import starlords.util.StringUtil;
import starlords.util.Utils;

import java.awt.*;
import java.util.*;
import java.util.List;

import static starlords.util.Constants.*;

public class LordAI implements EveryFrameScript {

    public static Logger log = Global.getLogger(LordAI.class);
    // all times in days
    public static final float UPDATE_INTERVAL = 0.25f;
    public static final int PATROL_DURATION = 21;
    public static final int RESPAWN_DURATION = 30;
    public static final int PRISON_ESCAPE_DURATION = 30;
    public static final int STANDBY_DURATION = 7;
    public static final int SMALL_OP_RECONSIDER_INTERVAL = 4;
    public static final int LARGE_OP_RECONSIDER_INTERVAL = 4;
    public static final int[] FEAST_COOLDOWN = new int[]{30, 90, 180, 365, 1000000};
    public static final int FOLLOW_DURATION = 30;
    public static final int CAMPAIGN_COOLDOWN = 180;
    public static final int RAID_COOLDOWN = 45;  // applies per market, not per lord
    public static final int CAMPAIGN_MAX_VIOLENCE= 6;
    public static final int RAID_MAX_VIOLENCE = 3;
    public static final int CAMPAIGN_MAX_DURATION = 120;
    public static final int FEAST_MAX_DURATION = 60;
    public static final String BUSY_REASON = "starlords_standby";
    public static final int FEAST_COST = 20000;
    public static final String CATEGORY = "starlords_ui";

    private static Random rand = new Random();
    private float lastUpdate;
    private final static HashSet<LordAction> sameFactionTargetActions = new HashSet<>(Arrays.asList(
            LordAction.FEAST, LordAction.COLLECT_TAXES));
    private final static HashSet<LordAction> friendlyTargetActions = new HashSet<>(Arrays.asList(
            LordAction.PATROL, LordAction.VENTURE, LordAction.DEFEND, LordAction.UPGRADE_FLEET, LordAction.FOLLOW));
    private final static HashSet<LordAction> hostileTargetActions = new HashSet<>(Arrays.asList(LordAction.RAID));

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        float days = Global.getSector().getClock().convertToDays(amount);
        lastUpdate += days;
        if (lastUpdate < UPDATE_INTERVAL) {
            return;
        }

        lastUpdate = 0;
        for (Lord lord : LordController.getLordsList()) {
            if (lord.getCurrAction() == null) {
                chooseAssignment(lord);
            }
            progressAssignment(lord);
        }

    }

    public static void chooseAssignment(Lord lord) {
        FactionAPI faction = lord.getFaction();
        float econLevel = lord.getEconLevel();
        float milLevel = lord.getMilitaryLevel();
        MarketAPI ventureTarget = null;

        ArrayList<Integer> weights = new ArrayList<>();
        ArrayList<LordAction> actions = new ArrayList<>();
        // if a time-sensitive priority task is in consideration, all lesser-priority task are blocked
        int priority = 10;
        LordEvent campaign = null;
        if (priority >= LordAction.CAMPAIGN.priority) {
            campaign = EventController.getCurrentCampaign(faction);
            if (campaign == null) {
                // consider starting new campaign if marshal. This is non-blocking
                if (lord.isMarshal() && Utils.getDaysSince(
                        EventController.getLastCampaignTime(faction)) >= CAMPAIGN_COOLDOWN) {
                    int weight = EventController.getStartCampaignWeight(lord);
                    //log.info("Campaign start weight: " + weight);
                    // weight += 1000000; // TODO
                    if (weight > 0) {
                        weights.add(weight);
                        actions.add(LordAction.CAMPAIGN);
                    }
                }

            } else {
                // join existing campaign
                int weight = EventController.getJoinCampaignWeight(lord);
                if (weight > 0) {
                    priority = LordAction.CAMPAIGN.priority;
                    weights.add(weight);
                    actions.add(LordAction.CAMPAIGN);
                }
            }
        }

        LordEvent attack = null;
        SectorEntityToken attackTarget = null;
        if (priority >= LordAction.RAID.priority) {
            // joining raid
            Pair<LordEvent, Integer> attackChoice = EventController.getPreferredRaidAttack(lord);
            attack = attackChoice.one;
            int raidWeight = attackChoice.two;

            // starting own raid
            Pair<SectorEntityToken, Integer> ownAttackChoice = EventController.getPreferredRaidLocation(lord);
            if (ownAttackChoice.two > raidWeight) {
                attack = null;
                raidWeight = ownAttackChoice.two;
                attackTarget = ownAttackChoice.one;
            }

            if (raidWeight > 0) {
                if (attack != null) priority = LordAction.RAID.priority;
                weights.add(raidWeight);
                actions.add(LordAction.RAID_TRANSIT);
            }
        }
        LordEvent defense = null;
        if (priority >= LordAction.DEFEND.priority) {
            Pair<LordEvent, Integer> defenseChoice = EventController.getPreferredDefense(lord);
            defense = defenseChoice.one;
            int defenseWeight = defenseChoice.two;
            if (defense != null && defenseWeight > 0) {
                priority = LordAction.DEFEND.priority;
                weights.add(defenseWeight);
                actions.add(LordAction.DEFEND_TRANSIT);
            }
        }

        LordEvent currFeast = null;
        if (priority >= LordAction.FEAST.priority) {
            int feastWeight = 0;
            currFeast = EventController.getCurrentFeast(faction);
            if (currFeast != null) {
                if (lord.getCurrAction() != LordAction.FEAST) {
                    // weight of joining existing feast
                    feastWeight = 100;
                    priority = LordAction.FEAST.priority;
                } else {
                    // weight of staying at feast
                    if (!currFeast.getOriginator().equals(lord)) {
                        feastWeight = 20;
                        priority = LordAction.FEAST.priority;
                    } else if (Utils.getDaysSince(currFeast.getStart()) < FEAST_MAX_DURATION) {
                        feastWeight = 250;
                        priority = LordAction.FEAST.priority;
                    }
                }

            } else if (Utils.getDaysSince(EventController.getLastFeastTime(faction)) >
                    FEAST_COOLDOWN[PoliticsController.getLaws(faction).getLawLevel(Lawset.LawType.FEAST_LAW).ordinal()]) {
                // weight of starting own feast
                if (lord.getWealth() >= FEAST_COST && milLevel > 1 && !lord.getFiefs().isEmpty()) {
                    feastWeight = 10;
                }
            }
            weights.add(feastWeight);
            if (lord.getCurrAction() == LordAction.FEAST) {
                actions.add(LordAction.FEAST); // skip transit if already present
            } else {
                actions.add(LordAction.FEAST_TRANSIT);
            }
        }

        if (priority >= LordAction.PATROL.priority) {
            int patrolWeight;
            switch (lord.getPersonality()) {
                case MARTIAL:
                    patrolWeight = 20;
                    break;
                case CALCULATING:
                    patrolWeight = 5;
                    break;
                default:
                    patrolWeight = 10;
            }
            patrolWeight = (int) Math.max(patrolWeight * milLevel, 1);
            weights.add(patrolWeight);
            actions.add(LordAction.PATROL_TRANSIT);
        }

        if (priority >= LordAction.COLLECT_TAXES.priority) {
            int taxWeight = 0;
            for (SectorEntityToken fief : lord.getFiefs()) {
                taxWeight += FiefController.getTax(fief.getMarket()) / 1000;
            }
            if (taxWeight > 0) {
                weights.add(taxWeight);
                actions.add(LordAction.COLLECT_TAXES_TRANSIT);
            }
        }

        if (priority >= LordAction.VENTURE.priority) {
            ventureTarget = FiefController.chooseVentureTarget(lord);
            if (ventureTarget != null) {
                int ventureWeight;
                switch (lord.getPersonality()) {
                    case MARTIAL:
                        ventureWeight = 5;
                        break;
                    case CALCULATING:
                        ventureWeight = 20;
                        break;
                    default:
                        ventureWeight = 10;
                }
                ventureWeight *= (2 - econLevel);
                if (ventureWeight > 0) {
                    weights.add(ventureWeight);
                    actions.add(LordAction.VENTURE_TRANSIT);
                }
            }
        }

        if (priority >= LordAction.UPGRADE_FLEET.priority) {
            int upgradeWeight = 0;
            if (lord.getFleet().getFlagship() == null) {
                upgradeWeight += 50;
            }
            if (econLevel > 1) {
                upgradeWeight += 35;
            }
            if (milLevel < 1 && econLevel > milLevel) {
                upgradeWeight += 10;
                upgradeWeight *= 2;
            }
            weights.add(upgradeWeight);
            actions.add(LordAction.UPGRADE_FLEET_TRANSIT);
        }

        LordAction currAction = Utils.weightedSample(actions, weights, rand);
        //log.info("Sending lord " + lord.getLordAPI().getNameString() + " on assignment " + currAction.toString());
        SectorEntityToken target = null;
        LordEvent beginEvent = null;
        switch(currAction) {
            case VENTURE_TRANSIT:
                target = ventureTarget.getPrimaryEntity();
                break;
            case FEAST:
            case FEAST_TRANSIT:
                if (currFeast != null) {
                    target = currFeast.getTarget();
                }
                break;
            case RAID_TRANSIT:
                if (attack == null) {
                    target = attackTarget;
                } else {
                    target = attack.getTarget();
                    beginEvent = attack;
                }
                break;
            case DEFEND_TRANSIT:
                target = defense.getTarget();
                beginEvent = defense;
                break;
            case CAMPAIGN:
                beginEvent = campaign;
        }
        beginAssignment(lord, currAction, target, beginEvent, false);
    }

    // called when Lord is first assigned the assignment to set fleet AI appropriately. Should usually be transit assignments
    // target is ignored except for venture, raid, and feast
    // event is ignord except for raid and defense
    public static void beginAssignment(Lord lord, LordAction currAction, SectorEntityToken target, LordEvent event, boolean playerDirected) {
        lord.setCurrAction(currAction);
        lord.setPlayerDirected(playerDirected);
        CampaignFleetAPI fleet = lord.getFleet();
        CampaignFleetAIAPI fleetAI = lord.getFleet().getAI();
        MemoryAPI mem = lord.getFleet().getMemoryWithoutUpdate();
        fleetAI.clearAssignments();
        Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
        Misc.clearFlag(mem, MemFlags.FLEET_IGNORES_OTHER_FLEETS);
        Misc.clearFlag(mem, FleetAIFlags.WANTS_TRANSPONDER_ON);
        if (!fleet.hasAbility(Abilities.TRANSPONDER)) {
            fleet.addAbility(Abilities.TRANSPONDER);
        }
        if (!fleet.hasAbility(Abilities.GO_DARK)) {
            fleet.addAbility(Abilities.GO_DARK);
        }

        // choose assignment target
        SectorEntityToken targetEntity;
        switch(currAction) {
            case UPGRADE_FLEET_TRANSIT:
                // same as collect taxes
            case PATROL_TRANSIT:
                // same as collect taxes
            case COLLECT_TAXES_TRANSIT:
                if (target == null) {
                    targetEntity = lord.getClosestBase();
                    if (targetEntity == null) {
                        targetEntity = Misc.findNearestLocalMarket(lord.getFleet(), 1e10f, null).getPrimaryEntity();
                    }
                } else {
                    targetEntity = target;
                }
                lord.setTarget(targetEntity);
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, targetEntity, 1000, new ActionCompleteScript(lord));
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                break;
            case VENTURE_TRANSIT:
                lord.setTarget(target);
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, target, 1000, new ActionCompleteScript(lord));
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                break;
            case FEAST_TRANSIT:
                if (target != null) {
                    targetEntity = target;
                } else {
                    targetEntity = lord.getClosestBase();
                    LordEvent newFeast = new LordEvent(LordEvent.FEAST, lord, targetEntity);
                    EventController.addFeast(newFeast);
                    lord.setFeastInteracted(false);
                    lord.addWealth(-1 * FEAST_COST);
                }
                lord.setTarget(targetEntity);
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, targetEntity, 1000, new ActionCompleteScript(lord));
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                break;
            case FEAST:
                standby(lord, target, StringUtil.getString(CATEGORY, "fleet_feast_desc", target.getMarket().getName()));
                break;
            case RAID_TRANSIT:
                lord.setTarget(target);
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, Misc.findNearestJumpPoint(target), 1000, new ActionCompleteScript(lord));
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                if (event != null) {
                    event.getParticipants().add(lord);
                } else {
                    EventController.addRaid(new LordEvent(LordEvent.RAID, lord, target));
                }
                break;
            case DEFEND_TRANSIT:
                lord.setTarget(target);
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, target, 1000, new ActionCompleteScript(lord));
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                event.getOpposition().add(lord);
                break;
            case CAMPAIGN:
                if (fleet.getAbility(Abilities.GO_DARK).isActive()) {
                    fleet.getAbility(Abilities.GO_DARK).deactivate();
                }
                if (!fleet.getAbility(Abilities.TRANSPONDER).isActive()) {
                    fleet.getAbility(Abilities.TRANSPONDER).activate();
                }
                fleet.removeAbility(Abilities.TRANSPONDER);
                fleet.removeAbility(Abilities.GO_DARK);
                Misc.setFlagWithReason(mem, FleetAIFlags.WANTS_TRANSPONDER_ON, BUSY_REASON, true, 1000);
                if (event != null) {
                    target = event.getOriginator().getFleet();
                    lord.setTarget(target);
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.ORBIT_AGGRESSIVE, target, 1000,
                            StringUtil.getString(CATEGORY, "fleet_campaign_follow_desc"), null);
                    Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                    event.getParticipants().add(lord);
                } else {
                    SectorEntityToken rallyPoint = lord.getClosestBase(false);
                    if (rallyPoint == null) {
                        rallyPoint = Misc.findNearestPlanetTo(lord.getFleet(), false, true);
                    }
                    lord.setTarget(rallyPoint);
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.GO_TO_LOCATION, rallyPoint, 1000, null);
                    fleetAI.addAssignment(FleetAssignment.ORBIT_PASSIVE, rallyPoint, 1000,
                            "Waiting for Lords to Assemble", null);
                    Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                    EventController.addCampaign(new LordEvent(LordEvent.CAMPAIGN, lord, null));
                }
                break;
            case FOLLOW:
                lord.setTarget(target);
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.ORBIT_AGGRESSIVE, target, 30, null);
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
        }
    }

    public static void progressAssignment(Lord lord) {
        CampaignFleetAIAPI fleetAI = lord.getFleet().getAI();
        final CampaignFleetAPI fleet = lord.getFleet();
        final MemoryAPI mem = fleet.getMemoryWithoutUpdate();
        // check for lord fleet defeat
        if (fleet.isEmpty() && lord.getCurrAction() != LordAction.RESPAWNING
                && lord.getCurrAction() != LordAction.IMPRISONED) {
            beginRespawn(lord);
            return;
        }

        // check if target is now invalid due to relation change
        boolean sameFactionFail = sameFactionTargetActions.contains(lord.getCurrAction().base)
                && !lord.getFaction().equals(lord.getTarget().getFaction());
        boolean friendlyFactionFail = friendlyTargetActions.contains(lord.getCurrAction().base)
                && lord.getFaction().isHostileTo(lord.getTarget().getFaction());
        boolean hostileFactionFail = hostileTargetActions.contains(lord.getCurrAction().base)
                && !lord.getFaction().isHostileTo(lord.getTarget().getFaction());
        if (sameFactionFail || friendlyFactionFail || hostileFactionFail) {
            EventController.removeFromAllEvents(lord);
            lord.setCurrAction(null);
            return;
        }

        // failsafe- transit assignments have no time limit, so if something breaks they can be stuck forever
        if (fleetAI.getCurrentAssignment() == null && !lord.isActionComplete() && lord.getCurrAction().isTransit()) {
            EventController.removeFromAllEvents(lord);
            lord.setCurrAction(null);
            return;
        }

        switch (lord.getCurrAction()) {
            case PATROL_TRANSIT:
                if (lord.isActionComplete()) {
                    if (lord.getTarget().getFaction().isHostileTo(lord.getFaction())) {
                        lord.setCurrAction(null);
                        return;
                    }
                    lord.setCurrAction(LordAction.PATROL);
                    fleetAI.clearAssignments();
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.PATROL_SYSTEM, null, 1000, null);
                }
                break;
            case PATROL:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > PATROL_DURATION) {
                    chooseAssignment(lord);
                }
                break;
            case COLLECT_TAXES_TRANSIT:
                if (lord.isActionComplete()) {
                    if (!lord.getFiefs().contains(lord.getTarget())) {
                        lord.setCurrAction(null);
                        return;
                    }
                    lord.setCurrAction(LordAction.COLLECT_TAXES);
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    standby(lord, lord.getTarget(), StringUtil.getString(
                            CATEGORY, "fleet_collect_taxes_desc", lord.getTarget().getMarket().getName()));
                }
                break;
            case COLLECT_TAXES:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > STANDBY_DURATION) {
                    for (SectorEntityToken marketEntity : lord.getFiefs()) {
                        MarketAPI market = marketEntity.getMarket();
                        lord.addWealth(PoliticsController.getTaxMultiplier(lord.getFaction())
                                * FiefController.getTax(market));
                        FiefController.setTax(market, 0);
                    }
                    chooseAssignment(lord);
                }
                break;
            case VENTURE_TRANSIT:
                if (lord.isActionComplete()) {
                    lord.setCurrAction(LordAction.VENTURE);
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    standby(lord, lord.getTarget(), StringUtil.getString(
                            CATEGORY, "fleet_venture_desc", lord.getTarget().getMarket().getName()));
                }
                break;
            case VENTURE:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > STANDBY_DURATION) {
                    MarketAPI market = lord.getTarget().getMarket();
                    lord.addWealth(PoliticsController.getTradeMultiplier(lord.getFaction()) * FiefController.getTrade(market));
                    // TODO make more frequent trades generate more taxes
                    FiefController.setTax(market, FiefController.getTax(market)
                            + PoliticsController.getTradeMultiplier(market.getFaction()) * FiefController.getTrade(market) / 2);
                    FiefController.setTrade(market, 0);
                    chooseAssignment(lord);
                }
                break;
            case UPGRADE_FLEET_TRANSIT:
                if (lord.isActionComplete()) {
                    lord.setCurrAction(LordAction.UPGRADE_FLEET);
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    standby(lord, lord.getTarget(), StringUtil.getString(
                            CATEGORY, "fleet_upgrade_fleet_desc", lord.getTarget().getMarket().getName()));
                }
                break;
            case UPGRADE_FLEET:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > STANDBY_DURATION) {
                    LordFleetFactory.upgradeFleet(lord);
                    chooseAssignment(lord);
                }
                break;
            case FEAST_TRANSIT:
                if (lord.isActionComplete()) {
                    lord.setCurrAction(LordAction.FEAST);
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    standby(lord, lord.getTarget(), StringUtil.getString(
                            CATEGORY, "fleet_feast_desc", lord.getTarget().getMarket().getName()));
                    LordEvent currFeast = EventController.getCurrentFeast(lord.getFaction());

                    if (!currFeast.getPastParticipants().contains(lord)) {
                        RelationController.modifyRelation(lord, currFeast.getOriginator(), 3);
                        for (Lord participant : currFeast.getParticipants()) {
                            RelationController.modifyRelation(lord, participant, 2);
                        }
                        if (!lord.getFaction().equals(Global.getSector().getPlayerFaction())) {
                            RelationController.modifyLoyalty(lord, 3);
                        }
                    }
                    if (!currFeast.getOriginator().equals(lord)) {
                        if (!currFeast.getPastParticipants().contains(lord)) {
                            lord.setFeastInteracted(false);
                            currFeast.getPastParticipants().add(lord);
                        }
                        currFeast.getParticipants().add(lord);
                    }
                }
                break;
            case FEAST:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > STANDBY_DURATION) {
                    chooseAssignment(lord);
                    // if this lord is the feast holder and ends the feast, update feast controller
                    if (lord.getCurrAction() != LordAction.FEAST) {
                        LordEvent currFeast = EventController.getCurrentFeast(lord.getFaction());
                        if (currFeast.getOriginator().equals(lord)) {
                            EventController.endFeast(currFeast);
                        } else {
                            currFeast.getParticipants().remove(lord);
                        }
                    }
                }
                break;
            case RAID_TRANSIT:
                if (lord.isActionComplete()) {
                    lord.setCurrAction(LordAction.RAID);
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    // TODO decide on whether convoy raiding, following leader, or going straight for planet
                    //LordEvent raid = EventController.getCurrentRaid(lord);
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.ATTACK_LOCATION, lord.getTarget(), 1000, null);
                }
                break;
            case RAID:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > SMALL_OP_RECONSIDER_INTERVAL) {
                    Pair<LordEvent, Integer> newWeight = EventController.getPreferredRaidAttack(lord);
                    // sometimes stop raids randomly so they dont go on forever
                    LordEvent raid = EventController.getCurrentRaid(lord);
                    FactionAPI faction = lord.getFaction();
                    if (lord.equals(raid.getOriginator())) {
                        if (Utils.isSomewhatClose(lord.getFleet(), raid.getTarget())) {
                            if (lord.getFleet().getBattle() == null && raid.getOffensiveType() != null &&
                                    Utils.getDaysSince(raid.getOffenseTimestamp())
                                            >= raid.getOffensiveType().chargeTime) {
                                // execute offensive
                                MarketCMD cmd = new MarketCMD(raid.getTarget());
                                float attackerStr = Utils.getRaidStr(
                                        lord.getFleet(), raid.getTotalMarines());
                                boolean failed = false;
                                switch (raid.getOffensiveType()) {
                                    case RAID_GENERIC:
                                        cmd.doGenericRaid(faction, attackerStr, 1);
                                        break;
                                    case RAID_INDUSTRY:
                                        Industry raidTarget = Utils.getIndustryToRaid(raid.getTarget().getMarket());
                                        if (raidTarget != null) {
                                            cmd.doIndustryRaid(faction, attackerStr, raidTarget, 1);
                                        } else {
                                            failed = true;
                                        }
                                        break;
                                }
                                if (!failed) {
                                    raid.setTotalViolence(raid.getTotalViolence()
                                            + raid.getOffensiveType().violence);
                                }
                                lord.setActionText(null);
                                raid.setOffensiveType(null);
                                fleetAI.removeFirstAssignmentIfItIs(FleetAssignment.ORBIT_PASSIVE);
                            } else if (raid.getOffensiveType() == null
                                    && Utils.isSomewhatClose(lord.getFleet(), raid.getTarget())) {
                                // plan new offensive
                                chooseNewOffensiveType(lord, raid);
                            }
                        } else {
                            // fleet is distracted, reset any offensive
                            lord.setActionText(null);
                            raid.setOffensiveType(null);
                            fleetAI.removeFirstAssignmentIfItIs(FleetAssignment.ORBIT_PASSIVE);
                        }
                    }
                    if (raid.getOffensiveType() == null && (raid.getTotalViolence() >= RAID_MAX_VIOLENCE
                            || newWeight.two <= 0 || rand.nextInt(8) == 0)) {
                        // choose new assignment
                        if (lord.equals(raid.getOriginator())) {
                            EventController.endRaid(raid);
                        } else {
                            raid.getParticipants().remove(lord);
                        }
                        chooseAssignment(lord);
                    } else {
                        // refresh assignment
                        lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp());
                    }
                }
                break;
            case DEFEND_TRANSIT:
                if (lord.isActionComplete()) {
                    lord.setCurrAction(LordAction.DEFEND);
                    Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
                    // TODO decide on whether holding at planet or engaging attacker head-on
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.PATROL_SYSTEM, lord.getTarget(), 1000, null);
                }
                break;
            case DEFEND:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > SMALL_OP_RECONSIDER_INTERVAL) {
                    Pair<LordEvent, Integer> newWeight = EventController.getPreferredDefense(lord);
                    if (newWeight.two <= 0) {
                        // choose new assignment
                        LordEvent raid = EventController.getCurrentDefense(lord);
                        raid.getOpposition().remove(lord);
                        chooseAssignment(lord);
                    } else {
                        // refresh assignment
                        lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp());
                    }
                }
                break;
            case CAMPAIGN:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) > LARGE_OP_RECONSIDER_INTERVAL) {
                    FactionAPI faction = lord.getFaction();
                    LordEvent campaign = EventController.getCurrentCampaign(faction);
                    int weight = EventController.getJoinCampaignWeight(lord);
                    if (campaign.getBattle() != null) {
                        // dont leave if battle ongoing
                        weight = Math.max(weight, 1);
                    }
                    if (weight > 0 && campaign.getTotalViolence() < CAMPAIGN_MAX_VIOLENCE) {
                        // continue campaign
                        lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp());
                        if (campaign.getOriginator().equals(lord)) {
                            if (campaign.getBattle() != null) {
                                // dont do any thinking while ground battle is ongoing
                                if (Utils.nexEnabled()) {
                                    GroundBattleIntel battle = (GroundBattleIntel) campaign.getBattle();
                                    if (battle.getOutcome() != null) {
                                        // TODO add surviving troops back
                                        if (battle.getOutcome()
                                                == GroundBattleIntel.BattleOutcome.ATTACKER_VICTORY) {
                                            campaign.setTotalViolence(campaign.getTotalViolence()
                                                    + LordEvent.OffensiveType.NEX_GROUND_BATTLE.violence);
                                        }
                                        lord.setActionText(null);
                                        campaign.setBattle(null);
                                        fleetAI.removeFirstAssignmentIfItIs(FleetAssignment.ORBIT_PASSIVE);
                                        lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp()
                                                - LARGE_OP_RECONSIDER_INTERVAL * ONE_DAY);
                                    }
                                } else {
                                    // idk, nex got disabled mid-battle? Just abort
                                    lord.setActionText(null);
                                    campaign.setBattle(null);
                                    fleetAI.removeFirstAssignmentIfItIs(FleetAssignment.ORBIT_PASSIVE);
                                    lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp()
                                            - LARGE_OP_RECONSIDER_INTERVAL * ONE_DAY);
                                }
                            } else if (campaign.getTarget() == null) {
                                // consider if we need to change campaign goal
                                // if fleet is still organizing, check if ready to pick target
                                // if fleet is defending, check if defense is still necessary
                                // if fleet is attacking, check if market is conquered?
                                int needed = Math.max(1, 3 * campaign.getParticipants().size() / 4);
                                int close = 0;
                                for (Lord participant : campaign.getParticipants()) {
                                    if (Utils.isClose(lord.getFleet(), participant.getFleet())) close++;
                                }
                                if (close >= needed) {
                                    // choose campaign target
                                    chooseNextCampaignTarget(lord, campaign);
                                }

                            } else {
                                MarketAPI target = campaign.getTarget().getMarket();
                                if (!lord.getFaction().isHostileTo(target.getFaction())) {
                                    // defensive campaign
                                    boolean defenseNeeded = false;
                                    for (LordEvent otherCampaign : EventController.getInstance().getCampaigns()) {
                                        if (otherCampaign.getFaction().isHostileTo(faction)
                                                && otherCampaign.getTarget() != null && otherCampaign.getTarget().getMarket().equals(target)) {
                                            defenseNeeded = true;
                                        }
                                    }
                                    if (!defenseNeeded) {
                                        // choose campaign target
                                        chooseNextCampaignTarget(lord, campaign);
                                    }

                                } else {
                                    // offensive campaign
                                    if (Utils.isSomewhatClose(
                                            lord.getFleet(), campaign.getTarget())) {
                                        if (lord.getFleet().getBattle() == null
                                                && campaign.getOffensiveType() != null
                                                && Utils.getDaysSince(campaign.getOffenseTimestamp())
                                                >= campaign.getOffensiveType().chargeTime) {
                                            // execute offensive
                                            MarketCMD cmd = new MarketCMD(campaign.getTarget());
                                            float attackerStr = Utils.getRaidStr(
                                                    lord.getFleet(), campaign.getTotalMarines());
                                            // raid, tactical bomb, saturation bomb, or ground battle
                                            boolean failed = false;
                                            int cost;
                                            CargoAPI cargo;
                                            switch (campaign.getOffensiveType()) {
                                                case RAID_GENERIC:
                                                    cmd.doGenericRaid(faction, attackerStr, 1);
                                                    break;
                                                case RAID_INDUSTRY:
                                                    Industry raidTarget = Utils.getIndustryToRaid(target);
                                                    if (raidTarget != null) {
                                                        cmd.doIndustryRaid(faction, attackerStr, raidTarget, 1);
                                                    } else {
                                                        failed = true;
                                                    }
                                                    break;
                                                case BOMBARD_TACTICAL:
                                                    cost = MarketCMD.getBombardmentCost(target, lord.getFleet());
                                                    // should we abort if cost somehow rose above lord fuel amount?
                                                    cmd.doBombardment(faction, MarketCMD.BombardType.TACTICAL);
                                                    // TODO remove fuel from all participants
                                                    lord.getFleet().getCargo().removeFuel(cost);
                                                    break;
                                                case BOMBARD_SATURATION:
                                                    cost = MarketCMD.getBombardmentCost(target, lord.getFleet());
                                                    cmd.doBombardment(faction, MarketCMD.BombardType.SATURATION);
                                                    lord.getFleet().getCargo().removeFuel(cost);
                                                    break;
                                                case NEX_GROUND_BATTLE:
                                                    // TODO bomb out station first
//                                                    Industry station = Misc.getStationIndustry(target);
//                                                    if (station != null) {
//                                                        OrbitalStation.disrupt(station);
//                                                    }
                                                    failed = true;  // dont add violence until battle ends
                                                    if (Utils.nexEnabled()) {
                                                        Object battle = beginNexGroundBattle(lord, campaign);
                                                        if (battle != null) {
                                                            lord.setActionText("Supporting Ground Invasion");
                                                            // remove troops and arms
                                                            for (Lord supporter : campaign.getParticipants()) {
                                                                if (campaign.getOriginator().getFleet().getContainingLocation().equals(
                                                                        supporter.getFleet().getContainingLocation())) {
                                                                    cargo = supporter.getFleet().getCargo();
                                                                    cargo.removeMarines(cargo.getMarines());
                                                                    cargo.removeCommodity(Commodities.HAND_WEAPONS,
                                                                            cargo.getCommodityQuantity(Commodities.HAND_WEAPONS));
                                                                }
                                                            }
                                                            cargo = campaign.getOriginator().getFleet().getCargo();
                                                            cargo.removeMarines(cargo.getMarines());
                                                            cargo.removeCommodity(Commodities.HAND_WEAPONS,
                                                                    cargo.getCommodityQuantity(Commodities.HAND_WEAPONS));
                                                        }
                                                        campaign.setBattle(battle);
                                                    }
                                                    break;
                                            }
                                            if (!failed) {
                                                campaign.setTotalViolence(campaign.getTotalViolence()
                                                        + campaign.getOffensiveType().violence);
                                            }
                                            if (campaign.getBattle() == null) {
                                                lord.setActionText(null);
                                                fleetAI.removeFirstAssignmentIfItIs(FleetAssignment.ORBIT_PASSIVE);
                                            }
                                            campaign.setOffensiveType(null);

                                        }
                                        if (lord.getFleet().getBattle() == null
                                                && campaign.getOffensiveType() == null && campaign.getBattle() == null
                                                && campaign.getTotalViolence() < CAMPAIGN_MAX_VIOLENCE) {
                                            // plan new offensive
                                            chooseNewOffensiveType(lord, campaign);
                                        }
                                    } else {
                                        // fleet is distracted, reset any offensive
                                        lord.setActionText(null);
                                        campaign.setOffensiveType(null);
                                        fleetAI.removeFirstAssignmentIfItIs(FleetAssignment.ORBIT_PASSIVE);
                                    }

                                }
                            }

                        } else {
                            // TODO consider if we need to change fleet flags
                        }
                    } else {
                        // quit campaign
                        if (campaign.getOriginator().equals(lord)) {
                            EventController.endCampaign(campaign);
                        } else {
                            campaign.getParticipants().remove(lord);
                            if (campaign.getParticipants().isEmpty()) {
                                EventController.endCampaign(campaign);
                            }
                        }
                        chooseAssignment(lord);
                    }
                }
                break;
            case FOLLOW:
                // TODO are these needed if not following player?
                // check if we need to play some tricks to catch up to target
                SectorEntityToken target = lord.getTarget();
                boolean fleetInWrongSystem = (target.isInHyperspace() && !fleet.isInHyperspace())
                        || !target.getContainingLocation().equals(fleet.getContainingLocation());
                if (fleetInWrongSystem && !fleet.isInHyperspaceTransition()
                        && fleetAI.getCurrentAssignmentType() == FleetAssignment.ORBIT_AGGRESSIVE) {
                    //fleet.getAbility(Abilities.TRANSVERSE_JUMP).activate();
                    JumpPointAPI waypoint = Misc.findNearestJumpPoint(fleet);
                    // TODO why are there multiple destinations?
                    final JumpPointAPI.JumpDestination dest = waypoint.getDestinations().get(0);  // in hyperspace
                    fleetAI.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, waypoint, 7, new Script() {
                        @Override
                        public void run() {
                            Global.getSector().doHyperspaceTransition(fleet, null, dest);
                            Misc.clearFlag(mem, MemFlags.FLEET_IGNORES_OTHER_FLEETS);
                        }
                    });
                    Misc.setFlagWithReason(mem, MemFlags.FLEET_IGNORES_OTHER_FLEETS, BUSY_REASON, true, 7);
                } else if (!target.isInHyperspace() && fleet.isInHyperspace()
                        && !fleet.isInHyperspaceTransition()
                        && fleetAI.getCurrentAssignmentType() == FleetAssignment.ORBIT_AGGRESSIVE) {
                    JumpPointAPI waypoint = Misc.findNearestJumpPoint(target);
                    final JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(
                            waypoint, null);
                    fleetAI.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION,
                            waypoint.getDestinations().get(0).getDestination(), 7, new Script() {
                                @Override
                                public void run() {
                                    Global.getSector().doHyperspaceTransition(fleet, null, dest);
                                    Misc.clearFlag(mem, MemFlags.FLEET_IGNORES_OTHER_FLEETS);
                                }
                            });
                    Misc.setFlagWithReason(mem, MemFlags.FLEET_IGNORES_OTHER_FLEETS, BUSY_REASON, true, 7);

                }
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) >= FOLLOW_DURATION) {
                    chooseAssignment(lord);
                }
                break;
            case IMPRISONED:
                Lord captor = LordController.getLordOrPlayerById(lord.getCaptor());
                if (captor == null || !captor.getFaction().isHostileTo(lord.getFaction())) {
                    beginRespawn(lord);
                    if (captor != null) {
                        lord.setCaptor(null);
                        captor.removePrisoner(lord.getLordAPI().getId());
                        Global.getSector().getCampaignUI().addMessage(
                                StringUtil.getString(CATEGORY_UI, "lord_freed_captivity",
                                        lord.getTitle() + " " + lord.getLordAPI().getNameString()),
                                lord.getFaction().getBaseUIColor());
                    }
                } else if (Utils.getDaysSince(lord.getAssignmentStartTime()) >= PRISON_ESCAPE_DURATION) {
                    if (new Random(lord.getLordAPI().getId().hashCode()
                            * lord.getAssignmentStartTime()).nextInt(100) < PRISON_ESCAPE_CHANCE) {
                        if (captor.isPlayer()) {
                            Global.getSector().getCampaignUI().addMessage(
                                    StringUtil.getString(CATEGORY_UI, "lord_escaped_captivity",
                                            lord.getTitle() + " " + lord.getLordAPI().getNameString(),
                                            captor.getFaction().getDisplayName()), Color.RED);
                        }
                        // TODO reduce relations with captor?
                        captor.removePrisoner(lord.getLordAPI().getId());
                        lord.setCaptor(null);
                        beginRespawn(lord);
                    }
                    lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp());
                }
                break;
            case RESPAWNING:
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) >= RESPAWN_DURATION) {
                    completeRespawn(lord);
                }
                break;
        }
    }

    // calls when feasts, raids, and campaigns begin, to preempt lords on less important tasks
    public static void triggerPreemptingEvent(LordEvent event) {
        if (event.getType().equals(LordEvent.FEAST)) {
            FactionAPI faction = event.getFaction();
            for (Lord lord : LordController.getLordsList()) {
                if (!lord.getFaction().equals(faction)) continue;
                if (lord.getCurrAction() == null || lord.getOrderPriority() > event.getAction().priority) {
                    // TODO check if lord wants to go to event, esp if already standing down
                    beginAssignment(lord, LordAction.FEAST_TRANSIT, event.getTarget(), event, false);
                }
            }
        }
        if (event.getType().equals(LordEvent.RAID)) {
            FactionAPI attackerFaction = event.getFaction();
            FactionAPI defenderFaction = event.getTarget().getMarket().getFaction();

            PriorityQueue<Pair<Lord, Integer>> attackPQ = new PriorityQueue<>(11, new Comparator<Pair<Lord, Integer>>() {
                @Override
                public int compare(Pair<Lord, Integer> o1, Pair<Lord, Integer> o2) {
                    return Integer.compare(o2.two, o1.two);
                }
            });
            PriorityQueue<Pair<Lord, Integer>> defendPQ = new PriorityQueue<>(11, new Comparator<Pair<Lord, Integer>>() {
                @Override
                public int compare(Pair<Lord, Integer> o1, Pair<Lord, Integer> o2) {
                    return Integer.compare(o2.two, o1.two);
                }
            });
            for (Lord lord : LordController.getLordsList()) {
                if (lord.getFaction().equals(attackerFaction)) {
                    if (lord.getCurrAction() == null || lord.getOrderPriority() > event.getAction().priority) {
                        int weight = EventController.getMilitaryOpWeight(lord, event.getTarget().getMarket(), event, false);
                        if (weight > 0) {
                            attackPQ.add(new Pair<>(lord, weight));
                        }
                    }
                } else if (lord.getFaction().equals(defenderFaction)) {
                    if (lord.getCurrAction() == null || lord.getOrderPriority() > LordAction.DEFEND.priority) {
                        int weight = EventController.getMilitaryOpWeight(lord, event.getTarget().getMarket(), event, true);
                        if (weight > 0) {
                            defendPQ.add(new Pair<>(lord, weight));
                        }
                    }
                }
            }

            int penalty = 0;
            Pair<Lord, Integer> curr = attackPQ.poll();
            while (curr != null && curr.two - penalty > 0) {
                beginAssignment(curr.one, LordAction.RAID_TRANSIT, event.getTarget(), event, false);
                penalty += EventController.CROWDED_MALUS;
                curr = attackPQ.poll();
            }

            penalty = 0;
            curr = defendPQ.poll();
            while (curr != null && curr.two - penalty > 0) {
                beginAssignment(curr.one, LordAction.DEFEND_TRANSIT, event.getTarget(), event, false);
                penalty += EventController.CROWDED_MALUS;
                curr = defendPQ.poll();
            }
        }
        if (event.getType().equals(LordEvent.CAMPAIGN)) {
            FactionAPI faction = event.getFaction();
            for (Lord lord : LordController.getLordsList()) {
                // start opposing campaign
                // TODO account for cooldown?
                if (lord.isMarshal() && faction.isHostileTo(lord.getFaction())
                        && lord.getOrderPriority() > LordAction.CAMPAIGN.priority) {
                    int weight = EventController.getStartCampaignWeight(lord);
                    if (weight > 0) {
                        beginAssignment(lord, LordAction.CAMPAIGN, null, null, false);
                    }
                }
                // join current campaign
                if (lord.getFaction().equals(faction)
                        && lord.getOrderPriority() > LordAction.CAMPAIGN.priority) {
                    int weight = EventController.getJoinCampaignWeight(lord);
                    if (weight > 0) {
                        beginAssignment(lord, LordAction.CAMPAIGN, null, event, false);
                    }
                }
            }

        }
    }

    private static void beginRespawn(Lord lord) {
        CampaignFleetAIAPI fleetAI = lord.getFleet().getAI();
        MemoryAPI mem = lord.getFleet().getMemoryWithoutUpdate();

        EventController.removeFromAllEvents(lord);
        lord.setCurrAction(LordAction.RESPAWNING);
        lord.setTarget(null);
        fleetAI.clearAssignments();
        Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
    }

    private static void completeRespawn(Lord lord) {
        CampaignFleetAPI fleet = lord.getFleet();
        SectorEntityToken respawnPoint = lord.getClosestBase();
        if (respawnPoint == null) {
            respawnPoint = Misc.findNearestPlanetTo(fleet, false, true);
        }
        respawnPoint.getContainingLocation().addEntity(fleet);
        fleet.setLocation(respawnPoint.getLocation().x, respawnPoint.getLocation().y);
        LordFleetFactory.addToLordFleet(new ShipRolePick(lord.getTemplate().flagShip), fleet, new Random());
        fleet.getFleetData().getMembersInPriorityOrder().get(0).setFlagship(true);
        float cost = LordFleetFactory.addToLordFleet(lord.getTemplate().shipPrefs, fleet, new Random(), 75, 1e8f);
        LordFleetFactory.populateCaptains(lord);
        lord.addWealth(-1 * cost);
        lord.setCurrAction(null);
        fleet.setHidden(false);
        fleet.setExpired(false);
        fleet.inflateIfNeeded();
        fleet.forceSync();
        fleet.fadeInIndicator();
    }

    private static void chooseNextCampaignTarget(Lord lord, LordEvent campaign) {
        CampaignFleetAPI fleet = lord.getFleet();
        CampaignFleetAIAPI fleetAI = fleet.getAI();
        MemoryAPI mem = lord.getFleet().getMemoryWithoutUpdate();
        MarketAPI target = EventController.getCampaignTarget(lord);

        if (target == null) {
            // whoops, faction must be at peace
            EventController.endCampaign(campaign);
        } else {
            campaign.setTarget(target.getPrimaryEntity());
            fleetAI.clearAssignments();
            if (target.getFaction().isHostileTo(lord.getFaction())) {
                if (!fleet.getContainingLocation().equals(target.getContainingLocation())) {
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.GO_TO_LOCATION, Misc.findNearestJumpPoint(target.getPrimaryEntity()), 1000, null);
                }
                fleetAI.addAssignment(
                        FleetAssignment.ATTACK_LOCATION, target.getPrimaryEntity(), 1000, null);
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
                if (target.getFaction().equals(Utils.getRecruitmentFaction())
                        || target.getFaction().equals(Global.getSector().getPlayerFaction())) {
                    Global.getSector().getIntelManager().addIntel(new HostileEventIntelPlugin(campaign));
                }

            } else {
                if (!fleet.getContainingLocation().equals(target.getContainingLocation())) {
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.GO_TO_LOCATION, Misc.findNearestJumpPoint(target.getPrimaryEntity()), 1000, null);
                }
                fleetAI.addAssignment(
                        FleetAssignment.GO_TO_LOCATION, target.getPrimaryEntity(), 1000, null);
                fleetAI.addAssignment(
                        FleetAssignment.DEFEND_LOCATION, target.getPrimaryEntity(), 1000, null);
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
            }
        }
    }

    // simulates lord doing something like collecting taxes, feasting, upgrading ships, etc.
    private static void standby(Lord lord, SectorEntityToken target, String message) {
        CampaignFleetAIAPI fleetAI = lord.getFleet().getAI();
        MemoryAPI mem = lord.getFleet().getMemoryWithoutUpdate();

        fleetAI.clearAssignments();
        fleetAI.addAssignmentAtStart(
                FleetAssignment.ORBIT_PASSIVE, target, 1000, message, null);
        Misc.setFlagWithReason(mem,
                MemFlags.FLEET_BUSY, BUSY_REASON, true, STANDBY_DURATION);
        Misc.setFlagWithReason(mem,
                MemFlags.FLEET_IGNORES_OTHER_FLEETS, BUSY_REASON, true, STANDBY_DURATION);
    }

    private static void chooseNewOffensiveType(Lord lord, LordEvent event) {
        int maxViolence = event.getType().equals(LordEvent.CAMPAIGN) ? CAMPAIGN_MAX_VIOLENCE : RAID_MAX_VIOLENCE;
        MarketAPI market = event.getTarget().getMarket();
        ArrayList<LordEvent.OffensiveType> options = new ArrayList<>();
        ArrayList<Integer> weights = new ArrayList<>();
        int fuelCost = MarketCMD.getBombardmentCost(market, lord.getFleet());
        int fuelAmt = (int) event.getTotalFuel();

        // perform harassment if lord fleet can't challenge defenses
        options.add(LordEvent.OffensiveType.RAID_GENERIC);
        weights.add(3);
        if (Utils.canRaidIndustry(market)) {
            options.add(LordEvent.OffensiveType.RAID_INDUSTRY);
            weights.add(3);
        }
        if (maxViolence >= LordEvent.OffensiveType.BOMBARD_TACTICAL.violence && fuelAmt >= fuelCost) {
            options.add(LordEvent.OffensiveType.BOMBARD_TACTICAL);
            weights.add(10);
        }
        if (lord.getPersonality().equals(LordPersonality.QUARRELSOME)
                && maxViolence >= LordEvent.OffensiveType.BOMBARD_SATURATION.violence && fuelAmt >= fuelCost) {
            options.add(LordEvent.OffensiveType.BOMBARD_SATURATION);
            weights.add(20);
        }
        if (Utils.nexEnabled() && maxViolence >= LordEvent.OffensiveType.NEX_GROUND_BATTLE.violence) {
            GroundBattleIntel tmp = new GroundBattleIntel(market, lord.getFaction(), market.getFaction());
            tmp.init();
            float defenderStr = GBUtils.estimateTotalDefenderStrength(tmp, true);
            tmp.endImmediately();
            float marines = event.getTotalMarines();
            float heavies = event.getTotalArms();
            marines = Math.max(0, marines - heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).personnel.mult);
            float attackerStr = marines * GroundUnitDef.getUnitDef(GroundUnitDef.MARINE).strength
                    + heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).strength;
            if (attackerStr > 0.8 * defenderStr) {
                options.add(LordEvent.OffensiveType.NEX_GROUND_BATTLE);
                weights.add(20);
            }
        }

        LordEvent.OffensiveType choice = Utils.weightedSample(options, weights, null);
        lord.getFleet().addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, market.getPrimaryEntity(), 2,
                StringUtil.getString(CATEGORY, "offensive_" + choice.toString().toLowerCase(), "Preparing"), null);
        lord.getFleet().addAssignmentAtStart(FleetAssignment.ORBIT_PASSIVE, market.getPrimaryEntity(), 100,
                StringUtil.getString(CATEGORY, "offensive_" + choice.toString().toLowerCase(), "Preparing"), null);
        //lord.setActionText(StringUtil.getString(CATEGORY, "offensive_" + choice.toString().toLowerCase(), "Preparing"));
        event.setOffensiveType(choice);
        event.setOffenseTimestamp(Global.getSector().getClock().getTimestamp());
    }


    public static Object beginNexGroundBattle(Lord leader, LordEvent campaign) {
        MarketAPI target = campaign.getTarget().getMarket();
        FactionAPI faction = leader.getFaction();
        GroundBattleIntel groundBattle = GroundBattleIntel.getOngoing(target);
        if (groundBattle == null || groundBattle.getOutcome() != null) {
            groundBattle = new GroundBattleIntel(target, faction, target.getFaction());
            groundBattle.init();
            groundBattle.start();
        }

        Boolean side = groundBattle.getSideToSupport(faction, false);
        if (side == null) return null;

        CampaignFleetAPI fleet = leader.getFleet();
        int marines = (int) campaign.getTotalMarines();
        int heavyArms = (int) campaign.getTotalArms();

        // create units
        List<GroundUnit> units = groundBattle.autoGenerateUnits(marines, heavyArms, faction, side, false, fleet);
        groundBattle.runAI(side, false);
        return groundBattle;
    }

    public static void playerOrder(Lord lord, LordAction order, SectorEntityToken target) {
        EventController.removeFromAllEvents(lord);
        beginAssignment(lord, order, target, null, true);
    }
}
