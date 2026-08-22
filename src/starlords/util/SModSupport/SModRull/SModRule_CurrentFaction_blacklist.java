package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.ArrayList;

public class SModRule_CurrentFaction_blacklist extends SModRule_Base {
    ArrayList<String> data;
    public SModRule_CurrentFaction_blacklist(ArrayList<String> data){
        this.data = data;
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        for (String a : data){
            if (a.equals(lord.getFaction().getId())){
                return false;
            }
        }
        return true;
    }
}

