package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import starlords.person.Lord;

import java.util.ArrayList;

public class SModRule_hullmods_whitelist extends SModRule_Base {
    ArrayList<String> data;
    public SModRule_hullmods_whitelist(ArrayList<String> data){
        this.data = data;
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        for (String a : data){
            if (member.getVariant().hasHullMod(a)){
                return true;
            }
        }
        return false;
    }
}
