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

public class DialogAddon_changeLordCredits extends DialogAddon_changeValue {
    public DialogAddon_changeLordCredits(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    protected void increaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        lord.addWealth(value*-1);
    }

    @Override
    protected void decreaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        lord.addWealth(value);
    }

    @Override
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return (int) lord.getWealth();
    }
}
