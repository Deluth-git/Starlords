package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.dialogControler.dialogRull.bases.DialogRule_minmax;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogRule_baseValue extends DialogRule_minmax {
    DialogValuesList value;
    @SneakyThrows
    public DialogRule_baseValue(JSONObject jsonObject, String key) {
        super(jsonObject, key);
        value = new DialogValuesList(jsonObject.getJSONObject(key),"value");
    }

    @Override
    protected int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        return value.getValue(lord, targetLord,targetMarket);
    }
}
