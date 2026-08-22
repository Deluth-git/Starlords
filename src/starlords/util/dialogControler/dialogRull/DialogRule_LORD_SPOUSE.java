package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;

import java.util.ArrayList;

public class DialogRule_LORD_SPOUSE  extends DialogRule_Base {
    ArrayList<DialogRule_Base> rules;
    @SneakyThrows
    public DialogRule_LORD_SPOUSE(JSONObject jsonObject){
        rules = DialogSet.getDialogRulesFromJSon(jsonObject);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        String id = lord.getSpouse();
        if (id == null) return false;
        Lord activeLord = LordController.getLordById(id);
        if (activeLord == null) return false;
        return rulesWork(activeLord, targetLord,targetMarket);
    }

    @Override
    public boolean condition(Lord lord) {
        return false;
    }
    private boolean rulesWork(Lord lord, Lord targetLord,MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,targetLord,targetMarket)) return false;
        }
        return true;
    }

}
