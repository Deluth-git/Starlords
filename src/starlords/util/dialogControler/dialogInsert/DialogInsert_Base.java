package starlords.util.dialogControler.dialogInsert;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import starlords.person.Lord;
import starlords.util.dialogControler.DialogSet;

public class DialogInsert_Base {
    protected String key;

    public String insert(String line,Lord lord, Lord targetLord, MarketAPI targetMarket){
        return DialogSet.insertData(line,key,getInsertedData(line, lord, targetLord, targetMarket));
    }
    public String getInsertedData(String line,Lord lord, Lord targetLord, MarketAPI targetMarket){
        return "";
    }
}
