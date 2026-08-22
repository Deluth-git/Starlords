package starlords.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.alliances.Alliance;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import starlords.ai.utils.TargetUtils;
import starlords.controllers.LordController;
import starlords.controllers.EventController;
import starlords.controllers.PoliticsController;
import starlords.controllers.RelationController;
import starlords.controllers.RequestController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;
import starlords.person.LordRequest;
import starlords.ui.ToolTip;
import starlords.util.factionUtils.FactionTemplateController;

import java.awt.*;
import java.util.*;
import java.util.List;

import static starlords.util.Constants.*;


public class Utils {

    public static final Logger log = Global.getLogger(Utils.class);

    private static final float SOMEWHAT_CLOSE_DIST = 1000;
    private static final float CLOSE_DIST = 500;
    private static final int FAST_PATROL_FP = 20;
    private static final int COMBAT_PATROL_FP = 40;
    private static final int HEAVY_PATROL_FP = 65;
    @Setter
    private static boolean showMessageLordCaptureReleaseEscape;
	public static final Random rand = new Random(); // for low-priority rng that doesn't need to be savescum-proof
    public static String getFactionTitle(String faction,int ranking){
        switch (ranking){
            case 2:
                return FactionTemplateController.getTemplate(faction).getT2_title();
            case 1:
                return FactionTemplateController.getTemplate(faction).getT1_title();
            default:
                return FactionTemplateController.getTemplate(faction).getT0_title();
        }
    }
    public static int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static boolean nexEnabled() {
        return Global.getSettings().getModManager().isModEnabled("nexerelin");
    }

    public static boolean secondInCommandEnabled() {
        return Global.getSettings().getModManager().isModEnabled("second_in_command");
    }

    public static void showUIMessageCaptureStatus(String message, FactionAPI faction) {
        if (showMessageLordCaptureReleaseEscape
                || LordController.getPlayerLord().getFaction().equals(faction)
                || faction.equals(Misc.getCommissionFaction()))
            Global.getSector().getCampaignUI().addMessage(message,faction.getBaseUIColor());
        //test
    }

	public static String getLordCurrOrders(Lord lord, double repForCurAction) {
		String orderStr;
		FactionAPI faction = lord.getFaction();
		boolean isSubject = faction.equals(Global.getSector().getPlayerFaction());
		boolean isMarried = lord.isMarried();
		CampaignFleetAPI fleet = lord.getLordAPI().getFleet();
		if (lord.getPlayerRel() < repForCurAction
				&& !isSubject && !isMarried && !DEBUG_MODE) {
			orderStr = "[REDACTED]";
		} else if (lord.getCurrAction() == LordAction.IMPRISONED) {
            Lord capture = LordController.getLordOrPlayerById(lord.getCaptor());
            if (capture != null) {
                orderStr = "Imprisoned by " + LordController.getLordOrPlayerById(lord.getCaptor()).getLordAPI().getNameString();
            }else{
                orderStr = "[REDACTED]";
            }
		} else if (lord.getCurrAction() == LordAction.COMPANION) {
			orderStr = "Traveling with you";
		} else if (lord.getCurrAction() == null || !fleet.isAlive()) {
			orderStr = "None";
		} else if (lord.getCurrAction() != LordAction.CAMPAIGN) {
			orderStr = StringUtil.getString(
					CATEGORY_UI, "fleet_" + lord.getCurrAction().base.toString().toLowerCase() + "_desc", lord.getTarget().getName());
		} else {
			if (lord.isMarshal()) {
				LordEvent campaign = EventController.getCurrentCampaign(lord.getLordAPI().getFaction());
				if (campaign.getTarget() == null) {
					orderStr = StringUtil.getString(CATEGORY_UI, "fleet_campaign_lead_desc", lord.getTarget().getName());
				} else {
					orderStr = StringUtil.getString(CATEGORY_UI, "fleet_campaign_lead_desc", campaign.getTarget().getName());
				}
			} else {
				orderStr = StringUtil.getString(CATEGORY_UI, "fleet_campaign_follow_desc");
			}
		}
		return orderStr;
	}

	public static void addShipHullsWithCodex (TooltipMakerAPI info, Lord lord, float pad) {
		info.beginImageWithText("",5,200,true);
		info.addImageWithText(pad);

		float totalChance = 0;

		SettingsAPI settings = Global.getSettings();
		List<ShipHullSpecAPI> hullList = new ArrayList<>();
		Map<ShipHullSpecAPI,Integer> hullIdQtyMap = new LinkedHashMap<>();
		for (Map.Entry<String,Integer> ship : lord.getTemplate().shipPrefs.entrySet()) {
			ShipHullSpecAPI shipHull = settings.getHullSpec(Misc.getHullIdForVariantId(ship.getKey()));
			if (hullList.contains(shipHull) == false){
				hullList.add(shipHull);
				hullIdQtyMap.merge(shipHull,ship.getValue(),Integer::sum);
				totalChance += ship.getValue();
			}
		}
		hullList.sort(Comparator.comparing(ShipHullSpecAPI::getHullSize));
		Collections.reverse(hullList);

		UIPanelAPI firstItem = null;
		UIPanelAPI prevItem = null;
		UIPanelAPI newItem = null;
		for (ShipHullSpecAPI ship : hullList) {
			TooltipMakerAPI image = info.beginImageWithText(ship.getSpriteName(),40,40,false);
			if (firstItem == null) {
				firstItem = info.addImageWithText(pad);;
				prevItem = firstItem;
			}
			else
			{
				newItem = info.addImageWithText(pad);
				newItem.getPosition().rightOfTop(prevItem, pad);
				prevItem = newItem;;
			}
			String tooltipStr = (int) Math.ceil((hullIdQtyMap.get(ship) / totalChance) * 30f)  + " x " + ship.getNameWithDesignationWithDashClass();
			info.addTooltipToPrevious( new ToolTip(200, tooltipStr, Color.WHITE, CodexDataV2.getShipEntryId(ship.getHullId())), TooltipMakerAPI.TooltipLocation.LEFT);
		}

		info.beginImageWithText("",5,200,true);
		newItem = info.addImageWithText(pad);
		newItem.getPosition().belowLeft(firstItem, pad);
	}

	public static void printLordData(Lord lord) {
		log.info("[StarLords] Lord " + lord.getLordAPI().getNameString());
		log.info("[StarLords] Lord ID: " + lord.getLordAPI().getId());
		log.info("[StarLords] Lord getFleet Check: " + lord.getFleet().toString());
		log.info("[StarLords] Lord Fleet ID: " + lord.getFleet().getId());
		log.info("[StarLords] Lord Fleet Name Check: " + lord.getFleet().getName());
		log.info("[StarLords] Lord Fleet location: " + lord.getFleet().getLocation().x + " ; " + lord.getFleet().getLocation().y);
		log.info("[StarLords] Lord Fleet Commander : " + lord.getFleet().getCommander().getName());
		for (FleetMemberAPI ship : lord.getFleet().getMembersWithFightersCopy()) {
			if (!ship.isFighterWing()) {
				log.info("[StarLords] Lord " + lord.getLordAPI().getNameString() + " fleet existing ship: " + ship.getShipName());
			}
		}
		try {
			log.info("[StarLords] Lord getContainingLocation Check: " + lord.getFleet().getContainingLocation().toString());
		} catch (Exception e) {
			log.info("[StarLords] Lord Could not get location: " + e.getMessage());

		}
	}

	public static void printFleetData(CampaignFleetAPI fleet) {
		log.info("[StarLords] Fleet Name Check: " + fleet.getName());
		log.info("[StarLords] Fleet ID: " + fleet.getId());
		try {
			log.info("[StarLords] Fleet getContainingLocation Check: " + fleet.getContainingLocation().toString());
		} catch (Exception e) {
			log.info("[StarLords] Fleet getContainingLocation Failed: " + e.getMessage());
		}
		log.info("[StarLords] Fleet location: " + fleet.getLocation().x + " ; " + fleet.getLocation().y);
		log.info("[StarLords] Fleet Commander : " + fleet.getCommander().getName());
		for (FleetMemberAPI ship : fleet.getMembersWithFightersCopy()) {
			if (!ship.isFighterWing()) {
				log.info("[StarLords] " + fleet.getName() + " fleet existing ship: " + ship.getShipName());
			}
		}
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

	public static MarketAPI getFactionMarket(String marketName, String factionId)
	{
		for (MarketAPI market : getFactionMarkets(factionId))
			if (market.getName().equalsIgnoreCase(marketName))
			{
				return market;
			}
		return null;
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
        Collections.sort(lords, new Comparator<Lord>() {
            @Override
            public int compare(Lord o1, Lord o2) {
                if (o1.isPlayer()) return -1;
                if (o2.isPlayer()) return 1;
                if (o1.isMarshal()) return -1;
                if (o2.isMarshal()) return 1;
                if (o1.getRanking() != o2.getRanking()) return o2.getRanking() - o1.getRanking();
                return o1.getLordAPI().getNameString().compareTo(o2.getLordAPI().getNameString());
            }
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
        if (loc1 == null || loc2 == null) return false;
        if (loc1.isHyperspace() && loc2.isHyperspace()) {
            return Misc.getDistance(entity1.getLocationInHyperspace(), entity2.getLocationInHyperspace()) < thres;
        }
        if (!loc1.isHyperspace() && !loc2.isHyperspace()) {
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
            SectorEntityToken out = findNearestLocation(fleet);
            if (out != null && out.getName() != null){
                if (out.getContainingLocation() != null) return out.getName() + " in " + out.getContainingLocation().getName();
                if (fleet.getContainingLocation() != null) return out.getName() + " in " + fleet.getContainingLocation().getName();
                return out.getName() + "";
            }
            if (fleet.getContainingLocation() != null) return "in " + fleet.getContainingLocation().getName();
            return "in an unknown location";
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

    /*public static int getNumMajorEnemies(FactionAPI faction) {
        int numEnemies = 0;
        for (FactionAPI faction2 : LordController.getFactionsWithLords()) {
            if (!Misc.isPirateFaction(faction2) && faction.isHostileTo(faction2)) numEnemies += 1;
        }
        return numEnemies;
    }*/
    /*public static int getNumMajorEnemiesForAttacks(FactionAPI faction){
        int numEnemies = 0;
        for (FactionAPI faction2 : LordController.getFactionsWithLords()) {
            if (Utils.canBeAttacked(faction2) && faction.isHostileTo(faction2)) numEnemies += 1;
        }
        return numEnemies;
    }*/
    public static int getNumMajorEnemiesForDiplomacy(FactionAPI faction){
        if (!FactionTemplateController.getTemplate(faction).isCanPreformDiplomacy()) return 0;
        int numEnemies = 0;
        for (FactionAPI faction2 : LordController.getFactionsWithLords()) {
            if (!FactionTemplateController.getTemplate(faction2).isCanPreformDiplomacy()) continue;
            if (TargetUtils.isAtWar(faction,faction2)) numEnemies++;
            //if (Utils.canHaveRelations(faction2) && faction.isHostileTo(faction2)) numEnemies += 1;
        }
        return numEnemies;
    }
    public static PersonAPI getLeader(FactionAPI faction) {
        return FactionTemplateController.getTemplate(faction).getLeader();
        /*switch (faction.getId()) {
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
        return null;*/
    }

    public static String getLiegeName(FactionAPI faction) {
        PersonAPI person = FactionTemplateController.getTemplate(faction).getLeader();
        if (person == null) return null;
        return person.getNameString();
        /*switch (faction.getId()) {
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
        return null;*/
    }

    public static PersonAPI clonePerson(PersonAPI person) {
        PersonAPI clone = person.getFaction().createRandomPerson(person.getGender());
        clone.setPortraitSprite(person.getPortraitSprite());
        clone.setPersonality(person.getPersonalityAPI().getId());
        clone.setName(person.getName());
        clone.setVoice(person.getVoice());
        clone.setImportance(person.getImportance());
        clone.setMarket(person.getMarket());
        clone.setPostId(person.getPostId());
        clone.setRankId(person.getRankId());
        clone.getStats().setLevel(person.getStats().getLevel());
        return clone;
    }

    public static SectorEntityToken findNearestMarket(SectorEntityToken target){
        SectorEntityToken output = null;
        MarketAPI marketTemp = Misc.findNearestLocalMarket(target, 1e10f, null);
        if (marketTemp != null && marketTemp.getPrimaryEntity() != null) output = marketTemp.getPrimaryEntity();
        if (output != null) return output;
        List<MarketAPI> a = Global.getSector().getEconomy().getMarketsCopy();

        //give up on finding a market in system. just get the closest one.
        MarketAPI sameFaction = null;
        MarketAPI friendly = null;
        MarketAPI notHostile = null;
        MarketAPI any = null;
        float sameFactionD = 2147483647;
        float friendlyD = 2147483647;
        float notHostileD = 2147483647;
        float anyD = 2147483647;
        for (MarketAPI b : a){
            if (b.getPrimaryEntity().getFaction().equals(target.getFaction())){
                float x = target.getLocationInHyperspace().x - b.getPrimaryEntity().getLocationInHyperspace().x;
                float y = target.getLocationInHyperspace().y - b.getPrimaryEntity().getLocationInHyperspace().y;
                x = Math.max(-1*x,x);
                y = Math.max(-1*y,y);
                float d = x+y;
                if (d < sameFactionD){
                    sameFactionD = d;
                    sameFaction = b;
                }
            }
            if (b.getPrimaryEntity().getFaction().getRelationshipLevel(target.getFaction()).isAtWorst(RepLevel.FAVORABLE)){
                float x = target.getLocationInHyperspace().x - b.getPrimaryEntity().getLocationInHyperspace().x;
                float y = target.getLocationInHyperspace().y - b.getPrimaryEntity().getLocationInHyperspace().y;
                x = Math.max(-1*x,x);
                y = Math.max(-1*y,y);
                float d = x+y;
                if (d < friendlyD){
                    friendlyD = d;
                    friendly = b;
                }
            }
            if (b.getPrimaryEntity().getFaction().getRelationshipLevel(target.getFaction()).isAtWorst(RepLevel.NEUTRAL)){
                float x = target.getLocationInHyperspace().x - b.getPrimaryEntity().getLocationInHyperspace().x;
                float y = target.getLocationInHyperspace().y - b.getPrimaryEntity().getLocationInHyperspace().y;
                x = Math.max(-1*x,x);
                y = Math.max(-1*y,y);
                float d = x+y;
                if (d < notHostileD){
                    notHostileD = d;
                    notHostile = b;
                }
            }
            float x = target.getLocationInHyperspace().x - b.getPrimaryEntity().getLocationInHyperspace().x;
            float y = target.getLocationInHyperspace().y - b.getPrimaryEntity().getLocationInHyperspace().y;
            x = Math.max(-1*x,x);
            y = Math.max(-1*y,y);
            float d = x+y;
            if (d < anyD){
                anyD = d;
                any = b;
            }
        }
        if (sameFaction != null) return sameFaction.getPlanetEntity();
        if (friendly != null) return friendly.getPlanetEntity();
        if (notHostile != null) return notHostile.getPlanetEntity();
        if (any != null) return any.getPlanetEntity();

        //if none exist, give up. give me a planet at random. who even cares anymore????
        //also of note: this is a type of emergency backup to prevent crashes. it should never run for any reason ever. but it can, if it must.
        List<StarSystemAPI> d = Global.getSector().getStarSystems();
        for (StarSystemAPI b : d){
            if(b.getPlanets().size() != 0){
                output = b.getPlanets().get(0);
                break;
            }
        }
        if (output != null) return output;

        Logger log = Global.getLogger(Utils.class);
        log.info("there is only the void, empty and terrible. nowhere to respawn. nowhere to stand. it is all gone. what have you done? why?");
        log.info("was it worth it?");
        return null;
    }

    public static SectorEntityToken findNearestLocation(SectorEntityToken target){
        SectorEntityToken output = findAnyStaticEntity(target);
        if (output != null) return output;
        output = Misc.getNearestStarSystem(target).getCenter();
        if (output != null){
            output = findAnyStaticEntity(output);
            if (output != null) return output;
        }
        Logger log = Global.getLogger(Utils.class);
        log.info("ERROR: failed to acquire anything in a targeted system. and I am forced to ask: what on earth was going on? if you get this error, something went wrong in starlords. please report this to the modders responsible for that wonderful mod.");
        //try one final time to get any starsystem, please please, get something to go to
        StarSystemAPI starSystem = Misc.getNearbyStarSystem(target);
        output = findAnyStaticEntity(starSystem.getCenter());
        if (output != null) return output;
        //at this point, why even bother? like, error handling is going to be required. the system does not even exist! wtf..
        return null;
    }
    public static SectorEntityToken findAnyStaticEntity(SectorEntityToken target){
        SectorEntityToken output = null;
        if (target.getContainingLocation() != null) output = Misc.findNearestPlanetTo(target,false,true);
        if (output != null) return output;
        if (!target.isInHyperspace() && target.getStarSystem() != null){
            if (target.getStarSystem().getJumpPoints() != null && target.getStarSystem().getJumpPoints().size() != 0){
                return getClosest(target.getLocation(),target.getStarSystem().getJumpPoints());
            }
            if (target.getStarSystem().getStar() != null){
                return target.getStarSystem().getStar();
            }
            if (target.getStarSystem().getCenter() != null){
                return target.getStarSystem().getCenter();
            }
            if (target.getStarSystem().getAsteroids() != null && target.getStarSystem().getAsteroids().size() != 0){
                return getClosest(target.getLocation(),target.getStarSystem().getAsteroids());
            }
            if(target.getStarSystem().getAllEntities() != null && target.getStarSystem().getAllEntities().size() != 0){
                return getClosest(target.getLocation(),target.getStarSystem().getAllEntities());
            }
            return null;
            //at this point, I need to start to look into nearby starsystems.
        }
        return null;
    }
    private static SectorEntityToken getClosest(Vector2f a, List<SectorEntityToken> b){
        //i didn't test this. like, at all. I really really hope this does not have issues.
        SectorEntityToken output = null;
        float distance = Float.POSITIVE_INFINITY;
        for (SectorEntityToken c : b){
            float x = a.x - c.getLocation().x;
            float y = a.y - c.getLocation().y;
            x = Math.max(-1*x,x);
            y = Math.max(-1*y,y);
            float d = x+y;
            if (d < distance){
                distance = d;
                output = c;
            }
        }
        return output;
    }
    //determines if a given market can be attacked (some markets should be be attacked. like, hidden dustkeeper markets darn it.)
    @Setter
    private static HashSet<String> forcedAttack;
    @Setter
    private static HashSet<String> forcedNoAttack;
    /*public static boolean canBeAttacked(MarketAPI market) {
        if (!canBeAttacked(market.getFaction())) return false;
        if (!Global.getSettings().getModManager().isModEnabled("nexerelin")) return true;
        return NexerlinUtilitys.canBeAttacked(market);
    }
    //determines if a faction can be attacked at all. (pirates cant be raided for example. (or at least I think so))
    public static boolean canBeAttacked(FactionAPI faction){
        HashSet<String> forced = forcedAttack;
        HashSet<String> prevented = forcedNoAttack;
        if (forced.contains(faction.getId())) return true;
        if (prevented.contains(faction.getId())) return false;
        //if (isMinorFaction(faction)) return false;
        if (!Global.getSettings().getModManager().isModEnabled("nexerelin")) return !isMinorFaction(faction);
        return NexerlinUtilitys.canBeAttacked(faction);
    }*/
    //determines if a faction can have there relations change. (aka, pirates don't have relationship changes. nore do some modded content.)
    @Setter
    private static HashSet<String> forcedRelations;
    @Setter
    private static HashSet<String> forcedNoRelations;
    /*public static boolean canHaveRelations(FactionAPI faction){
        HashSet<String> forced = forcedRelations;
        HashSet<String> prevented = forcedNoRelations;
        if (forced.contains(faction.getId())) return true;
        if (prevented.contains(faction.getId())) return false;
        //if (isMinorFaction(faction)) return false;
        if (!Global.getSettings().getModManager().isModEnabled("nexerelin")) return !isMinorFaction(faction);//return true;
        return NexerlinUtilitys.canChangeRelations(faction);
    }/**/
    //prevents some factions from having war / peace declared, engaging/getting in invasions, and being raided (like pirates, for not pirates)

	@Setter
	private static HashSet<String> forcedMinorFaction;
	@Setter
	private static HashSet<String> forcedNotMinorFaction;
    /*
	public static boolean isMinorFaction(FactionAPI faction) {
		HashSet<String> forced = forcedMinorFaction;
		HashSet<String> prevented = forcedNotMinorFaction;
		if (forced.contains(faction.getId())) return true;
		if (prevented.contains(faction.getId())) return false;
		if (!Global.getSettings().getModManager().isModEnabled("nexerelin")) return Misc.isPirateFaction(faction);
		//to do: use nexerlin to determine if a faction is a minor faction.
		return NexerlinUtilitys.isMinorFaction(faction);
	}*/

	public static float getTravelTime(SectorEntityToken origin, SectorEntityToken target) {
		float distance = Misc.getDistanceLY(origin,target);
		return distance / 1.6f; // 1.6 is average travel time
	}

	public static String getLordTravelTimeString(Lord lord, SectorEntityToken target) {
		String output = "";
		if (lord.getFleet() == null)
			return  "Unknown";
		if (lord.getFleet().getContainingLocation() == null || target.getContainingLocation() == null)
            return "Unknown";
        if (lord.getFleet().getContainingLocation().equals(target.getContainingLocation()))
			return  "In System";

		output = String.valueOf(Math.round(Utils.getTravelTime(lord.getFleet(),target)));

		return output + " days";
	}

	public static String getMinArrivalTime(List<Lord> lords, SectorEntityToken target) {
		float min = 999f;
		float resultTravel;
		if (lords.isEmpty())
			return "No Participants";

		for (Lord lord : lords) {
			if (lord.getFleet() == null) {
				continue;
			}
			if (lord.getFleet().getContainingLocation() == null || target.getContainingLocation() == null)
                continue;
            if (lord.getFleet().getContainingLocation().equals(target.getContainingLocation()))
				return "In System";

            resultTravel = Math.round(Utils.getTravelTime(lord.getFleet(),target));
            if (resultTravel < min)
                min = resultTravel;

		}

        if (min == 999f)
            return "Unknown";
		return Math.round(min) + " days";
	}

	public static boolean printLordsWithPrisonerProblems(boolean fix, boolean print) {

		String output = "";
		boolean problems = true;

		for (Lord lord : LordController.getLordsList()) {
			ArrayList<String> prisonersToRemove = new ArrayList<>();
			for (String prisonerID : lord.getPrisoners()) {
			    try {
                    Lord prisoner = LordController.getLordById(prisonerID);
                    //
                    if (prisoner.equals(null)) {
                        output += "[Star Lords] " + lord.getLordAPI().getNameString() + "(" + lord.getLordAPI().getId() + ")"
                                + " action: " + lord.getCurrAction()
                                + " location: " + lord.getFleet().getContainingLocation()
                                + " has prisoner (" + prisonerID + ") but prisonerID does not exist"
                                + System.lineSeparator();
                        if (fix) {
                            prisonersToRemove.add(prisonerID);
                        }
                        continue;
                    }
                    if (prisoner.getCaptor() != null) {
                        if (!Objects.equals(lord.getLordAPI().getId(), prisoner.getCaptor())) {
                            Lord prisonerCaptor = LordController.getLordById(prisoner.getCaptor());
                            output += "[Star Lords] " + lord.getLordAPI().getNameString() + "(" + lord.getLordAPI().getId() + ")"
                                    + " action: " + lord.getCurrAction()
                                    + " location: " + lord.getFleet().getContainingLocation()
                                    + " has prisoner " + prisoner.getLordAPI().getNameString() + "(" + prisoner.getLordAPI().getId() + ")"
                                    + " action:" + prisoner.getCurrAction()
                                    + " location:" + prisoner.getFleet().getContainingLocation()
                                    + " but prisoner has captor " + prisonerCaptor.getLordAPI().getNameString() + "(" + prisonerCaptor.getLordAPI().getId() + ")"
                                    + " action: " + prisonerCaptor.getCurrAction()
                                    + " location: " + prisonerCaptor.getFleet().getContainingLocation()
                                    + System.lineSeparator();
                            if (fix) {
                                prisonersToRemove.add(prisonerID);
                            }
                        }
                    } else {
                        output += "[Star Lords] " + lord.getLordAPI().getNameString() + "(" + lord.getLordAPI().getId() + ")"
                                + " action: " + lord.getCurrAction()
                                + " location: " + lord.getFleet().getContainingLocation()
                                + " has prisoner " + prisoner.getLordAPI().getNameString() + "(" + prisoner.getLordAPI().getId() + ")"
                                + " action:" + prisoner.getCurrAction()
                                + " location:" + prisoner.getFleet().getContainingLocation()
                                + " but prisoner has no captor "
                                + System.lineSeparator();
                        if (fix) {
                            prisonersToRemove.add(prisonerID);
                        }
                    }
                }catch(Exception e){
				    log.info("failed to get process log. error of: "+e);
                }
			}
			if (fix) {
				for (String prisonerRemove : prisonersToRemove) {
                    lord.removePrisoner(prisonerRemove);
                    LordController.getLordById(prisonerRemove).setCaptor(null);
                }
			}
		}
		if (output.isEmpty()) {
			output = "[Star Lords] No Prisoner Problems";
			problems = false;
		}
		if (print)
			log.info(output);
		return problems;
	}

	public static String printLordsWithPrisoners(ArrayList<Lord> lordsList) {
		String output = "";
		for (Lord lord : lordsList) {
			for (String prisonerID : lord.getPrisoners()) {
			    try {
                    output += "[Star Lords] " + lord.getLordAPI().getNameString() + "(" + lord.getLordAPI().getId() + ")";
                    Lord prisoner = LordController.getLordById(prisonerID);
                    if (prisoner != null)
                        output += " has prisoner " + prisoner.getLordAPI().getNameString() + "(" + prisoner.getLordAPI().getId() + ")"
                                + " captor " + prisoner.getCaptor()
                                + System.lineSeparator();
                    else
                        output += " has prisoner " + "(" + prisonerID + ") but id doesn't exist"
                                + System.lineSeparator();
                }catch (Exception e){
			        output += "\n ERROR. failed to get a prisoner. error of: "+e.getMessage();
                }
			}
		}
		return output;
	}

	public static void printLordsDetails() {
		List<String[]> list = new ArrayList<String[]>();

		String id = "";
		String name = "";
		String personality = "";
		String faction = "";
		String relWithFaction = "";
		String relWithMarshall = "";
		String currAction = "";
		String location = "";
		String fiefs = "";
		String chance = "";
        String fleetDoNotAdvance = "";

		for (Lord lord : LordController.getLordsList()) {
			Lord marshall = PoliticsController.getLordMarshall(lord);

			id = lord.getLordAPI().getId();
			name = lord.getLordAPI().getNameString();
			personality = lord.getPersonality().toString();
			faction = lord.getFaction().getDisplayName();
			relWithFaction = String.valueOf(RelationController.getLoyalty(lord));
			if (marshall != null && !lord.equals(marshall))
				relWithMarshall = String.valueOf(RelationController.getRelation(lord, marshall));
			else
				relWithMarshall = "N/A";
			currAction = String.valueOf(lord.getCurrAction());
			if(lord.getFleet() != null)
				location = String.valueOf(lord.getFleet().getContainingLocation());
			else
				location = String.valueOf(lord.getFleet());
			fiefs = String.valueOf(lord.getFiefs().size());
			chance = String.valueOf(DefectionUtils.getAutoBetrayalChance(lord));
            if (lord.getFleet() != null)
                fleetDoNotAdvance = lord.getFleet().isDoNotAdvanceAI().toString();
            else
                fleetDoNotAdvance = "null";

			list.add(new String[]{id, name, personality, faction, relWithFaction, relWithMarshall, currAction, location, fiefs, chance, fleetDoNotAdvance});

		}

		String output = "[Star Lords] Lords, Relations and Defection: " + System.lineSeparator();
		output += "[Star Lords]"
				+ "\tID"
				+ "\tName"
				+ "\tPersonality"
				+ "\tFaction"
				+ "\tRel Faction"
				+ "\tRel Marshall"
				+ "\tAction"
				+ "\tLocation"
				+ "\tFiefs"
				+ "\tChance"
				+ "\tFleet Do Not Advance"
				+ System.lineSeparator();
		for (String[] item : list) {
			output += "[Star Lords]"
					+ "\t" + item[0]
					+ "\t" + item[1]
					+ "\t" + item[2]
					+ "\t" + item[3]
					+ "\t" + item[4]
					+ "\t" + item[5]
					+ "\t" + item[6]
					+ "\t" + item[7]
					+ "\t" + item[8]
					+ "\t" + item[9]
					+ "\t" + item[10]
					+ System.lineSeparator();
		}

		log.info(output);

	}

	public static void printRequests() {
		String output = "";
		Lord lord = null;
		Lord captor = null;

		for (LordRequest request : RequestController.getRequestList()) {
			lord = request.getOriginator();
			captor = LordController.getLordById(request.getOriginator().getCaptor());
			output += "[Star Lords] Lord: " + lord.getLordAPI().getNameString() + System.lineSeparator()
					+ "[Star Lords] Lord ID: " + lord.getLordAPI().getId() + System.lineSeparator()
					+ "[Star Lords] Request: " + request + System.lineSeparator()
					+ "[Star Lords] Request type: " + request.getRequestCapitalized() + System.lineSeparator()
					+ "[Star Lords] Request player agreed: " + request.hasPlayerAgreed() + System.lineSeparator();
			if (captor != null) {
				output += "[Star Lords] Captor ID: " + captor.getLordAPI().getId() + System.lineSeparator()
						+ "[Star Lords] Captor Name: " + captor.getLordAPI().getNameString() + System.lineSeparator();
			}
		}
		if (output.isEmpty())
			output = "[Star Lords] No Requests";
		log.info(output);
	}

    public static void printLordsAlignments() {
        if (nexEnabled() == false) {
            log.info("Nexerelin Mod not enabled. Nothing to print.");
            return;
        }

        List<List<String>> list = new ArrayList<>();

        String id = "";
        String name = "";
        String faction = "";

        List<Alliance.Alignment> keySet = new ArrayList<>(NexerlinUtilitys.getFactionAlignments(LordController.getPlayerLord().getFaction().getId()).keySet());
        keySet.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Map<Alliance.Alignment, Float> rowAlignments = new LinkedHashMap<>();
        for (Alliance.Alignment key : keySet)
            rowAlignments.put(key,-2f);

        for (Lord lord : LordController.getLordsList()) {

            for (Map.Entry<Alliance.Alignment, Float> alignment : rowAlignments.entrySet()) {
                alignment.setValue(-2f);
            }


            id = lord.getLordAPI().getId();
            name = lord.getLordAPI().getNameString();
            faction = lord.getFaction().getDisplayName();
            List<String> row = new ArrayList<>();
            Map<Alliance.Alignment, Float> lordAlignments = lord.getAlignments();

            for (Map.Entry<Alliance.Alignment, Float> alignment : lordAlignments.entrySet()) {
                rowAlignments.merge(alignment.getKey(),alignment.getValue(), (defaultValue, lordValue) -> lordValue);
            }

            row.add(id);
            row.add(name);
            row.add(faction);
            Map<Alliance.Alignment, Float> factionAlignments = NexerlinUtilitys.getFactionAlignments(lord.getFaction().getId());
            for (Map.Entry<Alliance.Alignment, Float> alignment : rowAlignments.entrySet()) {
                row.add(String.valueOf(alignment.getValue()));
                row.add(String.valueOf(factionAlignments.get(alignment.getKey())));

            }

            list.add(row);

        }

        //Header
        String output = "[Star Lords] Lords Alignments: " + System.lineSeparator();
        output += "[Star Lords]"
                + "\tID"
                + "\tName"
                + "\tFaction";

        for (Map.Entry<Alliance.Alignment, Float> alignment : rowAlignments.entrySet()) {
            output += " \tLord " + alignment.getKey().getName();
            output += " \tFaction " + alignment.getKey().getName();
        }
        output += System.lineSeparator();

        for (List<String> row : list) {
            output += "[Star Lords]";
            for (String item : row) {
                output += "\t" + item;
            }
            output += System.lineSeparator();
        }

        log.info(output);

    }

    public static int getRandomChance(Lord lord, int bound) {
        Random rand = new Random(lord.getLordAPI().getId().hashCode() * Global.getSector().getClock().getTimestamp());
        return rand.nextInt(bound);
    }

    public static int getRandomChance(int bound) {
        Random rand = new Random();
        return rand.nextInt(bound);
    }
}
