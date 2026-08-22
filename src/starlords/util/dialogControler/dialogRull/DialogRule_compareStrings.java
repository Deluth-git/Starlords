package starlords.util.dialogControler.dialogRull;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;

public class DialogRule_compareStrings extends DialogRule_Base{
    String[] a = new String[2];
    int[] type = new int[2];
    @SneakyThrows
    public DialogRule_compareStrings(JSONObject json, String key){
        String[] keys = {"a","b"};
        json = json.getJSONObject(key);
        for (int b = 0; b < a.length; b++){
            JSONObject json2 = json.getJSONObject(keys[b]);
            if (json2.has("data")) a[b] = json2.getString("data");
            String typeTemp = json2.getString("type");
            switch (typeTemp){
                case "STATIC":
                    type[b] = 0;
                    break;
                case "DIALOG":
                    type[b] = 1;
                    break;
                case "LORD":
                    type[b] = 2;
                    break;
                case "MEMORY":
                    type[b] = 3;
                    break;
                case "FAV_ITEM":
                    type[b] = 4;
                    break;
            }
        }
    }

    @Override
    public boolean condition(Lord lord, Lord targetLord, MarketAPI market) {
        String b = getString(a[0],type[0],lord,targetLord,market);
        String c = getString(a[1],type[1],lord,targetLord,market);
        return b.equals(c);
    }

    String getString(String data, int type,Lord lord, Lord targetLord, MarketAPI market){
        switch (type){
            case 0:
                return data;
            case 1:
                return LordInteractionDialogPluginImpl.DATA_HOLDER.getString(data);
            case 2:
                return lord.getLordDataHolder().getString(data);
            case 3:
                String out = "";
                if(Global.getSector().getMemory().contains(data) && Global.getSector().getMemory().get(data) instanceof String) out = Global.getSector().getMemory().getString(data);
                return out;
            case 4:
                return lord.getTemplate().preferredItemId;
        }
        return "";
    }
}
