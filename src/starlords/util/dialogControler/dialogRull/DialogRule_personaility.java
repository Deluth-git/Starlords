package starlords.util.dialogControler.dialogRull;

import starlords.person.Lord;
import starlords.person.LordPersonality;

import java.util.ArrayList;

public class DialogRule_personaility extends DialogRule_Base {
    ArrayList<String> data;
    boolean[] personalitys = new boolean[4];
    public DialogRule_personaility(ArrayList<String> data){
        this.data = data;
        if (data.contains("Upstanding")){
            personalitys[0] = true;
        }
        if (data.contains("Quarrelsome")){
            personalitys[1] = true;
        }
        if (data.contains("Calculating")){
            personalitys[2] = true;
        }
        if (data.contains("Martial")){
            personalitys[3] = true;
        }
    }
    @Override
    public boolean condition(Lord lord) {
        lord.getPersonality();
        switch (lord.getPersonality()){
            case UPSTANDING:
                return personalitys[0];
            case QUARRELSOME:
                return personalitys[1];
            case CALCULATING:
                return personalitys[2];
            case MARTIAL:
                return personalitys[3];
        }
        return false;
    }
}
