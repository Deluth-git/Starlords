package starlords.generator.support;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.loading.RoleEntryAPI;
import lombok.Getter;
import org.apache.log4j.Logger;
import starlords.generator.LordGenerator;
import starlords.lunaSettings.StoredSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AvailableShipData {
    public static final String HULLTYPE_CARRIER = "CARRIER", HULLTYPE_WARSHIP = "WARSHIP", HULLTYPE_PHASE = "PHASE", HULLTYPE_COMBATCIV = "COMBATCIV", HULLTYPE_TANKER = "TANKER", HULLTYPE_CARGO = "CARGO", HULLTYPE_PERSONNEL = "PERSONNEL", HULLTYPE_LINER = "LINER", HULLTYPE_TUG = "TUG", HULLTYPE_UTILITY = "UTILITY";
    public static final String HULLSIZE_FIGHTER = ShipAPI.HullSize.FIGHTER.name(), HULLSIZE_FRIGATE = ShipAPI.HullSize.FRIGATE.name(), HULLSIZE_DESTROYER = ShipAPI.HullSize.DESTROYER.name(), HULLSIZE_CRUISER = ShipAPI.HullSize.CRUISER.name(), HULLSIZE_CAPITALSHIP = ShipAPI.HullSize.CAPITAL_SHIP.name();
    //this is going to hold 5 hashsets, each linking to a diffrent type of ship. thats warship, carrier, phase, combat civ, and civ.
    //NOTE: this will contain things on a vareant by vareant bases because WHAT THE FUCK how doe this even fucking work!?!?!!!!?
    @Getter
    private static AvailableShipData defaultShips;
    @Getter
    private HashMap<String, ShipData> unorganizedShips = new HashMap<>(); // This belongs somewhere else eventually
    //how this works, the first inputted string is the ship type (phase, carrier, extra), the second inputed string is the hullsize.
    @Getter
    private HashMap<String, HashMap<String,HashMap<String, ShipData>>> organizedShips = new HashMap<>();
    private static Logger log = Global.getLogger(AvailableShipData.class);
    public AvailableShipData(){
        startupType(HULLTYPE_CARRIER);
        startupType(HULLTYPE_WARSHIP);
        startupType(HULLTYPE_PHASE);
        startupType(HULLTYPE_COMBATCIV);
        startupType(HULLTYPE_TANKER);
        startupType(HULLTYPE_CARGO);
        startupType(HULLTYPE_LINER);
        startupType(HULLTYPE_PERSONNEL);
        startupType(HULLTYPE_TUG);
        startupType(HULLTYPE_UTILITY);
    }
    public static AvailableShipData getNewASD(Set<String> hullIds,boolean vareants){
        if (vareants) return getNewASDFromVariants(hullIds,defaultShips);
        return getNewASDFromHulls(hullIds,defaultShips);
    }
    public static AvailableShipData getNewASDFromVariants(Set<String> variantIDs, AvailableShipData AvailableShips){
        //log.info("DEBUG: attempting to get a new ASD with hullIds, AvailableShips: "+hullIds.size()+", "+AvailableShips.getUnorganizedShips().size());
        AvailableShipData b = new AvailableShipData();
        for (String a : variantIDs){
            //log.info("Looking at a hull of ID: "+a);
            ShipData c = AvailableShips.unorganizedShips.get(Global.getSettings().getVariant(a).getHullSpec().getHullId());
            if (c != null && c.getSpawnWeight().get(a) != null){
                //todo: I don't know how to get this to respect a factions faction specific variants. I must fix this at a latter date
                //log.info("  adding a new ShipData:");
                b.addShip(a,c.getSpawnWeight().get(a),c.getHullType());
            }
        }
        return b;

    }
    public static AvailableShipData getNewASDFromHulls(Set<String> hullIds, AvailableShipData AvailableShips){
        //log.info("DEBUG: attempting to get a new ASD with hullIds, AvailableShips: "+hullIds.size()+", "+AvailableShips.getUnorganizedShips().size());
        AvailableShipData b = new AvailableShipData();
        for (String a : hullIds){
            //log.info("Looking at a hull of ID: "+a);
            ShipData c = AvailableShips.unorganizedShips.get(a);
            if (c != null){
                //log.info("  adding a new ShipData:");
                b.unorganizedShips.put(a,c);
                b.organizedShips.get(c.getHullType()).get(c.getHullSize()).put(a,c);
            }
        }
        return b;

    }
    private void startupType(String type){
        HashMap<String,HashMap<String,ShipData>> a = new HashMap<>();
        a.put(HULLSIZE_FRIGATE,new HashMap<String,ShipData>());
        a.put(HULLSIZE_DESTROYER,new HashMap<String,ShipData>());
        a.put(HULLSIZE_CRUISER,new HashMap<String,ShipData>());
        a.put(HULLSIZE_CAPITALSHIP,new HashMap<String,ShipData>());
        organizedShips.put(type,a);
    }
    public static void startup(){
        //this reads and saves the default ship role of every ship in starsector (for ease of access, because by god)
        Logger log = Global.getLogger(StoredSettings.class);
        log.info("DEBUG: attempting to get AvailableShipData startup");
        List[] carriers =  {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.CARRIER_LARGE),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.CARRIER_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.CARRIER_SMALL)
        };
        List[] warships =  {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_CAPITAL),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_LARGE),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_SMALL),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_SMALL_FOR_SMALL_FLEET)
        };
        List[] phase =  {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PHASE_CAPITAL),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PHASE_LARGE),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PHASE_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PHASE_SMALL)
        };
        List[] combatCiv =  {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_FREIGHTER_LARGE),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_FREIGHTER_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.COMBAT_FREIGHTER_SMALL)
        };
        List[] tanker = {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.TANKER_LARGE),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.TANKER_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.TANKER_SMALL)
        };
        List[] cargo = {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.FREIGHTER_SMALL),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.FREIGHTER_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.FREIGHTER_LARGE)
        };
        List[] liner = {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.LINER_SMALL),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.LINER_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.LINER_LARGE),
        };
        List[] personnel = {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PERSONNEL_SMALL),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PERSONNEL_MEDIUM),
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.PERSONNEL_LARGE)
        };
        List[] tug = {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.TUG)
        };
        List[] utility = {
                Global.getSettings().getDefaultEntriesForRole(ShipRoles.UTILITY)
        };
        defaultShips = new AvailableShipData();
        defaultShips.addListShips(carriers,HULLTYPE_CARRIER);
        defaultShips.addListShips(warships,HULLTYPE_WARSHIP);
        defaultShips.addListShips(phase,HULLTYPE_PHASE);
        /*defaultShips.addListShips(combatCiv,HULLTYPE_COMBATCIV);
        defaultShips.addListShips(tanker,HULLTYPE_TANKER);
        defaultShips.addListShips(cargo,HULLTYPE_CARGO);
        defaultShips.addListShips(liner,HULLTYPE_LINER);
        defaultShips.addListShips(personnel,HULLTYPE_PERSONNEL);
        defaultShips.addListShips(tug,HULLTYPE_TUG);
        defaultShips.addListShips(utility,HULLTYPE_UTILITY);*/

    }
    private static void test2(String key){
        Logger log = Global.getLogger(AvailableShipData.class);
        log.info("DEBUG: getting ships of type: "+key);
        String[] sizes = {HULLSIZE_FRIGATE,HULLSIZE_DESTROYER,HULLSIZE_CRUISER,HULLSIZE_CAPITALSHIP};
        for(String hullSize : sizes) {
            log.info("  DEBUG: getting ships of hullsize: " + hullSize);
            for (ShipData a : defaultShips.organizedShips.get(key).get(hullSize).values()) {
                if (defaultShips.unorganizedShips.get(a.getHullID()) != null)log.info("      DEBUG: ship is in unorganizedShips list.");
                for (int b = 0; b < a.getSpawnWeight().size(); b++) {
                    log.info("      DEBUG: varient ID of : " + a.getSpawnWeight().keySet().toArray()[b] + " has a hull ID of: " + Global.getSettings().getVariant((String) a.getSpawnWeight().keySet().toArray()[b]).getHullSpec().getHullId());
                }
            }
        }
    }
    private static void test(String a){
        Logger log = Global.getLogger(StoredSettings.class);;
        ShipVariantAPI checked = Global.getSettings().getVariant(a);
        log.info("DEBUG: varient ID of : " +a+" has a hull ID of: "+checked.getHullSpec().getHullId());
    }
    public void addListShips(List[] list, String type){
        for (Object a : list){
            List b = (List)a;
            for(Object c : b) {
                RoleEntryAPI d = (RoleEntryAPI) c;
                addShip(d.getVariantId(), d.getWeight()/*,b.getFPCost()*/, type);
            }
        }
        test2(type);
    }
    public void addShip(String vareantID,float weight/*,float FPCost*/,String type){
        String hull = Global.getSettings().getVariant(vareantID).getHullSpec().getHullId();
        String size = Global.getSettings().getVariant(vareantID).getHullSpec().getHullSize().name();
        ShipData a = unorganizedShips.get(hull);
        if (size.equals(HULLSIZE_FIGHTER)) {
            size = HULLSIZE_FRIGATE;
        }
        if (Global.getSettings().getVariant(vareantID).getHullSpec().getHullId() == null){
            //double checking if the hullID actually exists???? how does this even happen!?!
            return;
        }
        if(a == null){
            a = new ShipData(hull,size,type);
            unorganizedShips.put(hull,a);
            organizedShips.get(type).get(size).put(hull,a);
        }
        a.addVariant(vareantID,weight);
    }
    public void addShip(ShipData data){
        String hull = data.getHullID();
        String size = data.getHullSize();
        unorganizedShips.put(hull,data);
        organizedShips.get(data.getHullType()).get(size).put(hull,data);
    }
    public void removeShip(String hullID){
        ShipData a = unorganizedShips.get(hullID);
        unorganizedShips.remove(hullID);
        organizedShips.get(a.getHullType()).get(a.getHullSize()).remove(hullID);
    }


    public ShipData getRandomShip(){
        Object[] a = this.unorganizedShips.values().toArray();
        if (a.length == 0) return null;
        return (ShipData) a[(int)(LordGenerator.getRandom().nextInt(a.length))];
    }
    /*public ShipData getRandomShip(int[] types, int[] sizes){
        Object[] a = this.organizedShips.get(type).get(size).values().toArray();
        if (a.length == 0) return null;
        return (ShipData) a[(int)(Math.random()*a.length)];
    }*/
    public ShipData getRandomShip(String type,String size){
        Object[] a = this.organizedShips.get(type).get(size).values().toArray();
        if (a.length == 0) return null;
        return (ShipData) a[(int)(LordGenerator.getRandom().nextInt(a.length))];
    }
}
