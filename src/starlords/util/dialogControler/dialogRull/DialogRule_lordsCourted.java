package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.controllers.LordController;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;

public class DialogRule_lordsCourted extends DialogRule_minmax {
    @SneakyThrows
    public DialogRule_lordsCourted(JSONObject jsonObject,String key){
        super(jsonObject,key);
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int numCourted = 0;
        for (Lord lord2 : LordController.getLordsList()) {
            if (lord2.isCourted()) numCourted += 1;
        }
        return numCourted;
    }
}
