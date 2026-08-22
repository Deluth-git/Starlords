package starlords.util.factionUtils.customTemplates;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import org.json.JSONObject;
import starlords.util.factionUtils.FactionTemplate;

public class FactionTemplate_player extends FactionTemplate {
    public FactionTemplate_player(String factionID) {
        super(factionID);
    }

    public FactionTemplate_player(String factionID, JSONObject json) {
        super(factionID, json);
    }

    @Override
    public PersonAPI getLeader() {
        return Global.getSector().getPlayerPerson();
    }
}
