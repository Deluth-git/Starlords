package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;

import java.util.ArrayList;

public class DialogRule_LordTags_blacklist extends DialogRule_Base {
    ArrayList<String> data;
    public DialogRule_LordTags_blacklist(ArrayList<String> data){
        this.data = data;
    }
    @Override
    public boolean condition(Lord lord) {
        for (String a : data){
            if (lord.getLordAPI().hasTag(a)){
                return false;
            }
        }
        return true;
    }
}
