package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;

import java.util.ArrayList;

public class DialogAddon_additionalText extends DialogAddon_Base{
    ArrayList<String> lineID = new ArrayList<>();
    @SneakyThrows
    public DialogAddon_additionalText(JSONObject json, String key){
        boolean isArray = true;
        try{
            json.getJSONArray(key);
        }catch (Exception e){
            isArray = false;
        }
        if (isArray){
            JSONArray json2 = json.getJSONArray(key);
            for (int a = 0; a < json2.length(); a++){
                lineID.add(json2.getString(a));
            }
        }else{
            lineID.add(json.getString(key));
        }
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        for (String a : lineID) {
            DialogSet.addParaWithInserts(a, lord,targetLord,targetMarket, textPanel, options, dialog);
        }
    }
}
