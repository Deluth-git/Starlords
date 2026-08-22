package starlords.util.dialogControler.dialogAddon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.DefectionUtils;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogValues.DialogValuesList;

public class DialogAddon_defectLordToFaction extends DialogAddon_Base{
    String faction;
    DialogValuesList rank;
    boolean takeFiefs=true;
    @SneakyThrows
    public DialogAddon_defectLordToFaction(JSONObject json, String key){
        if (json.get(key) instanceof JSONObject){
            JSONObject json2 = json.getJSONObject(key);
            faction = json2.getString("factionID");
            if (json2.has("newRank")) rank = new DialogValuesList(json2,"newRank");
            if (json2.has("includeFiefs")) takeFiefs = json2.getBoolean("includeFiefs");
            return;
        }
        faction = json.getString(key);
    }

    @Override
    public void apply(TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, Lord lord, Lord targetLord, MarketAPI targetMarket) {
        int rank = 0;
        if (this.rank != null) rank = this.rank.getValue(lord, targetLord,targetMarket);
        rank = Math.max(0,Math.min(rank,2));
        FactionAPI targetFaction;
        if (!faction.equals("playerCurrFaction")){
            targetFaction = Global.getSector().getFaction(faction);
        } else{
            targetFaction = Utils.getRecruitmentFaction();
        }
        DefectionUtils.performDefection(lord,targetFaction,false,takeFiefs);
        lord.setRanking(rank);
    }
}
