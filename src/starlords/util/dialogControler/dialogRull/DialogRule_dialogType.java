package starlords.util.dialogControler.dialogRull;

import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogRule_dialogType extends DialogRule_Base {
    ArrayList<String> whiteList = new ArrayList<>();
    ArrayList<String> blackList = new ArrayList<>();
    @SneakyThrows
    public DialogRule_dialogType(JSONObject json, String key){
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
    }
    @Override
    public boolean condition(Lord lord) {
        String target = LordInteractionDialogPluginImpl.getDialogType();
        for (String a : blackList){
            if (a.equals(target)) {
                return false;
            }
        }
        if (whiteList.size() == 0) return true;

        for (String a : whiteList){
            if (a.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
