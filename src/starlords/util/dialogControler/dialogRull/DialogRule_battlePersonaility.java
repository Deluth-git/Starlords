package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

import java.util.ArrayList;

public class DialogRule_battlePersonaility extends DialogRule_Base {
    ArrayList<String> data;
    public DialogRule_battlePersonaility(ArrayList<String> data){
        this.data = data;
    }
    @Override
    public boolean condition(Lord lord) {
        for (String a : data) {
            if (a.equals(lord.getLordAPI().getPersonalityAPI().getDisplayName())) {
                return true;
            }
        }
        return false;
    }
}
