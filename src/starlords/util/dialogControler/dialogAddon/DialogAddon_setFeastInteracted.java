package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.person.Lord;
import starlords.person.LordAction;

public class DialogAddon_setFeastInteracted extends DialogAddon_Base{
    boolean bol;
    @SneakyThrows
    public DialogAddon_setFeastInteracted(JSONObject json, String key){
        bol = json.getBoolean(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (EventController.getCurrentFeast(lord.getLordAPI().getFaction()) == null || lord.getCurrAction() != LordAction.FEAST) return;
        lord.setFeastInteracted(bol);
    }
}
