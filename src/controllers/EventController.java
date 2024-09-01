package controllers;

import ai.LordAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import lombok.Getter;
import org.apache.log4j.Logger;
import person.Lord;
import person.LordAction;
import person.LordEvent;
import person.LordPersonality;
import ui.EventIntelPlugin;
import util.StringUtil;
import util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static ai.LordAI.RAID_COOLDOWN;

// Catalogues events that Lords may want to participate in
// such as ongoing campaigns, contested sectors, pirate infestations, or feasts
// has nothing to do with java events
// also this isn't actually intel, it's only used for easy save/loading
public class EventController extends BaseIntelPlugin {

    // TODO nex invasions, pirate activity events
    private List<LordEvent> feasts = new ArrayList<>();
    private List<LordEvent> raids = new ArrayList<>();
    @Getter
    private List<LordEvent> campaigns = new ArrayList<>();

    // records time since last feast.raid
    private HashMap<String, Long> feastCounter = new HashMap<>();
    private HashMap<String, Long> campaignCounter = new HashMap<>();

    private static EventController instance = null;
    public static Logger log = Global.getLogger(EventController.class);
    private static String CATEGORY = "starlords_ui";

    public static final int CROWDED_MALUS = 25;

    private EventController() {
        setHidden(true);
    }

    public static long getLastFeastTime(FactionAPI faction) {
        if (!getInstance().feastCounter.containsKey(faction.getId())) return 0;
        return getInstance().feastCounter.get(faction.getId());
    }

    public static long getLastCampaignTime(FactionAPI faction) {
        if (!getInstance().campaignCounter.containsKey(faction.getId())) return 0;
        return getInstance().campaignCounter.get(faction.getId());
    }

    public static LordEvent getCurrentFeast(FactionAPI faction) {
        for (LordEvent feast : getInstance().feasts) {
            if (feast.getOriginator().getFaction().equals(faction)) {
                return feast;
            }
        }
        return null;
    }

    public static LordEvent getCurrentRaid(Lord lord) {
        for (LordEvent raid : getInstance().raids) {
            if (raid.getOriginator().equals(lord) || raid.getParticipants().contains(lord)) return raid;
        }
        return null;
    }

    public static LordEvent getCurrentDefense(Lord lord) {
        for (LordEvent raid : getInstance().raids) {
            if (raid.getOpposition().contains(lord)) return raid;
        }
        for (LordEvent raid : getInstance().campaigns) {
            if (raid.getOpposition().contains(lord)) return raid;
        }
        return null;
    }

    public static LordEvent getCurrentCampaign(FactionAPI faction) {
        for (LordEvent feast : getInstance().campaigns) {
            if (feast.getOriginator().getFaction().equals(faction)) {
                return feast;
            }
        }
        return null;
    }

    public static void addRaid(LordEvent raid) {
        getInstance().raids.add(raid);
        LordAI.triggerPreemptingEvent(raid);
    }

    public static void endRaid(LordEvent raid) {
        getInstance().raids.remove(raid);
        raid.setAlive(false);
        for (Lord lord : raid.getParticipants()) {
            lord.setCurrAction(null);
        }
        for (Lord lord : raid.getOpposition()) {
            lord.setCurrAction(null);
        }
    }

    public static void addCampaign(LordEvent campaign) {
        getInstance().campaigns.add(campaign);
        LordAI.triggerPreemptingEvent(campaign);
        FactionAPI faction = campaign.getOriginator().getFaction();
        if (faction.getId().equals(Misc.getCommissionFactionId()) || faction.getId().equals(Factions.PLAYER)) {
            // TODO add some sound
            Global.getSector().getIntelManager().addIntel(new EventIntelPlugin(campaign));
            if (campaign.getOriginator().isPlayer()) {
                Global.getSector().getCampaignUI().addMessage(
                        StringUtil.getString(CATEGORY, "campaign_start",
                                campaign.getOriginator().getLordAPI().getNameString(),
                                Misc.getNearestStarSystem(campaign.getOriginator().getFleet()).getBaseName()),
                        faction.getBaseUIColor());
            } else {
                Global.getSector().getCampaignUI().addMessage(
                        StringUtil.getString(CATEGORY, "campaign_start",
                                campaign.getOriginator().getLordAPI().getNameString(),
                                campaign.getOriginator().getTarget().getMarket().getName()),
                        faction.getBaseUIColor());
            }
        }
    }

    public static void endCampaign(LordEvent campaign) {
        getInstance().campaigns.remove(campaign);
        campaign.setAlive(false);
        for (Lord lord : campaign.getParticipants()) {
            lord.setCurrAction(null);
        }
        for (Lord lord : campaign.getOpposition()) {
            lord.setCurrAction(null);
        }
        if (campaign.getOriginator().getCurrAction() == LordAction.CAMPAIGN) {
            campaign.getOriginator().setCurrAction(null);
        }

        FactionAPI faction = campaign.getOriginator().getFaction();
        if (faction.getId().equals(Misc.getCommissionFactionId()) || faction.getId().equals(Factions.PLAYER)) {
            Global.getSector().getCampaignUI().addMessage(
                    StringUtil.getString(CATEGORY, "campaign_end"),
                    faction.getBaseUIColor());
        }
        getInstance().campaignCounter.put(faction.getId(), Global.getSector().getClock().getTimestamp());
    }

    // lord must be the marshal leading an existing campaign
    public static MarketAPI getCampaignTarget(Lord lord) {
        FactionAPI faction = lord.getLordAPI().getFaction();;
        MarketAPI preferred = null;
        int preferredWeight = Integer.MIN_VALUE;
        // check defensive options
        for (LordEvent campaign : getInstance().campaigns) {
            if (campaign.getOriginator().getFaction().isHostileTo(faction) && campaign.getTarget() != null) {
                FactionAPI defender = campaign.getTarget().getFaction();
                int weight = Integer.MIN_VALUE;
                if (defender.equals(faction)) {
                    weight = 35000 - (int) Utils.getHyperspaceDistance(campaign.getTarget(), lord.getLordAPI().getFleet());
                } else if (defender.isAtWorst(faction, RepLevel.FAVORABLE)) {
                    weight = 25000 - (int) Utils.getHyperspaceDistance(campaign.getTarget(), lord.getLordAPI().getFleet());
                }
                if (weight > preferredWeight) {
                    preferred = campaign.getTarget().getMarket();
                    preferredWeight = weight;
                }
            }
        }
        // check offensive options
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            FactionAPI otherFaction = market.getFaction();
            if (!Misc.isPirateFaction(otherFaction) && (LordController.getFactionsWithLords().contains(otherFaction) || otherFaction.isPlayerFaction())
                    && otherFaction.isHostileTo(faction)) {
                int weight = 15000 - (int) Utils.getHyperspaceDistance(market.getPrimaryEntity(), lord.getLordAPI().getFleet());
                if (weight > preferredWeight) {
                    preferred = market;
                    preferredWeight = weight;
                }
            }
        }
        if (preferredWeight < 0) return null;
        return preferred;
    }

    // gets ongoing raid that lord would most prefer joining
    public static Pair<LordEvent, Integer> getPreferredRaidAttack(Lord lord) {
        LordEvent preferred = null;
        int preferredWeight = 0;
        for (LordEvent raid : getInstance().raids) {
            if (!raid.getOriginator().getFaction().equals(lord.getLordAPI().getFaction())) continue;
            int currWeight = getMilitaryOpWeight(lord, raid.getTarget().getMarket(), raid, false);

            if (currWeight > preferredWeight) {
                preferred = raid;
                preferredWeight = currWeight;
            }
        }
        return new Pair<>(preferred, preferredWeight);
    }

    // gets ongoing defensive operation that lord would most prefer joining
    public static Pair<LordEvent, Integer> getPreferredDefense(Lord lord) {
        LordEvent preferred = null;
        int preferredWeight = 0;
        for (LordEvent raid : getInstance().raids) {
            if (!raid.getTarget().getFaction().equals(lord.getLordAPI().getFaction())) continue;
            int currWeight = getMilitaryOpWeight(lord, raid.getTarget().getMarket(), raid, true);

            if (currWeight > preferredWeight) {
                preferred = raid;
                preferredWeight = currWeight;
            }
        }
        for (LordEvent raid : getInstance().campaigns) {
            // make sure it's not a defensive or in-preparation campaign
            if (raid.getTarget() == null || !raid.getTarget().getFaction().equals(lord.getLordAPI().getFaction())) continue;
            if (!lord.getLordAPI().getFaction().isHostileTo(raid.getOriginator().getFaction())) continue;
            int currWeight = getMilitaryOpWeight(lord, raid.getTarget().getMarket(), raid, true);

            if (currWeight > preferredWeight) {
                preferred = raid;
                preferredWeight = currWeight;
            }
        }
        return new Pair<>(preferred, preferredWeight);
    }

    // gets location that lord would stage a raid, if he wanted to start his own
    public static Pair<SectorEntityToken, Integer>  getPreferredRaidLocation(Lord lord) {
        MarketAPI preferred = null;
        int preferredWeight = 0;
        // skip locations that already have a raid from the same faction
        HashSet<MarketAPI> seen = new HashSet<>();
        for (LordEvent raid : getInstance().raids) {
            if (raid.getOriginator().getFaction().equals(lord.getLordAPI().getFaction())) {
                seen.add(raid.getTarget().getMarket());
            }
        }
        for (LordEvent raid : getInstance().campaigns) {
            if (raid.getOriginator().equals(lord.getLordAPI().getFaction())
                    && raid.getTarget() != null) {
                seen.add(raid.getTarget().getMarket());
            }
        }
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.getFaction().isHostileTo(lord.getLordAPI().getFaction())) continue;
            if (Misc.isPirateFaction(market.getFaction())) continue;
            if (seen.contains(market)) continue;
            if (Misc.getDaysSinceLastRaided(market) < RAID_COOLDOWN) continue;
            int currWeight = getMilitaryOpWeight(lord, market, null, false);
            if (currWeight > preferredWeight) {
                preferred = market;
                preferredWeight = currWeight;
            }
        }

        SectorEntityToken target = null;
        if (preferred != null) {
            target = preferred.getPrimaryEntity();
        }
        return new Pair<>(target, preferredWeight);
    }


    public static void addFeast(LordEvent feast) {
        getInstance().feasts.add(feast);
        LordAI.triggerPreemptingEvent(feast);
        FactionAPI faction = feast.getOriginator().getFaction();
        if (faction.getId().equals(Misc.getCommissionFactionId()) || faction.getId().equals(Factions.PLAYER)) {
            Global.getSector().getCampaignUI().addMessage(
                    StringUtil.getString(CATEGORY, "feast_start",
                            feast.getOriginator().getLordAPI().getNameString(),
                            feast.getTarget().getMarket().getName()),
                    faction.getBaseUIColor());
            Global.getSector().getIntelManager().addIntel(new EventIntelPlugin(feast));
        }
    }

    public static void endFeast(LordEvent feast) {
        getInstance().feasts.remove(feast);
        feast.setAlive(false);
        FactionAPI faction = feast.getOriginator().getFaction();
        for (Lord lord : LordController.getLordsList()) {
            if (lord.getLordAPI().getFaction().equals(faction) && LordAction.base(lord.getCurrAction()) == LordAction.FEAST) {
                lord.setCurrAction(null);
            }
        }
        if (faction.getId().equals(Misc.getCommissionFactionId()) || faction.getId().equals(Factions.PLAYER)) {
            Global.getSector().getCampaignUI().addMessage(
                    StringUtil.getString(CATEGORY, "feast_end", feast.getTarget().getMarket().getName()),
                    faction.getBaseUIColor());
            getInstance().feastCounter.put(faction.getId(), Global.getSector().getClock().getTimestamp());
        }
    }

    // for when a lord is defecting or receives a player order, etc.
    public static void removeFromAllEvents(Lord lord) {
        LordEvent event;
        if (lord.getCurrAction() != null) {
            switch (LordAction.base(lord.getCurrAction())) {
                case RAID:
                    LordEvent raid = EventController.getCurrentRaid(lord);
                    if (raid != null) {
                        if (raid.getOriginator().equals(lord)) {
                            EventController.endRaid(raid);
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
        }
    }

    // weight for starting campaign
    // doesnt check many things like whether campaign already exists, campaign cooldown time
    public static int getStartCampaignWeight(Lord lord) {
        // no campaigns for LP/pirates
        FactionAPI faction = lord.getLordAPI().getFaction();
        if (Misc.isPirateFaction(faction)) return 0;
        boolean isAtWar = false;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            FactionAPI otherFaction = market.getFaction();
            if (LordController.getFactionsWithLords().contains(otherFaction)
                    && !Misc.isPirateFaction(otherFaction) && faction.isHostileTo(otherFaction)) {
                isAtWar = true;
                break;
            }
        }
        if (!isAtWar) return 0;

        // base personality weight. Starts negative to offset fleet power
        int weight = 0;
        switch(lord.getPersonality()) {
            case UPSTANDING:
                weight = -10;
                break;
            case CALCULATING:
                weight = -15;
                break;
            case QUARRELSOME:
                weight = -5;
                break;
        }

        // enemies starting a campaign makes campaign more likely
        for (LordEvent campaign : getInstance().campaigns) {
            if (campaign.getOriginator().getFaction().isHostileTo(faction)) {
                weight += 10;
            }
        }

        // evaluate own preparedness
        weight += lord.getLordAPI().getFleet().getFleetPoints() / 20;
        return weight;
    }

    // gets weight for joining faction's existing campaign
    // very likely to be true, calculation is simplified compared to for raids
    public static int getJoinCampaignWeight(Lord lord) {
        LordEvent campaign = getCurrentCampaign(lord.getLordAPI().getFaction());
        if (campaign == null) return 0;
        int weight = 0;
        if (lord.isMarshal()) weight += 10;
        switch(lord.getPersonality()) {
            case MARTIAL:
                weight = -5;
                break;
            case QUARRELSOME:
            case UPSTANDING:
                weight = -8;
                break;
            case CALCULATING:
                weight = -10;
                break;
        }
        // Distance
        // 20000 should be roughly maximum acceptable distance
        float dist = Math.max(0, Utils.getHyperspaceDistance(
                campaign.getOriginator().getFleet(), lord.getLordAPI().getFleet()) - 15000) / 1000;
        weight -= dist;

        // Relations with marshal
        weight += RelationController.getRelation(lord, campaign.getOriginator()) / 8;

        // Own Fleet strength
        weight += Math.min(20, lord.getLordAPI().getFleet().getFleetPoints() / 10);

        // Duration of campaign
        weight -= Utils.getDaysSince(campaign.getStart()) / 14;
        return weight;
    }

    // if event is null, assumes lord will originate a new event.
    // if event is not null, market is expected to be the target of event
    public static int getMilitaryOpWeight(Lord lord, MarketAPI market, LordEvent event, boolean defensive) {
        SectorEntityToken target = market.getPrimaryEntity();
        CampaignFleetAPI lordFleet = lord.getLordAPI().getFleet();
        Lord targetOwner = FiefController.getOwner(target.getMarket());
        // base weights
        int currWeight = 0;
        if (event == null) {
            switch(lord.getPersonality()) {
                case QUARRELSOME:
                case MARTIAL:
                    currWeight = 10;
                    break;
                case CALCULATING:
                    currWeight = -10;
                    break;
            }
        } else if (defensive) {
            switch(lord.getPersonality()) {
                case MARTIAL:
                    currWeight = 10;
                    break;
                case UPSTANDING:
                    currWeight = 20;
                    break;
            }
        }
        //log.info("Calculating operation weight. Base: " + currWeight);

        // negative weight from distance
        // 7000 should be roughly maximum acceptable distance for non-martial
        float dist = Math.max(0, Utils.getHyperspaceDistance(target, lordFleet) - 3000) / 200;
        if (lord.getPersonality() == LordPersonality.MARTIAL) dist *= 0.5;
        currWeight -= dist;
        //log.info("Distance factor: -" + dist);
        // negative weight from danger
        float enemyStrength = 0;
        if (!defensive) {
            enemyStrength += Utils.estimateMarketDefenses(target.getMarket());
        }
        if (event != null) {
            if (defensive) {
                enemyStrength += event.getOriginator().getFleet().getFleetPoints();
                for (Lord attacker : event.getParticipants()) {
                    enemyStrength += attacker.getLordAPI().getFleet().getFleetPoints();
                }

            } else {
                for (Lord defender : event.getOpposition()) {
                    enemyStrength += defender.getLordAPI().getFleet().getFleetPoints();
                }
            }
        }
        switch(lord.getPersonality()) {
            case MARTIAL:
                enemyStrength *= 0.1;
                break;
            case CALCULATING:
                enemyStrength *= 1.5;
        }
        //log.info("Enemy strength factor: -" + enemyStrength / 20);
        currWeight -= enemyStrength / 20;

        // Quarrelsome lords care more about relations, upstanding lords care about principles
        float relation_mod = 1;
        switch(lord.getPersonality()) {
            case UPSTANDING:
                relation_mod = 0.5f;
                break;
            case QUARRELSOME:
                relation_mod = 1.5f;
                break;
        }

        // positive weight from military strength and hatred of target

        //log.info("Own strength factor: " + (lordFleet.getFleetPoints() / 10 - 5));
        currWeight += lordFleet.getFleetPoints() / 10 - 5;
        if (defensive) {
            //log.info("Opponent Relation factor -" + relation_mod * RelationController.getRelation(lord, event.getOriginator()) / 8);
            currWeight -= relation_mod * RelationController.getRelation(lord, event.getOriginator()) / 8;
        } else if (targetOwner != null) {
            //log.info("Opponent Relation factor -" + relation_mod * RelationController.getRelation(lord, targetOwner) / 4);
            currWeight -= relation_mod * RelationController.getRelation(lord, targetOwner) / 4;
        }

        if (event != null) {
            // negative weight if too many existing participants
            if (defensive) {
                //log.info("Crowdedness factor " + CROWDED_MALUS * event.getOpposition().size());
                currWeight -= CROWDED_MALUS * event.getOpposition().size();
            } else {
                //log.info("Crowdedness factor " + CROWDED_MALUS * event.getParticipants().size());
                currWeight -= CROWDED_MALUS * event.getParticipants().size();
            }

            // weight from opinion of participants
            if (defensive && targetOwner != null) {
                //log.info("Ally Relation factor " + relation_mod * RelationController.getRelation(lord, targetOwner) / 2);
                currWeight += relation_mod * RelationController.getRelation(lord, targetOwner) / 2;
            } else if (!defensive) {
                //log.info("Ally Relation factor " + relation_mod * RelationController.getRelation(lord, event.getOriginator()) / 2);
                currWeight += relation_mod * RelationController.getRelation(lord, event.getOriginator()) / 2;
                for (Lord participant : event.getParticipants()) {
                    currWeight += relation_mod * Math.min(0, RelationController.getRelation(lord, participant) / 8);
                }
            }
        }
        return currWeight;
    }

    public static EventController getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(EventController.class);
            if (intel.isEmpty()) {
                instance = new EventController();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one EventController intel registered");
                }
                instance = (EventController) intel.get(0);
                // update lord references
                for (LordEvent event : instance.feasts) {
                    event.updateReferences();
                }
                for (LordEvent event : instance.campaigns) {
                    event.updateReferences();
                }
            }
        }
        return instance;
    }

    public static EventController getInstance() {
        return getInstance(false);
    }
}
