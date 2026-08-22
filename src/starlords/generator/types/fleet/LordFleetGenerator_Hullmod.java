package starlords.generator.types.fleet;

import com.fs.starfarer.api.Global;
import starlords.generator.LordGenerator;
import starlords.generator.support.AvailableShipData;
import starlords.generator.support.ShipData;

public class LordFleetGenerator_Hullmod extends LordFleetGeneratorBase{
    String target = null;

    public LordFleetGenerator_Hullmod(String name) {
        super(name);
    }

    @Override
    public AvailableShipData skimPossibleShips(AvailableShipData input) {
        int maxLoops = 5;
        while(maxLoops > 0 && target == null) {
            ShipData a = input.getRandomShip();
            Object[] b = a.getSpawnWeight().keySet().toArray();
            b = Global.getSettings().getVariant((String) b[LordGenerator.getRandom().nextInt(b.length)]).getHullMods().toArray();
            if (b.length == 0){
                maxLoops--;
                continue;
            }
            target = (String) b[(int) LordGenerator.getRandom().nextInt(b.length)];
            return super.skimPossibleShips(input);
            //target = Global.getSettings().getHullSpec(a.getHullID());
        }
        return LordGenerator.getFleetGeneratorBackup().skimPossibleShips(input);
    }
    @Override
    public ShipData filterShipData(ShipData data) {
        Object[] b = data.getSpawnWeight().keySet().toArray();
        boolean add = false;
        ShipData ship = new ShipData("","","");// = new ShipData();
        for (Object a : b){
            if (Global.getSettings().getVariant((String) a).getHullMods().contains(target)){
                if (!add){
                    ship = new ShipData(data.getHullID(),data.getHullSize(),data.getHullType());
                    add = true;
                }
                ship.addVariant((String)a,data.getSpawnWeight().get((String)a));
            }
        }
        if (!add) return null;
        return super.filterShipData(ship);
    }
}
