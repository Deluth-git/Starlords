package starlords.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.EventController;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.controllers.RelationController;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.HashMap;

import static starlords.util.Constants.*;
import static starlords.util.Constants.COMPLETELY_UNJUSTIFIED;

public class DefectionUtils {

    // -50 to +50, higher means prefers other faction
    public static int computeRelativeFactionPreference(Lord lord, FactionAPI newFaction) {
        return (RelationController.getLoyalty(lord, newFaction.getId()) - RelationController.getLoyalty(lord)) / 4;
    }

    // -15 to 15, higher means prefers other faction
    public static int computeRelativeLordPreference(Lord lord, FactionAPI newFaction) {
        int pref = 0;
        for (Lord lord2 : LordController.getLordsList()) {
            if (!lord.equals(lord2) && lord2.getLordAPI().getFaction().equals(lord.getLordAPI().getFaction())) {
                if (RelationController.getRelation(lord, lord2) > Utils.getThreshold(RepLevel.WELCOMING)) {
                    pref -= 3;
                } else if (RelationController.getRelation(lord, lord2) < Utils.getThreshold(RepLevel.SUSPICIOUS)) {
                    pref += 3;
                }
            } else if (lord2.getLordAPI().getFaction().equals(newFaction)) {
                if (RelationController.getRelation(lord, lord2) > Utils.getThreshold(RepLevel.WELCOMING)) {
                    pref += 3;
                } else if (RelationController.getRelation(lord, lord2) < Utils.getThreshold(RepLevel.SUSPICIOUS)) {
                    pref -= 3;
                } else {
                    pref -= 1;
                }
            }
        }
        return Math.max(-15, Math.min(15, pref));
    }

    // from 0 to 14 Maxes out at 8 lords and 6 fiefs
    public static int computeFactionLegitimacy(FactionAPI targetFaction) {
        int legitimacyLords = 0;
        int legitimacyFiefs = 0;
        for (Lord lord : LordController.getLordsList()) {
            if (lord.getLordAPI().getFaction().equals(targetFaction)) legitimacyLords += 1;
        }
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFaction().equals(targetFaction)) legitimacyFiefs += 1;
        }
        return Math.min(legitimacyLords, 8) + Math.min(legitimacyFiefs, 6);
    }

    public static int getBaseReluctance(Lord lord) {
        switch(lord.getPersonality()) {
            case UPSTANDING:
                return -50;
            case MARTIAL:
                return -40;
            case CALCULATING:
                return -30;
            case QUARRELSOME:
            default:
                return -20;
        }
    }

    // lords have this chance to defect every month.
    public static int getAutoBetrayalChance(Lord lord) {
        int loyalty = RelationController.getLoyalty(lord);
        switch(lord.getPersonality()) {
            case UPSTANDING:
                return Math.min(15, Utils.getThreshold(RepLevel.HOSTILE) - loyalty);
            case MARTIAL:
                return Math.min(15, Utils.getThreshold(RepLevel.INHOSPITABLE) - loyalty);
            case CALCULATING:
                return Math.min(15, Utils.getThreshold(RepLevel.SUSPICIOUS) - loyalty);
            case QUARRELSOME:
                return Math.min(15, Utils.getThreshold(RepLevel.NEUTRAL) - loyalty);
        }
        return 0;
    }

    // defects to any faction
    // chooses defection faction based on faction opinion and faction lord density
    public static void performDefection(Lord lord) {
        // precompute some stuff
        HashMap<String, Integer> marketValues = new HashMap<>();
        HashMap<String, Integer> lordValues = new HashMap<>();
        for (MarketAPI marketAPI : Global.getSector().getEconomy().getMarketsCopy()) {
            String id = marketAPI.getFactionId();
            if (!marketValues.containsKey(id)) {
                marketValues.put(id, 0);
            }
            marketValues.put(id, marketValues.get(id) + 20);
        }
        for (Lord lord2 : LordController.getLordsList()) {
            String id = lord2.getFaction().getId();
            if (!lordValues.containsKey(id)) {
                lordValues.put(id, 0);
            }
            lordValues.put(id, lordValues.get(id) + 20);
        }

        FactionAPI preferredFaction = Global.getSector().getFaction(Factions.PIRATES);
        int preferredWeight = 25;
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (Misc.getCommissionFaction() != null && faction.isPlayerFaction()) continue;
            if (faction.equals(lord.getFaction())) continue;
            if (faction.getId().equals(Factions.INDEPENDENT)) continue;
            int weight = RelationController.getLoyalty(lord, faction.getId());
            if (!Misc.isPirateFaction(faction)) {
                if (marketValues.containsKey(faction.getId())) {
                    weight += marketValues.get(faction.getId());
                }
                if (lordValues.containsKey(faction.getId())) {
                    weight -= lordValues.get(faction.getId());
                }
            } else {
                weight = Math.max(25, 25 + weight); // some base value for preferring pirates
            }

            if (weight > preferredWeight) {
                preferredFaction = faction;
                preferredWeight = weight;
            }
        }
        // this can only happen for pirates, but dont make pirates defect to themselves
        if (preferredFaction.equals(lord.getFaction())) return;
        performDefection(lord, preferredFaction, true);
    }

    // defects to specified faction
    public static void performDefection(Lord lord, FactionAPI faction, boolean showMessage) {
        EventController.removeFromAllEvents(lord);
        String oldFactionName = lord.getFaction().getDisplayNameWithArticle();
        lord.getLordAPI().getFleet().setFaction(faction.getId(), true);
        lord.getLordAPI().setFaction(faction.getId());
        lord.setCurrAction(null);
        lord.setRanking(0);
        LordController.updateFactionsWithLords();
        Misc.setFlagWithReason(lord.getLordAPI().getFleet().getMemoryWithoutUpdate(),
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, "starlords", true, 0);
        int newLoyalty = RelationController.getLoyalty(lord);
        if (newLoyalty < MIN_STARTING_LOYALTY_DEFECTION) {
            RelationController.modifyLoyalty(lord, MIN_STARTING_LOYALTY_DEFECTION - newLoyalty);
        }
        // fiefs defect with the lord as long as they aren't turning pirate
        if (!Misc.isPirateFaction(faction)) {
            for (SectorEntityToken fief : lord.getFiefs()) {
                fief.getMarket().setFactionId(faction.getId());
                fief.setFaction(faction.getId());
            }
        } else {
            for (SectorEntityToken fief : new ArrayList<>(lord.getFiefs())) {
                FiefController.setOwner(fief.getMarket(), null);
            }
        }
        if (showMessage) {
            Global.getSector().getCampaignUI().addMessage(
                    StringUtil.getString(CATEGORY_UI, "lord_defection",
                            lord.getLordAPI().getNameString(), oldFactionName, faction.getDisplayNameWithArticle()),
                    faction.getBaseUIColor());
        }
    }

    public static int computeClaimJustification(String claim, FactionAPI targetFaction) {
        int numQuals = 0;
        switch(claim) {
            case CLAIM_UPSTANDING:
                int numMarkets = 0;
                int totalStab = 0;
                for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                    if (market.getFaction().equals(targetFaction)) {
                        totalStab += market.getStabilityValue();
                        numMarkets += 1;
                    }
                }
                if (numMarkets >= 4) numQuals += 1;
                if (totalStab >= 9 * numMarkets) numQuals += 1;
                break;
            case CLAIM_MARTIAL:
                if (Global.getSector().getPlayerPerson().getStats().getLevel() >= 15) numQuals += 1;
                if (Global.getSector().getPlayerFleet().getFleetPoints() >= 200) numQuals += 1;
                break;
            case CLAIM_QUARRELSOME:
                return FULLY_JUSTIFIED;
        }
        if (numQuals == 2) {
            return FULLY_JUSTIFIED;
        } else if (numQuals == 1) {
            return SOMEWHAT_JUSTIFIED;
        }
        return COMPLETELY_UNJUSTIFIED;
    }
}
