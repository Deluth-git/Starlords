package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

public class SModRule_fighterBays extends SModRule_Base {
    int min = 1;
    int max = 999;
    @SneakyThrows
    public SModRule_fighterBays(JSONObject jsonObject){
        boolean hasMin = jsonObject.has("min");
        boolean hasMax = jsonObject.has("max");
        if (hasMax) max = jsonObject.getInt("max");
        if (hasMin) min = jsonObject.getInt("min");
        if (!hasMin && hasMax && max == 0){
            min = 0;
        }
    }
    @Override
    public boolean canAdd(FleetMemberAPI member, Lord lord) {
        int size = member.getVariant().getFittedWings().size();
        return size >= min && size <= min;
    }
}
