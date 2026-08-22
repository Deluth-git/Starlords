package starlords.util.dialogControler.dialogValues;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.Iterator;

public class DialogValuesList {
    ArrayList<DialogValue_base> values;
    int base = 0;
    double multi=1;
    DialogValuesList multi2 = null;
    @SneakyThrows
    public DialogValuesList(JSONObject json,String key){
        Logger log = Global.getLogger(DialogValuesList.class);
        values = new ArrayList<>();
        if (!(json.get(key) instanceof JSONObject)){
            base = json.getInt(key);
            log.info("getting basic value of: "+base);
            return;
        }
        json = json.getJSONObject(key);
        log.info("getting a single dialogValue list from key of: "+key);
        if (json.has("base")) base = json.getInt("base");
        if (json.has("multi")){
            if (json.get("multi") instanceof JSONObject) {
                multi2 = new DialogValuesList(json,"multi");
            }else{
                multi = json.getDouble("multi");
            }
        }
        for (Iterator it2 = json.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            switch (key2){
                case "relationWithPlayer":
                    values.add(new DialogValue_relationWithPlayer(json,key2));
                    break;
                case "lordLoyalty":
                    values.add(new DialogValue_lordLoyalty(json,key2));
                    break;
                case "lordLoyaltyToPlayerLord":
                    values.add(new DialogValue_lordLoyaltyToPlayerLord(json,key2));
                    break;
                case "playerWealth":
                    values.add(new DialogValue_playerWealth(json,key2));
                    break;
                case "lordWealth":
                    values.add(new DialogValue_lordWealth(json,key2));
                    break;
                case "playerLevel":
                    values.add(new DialogValue_playerLevel(json,key2));
                    break;
                case "lordLevel":
                    values.add(new DialogValue_lordLevel(json,key2));
                    break;
                case "playerFleetDP":
                    values.add(new DialogValue_playerFleetDP(json,key2));
                    break;
                case "lordFleetDP":
                    values.add(new DialogValue_lordFleetDP(json,key2));
                    break;
                case "playerRank":
                    values.add(new DialogValue_playerRank(json,key2));
                    break;
                case "lordRank":
                    values.add(new DialogValue_lordRank(json,key2));
                    break;
                case "playerLordRomanceAction":
                    values.add(new DialogValue_playerLordRomanceAction(json,key2));
                    break;
                case "lordsInFeast":
                    values.add(new DialogValue_lordsInFeast(json,key2));
                    break;
                case "lordProposalSupporters":
                    values.add(new DialogValue_lordProposalSupporters(json,key2));
                    break;
                case "lordProposalOpposers":
                    values.add(new DialogValue_lordProposalOpposers(json,key2));
                    break;
                case "playerProposalSupporters":
                    values.add(new DialogValue_playerProposalSupporters(json,key2));
                    break;
                case "playerProposalOpposers":
                    values.add(new DialogValue_playerProposalOpposers(json,key2));
                    break;
                case "curProposalSupporters":
                    values.add(new DialogValue_curProposalSupporters(json,key2));
                    break;
                case "curProposalOpposers":
                    values.add(new DialogValue_curProposalOpposers(json,key2));
                    break;
                case "optionOfCurrProposal":
                    values.add(new DialogValue_optionOfCurrProposal(json,key2));
                    break;
                case "optionOfPlayerProposal":
                    values.add(new DialogValue_optionOfPlayerProposal(json,key2));
                    break;
                case "playerMarketNumbers":
                    values.add(new DialogValue_playerMarketNumbers(json,key2));
                    break;
                case "lordMarketNumbers":
                    values.add(new DialogValue_lordMarketNumbers(json,key2));
                    break;
                case "playerCommissionedMarketNumbers":
                    values.add(new DialogValue_playerCommissionedMarketNumbers(json,key2));
                    break;
                case "playerMarketAverageStability":
                    values.add(new DialogValue_playerMarketAverageStability(json,key2));
                    break;
                case "lordMarketAverageStability":
                    values.add(new DialogValue_lordMarketAverageStability(json,key2));
                    break;
                case "playerCommissionedMarketAverageStability":
                    values.add(new DialogValue_playerCommissionedMarketAverageStability(json,key2));
                    break;
                case "validLordNumbers":
                    if (json.get(key2) instanceof JSONArray){
                        values.add(new DialogValue_validLordNumbers_Array(json, key2));
                    }else {
                        values.add(new DialogValue_validLordNumbers(json, key2));
                    }
                    break;
                case "limitedValue":
                    if (json.get(key2) instanceof JSONArray){
                        values.add(new DialogValue_limitedValue_Array(json, key2));
                    }else {
                        values.add(new DialogValue_limitedValue(json.getJSONObject(key2)));
                    }
                    break;
                case "conditionalValue":
                    if (json.get(key2) instanceof JSONArray){
                        values.add(new DialogValue_conditionalValue_Array(json, key2));
                    }else {
                        values.add(new DialogValue_conditionalValue(json, key2));
                    }
                    break;
                case "DialogData":
                    values.add(new DialogValue_DialogData_list(json,key2));
                    break;
                case "MemoryData":
                    values.add(new DialogValue_MemoryData_list(json,key2));
                    break;
                case "LordMemoryData":
                    values.add(new DialogValue_LordMemoryData_list(json,key2));
                    break;
                case "random":
                    values.add(new DialogValue_random(json,key2));
                    break;
                case "customDialogValue":
                    values.add(new DialogValue_customList(json,key2));
                    break;
                default:
                    log.info("      failed to add item of "+key2);
            }
            log.info("  added key item of key: "+key2);
        }
    }
    public int getValue(Lord lord, Lord targetLord, MarketAPI targetMarket){
        Logger log = Global.getLogger(DialogValuesList.class);
        log.info("  getting dialog value list value....");
        int base = this.base;
        log.info("      added base as: "+base);
        for (DialogValue_base a : values){
            int temp = a.computeValue(lord,targetLord,targetMarket);
            base+=temp;
            log.info("      adding "+temp+" from source of "+a.getClass().getName()+" for a new total of "+base);
        }
        float multi2 = 1;
        if (this.multi2 != null) multi2 = ((float)(this.multi2.getValue(lord, targetLord, targetMarket))/100);
        return (int) (base*multi*(multi2));
    }
}
