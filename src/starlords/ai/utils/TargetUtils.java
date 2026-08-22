package starlords.ai.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.intel.groundbattle.GBUtils;
import exerelin.campaign.intel.groundbattle.GroundBattleIntel;
import exerelin.campaign.intel.groundbattle.GroundUnitDef;
import lombok.Setter;
import org.apache.log4j.Logger;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.NexerlinUtilitys;
import starlords.util.Utils;
import starlords.util.factionUtils.FactionTemplate;
import starlords.util.factionUtils.FactionTemplateController;

import java.util.ArrayList;

import static starlords.ai.LordAI.*;

@Setter
public class TargetUtils {
    public static Logger log = Global.getLogger(TargetUtils.class);
    /*NOTE TO SELF:
    * YES THIS IS NOT EXPANDABLE. YES IT FEELS UNFINISHED. YA IT KINDA SUCKS.
    * THE ONLY REALY WAY TO IMPROVE THIS WITHOUT CREATING MORE ISSUES IS TO REPLACE THE INTIER LORD AI.
    * FOCUS ON FIXES FIST YOU SILLY PERSON. FIX THE THINGS, YOU CAN UPGRADE THE AI TO BE EXPANDABLE LATER.*/
    /*
    * ok, so something I might be forced to do, is at least move the viliance cost of each action into a single area. this would help a lot with settings.
    * ARGASGASGAGA
    * OK, SO HERES THE GOD DAM DEAL
    * IF I SIMPLY CREATE A CLASS TO HANDLE THE DIFFERENT OFFENCE TYPES, I COULD HAVE IT HOLD THE FOLLOWING DATA:
    *   -violence cost
    *   -what happens when it ends
    * NO. this will evolve into quite a big thing. I need to wait before upgrading the event controller. I have to.
    * so, heres whats going to happen:
    * 1) I am going to put the violence cost of each action into a single class, as static values (so I can change them with settings)
    * 2)
    *
    *
    *  */
    //I will add all this to the config when I get around to doing a full refactor of lord strategic AI.
    private static boolean canRaid_lord = true;
    private static boolean canTacticalBomb_lord = true;
    private static boolean canInvade_lord = false;
    private static boolean canSatbomb_lord = false;
    private static boolean canRaid_campaign = true;
    private static boolean canTacticalBomb_campaign = true;
    private static boolean canInvade_campaign = true;
    private static boolean canSatbomb_campaign = false;

    @Setter
    private static boolean canPlayerBeAttacked = true;
    @Setter
    private static boolean canPlayerBeAttackedBeforeStarlords = false;
    public static boolean isValidMarket(MarketAPI market){
        if (market == null) return false;
        if (market.isInHyperspace()) return false;
        if (market.getStarSystem() == null) return false;
        if (market.getStarSystem().getHyperspaceAnchor() == null) return false;
        //if (market.getPrimaryEntity() == null) return false;//?
        if (!market.isInEconomy()) return false;
        if (market.getSize() <= 2) return false;
        if (market.getIndustries().size() == 0) return false;
        return true;
    }
    public static boolean canBeFief(MarketAPI market){
        if (!isValidMarket(market)) return false;
        return true;
    }
    public static boolean canBeFief(Lord lord,MarketAPI market){
        if (!canBeFief(market)) return false;
        return true;
    }
    public static boolean canBeTradedWith(Lord lord,MarketAPI market){
        if (!isValidMarket(market)) return false;
        if (!FactionTemplateController.getTemplate(market.getFaction()).isCanBeTradedWith()) return false;
        if (market.isHidden() && market.getFaction().isAtBest(lord.getFaction(),RepLevel.WELCOMING)) return false;
        if (market.getFaction().isAtBest(lord.getFaction(),RepLevel.SUSPICIOUS)) return false;
        if (market.getFaction().equals(lord.getFaction())) return false;
        return true;
    }
    public static boolean canBeAttackedByLord(Lord lord, MarketAPI market){
        if (!isAttackable(lord,market)) return false;
        boolean[] attacks = possibleAttacksMarket(lord,market,ATTACK_TYPE_RAID);
        if (attacks == null) return false;
        for (boolean a : attacks){
            if (a) return true;
        }
        return false;
    }
    public static boolean canBeAttackedByCampaign(Lord lord, MarketAPI market){
        if (!isAttackable(lord,market)) return false;
        boolean[] attacks = possibleAttacksMarket(lord,market,ATTACK_TYPE_CAMPAIGN);
        if (attacks == null) return false;
        for (boolean a : attacks){
            if (a) return true;
        }
        return false;
    }
    public static boolean isAttackable(Lord lord, MarketAPI market){
        FactionTemplate a = FactionTemplateController.getTemplate(lord.getFaction());
        FactionTemplate b = FactionTemplateController.getTemplate(market.getFaction());
        if (!isValidMarket(market)) return false;
        if (market.getFaction().isPlayerFaction() && !canPlayerBeAttacked) return false;
        if (!a.isCanAttack()) return false;
        if (!b.isCanBeAttacked()) return false;
        if (market.isHidden()) return false;
        //note: this is needed at the following functions: EventController.getPreferredRaidLocation, EventController.getCampaignTarget
        if (!market.getFaction().isHostileTo(lord.getLordAPI().getFaction())) return false;
        //if (!Utils.canBeAttacked(market.getFaction())) return false;
        //if ((Utils.nexEnabled() && !NexerlinUtilitys.canBeAttacked(market))) return false;
        //if (!isAttackable(lord,market.getFaction())) return false;
        if (Misc.getDaysSinceLastRaided(market) < RAID_COOLDOWN) return false;
        if (market.getFaction().isPlayerFaction() && canPlayerBeAttackedBeforeStarlords) return true;
        if (!LordController.getFactionsWithLords().contains(market.getFaction())) return false;
        return true;
    }

    private static boolean canHaveCampains(FactionAPI faction_0, FactionAPI faction_1){

        FactionTemplate a = FactionTemplateController.getTemplate(faction_0);
        FactionTemplate b = FactionTemplateController.getTemplate(faction_1);
        if (!LordController.getFactionsWithLords().contains(faction_1)) return false;
        //a faction is at war when a faction can attack with a campian, or a they can attack a faction with a campain.
        if (!(a.isCanBeAttacked() && b.isCanAttack() && b.isCanHaveCampaigns()) && !(a.isCanAttack() && a.isCanHaveCampaigns() && b.isCanBeAttacked())) return false;
        boolean can = false;
        boolean[] possibility = possibleAttacksFaction(faction_0,faction_1,ATTACK_TYPE_CAMPAIGN);
        if (possibility != null) {
            for (boolean c : possibility) {
                if (c) {
                    can = true;
                    break;
                }
            }
        }
        boolean can2 = false;
        boolean[] possibility2 = possibleAttacksFaction(faction_1,faction_0,ATTACK_TYPE_CAMPAIGN);
        if (possibility2 != null) {
            for (boolean c : possibility2) {
                if (c) {
                    can2 = true;
                    break;
                }
            }
        }
        //a faction needs to have a valid combat type against the other faction.
        if (!(a.isCanBeAttacked() && b.isCanAttack() && can2) && !(a.isCanAttack() && b.isCanBeAttacked() && can)) return false;


        return true;
    }
    public static boolean isAtWar(FactionAPI faction_0, FactionAPI faction_1){
        //ok, so this is not required. the faction template should handle this.
        //additional data.
        if (!faction_1.isHostileTo(faction_0)) return false;
        return canHaveCampains(faction_0,faction_1);
    }

    public static final String ATTACK_TYPE_RAID = "ATTACK_TYPE_RAID", ATTACK_TYPE_CAMPAIGN = "ATTACK_TYPE_CAMPAIGN";

    private static boolean[] possibleAttacksMarket(Lord lord, MarketAPI market, String type){
        LordEvent event = EventController.getCurrentRaid(lord);
        if (event != null) return possibleAttacksMarket(lord, market,event, type);

        event = EventController.getCurrentCampaign(lord.getFaction());
        if (event == null) return possibleAttacksMarket(lord, market, null,type);
        if (event.getOriginator().equals(lord)) return possibleAttacksMarket(lord, market, event,type);
        //if (event.getParticipants().contains(lord)) return possibleAttacksMarket(lord, market, event,type);

        return possibleAttacksMarket(lord, market,null, type);
    }
    private static boolean[] possibleAttacksMarket(Lord lord, MarketAPI market,LordEvent event, String type){
        //modifying attacks of type ATTACK_TYPE_CAMPAIGN
        //modifying attacks of type ATTACK_TYPE_RAID
        //log.info("modifying attacks of type "+type+" to determine what is possible on this market....");
        boolean[] out = possibleAttacksLord(lord,market.getFaction(),type);
        if (out == null) return null;
        //vilance left is not required. that just seems to be used, (in the contect of chosing attacks) to prevent lords from invading or raiding at random.
        if (out[0]){
            //canRaid. (basic). this is always true for now.
        }
        if (out[1]){
            //canRaid. (industry)
            //make it so this can only raid industry's not yet raided this attack.
            if (!canRaidIndustry(market) ) out[1] = false;
            out[1] = canRaidIndustry(market);
        }
        if (out[2]){
            //canTacticalBomb
            //limit the number of Tactical bombs allowed to be dropped. whats the base game max?
            int fuelCost = MarketCMD.getBombardmentCost(market, lord.getFleet());
            int fuelAmt=0;
            if (event != null){
                fuelAmt = (int) event.getTotalFuel();
            }else{
                fuelAmt = (int) lord.getFleet().getCargo().getFuel();
            }
            if (fuelAmt < fuelCost) {
                out[2] = false;
            }
        }
        if (out[3]){
            if (Utils.nexEnabled() && !NexerlinUtilitys.canBeInvaded(market)){
                out[3] = false;
            }else {
                //canInvade
                GroundBattleIntel tmp = new GroundBattleIntel(market, lord.getFaction(), market.getFaction());
                tmp.init();
                float defenderStr = GBUtils.estimateTotalDefenderStrength(tmp, true);
                tmp.endImmediately();
                float attackerStr = 0;
                if (event != null) {
                    float marines = event.getTotalMarines();
                    float heavies = event.getTotalArms();
                    marines = Math.max(0, marines - heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).personnel.mult);
                    attackerStr = marines * GroundUnitDef.getUnitDef(GroundUnitDef.MARINE).strength
                            + heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).strength;
                } else {
                    float marines = lord.getFleet().getCargo().getMarines();
                    float heavies = lord.getFleet().getCargo().getCommodityQuantity(Commodities.HAND_WEAPONS);
                    marines = Math.max(0, marines - heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).personnel.mult);
                    attackerStr = marines * GroundUnitDef.getUnitDef(GroundUnitDef.MARINE).strength
                            + heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).strength;
                }
                if (attackerStr <= 0.8 * defenderStr) {
                    out[3] = false;
                }
            }

        }
        if (out[4]){
            if (Utils.nexEnabled() && !NexerlinUtilitys.canBeInvaded(market)){
                out[4] = false;
            }else {
                //canSatBomb.
                //limit the amount of sat bombs allowed.
                int fuelCost = MarketCMD.getBombardmentCost(market, lord.getFleet());
                int fuelAmt = 0;
                if (event != null) {
                    fuelAmt = (int) event.getTotalFuel();
                } else {
                    fuelAmt = (int) lord.getFleet().getCargo().getFuel();
                }
                if (fuelAmt < fuelCost) {
                    out[4] = false;
                }
            }
        }
        /*for (boolean a : out){
            log.info("  got modified market data as: " + a);
        }*/
        return out;
    }
    public static boolean canRaidIndustry(MarketAPI market) {
        for (Industry industry : market.getIndustries()) {
            if (!industry.canBeDisrupted()) continue;
            if (industry.getSpec().hasTag(Industries.TAG_UNRAIDABLE)) continue;
            return true;
        }
        return false;
    }

    public static int getViolenceLeft(LordEvent event){
        int maxViolenceAgainstTarget=0;
        int currentViolenceAgainstTarget=0;
        if (event != null) {
            currentViolenceAgainstTarget = event.getTotalViolence();
            maxViolenceAgainstTarget = event.getMaxViolence();
        }
        return Math.max(0,maxViolenceAgainstTarget - currentViolenceAgainstTarget);
    }
    public static int getCampaignViolenceLeft(LordEvent event){
        return getViolenceLeft(event);
        //note: this is diabled in preperation for upgrading campains to have multible targets. but thats not ready yet.
        /*int maxViolenceAgainstTarget=0;
        int currentViolenceAgainstTarget=0;
        if (event != null) {
            currentViolenceAgainstTarget = event.getTotalCampaignViolence();
            maxViolenceAgainstTarget = event.getMaxCampaignViolence();
        }
        return Math.max(0,maxViolenceAgainstTarget - currentViolenceAgainstTarget);*/
    }

    private static boolean[] possibleAttacksLord(Lord lord, FactionAPI factionAPI, String type){
        //log.info("getting possable attacks for lord...");
        int typesOfAttacks = 5;
        boolean[] out = possibleAttacksFaction(lord.getFaction(),factionAPI,type);
        if (out == null) return out;
        boolean[] lordAttacks = {
                lord.canRaid(),
                lord.canRaid(),
                lord.canTacticallyBomb(),
                lord.canPreformInvasion(),
                lord.canSatBomb()
        };
        boolean[] output = new boolean[typesOfAttacks];
        for (int c = 0; c < typesOfAttacks; c++){
            output[c] = out[c] && lordAttacks[c];
            //log.info("  getting combat (lord) possability as: "+output[c]);
        }
        return output;
    }
    private static boolean[] possibleAttacksFaction(FactionAPI factionAPI_0, FactionAPI factionAPI_1, String type){
       // log.info("getting faction targets of "+type+" for factions "+factionAPI_0.getId()+" to "+factionAPI_1.getId());
        int typesOfAttacks = 5;
        boolean[] settings = new boolean[typesOfAttacks];
        //(not required. faction template handles this.)boolean canInvadeTemp = (Utils.nexEnabled() || NexerlinUtilitys.canInvade(factionAPI_0));
        //boolean canBeInvadedTemp = (Utils.nexEnabled() || NexerlinUtilitys.canInvade(factionAPI_1));
        switch (type){
            case ATTACK_TYPE_RAID:
                settings = new boolean[]{
                        canRaid_lord,//there are two types of raids.
                        canRaid_lord,
                        canTacticalBomb_lord,
                        canInvade_lord && Utils.nexEnabled() && NexerlinUtilitys.invasionsEnabled(),
                        canSatbomb_lord && (!Utils.nexEnabled() || NexerlinUtilitys.invasionsEnabled())
                };
                break;
            case ATTACK_TYPE_CAMPAIGN:
                settings = new boolean[]{
                        canRaid_campaign,
                        canRaid_campaign,
                        canTacticalBomb_campaign,
                        canInvade_campaign && Utils.nexEnabled() && NexerlinUtilitys.invasionsEnabled(),
                        canSatbomb_campaign && (!Utils.nexEnabled() || NexerlinUtilitys.invasionsEnabled())
                };
                break;
        }
        FactionTemplate a = FactionTemplateController.getTemplate(factionAPI_0);
        FactionTemplate b = FactionTemplateController.getTemplate(factionAPI_1);
        if (!a.isCanAttack()) return null;
        if (!b.isCanBeAttacked()) return null;
        boolean[] lordFactionAttacks = {
                a.isCanRaid(),
                a.isCanRaid(),
                a.isCanTacticalBomb(),
                a.isCanInvade(),
                a.isCanSatBomb()
        };
        boolean[] targetFactionAttacks = {
                b.isCanBeRaided(),
                b.isCanBeRaided(),
                b.isCanBeTacticalBomb(),
                b.isCanInvade(),
                b.isCanBeSatBomb()
        };
        boolean[] output = new boolean[typesOfAttacks];
        for (int c = 0; c < typesOfAttacks; c++){
            output[c] = lordFactionAttacks[c] && targetFactionAttacks[c] && settings[c];
           // log.info("  getting combat (faction) possability as: "+output[c]);
        }
        return output;
    }
    public static LordEvent.OffensiveType getOffencive(Lord lord,MarketAPI market,LordEvent event,String type){
        //log.info("got offencive possibility as:");
        boolean[] out = possibleAttacksMarket(lord,market,event,type);
        ArrayList<LordEvent.OffensiveType> options = new ArrayList<>();
        ArrayList<Integer> weights = new ArrayList<>();
        if (out == null) return null;
        for (boolean a : out){
            //log.info("  getting offencive possibility: "+a);
        }
        if (out[0]){
            //canRaid. (basic). this is always true for now.
            options.add(LordEvent.OffensiveType.RAID_GENERIC);
            weights.add(3);
        }
        if (out[1]){
            //canRaid. (industry)
            options.add(LordEvent.OffensiveType.RAID_INDUSTRY);
            weights.add(3);
        }
        if (out[2]){
            //canTacticalBomb
            options.add(LordEvent.OffensiveType.BOMBARD_TACTICAL);
            weights.add(10);
        }
        if (out[3]){
            //canInvade
            options.add(LordEvent.OffensiveType.NEX_GROUND_BATTLE);
            weights.add(20);
        }
        if (out[4]) {
            //canSatBomb.
            options.add(LordEvent.OffensiveType.BOMBARD_SATURATION);
            weights.add(20);
        }
        return Utils.weightedSample(options, weights, null);
    }
}
