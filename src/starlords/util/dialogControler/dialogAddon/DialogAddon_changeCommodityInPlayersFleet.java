package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_changeValue;

import java.util.HashMap;

public class DialogAddon_changeCommodityInPlayersFleet extends DialogAddon_changeValue{
    String item;
    public DialogAddon_changeCommodityInPlayersFleet(JSONObject json, String key) {
        super(json, key);
        this.item=key;
    }

    @Override
    protected void increaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        Global.getSector().getPlayerFleet().getCargo().addCommodity(item,(value));
        HashMap<String,String> inserts = new HashMap<>();
        inserts.put("%c0",""+value);
        inserts.put("%c1",Global.getSector().getEconomy().getCommoditySpec(item).getName());
        DialogSet.addParaWithInserts("item_added",lord,targetLord,targetMarket,textPanel,options,dialog,false,inserts);
    }

    @Override
    protected void decreaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        value*=-1;
        value = (int) Math.min(value,Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(item));
        Global.getSector().getPlayerFleet().getCargo().removeCommodity(item,(value));

        HashMap<String,String> inserts = new HashMap<>();
        inserts.put("%c0",""+value);
        inserts.put("%c1",Global.getSector().getEconomy().getCommoditySpec(item).getName());
        DialogSet.addParaWithInserts("item_lost",lord,targetLord,targetMarket,textPanel,options,dialog,false,inserts);
    }

    @Override
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return (int) Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(item);
    }
}
