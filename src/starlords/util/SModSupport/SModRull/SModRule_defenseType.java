package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.Iterator;

public class SModRule_defenseType extends SModRule_Base {
    private static final String defense_none = "NONE";
    private static final String defense_phase = "PHASE";
    private static final String defense_front = "FRONT";
    private static final String defense_omi = "OMNI";
    private boolean[] allows = {false,false,false,false};
    @SneakyThrows
    public SModRule_defenseType(JSONObject json){
        for (Iterator it2 = json.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            boolean data = json.getBoolean(key2);
            switch (key2){
                case defense_none:
                    allows[0] = data;
                    break;
                case defense_phase:
                    allows[1] = data;
                    break;
                case defense_front:
                    allows[2] = data;
                    break;
                case defense_omi:
                    allows[3] = data;
                    break;
            }

        }
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        ShieldAPI.ShieldType type = member.getHullSpec().getShieldType();
        if (type.equals(ShieldAPI.ShieldType.NONE)){
            return allows[0];
        }
        if (type.equals(ShieldAPI.ShieldType.PHASE)){
            return allows[1];
        }
        if (type.equals(ShieldAPI.ShieldType.FRONT)){
            return allows[2];
        }
        if (type.equals(ShieldAPI.ShieldType.OMNI)){
            return allows[3];
        }
        return false;
    }
}
