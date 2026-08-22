package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_changeValue;

public class DialogAddon_romanceChange extends DialogAddon_changeValue {
    public DialogAddon_romanceChange(JSONObject json, String key) {
        super(json, key);
    }

    @Override
    protected void increaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        lord.setRomanticActions(lord.getRomanticActions()+value);
    }

    @Override
    protected void decreaseChange(int value, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        lord.setRomanticActions(lord.getRomanticActions()+value);
    }

    @Override
    protected int getCurrentValue(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return lord.getRomanticActions();
    }
}
