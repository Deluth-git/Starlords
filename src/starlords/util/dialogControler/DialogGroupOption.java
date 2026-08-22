package starlords.util.dialogControler;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;
import java.util.HashMap;

public class DialogGroupOption {
    public String type = "targetLord";
    public String line;
    private ArrayList<DialogRule_Base> rules;
    @SneakyThrows
    public DialogGroupOption(JSONObject json){
        rules = DialogSet.getDialogRulesFromJSon(json.getJSONObject("rules"));
        line = json.getString("optionData");
        if (json.has("type")) type = json.getString("type");
        /*
         * "targetLord". this advanced option runs the 'rules' with the 'lord' as this lord. clicking on an option sets the dialogs target lord to this lord.
         * "targetMarket". this advanced option runs the 'market rules', clicking on an option sets the target market to this market.
         */
    }
    public boolean canAddLord(Lord lord, Lord pastLord,MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,pastLord,targetMarket)) return false;
        }
        return true;
    }
    public boolean canAddMarket(Lord lord, Lord pastLord,MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,pastLord,targetMarket)) return false;
        }
        return true;
    }
    public void applyOptions(Lord pastLord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, HashMap<String,String> markersReplaced){
        switch (type) {
            case "targetLord" -> applyOptionLords(pastLord,targetMarket, textPanel, options, dialog, markersReplaced);
            case "targetMarket" -> applyOptionMarket(pastLord,targetLord, textPanel, options, dialog, markersReplaced);
        }
    }
    public void applyOptionLords(Lord pastLord, MarketAPI targetMarket, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, HashMap<String,String> markersReplaced){
        Logger log = Global.getLogger(DialogGroupOption.class);
        ArrayList<Lord> goodLords = new ArrayList<>();
        for (Lord lord : LordController.getLordsList()){
            if (canAddLord(pastLord,lord,targetMarket)){
                goodLords.add(lord);
            }
        }
        Utils.canonicalLordSort(goodLords);
        for (Lord lord : goodLords){
            log.info("  adding a group option of key, lord: "+ line +", "+lord.getLordAPI().getNameString());
            DialogSet.addOptionWithInserts(line,pastLord,lord,targetMarket,textPanel,options,dialog,markersReplaced);
        }
    }
    public void applyOptionMarket(Lord lord,Lord targetLord, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, HashMap<String,String> markersReplaced){
        ArrayList<MarketAPI> goodMarkets = new ArrayList<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()){
            if (canAddMarket(lord,targetLord,market)){
                goodMarkets.add(market);
            }
        }
        for (MarketAPI market : goodMarkets){
            DialogSet.addOptionWithInserts(line,lord,targetLord,market,textPanel,options,dialog,markersReplaced);
        }
    }
}
