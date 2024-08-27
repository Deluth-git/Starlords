package util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import controllers.LordController;
import person.Lord;
import person.LordAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Utils {

    private static final float SOMEWHAT_CLOSE_DIST = 1000;
    private static final float CLOSE_DIST = 500;
    private static final int FAST_PATROL_FP = 20;
    private static final int COMBAT_PATROL_FP = 40;
    private static final int HEAVY_PATROL_FP = 65;
    public static final Random rand = new Random();

    public static int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static boolean nexEnabled() {
        return Global.getSettings().getModManager().isModEnabled("nexerelin");
    }

    public static String sirOrMaam(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Ma'am" : "Sir";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "ma'am" : "sir";
    }

    public static String manOrWoman(PersonAPI person, boolean caps) {
        if (caps) {
            return person.getGender() == FullName.Gender.FEMALE ? "Woman" : "Man";
        }
        return person.getGender() == FullName.Gender.FEMALE ? "woman" : "man";
    }

    public static void adjustPlayerReputation(PersonAPI target, int delta) {
        CoreReputationPlugin.CustomRepImpact param = new CoreReputationPlugin.CustomRepImpact();
        param.delta = delta / 100f;
        Global.getSector().adjustPlayerReputation(
                new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM,
                        param, null, null, false, true),
                target);
    }

    public static List<MarketAPI> getFactionMarkets(String factionId)
    {
        List<MarketAPI> allMarkets = Global.getSector().getEconomy().getMarketsCopy();
        List<MarketAPI> ret = new ArrayList<>();
        for (MarketAPI market : allMarkets)
        {
            if (market.getFactionId().equals(factionId))
                ret.add(market);
        }
        return ret;
    }

    public static float getHyperspaceDistance(SectorEntityToken entity1, SectorEntityToken entity2)
    {
        if (entity1.getContainingLocation() == entity2.getContainingLocation()) return 0;
        return Misc.getDistance(entity1.getLocationInHyperspace(), entity2.getLocationInHyperspace());
    }

    public static float estimateMarketDefenses(MarketAPI market) {
        CampaignFleetAPI marketFleet = Misc.getStationFleet(market);
        float stationStrength = 0;
        if (marketFleet != null) {
            stationStrength += marketFleet.getFleetPoints();
        }
        // estimates patrol strength, from nex
        float patrolStrength = 0;

        int maxLight = (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).computeEffective(0);
        int maxMedium = (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).computeEffective(0);
        int maxHeavy = (int) market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).computeEffective(0);

        patrolStrength += maxLight * FAST_PATROL_FP;
        patrolStrength += maxMedium * COMBAT_PATROL_FP;
        patrolStrength += maxHeavy * HEAVY_PATROL_FP;

        float fleetSizeMult = market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
        fleetSizeMult = 1 + (fleetSizeMult - 1) * 0.75f;
        patrolStrength *= fleetSizeMult;

        return patrolStrength + stationStrength;
    }

    public static boolean canRaidIndustry(MarketAPI market) {
        for (Industry industry : market.getIndustries()) {
            if (!industry.canBeDisrupted()) continue;
            if (industry.getSpec().hasTag(Industries.TAG_UNRAIDABLE)) continue;
            return true;
        }
        return false;
    }

    public static Industry getIndustryToRaid(MarketAPI market) {
        ArrayList<Industry> options = new ArrayList<>();
        for (Industry industry : market.getIndustries()) {
            if (!industry.canBeDisrupted()) continue;
            if (industry.getSpec().hasTag(Industries.TAG_UNRAIDABLE)) continue;
            options.add(industry);
        }
        if (options.isEmpty()) return null;
        return options.get(new Random().nextInt(options.size()));
    }

    // sorts lords so player is first, then marshal, then by rank, then alphabetically within the same rank
    // used for a bunch of uis
    public static void canonicalLordSort(ArrayList<Lord> lords) {
        lords.sort((o1, o2) -> {
            if (o1.isPlayer()) return -1;
            if (o2.isPlayer()) return 1;
            if (o1.isMarshal()) return -1;
            if (o2.isMarshal()) return 1;
            if (o1.getRanking() != o2.getRanking()) return o2.getRanking() - o1.getRanking();
            return o1.getLordAPI().getNameString().compareTo(o2.getLordAPI().getNameString());
        });
    }

    // convenience wrapper
    public static float getDaysSince(long timestamp) {
        return Global.getSector().getClock().getElapsedDaysSince(timestamp);
    }

    public static boolean isSomewhatClose(SectorEntityToken entity1, SectorEntityToken entity2) {
        return isClose(entity1, entity2, SOMEWHAT_CLOSE_DIST);
    }

    public static boolean isClose(SectorEntityToken entity1, SectorEntityToken entity2) {
        return isClose(entity1, entity2, CLOSE_DIST);
    }

    public static boolean isClose(SectorEntityToken entity1, SectorEntityToken entity2, float thres) {
        if (entity1 == null || entity2 == null) return false;
        LocationAPI loc1 = entity1.getContainingLocation();
        LocationAPI loc2 = entity2.getContainingLocation();
        if (loc1.isHyperspace() && loc2.isHyperspace()) {
            return Misc.getDistance(entity1.getLocationInHyperspace(), entity2.getLocationInHyperspace()) < thres;
        }
        if (!loc2.isHyperspace() && !loc2.isHyperspace()) {
            return Misc.getDistance(entity1.getLocation(), entity2.getLocation()) < thres;
        }
        return false;
    }

    public static int getThreshold(RepLevel level) {
        int sign = 1;
        if (level.isAtBest(RepLevel.NEUTRAL)) sign = -1;
        return Math.round(sign * 100 * level.getMin());
    }

    public static String getNearbyDescription(CampaignFleetAPI fleet) {
        if (fleet.isInHyperspace()) {
            return "Hyperspace near " + Misc.getNearestStarSystem(fleet);
        } else {
            return Misc.findNearestPlanetTo(fleet, false, true).getName() + " in " + fleet.getContainingLocation().getName();
        }
    }

    public static FactionAPI getRecruitmentFaction() {
        FactionAPI faction = Misc.getCommissionFaction();
        if (faction == null) faction = Global.getSector().getPlayerFaction();
        return faction;
    }

    public static float getRaidStr(CampaignFleetAPI fleet, float marines) {
        float attackerStr = marines;
        StatBonus stat = fleet.getStats().getDynamic().getMod(Stats.PLANETARY_OPERATIONS_MOD);
        attackerStr = stat.computeEffective(attackerStr);
        return attackerStr;
    }

    public static String getTitle(FactionAPI faction, int rank) {
        String titleStr = "title_" + faction.getId() + "_" + rank;
        String ret = StringUtil.getString("starlords_title", titleStr);
        if (ret != null && ret.startsWith("Missing string")) {
            ret = StringUtil.getString("starlords_title", "title_default_" + rank);
        }
        return ret;
    }

    public static <T> T weightedSample(List<T> data, List<Integer> weights, Random rand) {
        if (data.isEmpty()) return null;
        if (rand == null) rand = new Random();
        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        int choice = rand.nextInt(totalWeight);
        T curr = null;
        for (int i = 0; i < weights.size(); i++) {
            if (weights.get(i) > choice) {
                curr = data.get(i);
                break;
            } else {
                choice -= weights.get(i);
            }
        }
        return curr;
    }

    // gets number of major faction enemies of specified faction
    // ignores pirate and lordless factions
    public static int getNumMajorEnemies(FactionAPI faction) {
        int numEnemies = 0;
        for (FactionAPI faction2 : LordController.getFactionsWithLords()) {
            if (!Misc.isPirateFaction(faction2) && faction.isHostileTo(faction2)) numEnemies += 1;
        }
        return numEnemies;
    }

    public static PersonAPI getLeader(FactionAPI faction) {
        switch (faction.getId()) {
            case Factions.PIRATES:
            case Factions.LUDDIC_CHURCH:
                return null;
            case Factions.HEGEMONY:
                return Global.getSector().getImportantPeople().getPerson(People.DAUD);
            case Factions.PERSEAN:
                return Global.getSector().getImportantPeople().getPerson(People.REYNARD_HANNAN);
            case Factions.DIKTAT:
                return Global.getSector().getImportantPeople().getPerson(People.ANDRADA);
            case Factions.LUDDIC_PATH:
                return Global.getSector().getImportantPeople().getPerson(People.COTTON);
            case Factions.TRITACHYON:
                return Global.getSector().getImportantPeople().getPerson(People.SUN);
            case Factions.PLAYER:
                return Global.getSector().getPlayerPerson();
        }
        return null;
    }

    public static String getLiegeName(FactionAPI faction) {
        switch (faction.getId()) {
            case Factions.PIRATES:
                return null;
            case Factions.HEGEMONY:
                return "Baikal Daud";
            case Factions.PERSEAN:
                return "Reynard Hannan";
            case Factions.DIKTAT:
                return "Phillip Andrada";
            case Factions.LUDDIC_CHURCH:
                return "The Pope";
            case Factions.LUDDIC_PATH:
                return "Livewell Cotton";
            case Factions.TRITACHYON:
                return "Artemisia Sun";
            case Factions.PLAYER:
                return Global.getSector().getPlayerPerson().getNameString();
        }
        return null;
    }
}
