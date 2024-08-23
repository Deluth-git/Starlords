package util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import controllers.LordController;
import person.Lord;
import person.LordPersonality;

import java.util.*;

public class LordFleetFactory extends FleetFactoryV3 {

    public static final float DP_CAP = 500;
    public static final float COST_MULT = 500;  // cost for a lord to buy a ship is its base DP cost * COST_MULT
    public static final float MOD_COST = 2000;  // cost for lord to buy 1 s-mod
    public static final int FUEL_COST = 10;  // cost for lord to buy 1 fuel
    public static final int MARINE_COST = 80;  // cost for lord to buy 1 marine
    public static final int HEAVY_ARMS_COST = 200;  // cost for lord to buy 1 heavy armament

    public static final String[] OFFICER_SKILLS = new String[]{
            Skills.FIELD_MODULATION, Skills.COMBAT_ENDURANCE, Skills.GUNNERY_IMPLANTS,
            Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION, Skills.ORDNANCE_EXPERTISE,
            Skills.POLARIZED_ARMOR, Skills.MISSILE_SPECIALIZATION, Skills.SYSTEMS_EXPERTISE
    };
    public static final int OFFICER_MAX_LEVEL = 7;

    public static float addToLordFleet(ShipRolePick pick, CampaignFleetAPI fleet, Random random) {
        return FleetFactoryV3.addToFleet(pick, fleet, random);
    }

    public static void populateCaptains(Lord lord) {
        for (FleetMemberAPI ship : lord.getLordAPI().getFleet().getFleetData().getMembersListCopy()) {
            if (ship.isFighterWing() || !ship.getCaptain().getNameString().isEmpty()) {
                continue;
            }
            if (ship.isFlagship()) {
                ship.setCaptain(lord.getLordAPI());
            } else {
                PersonAPI officer = lord.getLordAPI().getFaction().createRandomPerson();
                officer.setPersonality(lord.getTemplate().battlePersonality);
                upskillOfficer(officer);
                Misc.setUnremovable(officer, true);
                lord.getLordAPI().getFleet().getFleetData().addOfficer(officer);
                ship.setCaptain(officer);
            }
        }
    }

    // returns cost of ships purchased
    public static float addToLordFleet(HashMap<String, Integer> options, CampaignFleetAPI fleet, Random random, float dp, float cash) {
        int totalShips = 0;
        float totalDP = 0;
        float totalCost = 0;
        HashMap<String, FleetMemberAPI> cache = new HashMap<>();
        HashMap<String, Integer> variantCtr = new HashMap<>();
        // count current ships, to modify ship purchase weights
        for (FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
            if (ship.isFighterWing()) {
                continue;
            }
            totalDP += ship.getUnmodifiedDeploymentPointsCost();
            totalShips += 1;
            String variant = ship.getVariant().getHullVariantId();
            if (!variantCtr.containsKey(variant)) {
                variantCtr.put(variant, 1);
            } else {
                variantCtr.put(variant, variantCtr.get(variant) + 1);
            }
        }

        boolean isBroke = false;
        while (!isBroke) {
            // get possible ships to buy and weights
            int total = 0;
            HashMap<String, Integer> validKeys = new HashMap<>();
            for (String key : options.keySet()) {
                if (!cache.containsKey(key)) {
                    ShipVariantAPI variant = Global.getSettings().getVariant(key);
                    if (variant == null) {
                        log.info("INVALID VARIANT " + key);
                        continue;
                    }
                    FleetMemberAPI temp = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
                    cache.put(key, temp);
                }
                float variantDP = cache.get(key).getUnmodifiedDeploymentPointsCost();
                float cost = variantDP * COST_MULT;

                if (totalDP + variantDP > dp || cost + totalCost > cash) {
                    continue;
                }
                int existing = 0;
                if (variantCtr.containsKey(key)) {
                    existing = variantCtr.get(key);
                }
                int modifiedWeight = Math.max(1, options.get(key) - 100 * existing / Math.max(totalShips, 1));
                total += modifiedWeight;
                validKeys.put(key, modifiedWeight);
            }

            if (validKeys.isEmpty()) {
                isBroke = true;
                continue;
            }

            // choose ship to buy
            int rand = random.nextInt(total);
            String curr = null;
            int currVal = 0;
            for (String key : validKeys.keySet()) {
                currVal += validKeys.get(key);
                if (currVal >= rand) {
                    curr = key;
                    break;
                }
            }

            // buy ship
            totalDP += addToLordFleet(new ShipRolePick(curr), fleet, random);
            totalCost += cache.get(curr).getUnmodifiedDeploymentPointsCost() * COST_MULT;
            totalShips += 1;
            if (variantCtr.containsKey(curr)) {
                variantCtr.put(curr, variantCtr.get(curr) + 1);
            } else {
                variantCtr.put(curr, 1);
            }

        }
        fleet.getFleetData().sort();
        return totalCost;
    }

    public static float addModsToFleet(CampaignFleetAPI fleet, Random random, float cash) {
        float totalCost = 0;
//        List<FleetMemberAPI> ships = fleet.getFleetData().getMembersListCopy();
//        while (totalCost + MOD_COST < cash) {
//            totalCost += MOD_COST;
//            int idx = random.nextInt(ships.size());
//            FleetMemberAPI ship = ships.get(idx);
//
//        }
        return totalCost;
    }

    // buys fuel, marines, and heavy armaments
    public static float buyGoodsforFleet(Lord lord, CampaignFleetAPI fleet, Random random, float cash) {
        float totalCost = 0;
        CargoAPI cargo = fleet.getCargo();
        int fuelCap = cargo.getFreeFuelSpace();
        int crewCap = cargo.getFreeCrewSpace();
        float cargoCap = cargo.getSpaceLeft();
        int fuelRatio = random.nextInt(50);
        // need more fuel for saturation bombing
        if (lord.getPersonality() == LordPersonality.QUARRELSOME) {
            fuelRatio = 5 * fuelRatio / 4;
        }
        int fuelToBuy = Math.min(fuelCap, (int) (cash * fuelRatio / 100 / FUEL_COST));
        totalCost += fuelToBuy * FUEL_COST;
        cargo.addFuel(fuelToBuy);

        int marineRatio = 70 + random.nextInt(30);
        int marinesToBuy = Math.min(crewCap, (int) ((cash - totalCost) * marineRatio / 100 / MARINE_COST));
        totalCost += marinesToBuy * MARINE_COST;
        cargo.addMarines(marinesToBuy);

        int armsToBuy = (int) Math.min(cargoCap, (cash - totalCost) / HEAVY_ARMS_COST);
        totalCost += armsToBuy * HEAVY_ARMS_COST;
        cargo.addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.HAND_WEAPONS, armsToBuy);
        return totalCost;
    }

    public static void upgradeFleet(Lord lord) {
        // Restore flagship if it was destroyed previously
        float totalDP = 0;
        boolean hasFlagship = false;
        for (FleetMemberAPI ship : lord.getLordAPI().getFleet().getMembersWithFightersCopy()) {
            if (ship.isFighterWing()) {
                continue;
            }
            if (ship.isFlagship()) {
                hasFlagship = true;
            }
            totalDP += ship.getUnmodifiedDeploymentPointsCost();
        }
        if (!hasFlagship) {
            FleetMemberAPI flagShip = Global.getFactory().createFleetMember(FleetMemberType.SHIP, lord.getTemplate().flagShip);
            flagShip.setFlagship(true);
            lord.getLordAPI().getFleet().getFleetData().addFleetMember(flagShip);
        }

        // allocate wealth to buying ships and upgrades
        float shipFunds = Math.min(lord.getWealth(), lord.getWealth() * (2 - 2 * totalDP / DP_CAP));
        float cost = addToLordFleet(lord.getTemplate().shipPrefs, lord.getLordAPI().getFleet(), new Random(), DP_CAP, shipFunds);
        lord.addWealth(-1 * cost);
        log.info("Lord " + lord.getLordAPI().getNameString() + " purchased " + Math.round(cost) + " of ships.");
        cost = buyGoodsforFleet(lord, lord.getLordAPI().getFleet(), new Random(), lord.getWealth());
        lord.addWealth(-1 * cost);
        //cost = addModsToFleet(lord.getLordAPI().getFleet(), new Random(), lord.getWealth());
        populateCaptains(lord);
    }

    // gives person a combat skill that they didn't have previously
    public static void upskillOfficer(PersonAPI officer) {
        ArrayList<String> candidates = new ArrayList<>();
        for (String skill : OFFICER_SKILLS) {
            if (officer.getStats().getSkillLevel(skill) == 0) candidates.add(skill);
        }
        if (candidates.isEmpty()) return;
        officer.getStats().setSkillLevel(candidates.get(new Random().nextInt(candidates.size())), 2);
    }
}
