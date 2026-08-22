package starlords.util.dialogControler;

import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.DialogRule_Base;

import java.util.ArrayList;

public class LordDialogController {
    public String dialogLink;
    ArrayList<DialogRule_Base> rules = new ArrayList<>();
    public LordDialogController(String dialogLink){
        this.dialogLink = dialogLink;
    }
    public LordDialogController(String dialogLink, JSONObject jsonObject){
        this.dialogLink = dialogLink;
        rules = DialogSet.getDialogRulesFromJSon(jsonObject);
    }
    public boolean canUseDialog(Lord lord){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord)) return false;
        }
        return true;
    }
}
