package starlords.controllers;

import starlords.ai.LordStrategicModule;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.person.LordTemplate;
import starlords.ui.LordsIntelPlugin;
import starlords.util.Constants;
import starlords.util.LordFleetFactory;

import java.util.*;

import static starlords.util.Constants.DEBUG_MODE;

public class LordController {

    @Getter
    private static List<Lord> lordsList = new ArrayList<>();
    // maps person id to lord object
    private static HashMap<String, Lord> lordsMap = new HashMap<>();

    // maps person id to index in lord list
    private static HashMap<String, Integer> lordIdxMap = new HashMap<>();

    // set of all factions with at least 1 lord, cached for some diplo things
    @Getter
    private static HashSet<FactionAPI> factionsWithLords = new HashSet<>();

    // lord wrapper for player, used in starting feasts/campaigns and politics
    @Getter
    private static Lord playerLord;

    // maps lord name to corresponding template
    public static HashMap<String, LordTemplate> lordTemplates;

    public static Logger log = Global.getLogger(LordController.class);

    public static Lord getLordById(String id) {
        return lordsMap.get(id);
    }


    public static Lord getLordOrPlayerById(String id) {
        if (playerLord.getLordAPI().getId().equals(id)) return playerLord;
        return lordsMap.get(id);
    }

    private static void addLord(Lord lord) {
        lordsList.add(lord);
        lordsMap.put(lord.getLordAPI().getId(), lord);
        factionsWithLords.add(lord.getFaction());
    }

    public static int indexOf(Lord lord) {
        if (!lordIdxMap.containsKey(lord.getLordAPI().getId())) return -1;
        return lordIdxMap.get(lord.getLordAPI().getId());
    }

    // ensures lords are in a consistent order for save/reload
    private static void ensureLordOrder() {
        Collections.sort(lordsList, new Comparator<Lord>() {
            @Override
            public int compare(Lord o1, Lord o2) {
                return o1.getLordAPI().getId().compareTo(o2.getLordAPI().getId());
            }
        });
        lordIdxMap.clear();
        for (int i = 0; i < lordsList.size(); i++) {
            lordIdxMap.put(lordsList.get(i).getLordAPI().getId(), i);
        }
    }

    // Loads existing lords on save load.
    public static void loadLords() {
        lordsList.clear();
        lordsMap.clear();

        // update lords in intel plugin
        List<IntelInfoPlugin> lordIntel = Global.getSector().getIntelManager().getIntel();
        for (IntelInfoPlugin plugin : lordIntel) {
            if (plugin instanceof LordsIntelPlugin) {
                Lord newLord = ((LordsIntelPlugin) plugin).getLord();
                if (lordTemplates.containsKey(newLord.getTemplate().name)) {
                    newLord.setTemplate(lordTemplates.get(newLord.getTemplate().name));
                }
                addLord(newLord);
            }
        }
        ensureLordOrder();
        playerLord = Lord.createPlayer();
    }

    // Creates lords from templates in new game
    public static void createAllLords() {
        // Generate lords and lord fleets
        log.info("Generating lords");
        lordsList.clear();
        lordsMap.clear();

        HashSet<String> allocatedFiefs = new HashSet<>();
        for (LordTemplate template : lordTemplates.values()) {
            if (template.fief != null) allocatedFiefs.add(template.fief);
        }

        for (LordTemplate template : lordTemplates.values()) {
            Lord currLord = new Lord(template);
            MarketAPI lordMarket = null;
            if (currLord.getFiefs().isEmpty()) {
                if (template.fief != null) {
                    // world map was changed by some other mod, give this lord a random fief
                    lordMarket = getDefaultSpawnLoc(currLord, allocatedFiefs);
                    if (lordMarket != null) {
                        log.info("Dynamic allocating fief " + lordMarket.getId() + " to " + currLord.getLordAPI().getNameString());
                        allocatedFiefs.add(lordMarket.getId());
                        currLord.addFief(lordMarket);
                    }
                }
                if (lordMarket == null) {
                    lordMarket = getDefaultSpawnLoc(currLord, null);
                }
            } else {
                lordMarket = currLord.getFiefs().get(0).getMarket();
            }
            // TODO DEBUG
            if (DEBUG_MODE) {
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(5000000);
                if (currLord.getLordAPI().getFaction().getId().equals(Factions.HEGEMONY))
                    lordMarket = Global.getSector().getEconomy().getMarket("jangala");
                if (new Random().nextInt(2) == 0) {
                    currLord.getLordAPI().getRelToPlayer().setRel(2 * new Random().nextFloat() - 1);
                } else {
                    currLord.getLordAPI().getRelToPlayer().setRel(1);
                }
                currLord.getLordAPI().getRelToPlayer().setRel(0.95f);
//                if (currLord.getLordAPI().getFaction().getId().equals(Factions.PIRATES)) {
//                    lordMarket = Global.getSector().getEconomy().getMarket("jangala");
//                    currLord.getLordAPI().getRelToPlayer().setRel(0.5f);
//                }
//                if (currLord.getTemplate().name.contains("Brynhild")) {
//                    lordMarket = Global.getSector().getEconomy().getMarket("jangala");
//                    currLord.getLordAPI().getRelToPlayer().setRel(1);
//                }
                currLord.setKnownToPlayer(true);
                currLord.setPersonalityKnown(true);
            }

            log.info("Spawning lord in " + lordMarket.getId());

            FleetParamsV3 params = new FleetParamsV3();
            params.setSource(lordMarket, false);

            CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(
                    template.factionId, FleetTypes.TASK_FORCE, lordMarket);
            fleet.setName(template.fleetName);
            fleet.setNoFactionInName(true);
            fleet.setAIMode(true);
            fleet.setCommander(currLord.getLordAPI());
            fleet.setNoAutoDespawn(true);
            currLord.getLordAPI().setFleet(fleet);

            LordFleetFactory.addToLordFleet(new ShipRolePick(template.flagShip), fleet, new Random());
            fleet.getFleetData().getMembersInPriorityOrder().get(0).setFlagship(true);
            LordFleetFactory.addToLordFleet(template.shipPrefs, fleet, new Random(), 150, 1e8f);
            LordFleetFactory.populateCaptains(currLord);

            lordMarket.getPrimaryEntity().getContainingLocation().addEntity(fleet);
            fleet.setLocation(lordMarket.getPrimaryEntity().getLocation().x,
                    lordMarket.getPrimaryEntity().getLocation().y);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_WAR_FLEET, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.DO_NOT_TRY_TO_AVOID_NEARBY_FLEETS, true);
            fleet.addAbility(Abilities.TRANSVERSE_JUMP);
            //fleet.setAI(new LordCampaignFleetAI(currLord, fleet.getAI()));
            ModularFleetAIAPI baseAI = (ModularFleetAIAPI) fleet.getAI();
            baseAI.setStrategicModule(new LordStrategicModule(currLord, baseAI.getStrategicModule()));
            LordController.addLord(currLord);
            LordsIntelPlugin.createProfile(currLord);
            if (DEBUG_MODE) {
                fleet.getCargo().addFuel(fleet.getCargo().getFreeFuelSpace());
                fleet.getCargo().addMarines(fleet.getCargo().getFreeCrewSpace());
                fleet.getCargo().addCommodity(Commodities.HAND_WEAPONS, 200);
            }
        }
        log.info("DEBUG: Generated " + lordsList.size() + " lords");
        ensureLordOrder();
        playerLord = Lord.createPlayer();
    }

    // parses lord templates from lords.json
    public static void parseLordTemplates() {
        if (lordTemplates != null) return;
        lordTemplates = new HashMap<>();
        JSONObject templates = null;
        try {
            templates = Global.getSettings().getMergedJSONForMod("data/lords/lords.json", Constants.MOD_ID);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        for (Iterator it = templates.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            lordTemplates.put(key, new LordTemplate(key, templates.getJSONObject(key)));
        }
    }

    public static void updateFactionsWithLords() {
        factionsWithLords.clear();
        for (Lord lord : lordsList) {
            factionsWithLords.add(lord.getFaction());
        }
    }

    private static MarketAPI getDefaultSpawnLoc(Lord lord, HashSet<String> forbidden) {
        String id = null;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (forbidden != null && forbidden.contains(market.getId())) continue;
            if (market.getFaction().getId().equals(lord.getFaction().getId())) {
                if (id == null) {
                    id = market.getId();
                } else if (market.getId().hashCode() % 8 == lord.getLordAPI().getId().hashCode() % 8) {
                    id = market.getId();
                }
            }
        }
        return Global.getSector().getEconomy().getMarket(id);
    }
}
