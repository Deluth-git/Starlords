package starlords.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import starlords.person.Lord;
import starlords.person.LordPersonality;

import java.util.*;

public class LordFleetFactory extends FleetFactoryV3 {

    public static final float DP_CAP = 500;
    public static final float GARRISON_DP_CAP = 500;
    public static final float COST_MULT = 750;  // cost for a lord to buy a ship is its base DP cost * COST_MULT
    public static final float MOD_COST = 250;  // cost for a lord to buy a s-mod is its base DP cost * MOD_COST
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
                upskillOfficer(officer, true);
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

    // adds s-mods to fleet members
    public static float addModsToFleet(CampaignFleetAPI fleet, float cash) {
        float totalCost = 0;
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        Collections.shuffle(members);
        for (FleetMemberAPI member : members) {
            if (member.getVariant().getPermaMods().size() >= 3) continue;
            float modCost = MOD_COST * member.getUnmodifiedDeploymentPointsCost();
            if (member.isFlagship()) modCost = 0;
            // discount for very experienced ship
            if (member.getCaptain().getStats().getLevel() > 2 + 2 * member.getVariant().getSMods().size()) modCost /= 2;
            if (modCost + totalCost > cash) continue;

            String modToAdd = chooseSMod(member);
            log.info("Adding mod " + modToAdd + ". Ship has " + member.getVariant().getPermaMods().size());
            if (modToAdd != null) {
                totalCost += modCost;
                member.getVariant().addPermaMod(modToAdd, true);
            }
        }
        return totalCost;
    }

    // adds to garrison of a lord's fief
    public static float buyGarrison(Lord lord, float cash) {
        float totalCost = 0;
        if (cash <= 0) return 0;
        SectorEntityToken marketEntity = lord.getClosestBase();
        if (lord.getFiefs().contains(marketEntity)
                && marketEntity.getContainingLocation().equals(lord.getFleet().getContainingLocation())) {
            MarketAPI market = marketEntity.getMarket();
            CampaignFleetAPI stationFleet = Misc.getStationFleet(market);
            if (stationFleet == null) return 0;  // TODO
            totalCost = addToLordFleet(
                    lord.getTemplate().shipPrefs, stationFleet, new Random(), GARRISON_DP_CAP, cash);
            FleetParamsV3 params = new FleetParamsV3();
            params.factionId = market.getFactionId();
            params.officerLevelLimit = 5;
            params.commanderLevelLimit = 5;
            FleetFactoryV3.addCommanderAndOfficersV2(stationFleet, params, new Random());

        }
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
        for (FleetMemberAPI ship : lord.getFleet().getMembersWithFightersCopy()) {
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
            String name = lord.getFleet().getFleetData().pickShipName(flagShip, Utils.rand);
            flagShip.setShipName(name);
            lord.getFleet().getFleetData().addFleetMember(flagShip);
            flagShip.setFlagship(true);
        }

        // allocate wealth to buying ships and upgrades
        CargoAPI cargo = lord.getFleet().getCargo();
        float shipFunds = Math.min(lord.getWealth(), lord.getWealth() * (2 - 2 * totalDP / DP_CAP));
        // lords should buy a bit of fuel and marines before buying smods
        float minCargoFunds = Math.min(
                lord.getWealth() - shipFunds,
                FUEL_COST * Math.max(100, 500 - cargo.getFuel()) + MARINE_COST * Math.max(0, 200 - cargo.getMarines()));
        float modFunds = 3 * (lord.getWealth() - shipFunds - minCargoFunds) / 4;
        float cost = addToLordFleet(lord.getTemplate().shipPrefs, lord.getFleet(), new Random(), DP_CAP, shipFunds);
        lord.addWealth(-1 * cost);
        //log.info("Lord " + lord.getLordAPI().getNameString() + " purchased " + Math.round(cost) + " of ships.");
        cost = addModsToFleet(lord.getFleet(), modFunds);
        lord.addWealth(-1 * cost);
        cost = buyGarrison(lord, lord.getWealth() - minCargoFunds);
        lord.addWealth(-1 * cost);
        cost = buyGoodsforFleet(lord, lord.getFleet(), new Random(), lord.getWealth());
        lord.addWealth(-1 * cost);
        populateCaptains(lord);
    }

    // gives person a combat skill that they didn't have previously
    public static void upskillOfficer(PersonAPI officer, boolean elite) {
        ArrayList<String> candidates = new ArrayList<>();
        for (String skill : OFFICER_SKILLS) {
            if (officer.getStats().getSkillLevel(skill) == 0) candidates.add(skill);
        }
        if (candidates.isEmpty()) return;
        int skillLevel = elite ? 2 : 1;
        officer.getStats().setSkillLevel(candidates.get(new Random().nextInt(candidates.size())), skillLevel);
    }

    private static String chooseSMod(FleetMemberAPI member) {
        ArrayList<String> options = new ArrayList<>();
        ArrayList<Integer> weights = new ArrayList<>();
        boolean armorFocus = member.getVariant().getHullSpec().getManufacturer().equals("Low Tech")
                || member.getHullSpec().getShieldType() == ShieldAPI.ShieldType.NONE;
        boolean phaseFocus = member.getHullSpec().getShieldType() == ShieldAPI.ShieldType.PHASE;
        boolean shieldFocus = !armorFocus && !phaseFocus;

        // escort package is op
        if (member.getHullSpec().getHullSize() == ShipAPI.HullSize.DESTROYER) {
            addOption(options, weights, member, "escort_package", 100);
        }

        // onslaughts get expanded mags
        if (member.getHullId().equals("onslaught")) {
            addOption(options, weights, member, HullMods.MAGAZINES, 100);
        }

        // everyone likes flux
        addOption(options, weights, member, HullMods.FLUX_DISTRIBUTOR, 20);
        addOption(options, weights, member, HullMods.FLUX_COIL, 20);
        // everyone likes missiles
        addOption(options, weights, member, HullMods.MISSLERACKS, 10);
        // every ai likes omni shields
        if (member.getVariant().getHullSpec().getShieldType() == ShieldAPI.ShieldType.FRONT) {
            addOption(options, weights, member, HullMods.OMNI_SHIELD_CONVERSION, 30);
        }
        // safety overrides needs hardened subsystems
        if (member.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)) {
            addOption(options, weights, member, HullMods.HARDENED_SUBSYSTEMS, 100);
        }

        // add ITU if it's not there for some reason
        if (!member.getVariant().hasHullMod(HullMods.ADVANCED_TARGETING_CORE)
                && !member.getVariant().hasHullMod(HullMods.DEDICATED_TARGETING_CORE)
                && !member.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)
                && !member.getVariant().hasHullMod(HullMods.DISTRIBUTED_FIRE_CONTROL)) {
            addOption(options, weights, member, HullMods.INTEGRATED_TARGETING_UNIT, 100);
        }

        if (phaseFocus) {
            addOption(options, weights, member, HullMods.ADAPTIVE_COILS, 20);
            addOption(options, weights, member, HullMods.FLUXBREAKERS, 10);
            addOption(options, weights, member, HullMods.ARMOREDWEAPONS, 10);
            addOption(options, weights, member, HullMods.HARDENED_SUBSYSTEMS, 10);
        }

        if (armorFocus) {
            addOption(options, weights, member, HullMods.HEAVYARMOR, 80);
            addOption(options, weights, member, HullMods.ARMOREDWEAPONS, 20);
            addOption(options, weights, member, HullMods.REINFORCEDHULL, 30);
            addOption(options, weights, member, HullMods.FLUXBREAKERS, 10);
        }

        if (shieldFocus) {
            addOption(options, weights, member, HullMods.HARDENED_SHIELDS, 40);
            addOption(options, weights, member, HullMods.STABILIZEDSHIELDEMITTER, 20);
        }

        if (options.isEmpty()) return null;
        return Utils.weightedSample(options, weights, Utils.rand);
    }

    private static void addOption(
            ArrayList<String> options, ArrayList<Integer> weights,
            FleetMemberAPI member, String option, int weight) {
        if (member.getVariant().hasHullMod(option)) return;
        assert !member.getVariant().getPermaMods().contains(option);
        options.add(option);
        weights.add(weight);
    }
}
