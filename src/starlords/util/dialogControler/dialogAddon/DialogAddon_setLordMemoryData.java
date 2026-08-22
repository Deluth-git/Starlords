package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogValues.DialogValuesList;
import starlords.util.memoryUtils.DataHolder;

import java.util.HashMap;
import java.util.Iterator;

import static starlords.util.Constants.STARLORD_ADDITIONAL_MEMORY_KEY;

public class DialogAddon_setLordMemoryData extends DialogAddon_setDialogData{
    HashMap<String, DialogValuesList> time = new HashMap<>();
    @SneakyThrows
    public DialogAddon_setLordMemoryData(JSONObject json){
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
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord,Lord targetLord, MarketAPI targetMarket) {
        DataHolder DATA_HOLDER = lord.getLordDataHolder();
        applyStrings(DATA_HOLDER,lord,targetLord,targetMarket);
        applyBooleans(DATA_HOLDER,lord,targetLord,targetMarket);
        applyFloats(DATA_HOLDER,lord,targetLord,targetMarket);
        applyAddFloats(DATA_HOLDER,lord,targetLord,targetMarket);
        //lord.saveLordDataHolder();
    }
    public void applyStrings(DataHolder DATA_HOLDER,Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : strings.keySet()) {
            if (!time.containsKey(key)) {
                DATA_HOLDER.setString(key, strings.get(key));
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            DATA_HOLDER.setString(key,strings.get(key) ,time);
        }
    }
    public void applyBooleans(DataHolder DATA_HOLDER,Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : booleans.keySet()) {
            if (!time.containsKey(key)) {
                DATA_HOLDER.setBoolean(key, booleans.get(key));
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            DATA_HOLDER.setBoolean(key,booleans.get(key) ,time);
        }

    }
    public void applyFloats(DataHolder DATA_HOLDER,Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : setInts.keySet()) {
            int data = setInts.get(key).getValue(lord, targetLord, targetMarket);
            if (!time.containsKey(key)) {
                DATA_HOLDER.setInteger(key, data);
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            DATA_HOLDER.setInteger(key, data,time);
        }
    }
    public void applyAddFloats(DataHolder DATA_HOLDER,Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (String key : addIntsMin.keySet()) {
            int min = addIntsMin.get(key).getValue(lord, targetLord,targetMarket);
            int max = addIntsMax.get(key).getValue(lord, targetLord,targetMarket);
            int baseValue = DATA_HOLDER.getInteger(key);
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
                DATA_HOLDER.setInteger(key, data);
                continue;
            }
            int time = this.time.get(key).getValue(lord,targetLord,targetMarket);
            DATA_HOLDER.setInteger(key, data,time);
        }
    }
}
