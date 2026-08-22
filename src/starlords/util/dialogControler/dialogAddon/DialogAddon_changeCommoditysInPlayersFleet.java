package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogAddon.bases.DialogAddon_changeValue;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogAddon_changeCommoditysInPlayersFleet extends DialogAddon_Base{
    ArrayList<DialogAddon_changeValue> changes = new ArrayList<>();
    @SneakyThrows
    public DialogAddon_changeCommoditysInPlayersFleet(JSONObject json, String key){
        json = json.getJSONObject(key);
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            changes.add(new DialogAddon_changeCommodityInPlayersFleet(json,key2));
        }
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        for (DialogAddon_changeValue a : changes){
            a.apply(textPanel, options, dialog, lord, targetLord,targetMarket);
        }
    }
}
