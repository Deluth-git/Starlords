package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.Utils;

public class DialogValue_random extends DialogValue_base{
    DialogValuesList baseRange;
    DialogValuesList min;
    DialogValuesList max;
    @SneakyThrows
    public DialogValue_random(JSONObject json, String key) {
        super(json, key);
        if (!(json.get(key) instanceof JSONObject)){
            baseRange = new DialogValuesList(json,key);
            this.base=0;
            this.multi=1;
            return;
        }
        if (json.getJSONObject(key).has("min")) min = new DialogValuesList(json.getJSONObject(key),"min");
        if (json.getJSONObject(key).has("max")) max = new DialogValuesList(json.getJSONObject(key),"max");
        if (!json.getJSONObject(key).has("max") && !json.getJSONObject(key).has("min")){
            baseRange = new DialogValuesList(json,key);
        }
        this.base=0;
        this.multi=1;
    }

    @Override
    public int value(Lord lord, Lord targetLord, MarketAPI targetMarket) {
        if (baseRange != null){
            int min = 0;
            int max = baseRange.getValue(lord, targetLord,targetMarket);
            if (max < 0){
                min = max;
                max = 0;
            }
            int range = max - min;
            if (range == 0) return 0;
            if (range < 0){
                return Utils.rand.nextInt(range*-1)*-1;
            }
            return Utils.rand.nextInt(range);
        }
        int min = 0;
        int max = 0;
        if (this.min != null) min = this.min.getValue(lord, targetLord,targetMarket);
        if (this.max != null) max = this.max.getValue(lord, targetLord,targetMarket);
        max = Math.max(min,max);
        int range = max-min;
        boolean negitige=false;
        if (range < 0){
            negitige=true;
            range*=-1;
        }
        if (range != 0) range = Utils.rand.nextInt(range);
        if (negitige) range *=-1;
        return min+range;


        /*int base = 1;
        int min = 0;
        int max = 0;
        if (this.min != null) min = this.min.getValue(lord, targetLord);
        if (this.max != null) max = this.max.getValue(lord, targetLord);
        max = Math.max(min,max);
        int range = max-min;
        boolean negitige=false;
        if (range < 0){
            negitige=true;
        }*/
    }
}
