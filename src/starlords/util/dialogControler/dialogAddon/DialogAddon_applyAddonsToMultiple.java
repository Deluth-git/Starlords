package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;
import java.util.List;

public class DialogAddon_applyAddonsToMultiple extends DialogAddon_Base{
    int targetType = 0;
    ArrayList<DialogAddon_Base> addons;
    ArrayList<DialogRule_Base> rules;
    @SneakyThrows
    public DialogAddon_applyAddonsToMultiple(JSONObject json, String key){
        json = json.getJSONObject(key);
        String target = json.getString("target");
        switch (target){
            case "LORDS":
                targetType = 0;
                break;
            case "MARKETS":
                targetType = 1;
                break;
        }
        addons = DialogSet.getDialogAddonsFromJSon(json.getJSONObject("addons"));
        rules = DialogSet.getDialogRulesFromJSon(json.getJSONObject("rules"));
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        switch (targetType){
            case 0:
                applyLords(textPanel, options, dialog, lord, targetLord, targetMarket);
                break;
            case 1:
                applyMarkets(textPanel, options, dialog, lord, targetLord, targetMarket);
                break;
        }
    }
    private void applyLords(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){
        List<Lord> lords = LordController.getLordsList();
        for (Lord a : lords){
            if (canLord(textPanel, options, dialog, a, lord, targetMarket)){
                for (DialogAddon_Base b : addons){
                    b.apply(textPanel, options, dialog, a, lord, targetMarket);
                }
            }
        }
    }
    private void applyMarkets(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){
        List<MarketAPI> lords = Global.getSector().getEconomy().getMarketsCopy();
        for (MarketAPI a : lords){
            if (canLord(textPanel, options, dialog, lord,targetLord, a)){
                for (DialogAddon_Base b : addons){
                    b.apply(textPanel, options, dialog, lord,targetLord, a);
                }
            }
        }
    }
    private boolean canLord(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord, targetLord,targetMarket)) return false;
        }
        return true;
    }
}
