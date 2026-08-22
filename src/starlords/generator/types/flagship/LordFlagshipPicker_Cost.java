package starlords.generator.types.flagship;

import com.fs.starfarer.api.Global;
import starlords.generator.LordGenerator;
import starlords.generator.support.ShipData;

import java.util.ArrayList;

public class LordFlagshipPicker_Cost extends LordFlagshipPickerBase{
    public LordFlagshipPicker_Cost(String name) {
        super(name);
    }
    @Override
    public String pickFlagship(ArrayList<ShipData> ships) {
        //Object[] a = (ships.get((int)(Math.random()*ships.size()))).getSpawnWeight().keySet().toArray();
        float max = 0;
        ShipData ship = null;
        for (ShipData a : ships){
            float value = Global.getSettings().getHullSpec(a.getHullID()).getBaseValue();
            if (value > max){
                max = value;
                ship = a;
            }
        }
        Object[] a = ship.getSpawnWeight().keySet().toArray();
        return (String)a[(int) (LordGenerator.getRandom().nextInt(a.length))];
    }
}
