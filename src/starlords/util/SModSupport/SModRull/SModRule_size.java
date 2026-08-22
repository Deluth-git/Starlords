package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.Iterator;

public class SModRule_size extends SModRule_Base {
    boolean[] sizes = {false,false,false,false};
    @SneakyThrows
    public SModRule_size(JSONObject json){
        for (Iterator it2 = json.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            boolean data = json.getBoolean(key2);
            //ShipAPI.HullSize size = ShipAPI.HullSize.valueOf(key2);
            switch (key2){
                case "FRIGATE":
                    sizes[0] = data;
                    break;
                case "DESTROYER":
                    sizes[1] = data;
                    break;
                case "CRUISER":
                    sizes[2] = data;
                    break;
                case "CAPITAL_SHIP":
                    sizes[3] = data;
                    break;
            }

        }
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        ShipAPI.HullSize size = member.getHullSpec().getHullSize();
        switch (size){
            case FRIGATE:
                return sizes[0];
            case DESTROYER:
                return sizes[1];
            case CRUISER:
                return sizes[2];
            case CAPITAL_SHIP:
                return sizes[3];
        }
        return false;
    }
}
