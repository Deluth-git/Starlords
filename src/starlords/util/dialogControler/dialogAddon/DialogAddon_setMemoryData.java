package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

import java.util.HashMap;
import java.util.Iterator;

public class DialogAddon_setMemoryData extends DialogAddon_setDialogData{
    HashMap<String, DialogValuesList> time = new HashMap<>();
    @SneakyThrows
    public DialogAddon_setMemoryData(JSONObject json){
        super();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            if (json.get(key2) instanceof JSONObject && json.getJSONObject(key2).has("time")){
                time.put(key2,new DialogValuesList(json.getJSONObject(key2),"time"));
                addSingleItem(key2,"data",json.getJSONObject(key2));
                continue;
            }
            addSingleItem(key2,key2,json);
        }
    }
    @Override
    public void applyStrings(InteractionDialogAPI dialog, Lord lord,Lord targetLord,MarketAPI targetMarket){
        for (String key : strings.keySet()) {
            if (!time.containsKey(key)) {
                Global.getSector().getMemory().set(key, strings.get(key));
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            Global.getSector().getMemory().set(key, strings.get(key),time);
        }
    }
    @Override
    public void applyBooleans(InteractionDialogAPI dialog, Lord lord,Lord targetLord,MarketAPI targetMarket){
        for (String key : booleans.keySet()) {
            if (!time.containsKey(key)) {
                Global.getSector().getMemory().set(key, booleans.get(key));
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            Global.getSector().getMemory().set(key, booleans.get(key),time);
        }

    }
    @Override
    public void applyFloats(InteractionDialogAPI dialog, Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : setInts.keySet()) {
            if (!time.containsKey(key)) {
                Global.getSector().getMemory().set(key, setInts.get(key).getValue(lord, targetLord, targetMarket));
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            Global.getSector().getMemory().set(key, setInts.get(key),time);
        }
    }
    @Override
    public void applyAddFloats(InteractionDialogAPI dialog, Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : addIntsMin.keySet()) {
            int min = addIntsMin.get(key).getValue(lord, targetLord,targetMarket);
            int max = addIntsMax.get(key).getValue(lord, targetLord,targetMarket);
            int baseValue = Global.getSector().getMemory().getInt(key);
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

            if (!time.containsKey(key)) {
                Global.getSector().getMemory().set(key, data);
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            Global.getSector().getMemory().set(key, data,time);

        }
    }
}
