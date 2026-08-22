package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

import java.util.HashMap;
import java.util.Iterator;

public class DialogAddon_setDialogData extends DialogAddon_Base{
    protected HashMap<String,String> strings = new HashMap<>();
    protected HashMap<String,Boolean> booleans = new HashMap<>();
    protected HashMap<String, DialogValuesList> setInts = new HashMap<>();
    protected HashMap<String,DialogValuesList> addIntsMin = new HashMap<>();
    protected HashMap<String,DialogValuesList> addIntsMax = new HashMap<>();
    public DialogAddon_setDialogData(){

    }
    @SneakyThrows
    public DialogAddon_setDialogData(JSONObject json){
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addSingleItem(key2,key2,json);
        }
    }
    @SneakyThrows
    protected void addSingleItem(String id,String key2, JSONObject json){
        if (json.get(key2) instanceof JSONObject && (json.getJSONObject(key2).has("min") || json.getJSONObject(key2).has("max"))){
            JSONObject a = json.getJSONObject(key2);
            if (json.getJSONObject(key2).has("min")) addIntsMin.put(id,new DialogValuesList(json.getJSONObject(key2),"min"));
            if (json.getJSONObject(key2).has("max")) addIntsMax.put(id,new DialogValuesList(json.getJSONObject(key2),"max"));
            return;
        }
        if (json.get(key2) instanceof JSONObject){
            setInts.put(id,new DialogValuesList(json,key2));
            return;
        }
        boolean go = true;
        try{
            json.getInt(key2);
        }catch (Exception e){
            go = false;
        }
        if (go){
            setInts.put(id,new DialogValuesList(json,key2));
            return;
        }

        go = true;
        try {
            json.getBoolean(key2);
        }catch (Exception e){
            go = false;
        }
        if (go){
            boolean a = json.getBoolean(key2);
            booleans.put(id,a);
            return;

        }
        go = true;
        try {
            json.getString(key2);
        }catch (Exception e){
            go = false;
        }
        if (go){
            String a = json.getString(key2);
            strings.put(id,a);
            return;
        }
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord,Lord targetLord, MarketAPI targetMarket) {
        applyStrings(dialog, lord,targetLord,targetMarket);
        applyBooleans(dialog, lord,targetLord,targetMarket);
        applyFloats(dialog, lord,targetLord,targetMarket);
        applyAddFloats(dialog, lord,targetLord,targetMarket);

    }
    public void applyStrings(InteractionDialogAPI dialog, Lord lord,Lord targetLord,MarketAPI targetMarket){
        for (String key : strings.keySet()) {
            LordInteractionDialogPluginImpl.DATA_HOLDER.setString(key,strings.get(key));
        }
    }
    public void applyBooleans(InteractionDialogAPI dialog, Lord lord,Lord targetLord,MarketAPI targetMarket){
        for (String key : booleans.keySet()) {
            LordInteractionDialogPluginImpl.DATA_HOLDER.setBoolean(key,booleans.get(key));
        }

    }
    public void applyFloats(InteractionDialogAPI dialog, Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : setInts.keySet()) {
            LordInteractionDialogPluginImpl.DATA_HOLDER.setInteger(key, setInts.get(key).getValue(lord, targetLord, targetMarket));
        }
    }
    public void applyAddFloats(InteractionDialogAPI dialog, Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : addIntsMin.keySet()) {
            int min = addIntsMin.get(key).getValue(lord,targetLord,targetMarket);
            int max = addIntsMax.get(key).getValue(lord,targetLord,targetMarket);
            int baseValue = LordInteractionDialogPluginImpl.DATA_HOLDER.getInteger(key);
            max = Math.max(min,max);
            int range = max - min;
            if (range != 0){
                boolean negitive = false;
                if (range < 0){
                    negitive = true;
                }
                if (negitive) range *=-1;
                range = Utils.rand.nextInt(range);
                if (negitive) range *=-1;
            }
            int data = baseValue + (min+range);
            LordInteractionDialogPluginImpl.DATA_HOLDER.setInteger(key,data);
        }
    }
}
