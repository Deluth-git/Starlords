package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogAddon_playSound extends DialogAddon_Base {
    String soundID;
    DialogValuesList pitch;
    DialogValuesList volume;
    @SneakyThrows
    public DialogAddon_playSound(JSONObject json, String key){
        if (json.get(key) instanceof JSONObject){
            soundID = json.getJSONObject(key).getString("soundID");
            if (json.getJSONObject(key).has("pitch")) pitch = new DialogValuesList(json.getJSONObject(key),"pitch");
            if (json.getJSONObject(key).has("volume")) volume = new DialogValuesList(json.getJSONObject(key),"volume");
            return;
        }
        soundID = json.getString(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int pitch = 1;
        int volume = 1;
        if (this.pitch != null) pitch = this.pitch.getValue(lord, targetLord,targetMarket);
        if (this.volume != null) volume = this.volume.getValue(lord, targetLord,targetMarket);
        Global.getSoundPlayer().playUISound(soundID, pitch, volume);
    }
}
