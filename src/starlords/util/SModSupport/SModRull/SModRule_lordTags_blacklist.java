package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import starlords.person.Lord;

import java.util.ArrayList;

public class SModRule_lordTags_blacklist extends SModRule_Base {
    ArrayList<String> data;
    public SModRule_lordTags_blacklist(ArrayList<String> data){
        this.data = data;
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        for (String a : data){
            if (lord.getLordAPI().hasTag(a)){
                return false;
            }
        }
        return true;
    }
}
