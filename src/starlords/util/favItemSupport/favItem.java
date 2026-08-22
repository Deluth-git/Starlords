package starlords.util.favItemSupport;

import com.fs.starfarer.api.Global;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.util.Constants;
import starlords.util.dialogControler.DialogSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class favItem {
    /*ok, do... this is not done at all. I will wokr on this over time.*/
    private static HashMap<String,favItem> items = new HashMap<String,favItem>();
    @SneakyThrows
    public static void startup(){
        /*JSONObject jsonObject = Global.getSettings().getMergedJSONForMod("data/lords/favItems.json", Constants.MOD_ID);
        items = new HashMap<>();
        for (Iterator it2 = jsonObject.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            new DialogSet(key2,jsonObject.getJSONObject(key2));
        }*/
    }


    public favItem(String key, JSONObject json){


        items.put(key,this);
    }
}
