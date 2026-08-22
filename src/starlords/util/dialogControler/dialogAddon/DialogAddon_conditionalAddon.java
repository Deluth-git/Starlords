package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;

public class DialogAddon_conditionalAddon extends DialogAddon_Base{
    ArrayList<DialogRule_Base> rules;
    ArrayList<DialogAddon_Base> addons;
    @SneakyThrows
    public DialogAddon_conditionalAddon(JSONObject json, String key){
        JSONObject json2 = json.getJSONObject(key);
        rules = DialogSet.getDialogRulesFromJSon(json2.getJSONObject("rules"));
        addons = DialogSet.getDialogAddonsFromJSon(json2.getJSONObject("addons"));
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        for (DialogRule_Base a : rules){
            if (!a.condition(lord, targetLord,targetMarket)) return;
        }
        for (DialogAddon_Base a : addons){
            a.apply(textPanel, options, dialog, lord, targetLord, targetMarket);
        }
    }
}
