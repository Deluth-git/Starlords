package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogAddon_preventAttacksOnTargetLord extends DialogAddon_Base{
    int time;
    @SneakyThrows
    public DialogAddon_preventAttacksOnTargetLord(JSONObject json, String key){
        time = json.getInt(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (lord.getFleet() == null || targetLord == null || targetLord.getFleet() == null) return;
        lord.getFleet().getAI().doNotAttack(targetLord.getFleet(), time);
    }
}
