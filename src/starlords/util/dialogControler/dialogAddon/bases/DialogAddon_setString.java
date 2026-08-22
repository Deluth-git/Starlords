package starlords.util.dialogControler.dialogAddon.bases;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogAddon.DialogAddon_Base;
import starlords.util.dialogControler.dialogInsert.DialogInsertList;

public class DialogAddon_setString extends DialogAddon_Base {
    public DialogInsertList inserts = null;
    public String string="";
    public DialogAddon_setString(){

    }
    public DialogAddon_setString(JSONObject json, String key){
        init(json, key);
    }
    @SneakyThrows
    public void init(JSONObject json, String key){
        if (json.get(key) instanceof JSONObject){
            json = json.getJSONObject(key);
            inserts = new DialogInsertList(json,"customInserts");
            string = json.getString("String");
            return;
        }
        string = json.getString(key);
    }
    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        String line = DialogSet.insertDefaltData(string,lord,targetLord,targetMarket,true);
        if (inserts != null)inserts.insertData(line,lord,targetLord,targetMarket);
        applyString(line,textPanel,options,dialog,lord,targetLord,targetMarket);
    }
    protected void applyString(String string,TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket){

    }
}
