package starlords.generator.types.flagship;

import com.fs.starfarer.api.Global;
import starlords.generator.support.ShipData;

import java.util.ArrayList;

public class LordFlagshipPicker_DP extends LordFlagshipPickerBase{
    public LordFlagshipPicker_DP(String name) {
        super(name);
    }
    @Override
    public String pickFlagship(ArrayList<ShipData> ships) {
        //I cant find were the ships DP is stored, so I cant chose a flagship.
        //Object[] a = (ships.get((int)(Math.random()*ships.size()))).getSpawnWeight().keySet().toArray();
        /*float max = 0;
        ShipData ship = null;
        for (ShipData a : ships){
            //Global.getSettings().getVariant("")
            float value = Global.getSettings().getHullSpec(a.getHullID()).();
            //Global.getSettings().get
            if (value > max){
                max = value;
                ship = a;
            }
        }
        Object[] a = ship.getSpawnWeight().keySet().toArray();
        return (String)a[(int) (Math.random()*a.length)];*/
        return super.pickFlagship(ships);
    }
}
