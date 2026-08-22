package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import starlords.person.Lord;

import java.util.ArrayList;

public class SModRule_HullID_whitelist extends SModRule_Base{
    ArrayList<String> data;
    public SModRule_HullID_whitelist(ArrayList<String> data){
        this.data = data;
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        for (String a : data){
            if (a.equals(member.getHullSpec().getHullId())){
                return true;
            }
        }
        return false;
    }
}
