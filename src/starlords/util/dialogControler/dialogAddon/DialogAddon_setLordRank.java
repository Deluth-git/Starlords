package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogAddon_setLordRank extends DialogAddon_Base {
    int rank;
    @SneakyThrows
    public DialogAddon_setLordRank(JSONObject json, String key){
        rank = json.getInt(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord) {
        lord.setRanking(rank);
    }
}
