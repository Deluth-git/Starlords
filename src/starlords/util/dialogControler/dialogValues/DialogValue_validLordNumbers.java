package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONException;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;

public class DialogValue_validLordNumbers extends DialogValue_base{
    ArrayList<DialogRule_Base> rules;

    public DialogValue_validLordNumbers(JSONObject json) throws JSONException {
        super(json);
        rules = DialogSet.getDialogRulesFromJSon(json.getJSONObject("rules"));
    }
    public DialogValue_validLordNumbers(JSONObject json, String key) throws JSONException {
        this(json.getJSONObject(key));
    }

    @Override
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int value = 0;
        for (Lord a : LordController.getLordsList()){
            if (can(a,lord,targetMarket)) value++;
        }
        return value;
    }
    private boolean can(Lord lord, Lord targetLord, MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,targetLord,targetMarket)) return false;
        }
        return true;
    }
}
