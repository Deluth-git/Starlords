package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.person.Lord;

public class DialogRule_Base {
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market){
        return condition(lord,targetLord);
    }
    public boolean condition(Lord lord,Lord targetLord){
        return condition(lord);
    }
    public boolean condition(Lord lord){
        return true;
    }
}
