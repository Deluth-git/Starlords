package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import org.json.JSONObject;
import starlords.person.Lord;

public class DialogAddon_custom extends DialogAddon_Base{
    public JSONObject json;
    public DialogAddon_custom(String id){
        DialogAddon_customList.addons.put(id,this);
    }
    @Override
    @Deprecated
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord) {
        super.apply(textPanel, options, dialog, lord, targetLord);
    }
    @Override
    @Deprecated
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord) {
        super.apply(textPanel, options, dialog, lord);
    }
}
