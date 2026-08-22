package starlords.util.SModSupport;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.json.JSONObject;
import starlords.person.Lord;
import starlords.util.SModSupport.SModRull.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SModSet {
    @Getter
    private HashMap<String,Integer> SMods = new HashMap<>();
    private ArrayList<SModRule_Base> rules = new ArrayList<>();
    @Getter
    private static ArrayList<SModSet> sets = new ArrayList<>();

    @SneakyThrows
    public static void applySModSets(JSONObject jsonObject){
        sets = new ArrayList<SModSet>();
        for (Iterator it2 = jsonObject.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            new SModSet(jsonObject.getJSONObject(key2));
        }
    }
    public SModSet(){
        sets.add(this);
    }
    @SneakyThrows
    public SModSet(JSONObject jsonObject){
        if (jsonObject.has("rules")){
            JSONObject rulesAdded = jsonObject.getJSONObject("rules");
            for (Iterator it = rulesAdded.keys(); it.hasNext();) {
                String key = (String) it.next();
                switch (key) {
                    case "hullmods":
                        addRule_HullMods(rulesAdded,key);
                        break;
                    case "manufacture":
                        addRule_manufacture(rulesAdded,key);
                        break;
                    case "lordTags":
                        addRule_lordTags(rulesAdded,key);
                        break;
                    case "system":
                        addRule_system(rulesAdded,key);
                        break;
                    case "startingFaction":
                        addRule_startingFaction(rulesAdded,key);
                        break;
                    case "currentFaction":
                        addRule_currentFaction(rulesAdded,key);
                        break;
                    case "hullID":
                        addRule_hullID(rulesAdded,key);
                        break;
                    case "fighterBays":
                        addRule_fighterBays(rulesAdded,key);
                        break;
                    case "defenseType":
                        addRule_defenseType(rulesAdded,key);
                        break;
                    case "size":
                        addRule_size(rulesAdded,key);
                        break;
                    default:
                        break;
                }
            }
            sets.add(this);
        }
        if (jsonObject.has("S-Mods")){
            JSONObject rulesAdded = jsonObject.getJSONObject("S-Mods");
            for (Iterator it = rulesAdded.keys(); it.hasNext();) {
                String key = (String) it.next();
                SMods.put(key,rulesAdded.getInt(key));
            }
        }
    }
    public void addRule(SModRule_Base rule){
        rules.add(rule);
    }
    public void addSMod(String SMod, int weight){
        SMods.put(SMod,weight);
    }
    public boolean canAddMods(FleetMemberAPI member, Lord lord){
        for (SModRule_Base a : rules){
            if (!a.canAdd(member, lord)) return false;
        }
        return true;
    }


    @SneakyThrows
    private void addRule_HullMods(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_hullmods_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_hullmods_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_manufacture(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_manufacture_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_manufacture_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_lordTags(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_lordTags_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_lordTags_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_system(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_system_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_system_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_startingFaction(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_startingFaction_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_startingFaction_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_currentFaction(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_CurrentFaction_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_CurrentFaction_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_hullID(JSONObject rulesAdded, String key){
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = rulesAdded.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new SModRule_HullID_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new SModRule_HullID_blacklist(blackList));
    }
    @SneakyThrows
    private void addRule_fighterBays(JSONObject rulesAdded, String key){
        rules.add(new SModRule_fighterBays(rulesAdded.getJSONObject(key)));
    }
    @SneakyThrows
    private void addRule_defenseType(JSONObject rulesAdded, String key){
        rules.add(new SModRule_defenseType(rulesAdded.getJSONObject(key)));
    }
    @SneakyThrows
    private void addRule_size(JSONObject rulesAdded, String key){
        rules.add(new SModRule_size(rulesAdded.getJSONObject(key)));
    }
}
