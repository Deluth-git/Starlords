package starlords.util.factionUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.util.Constants;
import starlords.util.factionUtils.customTemplates.FactionTemplate_player;

import java.util.HashMap;
import java.util.Iterator;

public class FactionTemplateController {
    private static HashMap<String, FactionTemplate> templates = new HashMap<String, FactionTemplate>();
    public static void addTemplate(FactionTemplate template){
        templates.put(template.getFactionID(),template);
    }
    @SneakyThrows
    public static void init(){
        JSONObject jsonObject = Global.getSettings().getMergedJSONForMod("data/lords/faction.json", Constants.MOD_ID);
        templates = new HashMap<>();
        for (Iterator it2 = jsonObject.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            JSONObject json2 = jsonObject.getJSONObject(key2);
            if (key2.equals("player")){
                new FactionTemplate_player(key2,json2);
            }else {
                new FactionTemplate(key2, json2);
            }
        }
    }
    public static FactionTemplate getTemplate(String factionID){
        if (templates.containsKey(factionID)) return templates.get(factionID);
        return new FactionTemplate(factionID);
    }
    public static FactionTemplate getTemplate(FactionAPI faction){
        return getTemplate(faction.getId());
    }
}
