package controllers;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import faction.LawLevel;
import faction.LawProposal;
import faction.Lawset;
import org.apache.log4j.Logger;
import person.Lord;
import person.LordEvent;
import person.LordPersonality;
import ui.CouncilIntelPlugin;
import ui.LawsIntelPlugin;
import ui.ProposalIntelPlugin;
import util.DefectionUtils;
import util.StringUtil;
import util.Utils;

import java.lang.reflect.Array;
import java.util.*;

import static util.Constants.LIGHT_RED;
import static util.Constants.ONE_DAY;

public class PoliticsController implements EveryFrameScript {

    private HashMap<String, LawProposal> lordProposalsMap; // maps lord id to their proposal
    private HashMap<String, Lawset> factionLawsMap;
    private HashMap<String, LawProposal> factionCouncilMap;
    private HashMap<String, Long> factionTimestampMap; // tracks when the council last convened
    private HashMap<String, Long> lordTimestampMap; // tracks when lord last thought about proposing new legislation
    private float lastUpdate;

    private static Logger log = Global.getLogger(PoliticsController.class);
    public static PoliticsController instance;
    public static final float UPDATE_INTERVAL = 1;
    public static final float LORD_THINK_INTERVAL = 7;
    private static final int RECESS_DAYS = 2;//30;
    private static final int DEBATE_DAYS = 4;//60;

    private PoliticsController() {
        factionLawsMap = new HashMap<>();
        lordProposalsMap = new HashMap<>();
        factionCouncilMap = new HashMap<>();
        factionTimestampMap = new HashMap<>();
        lordTimestampMap = new HashMap<>();
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (Misc.isPirateFaction(faction)) continue;
            addFaction(faction);
        }
        Random rand = new Random();
        for (Lord lord : LordController.getLordsList()) {
            lordTimestampMap.put(lord.getLordAPI().getId(), Global.getSector().getClock().getTimestamp()
                    - rand.nextInt((int) (LORD_THINK_INTERVAL * ONE_DAY)));
        }
    }

    @Override
    public void advance(float amount) {
        float days = Global.getSector().getClock().convertToDays(amount);
        lastUpdate += days;
        if (lastUpdate < UPDATE_INTERVAL) {
            return;
        }
        lastUpdate = 0;
        // TODO update ruler approval/proposal submission
        checkProposalValidity(Utils.getRecruitmentFaction());
        updateAllProposals(Utils.getRecruitmentFaction());

        // checks if councils convene
        for (String factionStr : factionCouncilMap.keySet()) {
            FactionAPI faction = Global.getSector().getFaction(factionStr);
            if (Misc.isPirateFaction(faction)) continue;
            if (getTimeRemainingDays(faction) < 0) {
                checkProposalValidity(faction);
                if (factionCouncilMap.get(factionStr) == null) {
                    // choose next proposal
                    updateAllProposals(faction);
                    LawProposal nextProposal = getBestProposal(faction);
                    if (nextProposal != null) {
                        nextProposal.kill();
                    }
                    factionCouncilMap.put(factionStr, nextProposal);
                    PoliticsController.updateProposal(nextProposal);
                } else {
                    // vote on proposal
                    LawProposal proposal = factionCouncilMap.get(factionStr);
                    updateProposal(proposal);
                    resolveProposal(proposal);
                    lordProposalsMap.put(proposal.getOriginator(), null);
                    factionCouncilMap.put(factionStr, null);
                }
                factionTimestampMap.put(factionStr, Global.getSector().getClock().getTimestamp());
            }
        }

        // check for new fief to award
        for (Lawset laws : factionLawsMap.values()) {
            FactionAPI faction = Global.getSector().getFaction(laws.getFactionId());
            MarketAPI award = laws.getFiefAward();
            if (award == null || !faction.equals(award.getFaction())) {
                MarketAPI newAward = FiefController.chooseNextFiefAward(faction);
                laws.setFiefAward(newAward);
            }
        }

        // submit new proposals
        for (Lord lord : LordController.getLordsList()) {
            if (Misc.isPirateFaction(lord.getFaction())) continue;
            if (Utils.getDaysSince(lordTimestampMap.get(lord.getLordAPI().getId())) < LORD_THINK_INTERVAL) continue;
            lordTimestampMap.put(lord.getLordAPI().getId(), Global.getSector().getClock().getTimestamp());
            LawProposal currProposal = lordProposalsMap.get(lord.getLordAPI().getId());
            if (currProposal == null) {
                // lord submits new proposal
                LawProposal newProposal = createNewProposal(lord);
                lordProposalsMap.put(lord.getLordAPI().getId(), newProposal);
                Global.getSector().getIntelManager().addIntel(new ProposalIntelPlugin(newProposal), true);
            } else {
                // TODO check if lord should withdraw previous proposal, esp. for new fief award
//                boolean considerFiefProposal = getLaws(lord.getFaction()).getFiefAward() != null
//                        && currProposal.law != Lawset.LawType.AWARD_FIEF;
//                boolean refreshProposal = false;
//                if (currProposal.getSupportersCached() != null && currProposal.getSupportersCached().size() < 2) {
//                    refreshProposal = true;
//                }
            }
        }
    }

    // create specific proposal for lord, likely used for player
    public static void addProposal(Lord lord, LawProposal proposal) {
        LawProposal oldProposal = getInstance().lordProposalsMap.get(lord.getLordAPI().getId());
        if (oldProposal != null) {
            oldProposal.kill();
        }
        getInstance().lordProposalsMap.put(lord.getLordAPI().getId(), proposal);
        Global.getSector().getIntelManager().addIntel(new ProposalIntelPlugin(proposal), true);
    }

    // TODO creates a new proposal originated from lord
    // this follows a bit different logic from approval calculations for actions with targets
    // We consider approval for only the action, then add action targets later for efficiency
    public LawProposal createNewProposal(Lord lord) {
        Lawset.LawType bestType = null;
        String bestTargetLord = null;
        String bestTargetMarket = null;
        String bestTargetFaction = null;
        int bestLevel = 0;
        int bestWeight = 20;

        // laws

        // appoint marshal

        // grant fief

        // declare war

        // sue for peace



        ArrayList<Lord> targets = new ArrayList<>();
        for (Lord lord2 : LordController.getLordsList()) {
            if (lord2.getFaction().equals(lord.getFaction())) targets.add(lord2);
        }
        // TODO
        return new LawProposal(Lawset.LawType.APPOINT_MARSHAL, lord.getLordAPI().getId(),
                targets.get(new Random().nextInt(targets.size())).getLordAPI().getId(), null, null, 0);
    }

    // gets next proposal to enter council
    public LawProposal getBestProposal(FactionAPI faction) {
        LawProposal best = null;
        int bestWeight = 0;
        for (LawProposal proposal : lordProposalsMap.values()) {
            if (proposal == null) continue;
            if (proposal.getFaction().equals(faction)) {
                int weight = proposal.getTotalSupport();
                if (weight > bestWeight) {
                    best = proposal;
                    bestWeight = weight;
                }
            }
        }
        return best;
    }

    // does final vote tally and implements proposal if vote passes
    public void resolveProposal(LawProposal proposal) {
        // TODO change lord relations
        Pair<Integer, Integer> votes = countVotes(proposal, null, null);
        int totalSupport = votes.one;
        int totalOpposition = votes.two;
        boolean victorySound = true;
        boolean announce = proposal.getFaction().equals(Utils.getRecruitmentFaction());
        if (totalSupport > totalOpposition) {
            if (announce) {
                Global.getSector().getCampaignUI().addMessage("The council has passed law: " + proposal.getTitle(),
                        proposal.faction.getBaseUIColor());
            }
            // law passed
            Lawset laws = getLaws(proposal.getFaction());
            switch (proposal.law) {
                case CROWN_AUTHORITY:
                    laws.setCrownAuthority(LawLevel.values()[proposal.getTargetLevel()]);
                    break;
                case NOBLE_AUTHORITY:
                    laws.setNobleAuthority(LawLevel.values()[proposal.getTargetLevel()]);
                    break;
                case TRADE_LAW:
                    laws.setTradeFavor(LawLevel.values()[proposal.getTargetLevel()]);
                    break;
                case FEAST_LAW:
                    laws.setFeastLaw(LawLevel.values()[proposal.getTargetLevel()]);
                    break;
                case APPOINT_MARSHAL:
                    laws.setMarshal(proposal.getTargetLord());
                    LordEvent campaign = EventController.getCurrentCampaign(proposal.getFaction());
                    if (campaign != null) {
                        EventController.endCampaign(campaign);
                    }
                    break;
                case AWARD_FIEF:
                    FiefController.setOwner(
                            Global.getSector().getEconomy().getMarket(proposal.getTargetFief()),
                            proposal.getTargetLord());
                    laws.setFiefAward(FiefController.chooseNextFiefAward(proposal.getFaction()));
                    break;
                case DECLARE_WAR:
                    proposal.getFaction().setRelationship(proposal.getTargetFaction(), RepLevel.HOSTILE);
                    break;
                case SUE_FOR_PEACE:
                    LawProposal tmp = new LawProposal(
                            Lawset.LawType.SUE_FOR_PEACE, laws.getMarshal(), null, null,
                            proposal.getFaction().getId(), 0);
                    tmp.faction = Global.getSector().getFaction(proposal.getTargetFaction()); // have to mess with this a bit
                    updateProposal(tmp);
                    Pair<Integer, Integer> results = countVotes(tmp, null, null);
                    if (results.one > results.two) {
                        proposal.getFaction().setRelationship(proposal.getTargetFaction(), RepLevel.NEUTRAL);
                        if (announce) {
                            Global.getSector().getCampaignUI().addMessage("Your peace offer was accepted!",
                                    proposal.faction.getBaseUIColor());
                        }
                    } else if (announce) {
                        Global.getSector().getCampaignUI().addMessage("Your peace offer was rejected",
                                LIGHT_RED);
                        victorySound = false;
                    }
                    break;
                case REVOKE_FIEF:
                    FiefController.setOwner(
                            Global.getSector().getEconomy().getMarket(proposal.getTargetFief()),
                            null);
                    if (laws.getFiefAward() == null) {
                        laws.setFiefAward(FiefController.chooseNextFiefAward(proposal.getFaction()));
                    }
                    break;
                case CHANGE_RANK:
                    Lord lord = LordController.getLordOrPlayerById(proposal.getTargetLord());
                    lord.setRanking(proposal.getTargetLevel());
                    break;
                case EXILE_LORD:
                    // revoke all fiefs
                    Lord exile = LordController.getLordOrPlayerById(proposal.getTargetLord());
                    ArrayList<MarketAPI> toRemove = new ArrayList<>();
                    for (SectorEntityToken fief : exile.getFiefs()) {
                        toRemove.add(fief.getMarket());
                    }
                    for (MarketAPI market : toRemove) {
                        FiefController.setOwner(market, null);
                    }

                    if (exile.isPlayer()) {
                        FactionCommissionIntel intel = Misc.getCommissionIntel();
                        if (intel != null) {
                            intel.endMission();
                        }
                    } else {
                        DefectionUtils.performDefection(exile);
                    }
                    break;
            }
            checkProposalValidity(proposal.faction);
        } else {
            // law failed
            if (announce) {
                Global.getSector().getCampaignUI().addMessage("The council has voted down law: " + proposal.getTitle(),
                        LIGHT_RED);
            }
            victorySound = false;
        }

        if (announce) {
            if (victorySound) {
                Global.getSoundPlayer().playUISound("ui_rep_raise", 1, 1);
            } else {
                Global.getSoundPlayer().playUISound("ui_rep_drop", 1, 1);
            }
        }
    }


    // checks if proposals of a faction are still valid after
    public void checkProposalValidity(FactionAPI faction) {
        Lawset laws = getLaws(faction);
        for (String lordStr : lordProposalsMap.keySet()) {
            Lord lord = LordController.getLordOrPlayerById(lordStr);
            if (!lord.getFaction().equals(faction)) continue;
            LawProposal proposal = lordProposalsMap.get(lordStr);
            if (proposal == null) continue;
            boolean isValid = proposal.getFaction().equals(faction);

            // if law with levels, ensure it's 1 level away from current law
            // if fief award/revoke, ensure the fief is unclaimed and part of the faction
            // ensure target lord is still in the faction
            // ensure declare war target is still nonhostile, peace target is still hostile
            switch (proposal.law) {
                case CROWN_AUTHORITY:
                case NOBLE_AUTHORITY:
                case TRADE_LAW:
                case FEAST_LAW:
                    if (Math.abs(laws.getLawLevel(proposal.law).ordinal() - proposal.targetLevel) != 1) isValid = false;
                    break;
                case REVOKE_FIEF:
                case AWARD_FIEF:
                    MarketAPI targetFief = Global.getSector().getEconomy().getMarket(proposal.targetFief);
                    if (targetFief == null) {
                        isValid = false;
                    } else if (!faction.equals(targetFief.getFaction())) {
                        isValid = false;
                    } else if (proposal.law == Lawset.LawType.AWARD_FIEF && FiefController.getOwner(targetFief) != null) {
                        isValid = false;
                    } else if (proposal.law == Lawset.LawType.REVOKE_FIEF) {
                        Lord currOwner = FiefController.getOwner(targetFief);
                        if (currOwner == null || !currOwner.getLordAPI().getId().equals(proposal.targetLord)) isValid = false;
                    }
                    break;
                case DECLARE_WAR:
                case SUE_FOR_PEACE:
                    FactionAPI target = Global.getSector().getFaction(proposal.targetFaction);
                    if (faction.isHostileTo(target) == (proposal.law == Lawset.LawType.DECLARE_WAR)) isValid = false;
                    break;
            }
            if (proposal.targetLord != null) {
                Lord target = LordController.getLordOrPlayerById(proposal.targetLord);
                if (!target.getFaction().equals(proposal.getFaction())) isValid = false;
            }
            if (!isValid) {
                proposal.kill();
                lordProposalsMap.put(lordStr, null);
                if (proposal.equals(factionCouncilMap.get(faction.getId()))) {
                    factionCouncilMap.put(faction.getId(), null);
                    factionTimestampMap.put(faction.getId(), Global.getSector().getClock().getTimestamp());
                }
            }
        }
    }

    // updates vote tally for proposal
    public static void updateProposal(LawProposal proposal) {
        if (proposal == null) return;
        boolean isCouncil = proposal.equals(getCurrProposal(proposal.getFaction()));
        proposal.cacheSupporters();
        for (Lord critic : LordController.getLordsList()) {
            if (critic.getFaction().equals(proposal.getFaction())) {
                updateProposal(proposal, critic, isCouncil);
            }
        }
    }

    private static void updateProposal(LawProposal proposal, Lord critic, boolean isCouncil) {
        int opinionThreshold = 0;
        if (!isCouncil) opinionThreshold = 10; // make only diehard supporters support until council
        Pair<Integer, ArrayList<String>>  ret = getApproval(critic, proposal, isCouncil);
        int approval = ret.one;
        ArrayList<String> reasons = ret.two;
        if (approval > opinionThreshold) {
            proposal.getSupporters().add(critic.getLordAPI().getId());
            if (isCouncil) {
                proposal.getSupporterReasons().add(reasons);
                proposal.getSupporterVals().add(approval);
            }
        } else if (approval <= -1 * opinionThreshold) {
            proposal.getOpposers().add(critic.getLordAPI().getId());
            if (isCouncil) {
                proposal.getOpposerReasons().add(reasons);
                proposal.getOpposersVals().add(approval);
            }
        }
    }

    // updates vote tally for all of a faction's proposals at once. More efficient.
    public static void updateAllProposals(FactionAPI faction) {
        ArrayList<Lord> relevantLords = new ArrayList<>();
        for (Lord lord : LordController.getLordsList()) {
            if (lord.getFaction().equals(faction)) relevantLords.add(lord);
        }

        for (Lord proposer : relevantLords) {
            LawProposal proposal = getInstance().lordProposalsMap.get(proposer.getLordAPI().getId());
            if (proposal != null) {
                boolean isCouncil = proposal.equals(getCurrProposal(proposal.getFaction()));
                proposal.cacheSupporters();
                for (Lord critic : relevantLords) {
                    updateProposal(proposal, critic, isCouncil);
                }
            }
        }
    }

    public static Pair<Integer, ArrayList<String>> getApproval(Lord lord, LawProposal proposal, boolean itemized) {
        int approval = 0;
        ArrayList<String> reasons = new ArrayList<>();
        ArrayList<String> auxReasons = new ArrayList<>(); // add these at the end
        FactionAPI faction = lord.getFaction();
        Lawset laws = getLaws(faction);
        LawLevel level = laws.getLawLevel(proposal.law);
        Lord originator = LordController.getLordOrPlayerById(proposal.getOriginator());
        Lord beneficiary = null;
        Lord victim = null;
        int delta;
        int base = -20;
        int victimPenalty = 100;
        int beneficiaryBonus = 100;
        int raising = 1; // 1 if a law level is being raised, -1 if being lowered
        if (level != null && proposal.targetLevel < level.ordinal()) {
            raising = -1;
        }
        int numEnemies;

        String lordId = lord.getLordAPI().getId();
        // TODO add something for pathers
        switch (proposal.law) {
            case CROWN_AUTHORITY:
                // disliked by high rank
                delta = raising * -10 * lord.getRanking();
                approval += delta;
                if (itemized && delta != 0) auxReasons.add(addPlus(delta) + " Has high rank");
                // liked by sindria
                if (lord.getTemplate().factionId.equals(Factions.DIKTAT)) {
                    delta = raising * 20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Sindrian");
                }
                // hated by pirates
                if (lord.getTemplate().factionId.equals(Factions.PIRATES)) {
                    delta = raising * -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Pirate");
                }
                break;
            case NOBLE_AUTHORITY:
                if (lord.getRanking() == 0) {
                    // disliked by low rank
                    delta = raising * -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Has low rank");

                } else {
                    // liked by high rank
                    delta = raising * 15 * lord.getRanking();
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Has high rank");
                }
                // liked by persean
                if (lord.getTemplate().factionId.equals(Factions.PERSEAN)) {
                    delta = raising * 20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Persean");
                }
                // hated by pirates
                if (lord.getTemplate().factionId.equals(Factions.PIRATES)) {
                    delta = raising * -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Pirate");
                }
                break;
            case TRADE_LAW:
                // disliked if no fiefs owned
                if (lord.getFiefs().isEmpty()) {
                    delta = raising * -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " No fiefs to tax");
                }
                // liked by calculating
                if (lord.getPersonality() == LordPersonality.CALCULATING) {
                    delta = raising * -10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Mercantile");
                }
                // liked by tri-tach
                if (lord.getTemplate().factionId.equals(Factions.TRITACHYON)) {
                    delta = raising * -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Tri-Tachyon");
                }
                break;
            case FEAST_LAW:
                // disliked by quarrelsome
                if (lord.getPersonality() == LordPersonality.QUARRELSOME) {
                    delta = raising * -10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Likes Debauchery");
                }
                // liked by church
                if (lord.getTemplate().factionId.equals(Factions.LUDDIC_CHURCH)) {
                    delta = raising * 20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Luddic Asceticism");
                }
                break;
            case APPOINT_MARSHAL:
                //TODO add some job approval mechanic
                beneficiary = LordController.getLordOrPlayerById(proposal.getTargetLord());
                victim = LordController.getLordOrPlayerById(laws.getMarshal());
                base = -30;
                victimPenalty = 50;
                // affected by lord rank
                int currRank = 0;
                int newRank = beneficiary.getRanking();
                if (victim != null) {
                    currRank = victim.getRanking();
                }
                delta = 20 * (newRank - currRank);
                approval += delta;
                if (itemized) auxReasons.add(addPlus(delta) + " Rank difference with current Marshal");
                // disliked if on campaign
                if (EventController.getCurrentCampaign(faction) != null) {
                    delta = -50;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Disrupts ongoing campaign");
                }
                break;
            case AWARD_FIEF:
                beneficiary = LordController.getLordOrPlayerById(proposal.getTargetLord());
                // TODO see who captured fief
                // check beneficiary fiefs owned count vs average
                if (beneficiary.getFiefs().isEmpty()) {
                    delta = 25;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Recipient has no fiefs");
                } else if (beneficiary.getFiefs().size() >= originator.getFiefs().size()) {
                    delta = -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Recipient has too many fiefs");
                }
                // disliked by calculating
                if (lord.getPersonality() == LordPersonality.CALCULATING && !lord.equals(beneficiary)) {
                    delta = -10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Jealousy");
                }
                break;
            case DECLARE_WAR:
                base = -30;
                // liked by martial
                if (lord.getPersonality() == LordPersonality.MARTIAL) {
                    delta = 10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " War hawk");
                }
                // lord's current strength
                delta = (int) (20 * (lord.getMilitaryLevel() - 1));
                approval += delta;
                if (itemized) auxReasons.add(addPlus(delta) + " Personal military strength");

                // # existing enemies
                numEnemies = -1;
                // martial wants more enemies
                if (lord.getPersonality() == LordPersonality.MARTIAL) numEnemies -= 1;
                for (FactionAPI faction2 : LordController.getFactionsWithLords()) {
                    if (faction2.isHostileTo(faction)) {
                        numEnemies += 1;
                    }
                }
                delta = -20 * numEnemies;
                approval += delta;
                if (itemized && delta < 0) auxReasons.add(addPlus(delta) + " Existing enemies");
                if (itemized && delta > 0) auxReasons.add(addPlus(delta) + " Not enough enemies");
                // faction relations
                FactionAPI targetFaction = Global.getSector().getFaction(proposal.targetFaction);
                int relations = targetFaction.getRepInt(faction.getId());
                delta = -1 * relations / 4;
                approval += delta;
                if (itemized) auxReasons.add(addPlus(delta) + " Faction relations");

                // liked by hegemony
                if (lord.getTemplate().factionId.equals(Factions.HEGEMONY)) {
                    delta = 10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Hegemony Imperialism");
                }
                break;
            case SUE_FOR_PEACE:
                base = -30;
                // liked by calculating
                if (lord.getPersonality() == LordPersonality.CALCULATING) {
                    delta = 10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Likes peace");
                }
                // lord's current strength
                delta = (int) (-20 * (lord.getMilitaryLevel() - 1));
                approval += delta;
                if (itemized) auxReasons.add(addPlus(delta) + " Personal military strength");

                // disliked by hegemony
                if (lord.getTemplate().factionId.equals(Factions.HEGEMONY)) {
                    delta = -10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Hegemony Imperialism");
                }
                // # existing enemies
                numEnemies = -1;
                // martial wants more enemies
                if (lord.getPersonality() == LordPersonality.MARTIAL) numEnemies -= 1;
                for (FactionAPI faction2 : LordController.getFactionsWithLords()) {
                    if (faction2.isHostileTo(faction)) {
                        numEnemies += 1;
                    }
                }
                delta = 20 * numEnemies;
                approval += delta;
                if (itemized && delta > 0) auxReasons.add(addPlus(delta) + " Multi-front war");
                if (itemized && delta < 0) auxReasons.add(addPlus(delta) + " Wants more enemies");
                break;
            case REVOKE_FIEF:
                victim = LordController.getLordOrPlayerById(proposal.getTargetLord());
                base = -30;
                // disliked by upstanding
                if (lord.getPersonality() == LordPersonality.UPSTANDING) {
                    delta = -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Dishonorable");
                }
                if (lord.getPersonality() == LordPersonality.MARTIAL) {
                    delta = -10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Dishonorable");
                }
                // liked by quarrelsome
                if (lord.getPersonality() == LordPersonality.QUARRELSOME && !lord.equals(victim)) {
                    delta = 10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Loves suffering");
                }
                break;
            case CHANGE_RANK:
                Lord target = LordController.getLordOrPlayerById(proposal.getTargetLord());
                if (target.getRanking() < proposal.targetLevel) {
                    raising = 1;
                    beneficiary = target;
                } else {
                    raising = -1;
                    victim = target;
                }
                // liked/disliked by high rank
                if (lord.getRanking() > 0) {
                    delta = raising * -10 * lord.getRanking();
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Preserve rank exclusivity");
                }
                break;
            case EXILE_LORD:
                victim = LordController.getLordOrPlayerById(proposal.getTargetLord());
                victimPenalty = 1000;
                // disliked by upstanding
                if (lord.getPersonality() == LordPersonality.UPSTANDING) {
                    delta = -20;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Dishonorable");
                }
                if (lord.getPersonality() == LordPersonality.MARTIAL) {
                    delta = -10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Dishonorable");
                }
                // liked by quarrelsome
                if (lord.getPersonality() == LordPersonality.QUARRELSOME && !lord.equals(victim)) {
                    delta = 10;
                    approval += delta;
                    if (itemized) auxReasons.add(addPlus(delta) + " Loves suffering");
                }
                break;
        }

        approval += base;
        if (itemized) reasons.add(addPlus(base) + " Base Reluctance");


        if (lordId.equals(proposal.getOriginator())) {
            delta = 100;
            approval += delta;
            if (itemized) reasons.add(addPlus(delta) + " Proposer");
        } else {
            // opinion of proposer, +25/-25
            delta = RelationController.getRelation(
                    LordController.getLordOrPlayerById(lordId),
                    LordController.getLordOrPlayerById(proposal.getOriginator())) / 4;
            approval += delta;
            if (itemized && delta != 0) reasons.add(addPlus(delta) + " Opinion of proposer");
        }

        if (beneficiary != null) {
            if (lordId.equals(beneficiary.getLordAPI().getId())) {
                approval += beneficiaryBonus;
                if (itemized) reasons.add(addPlus(beneficiaryBonus) + " Beneficiary");
            } else  {
                // opinion of beneficiary, +25/-25
                delta = RelationController.getRelation(
                        LordController.getLordOrPlayerById(lordId),
                        beneficiary) / 4;
                approval += delta;
                if (itemized && delta != 0) reasons.add(addPlus(delta) + " Opinion of beneficiary");
            }
        }

        if (victim != null) {
            if (lordId.equals(victim.getLordAPI().getId())) {
                delta = -victimPenalty;
                approval += delta;
                if (itemized) reasons.add(addPlus(delta) + " Harmed by proposal");
            } else {
                // opinion of victim, +25/-25
                delta = -1 * RelationController.getRelation(
                        LordController.getLordOrPlayerById(lordId),
                        victim) / 4;
                approval += delta;
                if (itemized && delta != 0) reasons.add(addPlus(delta) + " Opinion of harmed parties");
            }
        }

        // preferred law level, for laws with levels
        if (proposal.law.pref != null) {  // kind of hacky
            int prefLevel = proposal.law.pref[lord.getPersonality().ordinal()];
            int currLevel = level.ordinal();
            if (Math.abs(prefLevel - currLevel) > Math.abs(prefLevel - proposal.targetLevel)) {
                // for
                delta = 10 * Math.abs(prefLevel - currLevel);
                approval += delta;
                if (itemized) reasons.add(addPlus(delta) + " Likes proposed law");
            } else {
                // against
                delta = -10 * Math.abs(prefLevel - proposal.targetLevel);
                approval += delta;
                if (itemized) reasons.add(addPlus(delta) + " Prefers status quo");
            }
        }


        if (proposal.equals(getInstance().factionCouncilMap.get(
                lord.getFaction().getId()))) {
            // political considerations- voting to maximize relations with potential friends
            // martial and quarrelsome vote with their friends. Calculating votes to make new friends
            if (proposal.getSupportersCached() != null && lord.getPersonality() != LordPersonality.UPSTANDING) {
                delta = 0;
                int ctr = 0;
                int threshold = Utils.getThreshold(RepLevel.WELCOMING);
                if (lord.getPersonality() == LordPersonality.CALCULATING) threshold = Utils.getThreshold(RepLevel.SUSPICIOUS);
                for (String lordStr : proposal.getSupportersCached()) {
                    int relation = RelationController.getRelation(lord, LordController.getLordOrPlayerById(lordStr));
                    if (relation > threshold) {
                        delta += (100 - Math.abs(relation));
                        ctr += 1;
                    }
                }
                for (String lordStr : proposal.getOpposersCached()) {
                    int relation = RelationController.getRelation(lord, LordController.getLordOrPlayerById(lordStr));
                    if (relation > threshold) {
                        delta -= (100 - Math.abs(relation));
                        ctr += 1;
                    }
                }
                delta = delta / Math.max(1, ctr);
                if (itemized && delta != 0) reasons.add(addPlus(delta) + " Political considerations");
            }

            // ruler approval if in council, +25/-25
            if (Utils.getLeader(faction) != null) {
                int sign = -1;
                if (proposal.isLiegeSupports()) sign = 1;
                delta = 25 * sign * RelationController.getLoyalty(lord) / 100;
                approval += delta;
                if (itemized && delta != 0) reasons.add(addPlus(delta) + " Influence of ruler");
            }
        }

        reasons.addAll(auxReasons);
        return new Pair<>(approval, reasons);
    }

    // Tallies votes for/against proposal. DOES NOT update vote weights first, so do that beforehand.
    // Updates supporters/opposition accordingly if they're not null
    public static Pair<Integer, Integer> countVotes(LawProposal proposal,
                                                    ArrayList<Lord> supporters, ArrayList<Lord> opposition) {
        int totalSupport = 0;
        int totalOpposition = 0;
        if (!proposal.getFaction().equals(Global.getSector().getPlayerFaction())) {
            if (proposal.isPlayerSupports()) {
                totalSupport += PoliticsController.getPoliticalWeight(LordController.getPlayerLord());
                if (supporters != null) supporters.add(LordController.getPlayerLord());
            } else {
                totalOpposition += PoliticsController.getPoliticalWeight(LordController.getPlayerLord());
                if (opposition != null) opposition.add(LordController.getPlayerLord());
            }
        }

        for (String lordStr : proposal.getSupporters()) {
            Lord lord = LordController.getLordById(lordStr);
            if (supporters != null) supporters.add(lord);
            totalSupport += PoliticsController.getPoliticalWeight(lord);
        }

        for (String lordStr : proposal.getOpposers()) {
            Lord lord = LordController.getLordById(lordStr);
            if (opposition != null) opposition.add(lord);
            totalOpposition += PoliticsController.getPoliticalWeight(lord);
        }

        if (Utils.getLeader(proposal.getFaction()) != null) {
            float liegeMultiplier = PoliticsController.getLiegeMultiplier(proposal.getFaction());
            if (proposal.isLiegeSupports()) {
                totalSupport = (int) (totalSupport * liegeMultiplier);
            } else {
                totalOpposition = (int) (totalOpposition * liegeMultiplier);
            }
        }
        return new Pair<>(totalSupport, totalOpposition);
    }

    // catches cases where new factions are added mid-game
    public static Lawset getLaws(FactionAPI faction) {
        HashMap<String, Lawset> factionLawsMap = getInstance().factionLawsMap;
        if (!factionLawsMap.containsKey(faction.getId())) {
            getInstance().addFaction(faction);
        }
        return factionLawsMap.get(faction.getId());
    }

    public static boolean playerFactionHasLaws() {
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        for (Lord lord : LordController.getLordsList()) {
            if (lord.getLordAPI().getFaction().equals(playerFaction)) return true;
        }
        return false;
    }

    public static LawProposal getCurrProposal(FactionAPI faction) {
        HashMap<String, LawProposal> currProposals = getInstance().factionCouncilMap;
        if (!currProposals.containsKey(faction.getId())) {
            getInstance().addFaction(faction);
        }
        return currProposals.get(faction.getId());
    }

    public static int getPoliticalWeight(Lord lord) {
        LawLevel nobleAuthority = getLaws(lord.getFaction()).getNobleAuthority();
        float upperNobleMult = 0;
        switch (nobleAuthority) {
            case LOW:
                upperNobleMult = 0.5f;
                break;
            case MEDIUM:
                upperNobleMult = 1;
                break;
            case HIGH:
                upperNobleMult = 2f;
                break;
            case VERY_HIGH:
                upperNobleMult = 3f;
                break;
        }
        return (int) (1000 + 1000 * upperNobleMult * lord.getRanking());
    }

    // gets multiplier of political weight from ruler support, determined by crown authority
    public static float getLiegeMultiplier(FactionAPI faction) {
        LawLevel nobleAuthority = getLaws(faction).getCrownAuthority();
        float mult = 1;
        switch(nobleAuthority) {
            case LOW:
                mult = 1.2f;
                break;
            case MEDIUM:
                mult = 1.5f;
                break;
            case HIGH:
                mult = 2;
                break;
            case VERY_HIGH:
                mult = 3;
                break;
        }
        return mult;
    }

    public static int getTimeRemainingDays(FactionAPI faction) {
        int timeLimit = RECESS_DAYS;
        if (getCurrProposal(faction) != null) {
            timeLimit = DEBATE_DAYS;
        }
        long proposalStart = getInstance().factionTimestampMap.get(faction.getId());
        return Math.round(timeLimit - Global.getSector().getClock().getElapsedDaysSince(proposalStart));
    }

    private void addFaction(FactionAPI faction) {
        factionLawsMap.put(faction.getId(), new Lawset(faction));
        factionCouncilMap.put(faction.getId(), null);
        factionTimestampMap.put(faction.getId(), Global.getSector().getClock().getTimestamp());
    }

    private static String addPlus(int delta) {
        if (delta <= 0) return Integer.toString(delta);
        return "+" + delta;
    }

    public static PoliticsController getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            PoliticsController curr = null;
            List<EveryFrameScript> scripts = Global.getSector().getScripts();
            for (EveryFrameScript script : scripts) {
                if (script instanceof PoliticsController) {
                    curr = (PoliticsController) script;
                }
            }
            if (curr == null) {
                instance = new PoliticsController();
                Global.getSector().addScript(instance);
            } else {
                instance = curr;
            }
        }
        return instance;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    public static PoliticsController getInstance() {
        return getInstance(false);
    }
}
