package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogAddon_preventAttacksOnPlayer extends DialogAddon_Base{
    int time;
    @SneakyThrows
    public DialogAddon_preventAttacksOnPlayer(JSONObject json, String key){
        time = json.getInt(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (lord.getFleet() == null) return;
        lord.getFleet().getAI().doNotAttack(Global.getSector().getPlayerFleet(), time);
        Misc.setFlagWithReason(lord.getFleet().getMemoryWithoutUpdate(),
                MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, "starlords", true, time);
    }
}
