package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_changeValue;

import java.util.HashMap;

public class DialogAddon_creditsChange extends DialogAddon_changeValue {
    public DialogAddon_creditsChange(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    protected void increaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        Global.getSector().getPlayerFleet().getCargo().getCredits().add(value);

        HashMap<String,String> inserts = new HashMap<>();
        inserts.put("%c0",""+value);
        DialogSet.addParaWithInserts("credits_increase",lord,targetLord,targetMarket,textPanel,options,dialog,false,inserts);
    }

    @Override
    protected void decreaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        value*=-1;
        value = Math.min((int) value, (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get());
        Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(value);

        HashMap<String,String> inserts = new HashMap<>();
        inserts.put("%c0",""+value);
        DialogSet.addParaWithInserts("credits_decrease",lord,targetLord,targetMarket,textPanel,options,dialog,false,inserts);
    }

    @Override
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
    }
}
