package starlords.util.dialogControler;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONException;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;
import java.util.Iterator;

public class LordDialog {
    public ArrayList<ArrayList<DialogSet>> organizedDialogSets = new ArrayList<>();
    private ArrayList<DialogRule_Base> rules = new ArrayList<>();
    public LordDialog(String key,JSONObject jsonObject) throws JSONException {
        organizedDialogSets = new ArrayList<>();
        rules = DialogSet.getDialogRulesFromJSon(jsonObject.getJSONObject("rules"));
        JSONObject jsonObject2 = jsonObject.getJSONObject("lines");
        for (Iterator it2 = jsonObject2.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            new DialogSet(key2,jsonObject2.getJSONObject(key2),this);
        }
        int priority = 0;
        if (jsonObject.has("priority")) priority = jsonObject.getInt("priority");
        while (DialogSet.organizedDialogs.size() <= priority){
            DialogSet.organizedDialogs.add(new ArrayList<>());
        }
        DialogSet.organizedDialogs.get(priority).add(this);
        DialogSet.dialogs.put(key,this);
    }
    public DialogSet getSet(Lord lord, Lord targetLord, MarketAPI targetMarket, String id){
        if (!isAllowed(lord,targetLord)) return null;
        for (int a = organizedDialogSets.size() - 1; a >= 0; a--){
            for (DialogSet b : organizedDialogSets.get(a)){
                if (b.hasLine(id) && b.canUseDialog(lord,targetLord,targetMarket)) return b;
            }
        }
        return null;
    }
    private boolean isAllowed(Lord lord,Lord targetLord){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,targetLord)) return false;
        }
        return true;
    }
}
