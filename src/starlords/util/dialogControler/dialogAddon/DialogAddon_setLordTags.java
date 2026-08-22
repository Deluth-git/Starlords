package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogAddon_setLordTags extends DialogAddon_Base{
    ArrayList<String> addData = new ArrayList<>();
    ArrayList<String> removeData = new ArrayList<>();
    @SneakyThrows
    public DialogAddon_setLordTags(JSONObject json){
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            boolean a = json.getBoolean(key2);
            if (a){
                addData.add(key2);
                continue;
            }
            removeData.add(key2);
        }
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord) {
        for (String a : addData){
            if(lord.getLordAPI().hasTag(a)) continue;
            lord.getLordAPI().addTag(a);
        }
        for (String a : removeData){
            if(lord.getLordAPI().hasTag(a)) lord.getLordAPI().removeTag(a);
        }
    }
}
