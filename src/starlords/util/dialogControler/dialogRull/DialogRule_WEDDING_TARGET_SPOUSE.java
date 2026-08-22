package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;

import java.util.ArrayList;

public class DialogRule_WEDDING_TARGET_SPOUSE  extends DialogRule_Base {
    ArrayList<DialogRule_Base> rules;
    @SneakyThrows
    public DialogRule_WEDDING_TARGET_SPOUSE(JSONObject jsonObject){
        rules = DialogSet.getDialogRulesFromJSon(jsonObject);
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord,MarketAPI targetMarket) {
        boolean check = EventController.getCurrentFeast(lord.getLordAPI().getFaction()) != null;
        if (!check) return false;
        Lord activeLord = EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getWeddingCeremonyTarget();
        if (activeLord == null) return false;
        String id = activeLord.getSpouse();
        if (id == null) return false;
        activeLord = LordController.getLordById(id);
        if (activeLord == null) return false;
        return rulesWork(activeLord, targetLord,targetMarket);
    }

    @Override
    public boolean condition(Lord lord) {
        return false;
    }
    private boolean rulesWork(Lord lord, Lord targetLord, MarketAPI market){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,targetLord,market)) return false;
        }
        return true;
    }

}
