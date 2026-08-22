package starlords.generator;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketDemandAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import starlords.controllers.LordController;
import starlords.generator.support.AvailableShipData;
import starlords.generator.support.ShipData;
import starlords.generator.types.flagship.LordFlagshipPickerBase;
import starlords.generator.types.fleet.LordFleetGeneratorBase;
import starlords.listeners.LordGeneratorListener_base;
import starlords.person.Lord;
import starlords.person.LordTemplate;
import starlords.person.PosdoLordTemplate;
import starlords.util.Utils;
import starlords.util.WeightedRandom;

import java.util.*;

public class LordGenerator {
    @Setter
    private static WeightedRandom[] sizeRatio = new WeightedRandom[4];
    @Setter
    private static WeightedRandom[] typeRatio = new WeightedRandom[3];
    @Setter
    private static WeightedRandom starlordLevelRatio;
    @Setter
    private static String fleetAdjective;//NOTE: this can go into the 'strings' file? or should it. arg?!??
    @Setter
    private static double isMaleChance;
    @Setter
    private static double[] personalityRatio = {};
    private static final String[] personalities = {
            "Quarrelsome",
            "Calculating",
            "Martial",
            "Upstanding"
    };
    @Setter
    private static double[] battlePersonalityRatio = {};
    private static final String[] battlePersonalities = {
            "Timid",
            "Cautious",
            "Steady",
            "Aggressive",
            "Reckless"
    };
    @Setter
    private static WeightedRandom maxShipRatio;
    @Setter
    private static WeightedRandom minShipRatio;
    @Setter
    private static WeightedRandom shipSpawnRatio;
    private static ArrayList<LordFleetGeneratorBase> fleetGeneratorTypes = new ArrayList<>();
    private static ArrayList<Double> fleetGeneratorRatio = new ArrayList<>();
    @Setter
    @Getter
    private static LordFleetGeneratorBase fleetGeneratorBackup = new LordFleetGeneratorBase("base");

    private static ArrayList<LordFlagshipPickerBase> flagshipPickerTypes = new ArrayList<>();
    private static ArrayList<Double> flagshipPickerRatio = new ArrayList<>();
    @Setter
    @Getter
    private static LordFlagshipPickerBase flagshipGeneratorBackup;

    @Setter
    private static double oddsOfNonePriorityShips;
    @Setter
    private static double oddsOfNoneSelectedFlagship;

    private static final Logger log = Global.getLogger(LordGenerator.class);
    @Getter
    private static Random random = new Random();

    public static void tempTest(){
        Logger log = Global.getLogger(LordGenerator.class);;
        //log.info("DEBUG: got random of: "+maxShipRatio.getRandom());
        //log.info("DEBUG: got random of: "+minShipRatio.getRandom());
        log.info("DEBUG: got random of: "+starlordLevelRatio.getRandom());
        log.info("DEBUG: got random of: "+sizeRatio[0].getRandom());
        log.info("DEBUG: got random of: "+sizeRatio[1].getRandom());
        log.info("DEBUG: got random of: "+sizeRatio[2].getRandom());
        log.info("DEBUG: got random of: "+sizeRatio[3].getRandom());
    }
    public static void createStarlord(String factionID){
        PosdoLordTemplate lord = generateStarlord(factionID);
        lord.preferredItemId = getFavCommodity(factionID);
        LordGeneratorListener_base.runEditLord(lord,null,null,0,0);
        logLord(lord);

        LordTemplate template = new LordTemplate(lord);
        Lord currLord = new Lord(template);
        LordGeneratorListener_base.runEditLordPersons(currLord.getLordAPI(),null,null,0,0);

        LordController.addLordMidGame(template,currLord);
    }
    public static void createStarlord(String factionID, com.fs.starfarer.api.campaign.SectorEntityToken system, float x, float y){
        PosdoLordTemplate lord = generateStarlord(factionID);
        lord.preferredItemId = getFavCommodity(factionID);
        LordGeneratorListener_base.runEditLord(lord,null,system,x,y);
        logLord(lord);

        LordTemplate template = new LordTemplate(lord);
        Lord currLord = new Lord(template);
        LordGeneratorListener_base.runEditLordPersons(currLord.getLordAPI(),null,system,x,y);

        LordController.addLordMidGame(template,currLord,system,x,y);
    }
    public static void createStarlord(String factionID,MarketAPI market){
        PosdoLordTemplate lord = generateStarlord(factionID);
        lord.preferredItemId = getFavCommodity(market);
        LordGeneratorListener_base.runEditLord(lord,market,null,0,0);
        logLord(lord);

        LordTemplate template = new LordTemplate(lord);
        Lord currLord = new Lord(template);
        LordGeneratorListener_base.runEditLordPersons(currLord.getLordAPI(),market,null,0,0);

        LordController.addLordMidGame(template,market,currLord);
    }
    @SneakyThrows
    public static PosdoLordTemplate generateStarlord(String factionID){
        log.info("DEBUG: generating a new starlord...");
        PosdoLordTemplate lord = new PosdoLordTemplate();
        lord.factionId=factionID;
        lord.level=starlordLevelRatio.getRandom();
        lord.personality = personalities[getValueFromWeight(personalityRatio)];
        lord.battlePersonality = battlePersonalities[getValueFromWeight(battlePersonalityRatio)];
        lord.ranking=0;
        lord.fief = "null";
        generateAllShipsForLord(lord,factionID);
        generatePerson(lord,factionID);
        return lord;
    }
    private static void logLord(PosdoLordTemplate lord){
        log.info("  stats gotten as:");
        log.info("      faction: "+lord.factionId);
        log.info("      level: "+lord.level);
        log.info("      personality: "+lord.personality);
        log.info("      battlePersonality: "+lord.battlePersonality);
        log.info("      ranking: "+lord.ranking);
        log.info("      fief: "+lord.fief);
        log.info("      fav commodity: "+lord.preferredItemId);
        log.info("      name: "+lord.name);
        log.info("      isMale: "+lord.isMale);
        log.info("      fleetname: "+lord.fleetName);
        log.info("      flagship: "+lord.flagShip);
        log.info("      fleet status:");
        int friget=0;
        int destroyer=0;
        int cruiser=0;
        int capital3=0;

        int warship=0;
        int phaseship=0;
        int carrier=0;
        for(Object a : lord.shipPrefs.keySet().toArray()){
            log.info("          "+((String) a) + "at weight of : "+lord.shipPrefs.get((String) a));
            int weight = lord.shipPrefs.get((String) a);
            ShipData b = AvailableShipData.getDefaultShips().getUnorganizedShips().get(Global.getSettings().getVariant((String)a).getHullSpec().getHullId());//Global.getSettings().getVariant((String)a).getHullSpec().getHullId()
            switch (b.getHullType()){
                case AvailableShipData.HULLTYPE_WARSHIP:
                    warship+=weight;
                    break;
                case AvailableShipData.HULLTYPE_PHASE:
                    phaseship+=weight;
                    break;
                case AvailableShipData.HULLTYPE_CARRIER:
                    carrier+=weight;
                    break;
                default:
                    break;
            }
            //cant switch this because I suck
            if (b.getHullSize().equals(AvailableShipData.HULLSIZE_FRIGATE)) friget+=weight;
            if (b.getHullSize().equals(AvailableShipData.HULLSIZE_DESTROYER)) destroyer+=weight;
            if (b.getHullSize().equals(AvailableShipData.HULLSIZE_CRUISER)) cruiser+=weight;
            if (b.getHullSize().equals(AvailableShipData.HULLSIZE_CAPITALSHIP)) capital3+=weight;
        }
        log.info("      fleet size composition: "+AvailableShipData.HULLSIZE_FRIGATE+": "+friget+", "+AvailableShipData.HULLSIZE_DESTROYER+": "+destroyer+", "+AvailableShipData.HULLSIZE_CRUISER+": "+cruiser+", "+AvailableShipData.HULLSIZE_CAPITALSHIP+": "+capital3);
        log.info("      fleet type composition: "+AvailableShipData.HULLTYPE_WARSHIP+": "+warship+", "+AvailableShipData.HULLTYPE_CARRIER+": "+carrier+", "+AvailableShipData.HULLTYPE_PHASE+": "+phaseship);

        /*/
        log.info("      getting temp possable ships of 'carrier large'");
        Set<String> a = Global.getSector().getFaction(lord.factionId).getVariantsForRole(ShipRoles.CARRIER_LARGE);
        for (String b : a){
            log.info("          ship vareant ID, hull ID: "+b+", "+Global.getSettings().getVariant(b).getHullSpec().getHullId());
        }
        log.info("      getting temp possable ships of 'carrier small'");
        a = Global.getSector().getFaction(lord.factionId).getVariantsForRole(ShipRoles.CARRIER_SMALL);
        for (String b : a){
            log.info("          ship vareant ID, hull ID: "+b+", "+Global.getSettings().getVariant(b).getHullSpec().getHullId());
        }

        log.info("      getting temp possible ships of 'warship capital'");
        a = Global.getSector().getFaction(lord.factionId).getVariantsForRole(ShipRoles.COMBAT_CAPITAL);
        for (String b : a){
            log.info("          ship vareant ID, hull ID: "+b+", "+Global.getSettings().getVariant(b).getHullSpec().getHullId());
        }
        log.info("      getting temp possible ships of 'warship small'");
        a = Global.getSector().getFaction(lord.factionId).getVariantsForRole(ShipRoles.COMBAT_SMALL);
        for (String b : a){
            log.info("          ship vareant ID, hull ID: "+b+", "+Global.getSettings().getVariant(b).getHullSpec().getHullId());
        }/**/
    }
    private static String getFavCommodity(String factionID){
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        ArrayList<MarketAPI> factionMarkets = new ArrayList<>();
        for (MarketAPI a : markets){
            if (a.getFactionId().equals(factionID)) factionMarkets.add(a);
        }
        if (factionMarkets.size() != 0){
            return getFavCommodity(factionMarkets.get((int) (random.nextInt(factionMarkets.size()))));
        }
        if (markets.size() != 0){
            return getFavCommodity(markets.get((int) (random.nextInt(markets.size()))));
        }
        return null;
    }
    private static String getFavCommodity(MarketAPI market){
        List<Industry> a = market.getIndustries();
        ArrayList<String> items = new ArrayList<>();
        for (Industry b : a){
            for (MutableCommodityQuantity c : b.getAllDemand()) {
                if (c.getQuantity().getModifiedValue() >= 1 && !items.contains(c.getCommodityId()) && !c.getCommodityId().equals("ships")) {
                    items.add(c.getCommodityId());
                }
            }
            for (MutableCommodityQuantity c : b.getAllSupply()) {
                if (c.getQuantity().getModifiedValue() >= 1 && !items.contains(c.getCommodityId()) && !c.getCommodityId().equals("ships")) {
                    items.add(c.getCommodityId());
                }
            }
        }
        if (items.size() == 0) return null;
        return items.get((int) (random.nextInt(items.size())));
    }

    private static void generateAllShipsForLord(PosdoLordTemplate lord, String factionID){
        int[] sizeratio = {
                sizeRatio[0].getRandom(),
                sizeRatio[1].getRandom(),
                sizeRatio[2].getRandom(),
                sizeRatio[3].getRandom()
        };
        if (sizeratio[0] == 0 && sizeratio[1] == 0 && sizeratio[2] == 0 && sizeratio[3] == 0){
            sizeratio[(int)(random.nextInt(4))] = 1;
        }
        int[] typeratio = {
                typeRatio[0].getRandom(),
                typeRatio[1].getRandom(),
                typeRatio[2].getRandom()
                /*typeRatio[3].getRandom(),
                typeRatio[4].getRandom(),
                typeRatio[5].getRandom(),
                typeRatio[6].getRandom(),
                typeRatio[7].getRandom(),
                typeRatio[8].getRandom(),
                typeRatio[9].getRandom()*/
        };
        //log.info("  sizeRatio: "+sizeratio[0]+", "+sizeratio[1]+", "+sizeratio[2]+", "+sizeratio[3]);
        //log.info("  typeRatio: "+typeratio[0]+", "+typeratio[1]+", "+typeratio[2]);
        if (typeratio[0] == 0 && typeratio[1] == 0 && typeratio[2] == 0){
            typeratio[(int)(random.nextInt(3))] = 1;
        }
        int maxShip = maxShipRatio.getRandom();
        int minShip = minShipRatio.getRandom();
        maxShip = Math.max(maxShip,minShip);
        boolean useAllShips = oddsOfNonePriorityShips > random.nextDouble();
        //lord.shipPrefs=generateShips(factionID,useAllShips,minShip,maxShip,sizeratio,typeratio);
        generateShips(lord,factionID,useAllShips,minShip,maxShip,sizeratio,typeratio);
    }
    private static ArrayList<ShipData> getShips(AvailableShipData ships,int[] sizeratio,int[] typeratio,int targetShips){
        ArrayList<ShipData> output = new ArrayList<>();
        String[] sizes = {
                AvailableShipData.HULLSIZE_FRIGATE,
                AvailableShipData.HULLSIZE_DESTROYER,
                AvailableShipData.HULLSIZE_CRUISER,
                AvailableShipData.HULLSIZE_CAPITALSHIP
        };
        String[] types = {
                AvailableShipData.HULLTYPE_WARSHIP,
                AvailableShipData.HULLTYPE_CARRIER,
                AvailableShipData.HULLTYPE_PHASE
                /*AvailableShipData.HULLTYPE_CARGO,
                AvailableShipData.HULLTYPE_COMBATCIV,
                AvailableShipData.HULLTYPE_LINER,
                AvailableShipData.HULLTYPE_PERSONNEL,
                AvailableShipData.HULLTYPE_TANKER,
                AvailableShipData.HULLTYPE_TUG,
                AvailableShipData.HULLTYPE_UTILITY,*/
        };
        int[] tempSize = new int[sizeratio.length];
        int[] tempType = new int[typeratio.length];
        int maxLoops = targetShips * 5;
        while(ships.getUnorganizedShips().size() > 0 && output.size() < targetShips && maxLoops > 0){
            int s = getValueFromWeight(sizeratio);//getLowestNumberID(sizeratio,tempSize);
            s = Math.max(s, 0);
            int t = getValueFromWeight(typeratio);//getLowestNumberID(typeratio,tempType);
            t = Math.max(t, 0);
            ShipData ship = ships.getRandomShip(types[t],sizes[s]);
            if (ship == null) {
                maxLoops--;
                continue;
            }
            output.add(ship);
            ships.removeShip(ship.getHullID());
            maxLoops--;
        }
        //a backup, do to the possibility of 'missing' elegable ships in the past while loop (if both counters end up in sync). this does provide an incorrect ratio though.
        while (ships.getOrganizedShips().size() > 0 && output.size() < targetShips){
            boolean emergencyBreak = true;
            for (int a = 0; a < typeratio.length; a++){
                if (typeratio[a] == 0){
                    continue;
                }
                for (int b = 0; b < sizeratio.length; b++){
                    if (sizeratio[b] == 0){
                        continue;
                    }
                    ShipData ship = ships.getRandomShip(types[a],sizes[b]);
                    if (ship == null) {
                        continue;
                    }
                    output.add(ship);
                    ships.removeShip(ship.getHullID());
                    if (output.size() >= targetShips) break;
                    emergencyBreak = false;
                }
            }
            if (emergencyBreak) break;
        }
        return output;
    }
    private static int getIdTypes(String type){
        String[] a = new String[]{
            AvailableShipData.HULLTYPE_WARSHIP,
            AvailableShipData.HULLTYPE_CARRIER,
            AvailableShipData.HULLTYPE_PHASE
        };
        for (int b = 0; b < a.length; b++){
            if (a[b].equals(type)) return b;
        }
        return -1;
    }
    private static int getIdSizes(String size){
        String[] a = new String[]{
            AvailableShipData.HULLSIZE_FRIGATE,
            AvailableShipData.HULLSIZE_DESTROYER,
            AvailableShipData.HULLSIZE_CRUISER,
            AvailableShipData.HULLSIZE_CAPITALSHIP
        };
        for (int b = 0; b < a.length; b++){
            if (a[b].equals(size)) return b;
        }
        return -1;
    }
    private static HashMap<String, Integer> assingFleetSpawnWeights(ArrayList<ShipData> shipDatas, int[] sizes, int[] types){
        //int[] output = new int[shipDatas.size()];
        HashMap<String,Integer> output = new HashMap<>();
        ArrayList<Double> shipOdds = new ArrayList<>();
        for(ShipData a : shipDatas) {
            for (Object b : a.getSpawnWeight().values().toArray()) {
                //String vareantID = (String) a.getSpawnWeight().keySet().toArray()[b];
                float weight = (float) b;//get(vareantID);
                shipOdds.add((double) (weight*shipSpawnRatio.getRandom()));
            }
        }
        double[] outputtemp = new double[shipOdds.size()];
        double[] outputtemp2 = new double[shipOdds.size()];
        double[] totalS = new double[4];
        double[] totalT = new double[3];
        int allS=0;
        int allT=0;
        int d = 0;
        for(ShipData a : shipDatas){
            for(int b = 0; b < a.getSpawnWeight().size(); b++) {
                //String vareantID = (String) a.getSpawnWeight().keySet().toArray()[b];
                double weight = shipOdds.get(d);//(float)a.getSpawnWeight().values().toArray()[b];//get(vareantID);
                totalS[(int) getIdSizes(a.getHullSize())] += weight;
                totalT[(int) getIdTypes(a.getHullType())] += weight;
                allS++;
                allT++;
                d++;
            }
        }
        double typeSize = types[0]+types[1]+types[2];
        double sizeSize = sizes[0]+sizes[1]+sizes[2]+sizes[3];
        double[] sizesMulti = new double[sizes.length];
        for(int a = 0; a < sizes.length; a++){
            double b = totalS[a] / allS;
            double c = sizes[a] / sizeSize;
            sizesMulti[a] = c/b;
        }
        d = 0;
        for(ShipData a : shipDatas){
            for(int b = 0; b < a.getSpawnWeight().size(); b++) {
                double weight = shipOdds.get(d);//(float)a.getSpawnWeight().values().toArray()[b];//get(vareantID);
                double t = (weight * sizesMulti[getIdSizes(a.getHullSize())]) * 10;
                outputtemp[d] = t;
                d++;
            }
        }

        double[] typesMulti = new double[types.length];
        for(int a = 0; a < types.length; a++){
            double b = totalT[a] / allT;
            double c = types[a] / typeSize;
            typesMulti[a] = c/b;
        }
        d = 0;
        for(ShipData a : shipDatas){
            for(int b = 0; b < a.getSpawnWeight().size(); b++) {
                double weight = shipOdds.get(d);//(float)a.getSpawnWeight().values().toArray()[b];//get(vareantID);
                double t = (weight * typesMulti[getIdTypes(a.getHullType())]) * 10;
                outputtemp2[d] = t;
                d++;
            }
        }

        d=0;
        for(ShipData a : shipDatas) {
            Object[] keys = a.getSpawnWeight().keySet().toArray();
            for (int b = 0; b < a.getSpawnWeight().size(); b++) {
                String key = (String)keys[b];
                output.put(key, (int) Math.max((outputtemp[d]+outputtemp2[d])/2,1));
                d++;
            }
        }
        /*for(int a = 0; a < output.size(); a++){
            output[a] = (outputtemp[a]+outputtemp2[a])/2;
        }*/

        return output;
    }
    private static void generateShips(PosdoLordTemplate lord,String factionID,boolean useAllShips,int minShip,int maxShip,int[] sizeratio,int[] typeratio){
        //generate possible ships
        AvailableShipData availableShipData = getAvailableShips(factionID,useAllShips);
        ArrayList<ShipData> ships = new ArrayList<>();
        if (availableShipData == null){
            //the 'spawn of malice' are the custom people I asked malice to make for this easter egg. a reword for finding so many dammed crashes.
            String[][] spawnOfMalice = {
                    {"Lucious"," The Final Boss's","UnKite Plahanx","Lord who's desire can be explained in one word: \"Power\", - though seems to be that \"Knowledge - is Power\" is not in his book. Many deserters, smugglers and even full-fledged pirate warlords rushed to explore a dormant corpse of the former Domain of Man, many gained something in return - revolts, food shortages, women shortages(?) and essentially a headache. Others in most cases - gruesome death of suffocation, evaporation and boredom after all. But some of them could come across at very invaluable tech of the old, for example, domain nanoforge."},
                    {"Grigoriy"," Aznaev's","Alex's Watcher PMC","Lord who knows something, something that they shouldn't. it was from the very start and it knows, that reality is not what it claims to be. It's revolting for them to think and feel at this point, a mere fleeting thought about this pleasant lie, which is \"life\", crossing its mind every night, but this is enough - They Has No Mouth, and They Must Scream and someone, who's named as \"Alex\" must pay for his suffering"},
            };
            //Lord who knows something, something that they shouldn't. it was from the very start and it knows, that reality is not what it claims to be. It's revolting for them to think and feel at this point, a mere fleeting thought about this pleasant lie, which is "life", crossing its mind every night, but this is enough - They Has No Mouth, and They Must Scream and someone, who's named as "Alex" must pay for his suffering
            int person = Utils.nextInt(spawnOfMalice.length - 1);
            lord.lore="Every time you so mush as think of this terrifying individual, you shudder in fear. you don't know what they are, or who sent them, but you do know they exist. and they are angry.";
            if (LordController.getLordByFirstName(spawnOfMalice[person][0]) != null){
                lord.name = spawnOfMalice[person][0] + spawnOfMalice[person][1];
                if (spawnOfMalice[person][2] != null) lord.lore = spawnOfMalice[person][2];
                lord.fleetName = spawnOfMalice[person][3];
            }
            log.info("WARNING: was forced to use the final emergency fleet generator for a starlords fleet");
            String finalBackup = "kite_pirates_Raider";//the ultimate weapon of the final war
            lord.shipPrefs = new HashMap<>();
            lord.shipPrefs.put(finalBackup,1);
            lord.flagShip = finalBackup;
            return;
        }
        int maxLoops = 5;
        maxLoops = fleetGeneratorTypes.size()!=0 ? maxLoops : 0;
        int targetShip = (int) ((random.nextDouble() * (maxShip-minShip)) + minShip);
        log.info("  attempting to generate ships with a allShips, minShip, maxShip, targetShip of: "+useAllShips+", "+minShip+", "+maxShip+", "+targetShip);
        //log.info("  size ratio is: "+sizeratio[0]+", "+sizeratio[1]+", "+sizeratio[2]+", "+sizeratio[3]);
        //log.info("  type ratio is: "+typeratio[0]+", "+typeratio[1]+", "+typeratio[2]);
        while (availableShipData.getUnorganizedShips().size() != 0 && ships.size() < targetShip && maxLoops > 0) {
            //so, since this is the stage were I get any ships I might want, lets ignore the max ship count here.
            AvailableShipData skimmedShips = fleetGeneratorTypes.get(getValueFromWeight(fleetGeneratorRatio)).skimPossibleShips(availableShipData);
            //log.info("      got "+skimmedShips.getUnorganizedShips().size()+" ships from skiming..");
            ArrayList<ShipData> newShips = getShips(skimmedShips,sizeratio,typeratio,targetShip);
            //log.info("      got "+newShips.size()+" ships after adjusting for sizes...");
            for (ShipData a : newShips){
                ships.add(a);
                availableShipData.removeShip(a.getHullID());//this already happens in getShips.
            }
            //log.info("      got "+availableShipData.getUnorganizedShips().size()+" possible ships left to grab...");
            maxLoops--;
        }
        //backup generator. runs if I failed to reach the min number of ships I wanted with more picky generators.
        if(ships.size() < minShip){
            log.info("  attempting to generate ships using first backup...");
            AvailableShipData skimmedShips = fleetGeneratorBackup.skimPossibleShips(availableShipData);
            //log.info("      got "+skimmedShips.getUnorganizedShips().size()+" ships from skiming..");
            ArrayList<ShipData> newShips = getShips(skimmedShips,sizeratio,typeratio,targetShip);
            //log.info("      got "+newShips.size()+" ships after adjusting for sizes...");
            for (ShipData a : newShips){
                ships.add(a);
                availableShipData.removeShip(a.getHullID());//this already happens in getShips.
            }
            //log.info("      got "+availableShipData.getUnorganizedShips().size()+" possible ships left to grab...");
        }
        //final backup generator. only activates if I still don't have enough ships (signaling that my ship ratios cannot produce ships)
        if (ships.size() < minShip){
            log.info("  attempting to generate ships using final backup...");
            ArrayList<ShipData> newShips = getShips(availableShipData,new int[]{1,1,1,1},new int[]{1,1,1},minShip);
            //log.info("      got "+newShips.size()+" ships from size with no skimming...");
            for (ShipData a : newShips){
                //this does not edit availableShipData because its already edited in getships
                ships.add(a);
            }
            //log.info("      got "+availableShipData.getUnorganizedShips().size()+" possible ships left to grab...");
        }
        //generate ship ratio
        lord.shipPrefs = assingFleetSpawnWeights(ships,sizeratio,typeratio);

        //get unselected ship for possible flagship if required.
        if (availableShipData.getUnorganizedShips().size() != 0 && oddsOfNoneSelectedFlagship < random.nextDouble()){
            ships = new ArrayList<>();
            for(Object a : availableShipData.getUnorganizedShips().values().toArray()){
                ships.add((ShipData) a);
            }
        }
        //pick flagship
        lord.flagShip = pickFlagship(ships);
    }
    private static String pickFlagship(ArrayList<ShipData> ships){
        if (flagshipPickerTypes.size() == 0){
            return flagshipGeneratorBackup.pickFlagship(ships);
        }
        return flagshipPickerTypes.get(getValueFromWeight(flagshipPickerRatio)).pickFlagship(ships);
    }
    public static AvailableShipData getAvailableShips(String factionID,boolean allShips){
        Logger log = Global.getLogger(LordGenerator.class);
        //log.info("DEBUG: attempting to get available ships with a factionID, allShips of: "+factionID+", "+allShips);
        Set<String> a = null;
        AvailableShipData b=null;
        //Global.getSector().getFaction("").ship
        if (!allShips) {
            log.info("DEBUG: attempting to get priority ships...");
            //a = Global.getSector().getFaction(factionID).getPriorityShips();//NOTE: this would only get ships that are prioritized. sounds great, but factions like the heg only have 5 prioritized ships...
            a = Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.CARRIER_LARGE);
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.CARRIER_MEDIUM));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.CARRIER_SMALL));

            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.COMBAT_CAPITAL));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.COMBAT_LARGE));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.COMBAT_MEDIUM));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.COMBAT_SMALL));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.COMBAT_SMALL_FOR_SMALL_FLEET));

            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.PHASE_CAPITAL));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.PHASE_LARGE));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.PHASE_MEDIUM));
            a.addAll(Global.getSector().getFaction(factionID).getVariantsForRole(ShipRoles.PHASE_SMALL));
            if (a.size() != 0) {
                b = AvailableShipData.getNewASD(a,true);
                log.info("DEBUG: got available ships ships (from priority) as: "+b.getUnorganizedShips().size());
            }
        }
        if (allShips || a == null || a.size() == 0 || b == null || b.getUnorganizedShips().size() == 0){
            //log.info("DEBUG: attempting to get all ships...");
            a = Global.getSector().getFaction(factionID).getKnownShips();
            //a = Global.getSector().getFaction(factionID).getFactionSpec().getKnownShips();
            if (a.size() != 0) {
                b = AvailableShipData.getNewASD(a,false);
                //log.info("DEBUG: got available ships ships (from all) as: "+b.getUnorganizedShips().size());
            }
        }
        if (a.size() == 0){
            //log.info("DEBUG: failed to get any ships at all. returning null");
            return null;
        }
        //log.info("DEBUG: got "+a.size()+" ships");
        //log.info("DEBUG: got "+b.getUnorganizedShips().size()+" electable ships");
        if (b.getUnorganizedShips().size() == 0) return null;
        //log.info("DEBUG: successfully got all ships. returning...");
        return b;
    }

    public static void generatePerson(PosdoLordTemplate lord,String factionID){
        lord.isMale=isMaleChance < random.nextDouble();
        FullName.Gender gender = FullName.Gender.FEMALE;
        if (lord.isMale) gender = FullName.Gender.MALE;

        PersonAPI person = Global.getSector().getFaction(factionID).createRandomPerson(gender);
        lord.portrait = person.getPortraitSprite();
        if (lord.name == null)lord.name = person.getNameString();
        for (int a = 0; a < 5 && (lord.portrait == null || lord.name == null); a++){
            person = Global.getSector().getFaction(factionID).createRandomPerson(gender);
            if (lord.portrait == null) lord.portrait = person.getPortraitSprite();
            if (lord.name == null) lord.name = person.getNameString();
        }
        if (lord.portrait == null || lord.name == null){
            person = Global.getSector().getFaction("independent").createRandomPerson(gender);
            if (lord.portrait == null) lord.portrait = person.getPortraitSprite();
            if (lord.name == null) lord.name = person.getNameString();
        }
        if (lord.fleetName == null)lord.fleetName = person.getNameString()+fleetAdjective;
    }

    private static int getValueFromWeight(ArrayList<Double> weight){
        double totalValue = 0;
        double randomValue = random.nextDouble();
        for (double a : weight){
            totalValue+=a;
        }
        totalValue*=randomValue;
        double currentValue = 0;
        for (int a = 0; a < weight.size(); a++){
            currentValue+=weight.get(a);
            if (currentValue >= totalValue) return a;
        }
        return -1;//force a crash because I did something wrong and need to fix this.
    }
    public static int getValueFromWeight(double[] weight){
        double totalValue = 0;
        double randomValue = random.nextDouble();
        for (double a : weight){
            totalValue+=a;
        }
        totalValue*=randomValue;
        double currentValue = 0;
        for (int a = 0; a < weight.length; a++){
            currentValue+=weight[a];
            if (currentValue >= totalValue) return a;
        }
        return -1;//force a crash because I did something wrong and need to fix this.
    }
    private static int getValueFromWeight(int[] weight){
        double[] a = new double[weight.length];
        for (int b = 0; b < a.length; b++){
            a[b] = weight[b];
        }
        return getValueFromWeight(a);//force a crash because I did something wrong and need to fix this.
    }
    private static int getLowestNumberID(int[] list,int[] tempData){
        //please note: temp data is always blank.
        int out = 0;
        int a2=0;
        while(true) {
            for (int a = 0; a < list.length; a++) {
                if (tempData[a] > out){
                    out = tempData[a];
                    a2 = a;
                }
            }
            if (out != 0) {
                tempData[a2]--;
                return a2;
            }
            for (int a = 0; a < list.length; a++) {
                tempData[a] = list[a];
            }
        }
    }
    public static int[][] targetNumbers(int[] listA,int[] listB,int target){
        ///is this even required? yes it is. it will remove I cant od this this way!
        /*IF sucsesfull, this would in fact remove the issue of having the wrong 'raitio' of ships, BUT
        * 1) there is no garenty the wanted raitio is even truely possable????
        *   -yes there is. getting the right number of different ships might be impossible, but getting the right number of ships 100% is possible. because I can just modify the frequancy they spawn at
        * 2) there is no garenty I have enouth ships for each type anyways. but is that a issue?
        * ...
        * ok solution time:
        * make it like... I get one ship of one type / size, the another of a diffrent type. but I then change the numbers so like...
        * so like, if I wanted a raitio of 5/4/2/1, I would have a total of 11, were I got in an order that would result in a equal distribution. so..
        * I would get 1,1,2,1,2,1,2,3,1,2,3,1,reset because everything is zero.*/
        int[][] output = new int[listA.length][listB.length];
        int totala = 0;
        int totalb = 0;
        for (int a = 0; a < listA.length; a++){
            totala+=listA[a];
        }
        for (int b = 0; b < listB.length; b++){
            totalb+=listB[b];
        }
        int totalWantedShips = totala*totalb;

        return output;
    }

    public static void addFleetGenerator(LordFleetGeneratorBase generator, double weight){
        if (generator == null || weight == 0) return;
        for (int a = 0; a < fleetGeneratorTypes.size(); a++){
            if(fleetGeneratorTypes.get(a).getName().equals(generator.getName())){
                fleetGeneratorTypes.remove(a);
                fleetGeneratorRatio.remove(a);
                break;
            }
        }
        fleetGeneratorTypes.add(generator);
        fleetGeneratorRatio.add(weight);
    }
    public static void addFlagshipPicker(LordFlagshipPickerBase picker, double weight){
        if (picker == null || weight == 0) return;
        for (int a = 0; a < flagshipPickerTypes.size(); a++){
            if(flagshipPickerTypes.get(a).getName().equals(picker.getName())){
                flagshipPickerTypes.remove(a);
                flagshipPickerRatio.remove(a);
                break;
            }
        }
        flagshipPickerTypes.add(picker);
        flagshipPickerRatio.add(weight);
    }
}
