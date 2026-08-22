package starlords.util.dialogControler.dialogAddon.bases;

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
import starlords.util.dialogControler.dialogAddon.DialogAddon_Base;

public class DialogAddon_changeValue extends DialogAddon_Base {
    private int max = 2147483647;
    private int min = -2147483647;
    private DialogValuesList maxList;
    private DialogValuesList minList;
    boolean set = false;
    public DialogAddon_changeValue(){

    }
    @SneakyThrows
    public DialogAddon_changeValue(JSONObject json, String key){
        init(json,key);
    }
    @SneakyThrows
    public void init(JSONObject json, String key){
        if (json.get(key) instanceof  JSONObject && json.getJSONObject(key).has("setValue")){
            json = json.getJSONObject(key);
            set = json.getBoolean("setValue");
            key = "value";
        }
        if (!(json.get(key) instanceof JSONObject)){
            int value = json.getInt(key);
            max=value;
            min=value;
            return;
        }
        JSONObject json2 = json.getJSONObject(key);
        if (json2.has("max")){
            maxList = new DialogValuesList(json2,"max");
        }
        if (json2.has("min")){
            minList = new DialogValuesList(json2,"min");
        }
        if ((!json2.has("min") && !json2.has("max"))){
            minList = new DialogValuesList(json,key);
            maxList = minList;
            return;
        }
    }
    private int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket){
        int max = this.max;
        if (maxList != null) max = maxList.getValue(lord, targetLord,targetMarket);
        int min = this.min;
        if (minList != null) {
            if (!minList.equals(maxList)) {
                min = minList.getValue(lord, targetLord,targetMarket);
            }else{
                min = max;
            }
        }

        int ranChange = max - min;
        boolean isNegative = false;
        if (ranChange < 0){
            isNegative = true;
            ranChange*=-1;
        }
        if (ranChange != 0) ranChange = Utils.rand.nextInt(ranChange);
        if (isNegative) ranChange*=-1;
        int change = min + ranChange;
        return change;
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int value = getValue(lord, targetLord,targetMarket);
        if (set){
            value = value - getCurrentValue(textPanel, options, dialog, lord, targetLord, targetMarket);
        }
        if (value > 0){
            increaseChange(value,textPanel,options,dialog,lord,targetLord,targetMarket);
        }else if(value < 0){
            decreaseChange(value,textPanel,options,dialog,lord,targetLord,targetMarket);
        }
    }
    protected void increaseChange(int value,TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){

    }
    protected void decreaseChange(int value,TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){

    }
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){
        return 0;
    }
}
