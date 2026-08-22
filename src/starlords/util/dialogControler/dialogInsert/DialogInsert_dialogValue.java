package starlords.util.dialogControler.dialogInsert;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogInsert_dialogValue extends DialogInsert_Base{
    DialogValuesList list;
    @SneakyThrows
    public DialogInsert_dialogValue(JSONObject json){
        key = json.getString("replaced");
        list = new DialogValuesList(json,"dialogValue");
    }

    @Override
    public String getInsertedData(String line, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return ""+list.getValue(lord, targetLord, targetMarket);
    }
}
