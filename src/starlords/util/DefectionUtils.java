package starlords.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import starlords.controllers.EventController;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.controllers.RelationController;
import starlords.controllers.RequestController;
import starlords.person.Lord;
import starlords.person.LordRequest;
import starlords.util.factionUtils.FactionTemplateController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static starlords.util.Constants.*;
import static starlords.util.Constants.COMPLETELY_UNJUSTIFIED;

public class DefectionUtils {

	private static final Logger log = Logger.getLogger(DefectionUtils.class);
	public static int MIN_STABILITY_TO_PREVENT_FIEF_DEFECTION;

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
		int chance = 50;
		int lordsInFaction = 0;

		//If the Faction has no markets, keep chance to 50
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.getFactionId().equals(lord.getFaction().getId())) {
				chance = 0;
				break;
			}
		}

		if (chance > 0){
			return chance;
		}

		//If the faction still has markets then only defect if there are at least 4 lords
		for (Lord lordfaction : LordController.getLordsList()) {
				if (lord.getFaction().equals(lordfaction.getFaction()))
					lordsInFaction++;
			}
		if (lordsInFaction < 4)
			return 0;

		switch (lord.getPersonality()) {
			case UPSTANDING:
				chance = Math.min(15, (Utils.getThreshold(RepLevel.HOSTILE) - loyalty) / 2);
			case MARTIAL:
				chance = Math.min(15, (Utils.getThreshold(RepLevel.INHOSPITABLE) - loyalty) / 2);
			case CALCULATING:
				chance = Math.min(15, (Utils.getThreshold(RepLevel.SUSPICIOUS) - loyalty) / 2);
			case QUARRELSOME:
				chance = Math.min(15, (Utils.getThreshold(RepLevel.NEUTRAL) - loyalty) / 2);

		}

		//Luddic Path, Knight of Eva and Luddic Knights are fanatics so need really bad relations
		List<String> fanaticFaction = new ArrayList<String>();
		fanaticFaction.add("Luddic Path");
		fanaticFaction.add("Knights of Eva");
		fanaticFaction.add("Knights of Ludd");

		if (fanaticFaction.contains(lord.getFaction().getDisplayName()))
			chance -= 20;

		int fiefs = lord.getFiefs().size();
		if (fiefs > 0)
			chance -= fiefs * 10;
		else
			chance += 10;

		chance += lord.getEscapeAttempts();

		return chance;
	}

	public static boolean getFactionDefectionEligibility(Lord lord, FactionAPI faction) {
		if (!FactionTemplateController.getTemplate(faction).isCanStarlordsJoin()) return false;
		//todo: If i get the time, i really really need to make it so lords can offer to defect over to the player faction. please I beg of you let that happen. I waited for nearly 4 cycles for one to join me and now I learn that is not a thing and this can cause a issue with large number of lords were you cant get any up to the required reputation and this bugs me. plz fix.
		//todo:... is this fixed? arg, I dont knowwwwww. I better ignore it.
		if (Misc.getCommissionFaction() != null && faction.isPlayerFaction()) return false;
		if (faction.isPlayerFaction() && lord.getLordAPI().getRelToPlayer().isAtBest(RepLevel.WELCOMING)) return false;
		if (faction.isPlayerFaction() && !LordController.getFactionsWithLords().contains(faction)) return false;
		if (faction.equals(lord.getFaction())) return false;
		//todo: switch this from 'independents' to a function that sees if this faction does not allow lords.
		//if (faction.getId().equals(Factions.INDEPENDENT)) return false;

		return true;
	}

	public static FactionAPI getLordPreferredFaction(Lord lord, boolean printToLog) {

		// number of markets in target factions
		HashMap<String, Integer> marketValues = new HashMap<>();
		// number of lords in target factions
		HashMap<String, Integer> lordValues = new HashMap<>();
		//aggregated relations of lord with lords of target factions
		HashMap<String, Float> lordRelationValues = new HashMap<>();

		for (MarketAPI marketAPI : Global.getSector().getEconomy().getMarketsCopy()) {
			String id = marketAPI.getFactionId();
			if (!marketValues.containsKey(id)) {
				marketValues.put(id, 0);
			}
			marketValues.put(id, marketValues.get(id) + 1);
		}
		for (Lord lord2 : LordController.getLordsList()) {
			String id = lord2.getFaction().getId();
			if (!lordValues.containsKey(id)) {
				lordValues.put(id, 0);
			}
			if (!lordRelationValues.containsKey(id)) {
				lordRelationValues.put(id, 0f);
			}

			lordValues.put(id, lordValues.get(id) + 1);
			lordRelationValues.put(id, lordRelationValues.get(id) + RelationController.getRelation(lord,lord2));
		}

		//average relations with lords of faction and normalize (divide by 10)
		for (Map.Entry<String,Float> entry : lordRelationValues.entrySet()) {
			entry.setValue(entry.getValue() / (float) lordValues.get(entry.getKey()) / 10f);
		}

		String output = "[Star Lords]\tLordID\tLordName\tLordFaction\tTargetFaction\tRelation\tAlignmentDifference\tDefectionWeight" + System.lineSeparator();

		FactionAPI preferredFaction = Global.getSector().getFaction(Factions.PIRATES);
		float preferredWeight = 0;

		for (FactionAPI faction : Global.getSector().getAllFactions()) {

			if (getFactionDefectionEligibility(lord,faction) == false) continue;

			float weight = 0;

			weight += lordRelationValues.getOrDefault(faction.getId(), 5f);
			weight += marketValues.getOrDefault(faction.getId(), -10);
			weight -= lordValues.getOrDefault(faction.getId(), 0);
			if (Utils.nexEnabled())
				weight -= NexerlinUtilitys.getAlignmentsComparison(
								lord.getAlignments(),
								NexerlinUtilitys.getFactionAlignments(faction.getId()));


			output += "[Star Lords]" + "\t"
					+ lord.getLordAPI().getId() + "\t"
					+ lord.getLordAPI().getNameString() + "\t"
					+ lord.getLordAPI().getFaction().getDisplayName() + "\t"
					+ faction.getDisplayName() + "\t"
					+ lordRelationValues.getOrDefault(faction.getId(), 5f) + "\t";
			if (Utils.nexEnabled())
				output += NexerlinUtilitys.getAlignmentsComparison(lord.getAlignments(),NexerlinUtilitys.getFactionAlignments(faction.getId())) + "\t";

			output += weight + "\t" + System.lineSeparator();

			if (weight > preferredWeight) {
				preferredFaction = faction;
				preferredWeight = weight;
			}
		}

		if (printToLog)
			log.info(output);

		return preferredFaction;
	}

	// defects to any faction
	// chooses defection faction based on faction opinion and faction lord density
	public static void performDefection(Lord lord) {
		LordRequest openDefectionRequest = RequestController.getCurrentDefectionRequest(lord);
		if (openDefectionRequest != null) {
			RequestController.endRequest(openDefectionRequest);
		}

        FactionAPI preferredFaction = getLordPreferredFaction(lord, false);
        // this can only happen for pirates, but dont make pirates defect to themselves
        if (preferredFaction.equals(lord.getFaction())) return;
        performDefection(lord, preferredFaction, true);
    }

    // defects to specified faction
    public static void performDefection(Lord lord, FactionAPI faction, boolean showMessage,boolean includeFiefs) {
        EventController.removeFromAllEvents(lord);
        FactionAPI oldFaction = lord.getFaction();
        String oldFactionName = lord.getFaction().getDisplayNameWithArticle();
        if (faction.isPlayerFaction()) lord.setKnownToPlayer(true);
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
        // changed this into faction that can be attacked. I considered this for all minor factions, but some of them can be attacked, and such can get back there markets so...
        if (includeFiefs && FactionTemplateController.getTemplate(oldFaction).isCanLordsTakeFiefsWithDefection() && !FactionTemplateController.getTemplate(faction).isCanLordsTakeFiefsWithDefection()) {
            for (SectorEntityToken fief : new ArrayList<>(lord.getFiefs())) {
                if (fief.getMarket().getStabilityValue() < MIN_STABILITY_TO_PREVENT_FIEF_DEFECTION) {
	                fief.getMarket().setFactionId(faction.getId());
	                fief.setFaction(faction.getId());

	                for (SectorEntityToken entity : fief.getMarket().getConnectedEntities()) {
						if (entity == null || entity.getCustomEntityType() == null) continue;
		                if (entity.getCustomEntityType().equals(Entities.STATION_BUILT_FROM_INDUSTRY))
		                    entity.setFaction(faction.getId());
	                }
                }
				else
	                FiefController.setOwner(fief.getMarket(), null);
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
    public static void performDefection(Lord lord, FactionAPI faction, boolean showMessage){
        performDefection(lord, faction, showMessage,true);
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
