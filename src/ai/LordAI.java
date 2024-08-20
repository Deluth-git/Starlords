package ai;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import controllers.*;
import faction.Lawset;
import org.apache.log4j.Logger;
import person.Lord;
import person.LordAction;
import person.LordEvent;
import scripts.ActionCompleteScript;
import scripts.RemoveBusyFlagScript;
import util.LordFleetFactory;
import util.StringUtil;
import util.Utils;

import java.awt.*;
import java.util.*;

public class LordAI implements EveryFrameScript {

    public static Logger log = Global.getLogger(LordAI.class);
    // all times in days
    public static final float UPDATE_INTERVAL = 0.25f;
    public static final int PATROL_DURATION = 30;
    public static final int RESPAWN_DURATION = 7;
    public static final int STANDBY_DURATION = 7;
    public static final int SMALL_OP_RECONSIDER_INTERVAL = 7;
    public static final int LARGE_OP_RECONSIDER_INTERVAL = 7;
    public static final int[] FEAST_COOLDOWN = new int[]{30, 90, 180, 365, 1000000};
    public static final int FOLLOW_DURATION = 30;
    public static final int CAMPAIGN_COOLDOWN = 180;
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
        int currDay = Global.getSector().getClock().getDay();
        long timestamp = Global.getSector().getClock().getTimestamp();
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
                    log.info("Campaign start weight: " + weight);
                    //weight += 1000000; // TODO
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
            if (raidWeight > 0) {
                priority = LordAction.RAID.priority;
            }

            // starting own raid
            Pair<SectorEntityToken, Integer> ownAttackChoice = EventController.getPreferredRaidLocation(lord);
            if (ownAttackChoice.two > raidWeight) {
                attack = null;
                raidWeight = ownAttackChoice.two;
                attackTarget = ownAttackChoice.one;
            }

            if (raidWeight > 0) {
                weights.add(raidWeight);
                actions.add(LordAction.RAID_TRANSIT);
            }
        }
        LordEvent defense = null;
        if (priority >= LordAction.DEFEND.priority) {
            Pair<LordEvent, Integer> defenseChoice = EventController.getPreferredDefense(lord);
            defense = defenseChoice.one;
            int defenseWeight = defenseChoice.two;
            if (defense != null) {
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
                priority = LordAction.FEAST.priority;
                if (lord.getCurrAction() != LordAction.FEAST) {
                    // weight of joining existing feast
                    feastWeight = 100;
                } else {
                    // weight of staying at feast
                    if (!currFeast.getOriginator().equals(lord)) {
                        feastWeight = 20;
                    } else if (Utils.getDaysSince(currFeast.getStart()) < FEAST_MAX_DURATION) {
                        feastWeight = 250;
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
            patrolWeight *= milLevel;
            weights.add(patrolWeight);
            actions.add(LordAction.PATROL_TRANSIT);
        }

        if (priority >= LordAction.COLLECT_TAXES.priority) {
            int taxWeight = 0;
            for (SectorEntityToken fief : lord.getFiefs()) {
                taxWeight += FiefController.getTax(fief.getMarket()) / 1000;
            }
            weights.add(taxWeight);
            actions.add(LordAction.COLLECT_TAXES_TRANSIT);
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
                weights.add(ventureWeight);
                actions.add(LordAction.VENTURE_TRANSIT);
            }
        }

        if (priority >= LordAction.UPGRADE_FLEET.priority) {
            int upgradeWeight = 0;
            if (lord.getLordAPI().getFleet().getFlagship() == null) {
                upgradeWeight += 25;
            }
            if (econLevel > 1) {
                upgradeWeight += 25;
            }
            if (milLevel < 1 && econLevel > milLevel) {
                upgradeWeight += 10;
                upgradeWeight *= 2;
            }
            weights.add(upgradeWeight);
            actions.add(LordAction.UPGRADE_FLEET_TRANSIT);
        }

        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        int choice = rand.nextInt(totalWeight);
        LordAction currAction = null;
        for (int i = 0; i < weights.size(); i++) {
            if (weights.get(i) > choice) {
                currAction = actions.get(i);
                break;
            } else {
                choice -= weights.get(i);
            }
        }
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
        CampaignFleetAIAPI fleetAI = lord.getLordAPI().getFleet().getAI();
        MemoryAPI mem = lord.getLordAPI().getFleet().getMemoryWithoutUpdate();
        fleetAI.clearAssignments();
        Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
        Misc.clearFlag(mem, MemFlags.FLEET_IGNORES_OTHER_FLEETS);

        // choose assignment target
        SectorEntityToken targetEntity;
        switch(currAction) {
            case UPGRADE_FLEET_TRANSIT:
                // same as collect taxes
            case PATROL_TRANSIT:
                // same as collect taxes
            case COLLECT_TAXES_TRANSIT:
                targetEntity = lord.getClosestBase();
                if (targetEntity == null) {
                    targetEntity = Misc.findNearestLocalMarket(lord.getLordAPI().getFleet(), 1e10f, null).getPrimaryEntity();
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
                        rallyPoint = Misc.findNearestPlanetTo(lord.getLordAPI().getFleet(), false, true);
                    }
                    lord.setTarget(rallyPoint);
                    fleetAI.addAssignmentAtStart(
                            FleetAssignment.GO_TO_LOCATION, rallyPoint, 1000, null);
                    fleetAI.addAssignment(FleetAssignment.ORBIT_PASSIVE, rallyPoint, 1000, null);
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
        CampaignFleetAIAPI fleetAI = lord.getLordAPI().getFleet().getAI();
        MemoryAPI mem = lord.getLordAPI().getFleet().getMemoryWithoutUpdate();
        // check for lord fleet defeat
        if (lord.getLordAPI().getFleet().isEmpty() && lord.getCurrAction() != LordAction.RESPAWNING) {
            beginRespawn(lord);
            return;
        }

        // check if target is now invalid due to relation change
        // TODO campaigns need special logic
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
                    RelationController.modifyRelation(lord, currFeast.getOriginator(), 3);
                    for (Lord participant : currFeast.getParticipants()) {
                        RelationController.modifyRelation(lord, participant, 2);
                    }
                    if (!currFeast.getOriginator().equals(lord)) {
                        lord.setFeastInteracted(false);
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
                    if (newWeight.two <= 0 || rand.nextInt(8) == 0) {
                        // choose new assignment
                        LordEvent raid = EventController.getCurrentRaid(lord);
                        if (raid.getOriginator().equals(lord)) {
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
                    if (weight > 0) {
                        // continue campaign
                        lord.setAssignmentStartTime(Global.getSector().getClock().getTimestamp());
                        if (campaign.getOriginator().equals(lord)) {
                            // consider if we need to change campaign goal
                            // if fleet is still organizing, check if ready to pick target
                            // if fleet is defending, check if defense is still necessary
                            // if fleet is attacking, check if market is conquered?
                            if (campaign.getTarget() == null) {
                                int needed = Math.max(1, 3 * campaign.getParticipants().size() / 4);
                                int close = 0;
                                for (Lord participant : campaign.getParticipants()) {
                                    if (Utils.isClose(lord.getLordAPI().getFleet(), participant.getLordAPI().getFleet())) close++;
                                }
                                if (close >= needed) {
                                    // choose campaign target
                                    chooseNextCampaignTarget(lord, campaign);
                                }

                            } else {
                                MarketAPI target = campaign.getTarget().getMarket();
                                if (!target.getFaction().isHostileTo(lord.getFaction())) {
                                    // defensive campaign
                                    boolean defenseNeeded = false;
                                    for (LordEvent otherCampaign : EventController.getInstance().getCampaigns()) {
                                        if (otherCampaign.getOriginator().getFaction().isHostileTo(faction)
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
                if (Utils.getDaysSince(lord.getAssignmentStartTime()) >= FOLLOW_DURATION) {
                    chooseAssignment(lord);
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
            FactionAPI faction = event.getOriginator().getFaction();
            for (Lord lord : LordController.getLordsList()) {
                if (!lord.getFaction().equals(faction)) continue;
                if (lord.getCurrAction() == null || lord.getOrderPriority() > event.getAction().priority) {
                    // TODO check if lord wants to go to event, esp if already standing down
                    beginAssignment(lord, LordAction.FEAST_TRANSIT, event.getTarget(), event, false);
                }
            }
        }
        if (event.getType().equals(LordEvent.RAID)) {
            FactionAPI attackerFaction = event.getOriginator().getFaction();
            FactionAPI defenderFaction = event.getTarget().getMarket().getFaction();

            PriorityQueue<Pair<Lord, Integer>> attackPQ = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.two, o1.two));
            PriorityQueue<Pair<Lord, Integer>> defendPQ = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.two, o1.two));
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
            FactionAPI faction = event.getOriginator().getFaction();
            for (Lord lord : LordController.getLordsList()) {
                // start opposing campaign
                if (lord.isMarshal() && lord.getOrderPriority() > LordAction.CAMPAIGN.priority) {
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
        CampaignFleetAIAPI fleetAI = lord.getLordAPI().getFleet().getAI();
        MemoryAPI mem = lord.getLordAPI().getFleet().getMemoryWithoutUpdate();

        // remove any events lord is participating in
        switch (LordAction.base(lord.getCurrAction())) {
            case RAID:
                LordEvent raid = EventController.getCurrentRaid(lord);
                if (raid != null) {
                    if (raid.getOriginator().equals(lord)) {
                        EventController.endCampaign(raid);
                    } else {
                        raid.getParticipants().remove(lord);
                    }
                }
                break;
            case DEFEND:
                LordEvent defense = EventController.getCurrentDefense(lord);
                if (defense != null) {
                    defense.getOpposition().remove(lord);
                }
                break;
            case CAMPAIGN:
                LordEvent campaign = EventController.getCurrentCampaign(lord.getFaction());
                if (campaign != null) {
                    if (campaign.getOriginator().equals(lord)) {
                        EventController.endCampaign(campaign);
                    } else {
                        campaign.getParticipants().remove(lord);
                        if (campaign.getParticipants().isEmpty()) {
                            EventController.endCampaign(campaign);
                        }
                    }
                }
                break;
            case FEAST:
                LordEvent feast = EventController.getCurrentFeast(lord.getFaction());
                if (feast != null) {
                    if (feast.getOriginator().equals(lord)) {
                        EventController.endFeast(feast);
                    } else {
                        feast.getParticipants().remove(lord);
                    }
                }
                break;
        }

        lord.setCurrAction(LordAction.RESPAWNING);
        lord.setTarget(null);
        fleetAI.clearAssignments();
        Misc.clearFlag(mem, MemFlags.FLEET_BUSY);
    }

    private static void completeRespawn(Lord lord) {
        CampaignFleetAPI fleet = lord.getLordAPI().getFleet();
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
        CampaignFleetAIAPI fleetAI = lord.getLordAPI().getFleet().getAI();
        MemoryAPI mem = lord.getLordAPI().getFleet().getMemoryWithoutUpdate();
        MarketAPI target = EventController.getCampaignTarget(lord);

        if (target == null) {
            // whoops, faction must be at peace
            EventController.endCampaign(campaign);
        } else {
            campaign.setTarget(target.getPrimaryEntity());
            fleetAI.clearAssignments();
            if (target.getFaction().isHostileTo(lord.getFaction())) {
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, Misc.findNearestJumpPoint(target.getPrimaryEntity()), 1000, null);
                fleetAI.addAssignment(
                        FleetAssignment.ATTACK_LOCATION, target.getPrimaryEntity(), 1000, null);
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);

            } else {
                fleetAI.addAssignmentAtStart(
                        FleetAssignment.GO_TO_LOCATION, target.getPrimaryEntity(), 1000, null);
                fleetAI.addAssignment(
                        FleetAssignment.PATROL_SYSTEM, target.getPrimaryEntity(), 1000, null);
                Misc.setFlagWithReason(mem, MemFlags.FLEET_BUSY, BUSY_REASON, true, 1000);
            }
        }
    }

    // simulates lord doing something like collecting taxes, feasting, upgrading ships, etc.
    private static void standby(Lord lord, SectorEntityToken target, String message) {
        CampaignFleetAIAPI fleetAI = lord.getLordAPI().getFleet().getAI();
        MemoryAPI mem = lord.getLordAPI().getFleet().getMemoryWithoutUpdate();

        fleetAI.clearAssignments();
        fleetAI.addAssignmentAtStart(
                FleetAssignment.ORBIT_PASSIVE, target, 1000, message, null);
        Misc.setFlagWithReason(mem,
                MemFlags.FLEET_BUSY, BUSY_REASON, true, STANDBY_DURATION);
        Misc.setFlagWithReason(mem,
                MemFlags.FLEET_IGNORES_OTHER_FLEETS, BUSY_REASON, true, STANDBY_DURATION);
    }

    public static void playerOrder(Lord lord, LordAction order, SectorEntityToken target) {
        EventController.removeFromAllEvents(lord);
        beginAssignment(lord, order, target, null, true);
    }
}
