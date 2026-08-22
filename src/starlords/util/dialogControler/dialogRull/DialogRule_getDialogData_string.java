package starlords.util.dialogControler.dialogRull;

import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogRule_getDialogData_string extends DialogRule_Base{
    String key;
    ArrayList<String> whiteList;
    ArrayList<String> blackList;

    @SneakyThrows
    public DialogRule_getDialogData_string(String key, JSONObject json){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        for (Iterator it2 = json.keys(); it2.hasNext();) {
            String key3 = (String) it2.next();
            if (json.getBoolean(key3)){
                whiteList.add(key3);
                continue;
            }
            blackList.add(key3);
        }
        this.blackList=blackList;
        this.whiteList=whiteList;
        this.key=key;
    }

    @Override
    public boolean condition(Lord lord) {
        String rel = getString(lord);
        for (String a : whiteList){
            if (!rel.equals(a)){
                return false;
            }
        }
        for (String a : blackList){
            if (rel.equals(a)){
                return false;
            }
        }
        return true;
    }
    protected String getString(Lord lord){
        String rel = "";
        rel = LordInteractionDialogPluginImpl.DATA_HOLDER.getString(key);
        return rel;
    }
}
