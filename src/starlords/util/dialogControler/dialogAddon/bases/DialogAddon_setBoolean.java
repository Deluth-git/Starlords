package starlords.util.dialogControler.dialogAddon.bases;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogAddon.DialogAddon_Base;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;

public class DialogAddon_setBoolean extends DialogAddon_Base {
    public ArrayList<DialogRule_Base> rules = null;
    public boolean bol=false;
    public DialogAddon_setBoolean(){

    }
    public DialogAddon_setBoolean(JSONObject json, String key){
        init(json, key);
    }
    @SneakyThrows
    public void init(JSONObject json, String key){
        if (json.get(key) instanceof JSONObject){
            rules = DialogSet.getDialogRulesFromJSon(json.getJSONObject(key));
            return;
        }
        bol = json.getBoolean(key);
    }
    private boolean getBooleanFromRules(Lord lord,Lord targetLord,MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord, targetLord,targetMarket)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        boolean bol = this.bol;
        if (rules != null) {
            bol = getBooleanFromRules(lord, targetLord, targetMarket);
        }
        applyBoolean(bol,textPanel,options,dialog,lord,targetLord,targetMarket);
    }
    protected void applyBoolean(boolean bol,TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){

    }
}
