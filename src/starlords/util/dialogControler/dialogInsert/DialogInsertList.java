package starlords.util.dialogControler.dialogInsert;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogInsertList {
    private ArrayList<DialogInsert_Base> inserts = new ArrayList<>();
    @SneakyThrows
    public DialogInsertList(JSONObject json, String key){
        if (json.get(key) instanceof JSONArray){
            JSONArray array = json.getJSONArray(key);
            for (int a = 0; a < array.length(); a++){
                inserts.add(getInsert(array.getJSONObject(a)));
            }
            return;
        }
        inserts.add(getInsert(json.getJSONObject(key)));
    }
    private DialogInsert_Base getInsert(JSONObject json){
        if (json.has("dialogValue")){
            return new DialogInsert_dialogValue(json);
        }
        if (json.has("memory")){
            return new DialogInsert_memory(json);
        }
        if (json.has("lordMemory")){
            return new DialogInsert_lordMemory(json);
        }
        if (json.has("dialogData")){
            return new DialogInsert_dialogData(json);
        }
        if (json.has("customDialogInsert")){
            return new DialogInsert_customList(json);
        }
        DialogInsert_Base error = new DialogInsert_Base();
        error.key = "";
        return error;
    }
    public String insertData(String line, Lord lord, Lord targetLord, MarketAPI targetMarket){
        for (DialogInsert_Base a : inserts){
            line = a.insert(line, lord, targetLord, targetMarket);
        }
        return line;
    }
}
