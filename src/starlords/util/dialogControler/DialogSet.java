package starlords.util.dialogControler;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawProposal;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.Constants;
import starlords.util.GenderUtils;
import starlords.util.Utils;
import starlords.util.dialogControler.dialogInsert.DialogInsertList;
import starlords.util.dialogControler.dialogRull.*;
import starlords.util.dialogControler.dialogAddon.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DialogSet {
    //for getting dialog types with a search
    //private static HashMap<String,DialogSet> dialogSets = new HashMap<>();
    //for getting organized dialog data
    //private static ArrayList<ArrayList<DialogSet>> organizedDialogSets = new ArrayList<>();
    //for getting dialog with a search
    public static HashMap<String,LordDialog> dialogs = new HashMap<>();
    //for getting organized dialog data
    public static ArrayList<ArrayList<LordDialog>> organizedDialogs = new ArrayList<>();
    //for getting the default option set of any given line.
    private static HashMap<String,String> defaltOptionSets = new HashMap<>();
    private static LordDialog backup;
    @SneakyThrows
    public static void setup(){
        setupA();
        setupB();
    }
    @SneakyThrows
    private static void setupA(){
        JSONObject jsonObject = Global.getSettings().getMergedJSONForMod("data/lords/dialog/dialog.json", Constants.MOD_ID);
        dialogs = new HashMap<>();
        organizedDialogs = new ArrayList<>();
        for (Iterator it2 = jsonObject.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            new LordDialog(key2,jsonObject.getJSONObject(key2));
        }
    }
    @SneakyThrows
    private static void setupB(){
        JSONObject jsonObject = Global.getSettings().getMergedJSONForMod("data/lords/dialog/default_dialog_options.json",Constants.MOD_ID);
        defaltOptionSets = new HashMap<>();
        for (Iterator it2 = jsonObject.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            defaltOptionSets.put(key2,jsonObject.getString(key2));
        }
    }

    public static void addParaWithInserts(String key, Lord lord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel,OptionPanelAPI options, InteractionDialogAPI dialog){
        addParaWithInserts(key, lord,targetLord,targetMarket, textPanel, options,dialog,false);
    }
    public static void addParaWithInserts(String key, Lord lord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel,OptionPanelAPI options, InteractionDialogAPI dialog,boolean forceHide){
        addParaWithInserts(key, lord,targetLord,targetMarket, textPanel, options,dialog,forceHide,new HashMap<String,String>());
    }
    public static void addParaWithInserts(String key, Lord lord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel,OptionPanelAPI options, InteractionDialogAPI dialog,boolean forceHide,HashMap<String,String> markersReplaced) {
        key = attemptKeyRedirection(key);
        DialogSet set = getSet(lord,targetLord,targetMarket, key);
        if (set == null) {
            Logger log = Global.getLogger(DialogSet.class);
            log.info("ERROR: FAILED TO GET DIALOG SET OF KEY: "+key);
            return;
        }
        set.applyLine(key, lord,targetLord,targetMarket, textPanel, options,dialog,forceHide,markersReplaced);
    }

    public static void addOptionWithInserts(String key, Lord lord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, HashMap<String,String> markersReplaced){
        key = attemptKeyRedirection(key);
        DialogSet set = getSet(lord,targetLord,targetMarket, key);
        if (set == null) return;
        set.applyOption(key,  lord,targetLord, targetMarket,  textPanel,  options,dialog, markersReplaced);
    }

    private static DialogSet getSet(Lord lord,Lord targetLord, MarketAPI targetMarket,String id){
        Logger log = Global.getLogger(DialogSet.class);
        log.info("attempting to get set line ID of: "+id);
        if (lord.getTemplate().dialogOverride != null) {
            for (String a : lord.getTemplate().dialogOverride) {
                DialogSet out = dialogs.get(a).getSet(lord, targetLord, targetMarket, id);
                if (out != null) return out;
            }
        }
        for (int a = organizedDialogs.size() - 1; a > 0; a--){
            //never runs Dialogs with a priority of 0. they are reserved for dialogOverrides.
            for(LordDialog b : organizedDialogs.get(a)) {
                DialogSet out = b.getSet(lord,targetLord,targetMarket,id);
                if (out != null) return out;
            }
        }
        DialogSet out = dialogs.get("default").getSet(lord,targetLord,targetMarket,id);
        if (out != null) return out;
        return null;
    }
    private static String attemptKeyRedirection(String key){
        if (key == null) return key;
        if (key.equals("optionSet_beginning")) return LordInteractionDialogPluginImpl.startingDialogSet;
        return key;
    }

    public static String getLine(Lord lord,Lord targetLord, MarketAPI targetMarket,String id){
        DialogSet set = getSet(lord,targetLord,targetMarket, id);
        if (set != null) return set.getLine(id);
        return "ERROR: unable to get line of dialog. id of: "+id;
    }
    @Deprecated
    public static String getLineWithInserts(Lord lord,String id){
        //String line = getLine(lord,null,id);
        return "";//insertDefaltData(line,lord);
    }
    public static String insertData(String line, String mark, String replace){
        /*StringBuilder out = new StringBuilder();
        if (markAtStart(line,mark)){
            out.append(replaced);
        }
        String[] lines = line.split(mark);
        for (String a : lines){
            out.append(a);
            out.append(replaced);
        }
        if (markAtEnd(line,mark)){
            out.append(replaced);
        }*/
        return line.replaceAll(mark,replace);
    }
    public static String insertAdditionalData(String line,HashMap<String,String> markersReplaced){
        for (String a : markersReplaced.keySet()){
            line = insertData(line,a,markersReplaced.get(a));
        }
        return line;
    }
    public static String insertDefaltData(String line, Lord lord,Lord targetLord,MarketAPI targetMarket,boolean clone){
        if (!clone) return insertDefaltData(line, lord, targetLord, targetMarket);
        StringBuilder lineTemp = new StringBuilder();
        for (char a : line.toCharArray()){
            lineTemp.append(a);
        }
        line = lineTemp.toString();
        return insertDefaltData(line, lord, targetLord, targetMarket);
    }
    public static String insertDefaltData(String line, Lord lord,Lord targetLord,MarketAPI targetMarket){
        line = getPlayerStringMods(line,lord);
        line = getPlayerPartnerStringMods(line,lord);

        line = getLordStringMods(line,lord);
        line = getLordPartnerStringMods(line,lord);

        line = getHostLordStringMods(line,lord);
        line = getHostLordPartnerStringMods(line,lord);

        line = getWeddingTargetLordStringMods(line,lord);
        line = getWeddingTargetPartnerStringMods(line,lord);

        line = getSecondLordStringMods(line,targetLord);
        line = getSecondLordPartnerStringMods(line,targetLord);

        line = getTargetMarketStringMods(line,targetMarket);
        return line;
    }

    private static String getPlayerStringMods(String line, Lord lord){
        String data = Utils.getRecruitmentFaction().getDisplayName();
        line = insertData(line,"%PLAYER_FACTION_NAME",data);

        data = Global.getSector().getPlayerPerson().getNameString();
        line = insertData(line,"%PLAYER_NAME",data);

        data = Global.getSector().getPlayerPerson().getName().getFirst();
        line = insertData(line,"%PLAYER_NAME_FIRST",data);

        data = Global.getSector().getPlayerPerson().getName().getLast();
        line = insertData(line,"%PLAYER_NAME_LAST",data);

        data = LordController.getPlayerLord().getTitle();
        if (data == null) data = "";
        line = insertData(line,"%PLAYER_TITLE",data);

        data = Global.getSector().getPlayerPerson().getManOrWoman();
        line = insertData(line,"%PLAYER_GENDER_MAN_OR_WOMEN",data);

        data = Global.getSector().getPlayerPerson().getHeOrShe();
        line = insertData(line,"%PLAYER_GENDER_HE_OR_SHE",data);

        data = Global.getSector().getPlayerPerson().getHimOrHer();
        line = insertData(line,"%PLAYER_GENDER_HIM_OR_HER",data);

        data = Global.getSector().getPlayerPerson().getHisOrHer();
        line = insertData(line,"%PLAYER_GENDER_HIS_OR_HER",data);

        data = LordController.getPlayerLord().getFormalWear();
        line = insertData(line,"%PLAYER_GENDER_SUIT_OR_DRESS",data);

        data = GenderUtils.sirOrMaam(Global.getSector().getPlayerPerson(), false);
        line = insertData(line,"%PLAYER_GENDER_SIR_OR_MAAM",data);

        data = Global.getSector().getPlayerPerson().getGender().name();
        line = insertData(line,"%PLAYER_GENDER_NAME",data);

        data = Global.getSector().getPlayerPerson().getGender().name();
        line = insertData(line,"%PLAYER_GENDER_NAME",data);

        FleetMemberAPI flownShip = getFlownShip(Global.getSector().getPlayerPerson(),Global.getSector().getPlayerFleet());
        data = "nothing";//todo: move this, like everything else, into the strings file. for modality of different languages.
        if (flownShip != null) data = flownShip.getHullSpec().getHullName();
        line = insertData(line,"%PLAYER_FLAGSHIP_HULLNAME",data);

        data = "nothing";//todo: move this, like everything else, into the strings file. for modality of different languages.
        if (flownShip != null) data = flownShip.getShipName();
        line = insertData(line,"%PLAYER_FLAGSHIP_NAME",data);

        data = GenderUtils.husbandOrWife(Global.getSector().getPlayerPerson(), false);
        line = insertData(line,"%PLAYER_GENDER_HUSBAND_OR_WIFE",data);

        data = "nothing";
        LawProposal lordProposal = PoliticsController.getProposal(lord);
        if (lordProposal != null) data = lordProposal.getTitle();
        line = insertData(line,"%PLAYER_PROPOSAL_NAME",data);

        data = "nowhere";
        if (Global.getSector().getPlayerFleet() != null && Utils.getNearbyDescription(Global.getSector().getPlayerFleet()) != null) data = Utils.getNearbyDescription(Global.getSector().getPlayerFleet());
        line = insertData(line,"%PLAYER_FLEET_LOCATION",data);

        data = "noone";
        if (LordController.getPlayerLord().getLiegeName() != null) data = LordController.getPlayerLord().getLiegeName();
        line = insertData(line,"%PLAYER_LIEGE_NAME",data);

        data = Utils.getFactionTitle(lord.getFaction().getId(),0);
        line = insertData(line,"%PLAYER_FACTION_RANK_TITLE0",data);

        data = Utils.getFactionTitle(lord.getFaction().getId(),1);
        line = insertData(line,"%PLAYER_FACTION_RANK_TITLE1",data);

        data = Utils.getFactionTitle(lord.getFaction().getId(),2);
        line = insertData(line,"%PLAYER_FACTION_RANK_TITLE2",data);
        return line;
    }
    private static String getPlayerPartnerStringMods(String line, Lord lord){
        if (LordController.getPlayerLord().getSpouse() != null) return get_Person_StringMods(line,LordController.getLordOrPlayerById(LordController.getPlayerLord().getSpouse()),"PLAYER_SPOUSE_");
        return get_null_Person_StringMods(line,"PLAYER_SPOUSE_");
    }
    private static String getLordStringMods(String line, Lord lord){
        return get_Person_StringMods(line,lord,"LORD_");
    }
    private static String getLordPartnerStringMods(String line, Lord lord){
        if (lord.getSpouse() == null) return get_null_Person_StringMods(line,"LORD_SPOUSE_");
        lord = LordController.getLordOrPlayerById(lord.getSpouse());
        return get_Person_StringMods(line,lord,"LORD_SPOUSE_");
    }
    private static String getHostLordStringMods(String line, Lord lord){
        boolean check = EventController.getCurrentFeast(lord.getLordAPI().getFaction()) != null && EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator() != null;
        if (check){
            Lord temp = EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator();
            if (temp != null){
                lord = temp;
                return get_Person_StringMods(line,lord,"LORD_HOST_");
            }
        }
        return get_null_Person_StringMods(line,"LORD_HOST_");
    }
    private static String getHostLordPartnerStringMods(String line, Lord lord){
        boolean check = EventController.getCurrentFeast(lord.getLordAPI().getFaction()) != null && EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator() != null;
        if (check){
            Lord temp = EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getOriginator();
            if (temp != null) {
                lord = temp;
                if (lord.getSpouse() != null){
                    lord = LordController.getLordOrPlayerById(lord.getSpouse());
                    return get_Person_StringMods(line,lord,"LORD_HOST_SPOUSE_");
                }
            }
        }
        return get_null_Person_StringMods(line,"LORD_HOST_SPOUSE_");
    }
    private static String getWeddingTargetLordStringMods(String line, Lord lord){
        boolean check = EventController.getCurrentFeast(lord.getLordAPI().getFaction()) != null;
        if (check){
            Lord temp = EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getWeddingCeremonyTarget();
            if (temp != null) return get_Person_StringMods(line,temp,"WEDDING_TARGET_");
        }
        return get_null_Person_StringMods(line,"WEDDING_TARGET_");
    }
    private static String getWeddingTargetPartnerStringMods(String line, Lord lord){
        boolean check = EventController.getCurrentFeast(lord.getLordAPI().getFaction()) != null;
        if (check){
            Lord temp = EventController.getCurrentFeast(lord.getLordAPI().getFaction()).getWeddingCeremonyTarget();
            if (temp != null){
                lord = temp;
                if (lord.getSpouse() != null){
                    lord = LordController.getLordOrPlayerById(lord.getSpouse());
                    return get_Person_StringMods(line,lord,"WEDDING_TARGET_SPOUSE_");
                }
            }
        }
        return get_null_Person_StringMods(line,"WEDDING_TARGET_SPOUSE_");
    }
    private static String getSecondLordStringMods(String line,Lord targetLord){
        if (targetLord == null) return get_null_Person_StringMods(line,"SECOND_LORD_");
        return get_Person_StringMods(line,targetLord,"SECOND_LORD_");
    }
    private static String getSecondLordPartnerStringMods(String line,Lord targetLord){
        if (targetLord == null || targetLord.getSpouse() == null) return get_null_Person_StringMods(line,"SECOND_LORD_SPOUSE_");
        targetLord = LordController.getLordOrPlayerById(targetLord.getSpouse());
        return get_Person_StringMods(line,targetLord,"SECOND_LORD_SPOUSE_");
    }

    private static String get_Person_StringMods(String line, Lord lord, String key){
        String data = lord.getFaction().getDisplayName();
        line = insertData(line,"%"+key+"FACTION_NAME",data);

        data = "";
        if (lord.getTemplate() != null)data = Global.getSector().getFaction(lord.getTemplate().factionId).getDisplayName();
        line = insertData(line,"%"+key+"STARTING_FACTION_NAME",data);

        data = lord.getLordAPI().getNameString();
        line = insertData(line,"%"+key+"NAME",data);

        data = lord.getLordAPI().getName().getFirst();
        line = insertData(line,"%"+key+"NAME_FIRST",data);

        data = lord.getLordAPI().getName().getLast();
        line = insertData(line,"%"+key+"NAME_LAST",data);

        data = lord.getTitle();
        line = insertData(line,"%"+key+"TITLE",data);

        data = lord.getLordAPI().getManOrWoman();
        line = insertData(line,"%"+key+"GENDER_MAN_OR_WOMEN",data);

        data = lord.getLordAPI().getHeOrShe();
        line = insertData(line,"%"+key+"GENDER_HE_OR_SHE",data);

        data = lord.getLordAPI().getHimOrHer();
        line = insertData(line,"%"+key+"GENDER_HIM_OR_HER",data);

        data = lord.getLordAPI().getHisOrHer();
        line = insertData(line,"%"+key+"GENDER_HIS_OR_HER",data);

        data = lord.getFormalWear();
        line = insertData(line,"%"+key+"GENDER_SUIT_OR_DRESS",data);

        data = GenderUtils.sirOrMaam(lord.getLordAPI(), false);
        line = insertData(line,"%"+key+"GENDER_SIR_OR_MAAM",data);

        data = lord.getLordAPI().getGender().name();
        line = insertData(line,"%"+key+"GENDER_NAME",data);

        FleetMemberAPI flownShip = getFlownShip(lord.getLordAPI(),lord.getFleet());
        data = "nothing";//todo: move this, like everything else, into the strings file. for modality of different languages.
        if (flownShip != null) data = flownShip.getHullSpec().getHullName();
        line = insertData(line,"%"+key+"FLAGSHIP_HULLNAME",data);

        data = "nothing";//todo: move this, like everything else, into the strings file. for modality of different languages.
        if (flownShip != null) data = flownShip.getShipName();
        line = insertData(line,"%"+key+"FLAGSHIP_NAME",data);

        data = GenderUtils.husbandOrWife(lord.getLordAPI(), false);
        line = insertData(line,"%"+key+"GENDER_HUSBAND_OR_WIFE",data);

        data = "nothing";
        LawProposal lordProposal = PoliticsController.getProposal(lord);
        if (lordProposal != null) data = lordProposal.getTitle();
        line = insertData(line,"%"+key+"PROPOSAL_NAME",data);

        data = "nowhere";
        if (lord.getLordAPI().getFleet() != null && Utils.getNearbyDescription(lord.getLordAPI().getFleet()) != null) data = Utils.getNearbyDescription(lord.getLordAPI().getFleet());
        line = insertData(line,"%"+key+"FLEET_LOCATION",data);

        data = "noone";
        if (lord.getLiegeName() != null) data = lord.getLiegeName();
        line = insertData(line,"%"+key+"LIEGE_NAME",data);

        data = Utils.getFactionTitle(lord.getFaction().getId(),0);
        line = insertData(line,"%"+key+"FACTION_RANK_TITLE0",data);

        data = Utils.getFactionTitle(lord.getFaction().getId(),1);
        line = insertData(line,"%"+key+"FACTION_RANK_TITLE1",data);

        data = Utils.getFactionTitle(lord.getFaction().getId(),2);
        line = insertData(line,"%"+key+"FACTION_RANK_TITLE2",data);
        return line;
    }
    private static String get_null_Person_StringMods(String line, String key){
        String data = "ERROR: failed to get date of: '"+key+"'";
        line = insertData(line,"%"+key+"FACTION_NAME",data);

        line = insertData(line,"%"+key+"STARTING_FACTION_NAME",data);

        line = insertData(line,"%"+key+"NAME",data);

        line = insertData(line,"%"+key+"NAME_FIRST",data);

        line = insertData(line,"%"+key+"NAME_LAST",data);

        line = insertData(line,"%"+key+"TITLE",data);

        line = insertData(line,"%"+key+"GENDER_MAN_OR_WOMEN",data);

        line = insertData(line,"%"+key+"GENDER_HE_OR_SHE",data);

        line = insertData(line,"%"+key+"GENDER_HIM_OR_HER",data);

        line = insertData(line,"%"+key+"GENDER_HIS_OR_HER",data);

        line = insertData(line,"%"+key+"GENDER_SUIT_OR_DRESS",data);

        line = insertData(line,"%"+key+"GENDER_SIR_OR_MAAM",data);

        line = insertData(line,"%"+key+"GENDER_NAME",data);

        line = insertData(line,"%"+key+"FLAGSHIP_HULLNAME",data);

        line = insertData(line,"%"+key+"FLAGSHIP_NAME",data);

        line = insertData(line,"%"+key+"GENDER_HUSBAND_OR_WIFE",data);

        line = insertData(line,"%"+key+"PROPOSAL_NAME",data);

        line = insertData(line,"%"+key+"FLEET_LOCATION",data);

        line = insertData(line,"%"+key+"LIEGE_NAME",data);
        return line;
    }

    private static String getTargetMarketStringMods(String line,MarketAPI market){
        if (market != null) return get_Market_StringMods(line,market,"TARGET_MARKET_");
        return get_null_Market_StringMods(line,"TARGET_MARKET_");
    }
    private static String get_Market_StringMods(String line,MarketAPI market,String key){
        String data = market.getName();
        line = insertData(line,"%"+key+"NAME",data);

        data = market.getFaction().getDisplayName();
        line = insertData(line,"%"+key+"FACTION",data);

        data = "nowere";
        if (market.getStarSystem() != null)data = market.getStarSystem().getName();
        line = insertData(line,"%"+key+"SYSTEM",data);

        data = market.getSize()+"";
        line = insertData(line,"%"+key+"SIZE",data);

        data = market.getStabilityValue()+"";
        line = insertData(line,"%"+key+"STABILITY",data);

        return line;
    }
    private static String get_null_Market_StringMods(String line,String key){
        String data = "nothing";
        line = insertData(line,"%"+key+"NAME",data);

        line = insertData(line,"%"+key+"FACTION",data);

        line = insertData(line,"%"+key+"SYSTEM",data);

        line = insertData(line,"%"+key+"SIZE",data);

        line = insertData(line,"%"+key+"STABILITY",data);

        return line;
    }

    private static FleetMemberAPI getFlownShip(PersonAPI lord, CampaignFleetAPI fleet){
        if (fleet == null) return null;
        FleetMemberAPI output = fleet.getFlagship();
        if (output != null && output.getCaptain() != null && fleet.getFlagship().getCaptain().getId().equals(lord.getId())) return output;
        for (FleetMemberAPI a : fleet.getFleetData().getMembersListCopy()){
            if (a.getCaptain() != null && a.getCaptain().getId().equals(lord.getId())){
                return a;
            }
        }
        return null;
    }

    private static boolean markAtStart(String line, String mark){
        for (int a = 0; a < mark.length(); a++){
            if (a > line.length()) return false;
            if (line.charAt(a) != mark.charAt(a)) return false;
        }
        return true;
    }
    private static boolean markAtEnd(String line, String mark){
        int b = line.length() - 1;
        for (int a = mark.length(); a >= 0; a--){
            if (b < 0) return false;
            if (line.charAt(b) != mark.charAt(a)) return false;
            b--;
        }
        return true;
    }

    public static ArrayList<Object> getOrganizedReplacedMarkers(String line, String[] markers, String[] replacedData){
        //this was built with the objective of working with the games base highlight system, allowing flexible marker positions with highlights.
        //it was NEVER TESTED. at all. but I put a lot of work into it, so I will keep it just in case.
        String replacedMarker= "%s";
        ArrayList<Object> out = new ArrayList<>();

        // get positions of each marker in line. gets an array to 'out' for each different marker.
        ArrayList<ArrayList<Integer>> stringPos = getPositionOfMarkers(line,markers);

        // organizes the markers into there positions in the inputted line.
        int strings=0;
        for (ArrayList<Integer> a : stringPos){
            strings+=a.size();
        }
        String[] markerOutput = new String[strings];
        int[] id = new int[strings];
        int a = 0;
        while(a < strings){
            int b = 9999999;
            int id2 = 0;
            for (int c = 0; c < stringPos.size(); c++){
                ArrayList<Integer> d = stringPos.get(c);
                if (d.get(id[c]) < b){
                    b = id[c];
                    id2 = c;
                }
            }
            markerOutput[a] = replacedData[id2];
            a++;
        }

        //modify the primary string, so its markers are replaced with modified markers.
        for (int b = 0; b < markers.length; b++){
            line = insertData(line,markers[b],replacedMarker);
        }
        out.add(line);
        out.add(markerOutput);
        return out;
    }
    private static ArrayList<ArrayList<Integer>> getPositionOfMarkers(String line, String[] markers){
        ArrayList<ArrayList<Integer>> out = new ArrayList<>();

        for (String mark : markers) {
            ArrayList<Integer> out2 = new ArrayList<>();
            if (markAtStart(line, mark)) {
                out2.add(0);
            }
            String[] lines = line.split(mark);
            int pos = 0;
            for (int a = 0; a < lines.length; a++) {
                pos += lines.length;
                out2.add(pos);
            }
            if (markAtEnd(line, mark)) {
                out2.add(line.length()-1);
            }
            out.add(out2);
        }


        return out;
    }


    @Getter
    private HashMap<String,String> dialog = new HashMap<>();
    private HashMap<String,String> dialogOptionData = new HashMap<>();
    private HashMap<String,DialogGroupOption> advancedDialogOptionData = new HashMap<>();
    private HashMap<String,String> hint = new HashMap<>();
    private HashMap<String,String> shotcut = new HashMap<>();
    private ArrayList<DialogRule_Base> rules = new ArrayList();
    private HashMap<String,ArrayList<DialogRule_Base>> hide = new HashMap<>();
    private HashMap<String,ArrayList<DialogRule_Base>> enable = new HashMap<>();
    private HashMap<String,ArrayList<String>> additionalOptions = new HashMap<>();
    private HashMap<String,ArrayList<DialogAddon_Base>> addons = new HashMap<>();
    private HashMap<String, Color> colorOverride = new HashMap<>();
    private HashMap<String, Color> colorHighlight = new HashMap<>();
    private HashMap<String,String[]> highlight = new HashMap<>();
    private HashMap<String, DialogInsertList> customInserts = new HashMap<>();
    public DialogSet(String name,int priority,LordDialog dialog){
        while (dialog.organizedDialogSets.size() < priority){
            dialog.organizedDialogSets.add(new ArrayList<>());
        }
        dialog.organizedDialogSets.get(priority).add(this);
        //dialogSets.put(name,this);
    }
    @SneakyThrows
    public DialogSet(String name,JSONObject jsonObject,LordDialog dialog){
        int priority = 2;

        //add conditions
        if (jsonObject.has("rules")){
            JSONObject rulesTemp = jsonObject.getJSONObject("rules");
            rules = getDialogRulesFromJSon(rulesTemp);
        }
        //add lines
        if (jsonObject.has("lines")){
            JSONObject lines = jsonObject.getJSONObject("lines");
            for (Iterator it = lines.keys(); it.hasNext();) {
                String key = (String) it.next();
                boolean isObject = true;
                try{
                    lines.getJSONObject(key);
                }catch (Exception e){
                    isObject = false;
                }
                if (isObject){
                    getLineAsObjectFromJSon(key,lines.getJSONObject(key));
                    continue;
                }
                addLine(key,lines.getString(key));
            }
        }
        if (jsonObject.has("priority")) priority = jsonObject.getInt("priority");
        while (dialog.organizedDialogSets.size() <= priority){
            dialog.organizedDialogSets.add(new ArrayList<>());
        }
        dialog.organizedDialogSets.get(priority).add(this);
    }
    public boolean canUseDialog(Lord lord,Lord targetLord,MarketAPI targetMarket){
        for (DialogRule_Base a : rules){
            if (!a.condition(lord,targetLord,targetMarket)) return false;
        }
        return true;
    }
    @Deprecated
    public void applyLine(String key, Lord lord, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, boolean forceHide, HashMap<String,String> markersReplaced){
        applyLine(key, lord,null,null, textPanel, options, dialog, forceHide, markersReplaced);
    }
    public void applyLine(String key, Lord lord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, boolean forceHide, HashMap<String,String> markersReplaced){
        Logger log = Global.getLogger(DialogSet.class);
        log.info("applying line of key: "+key);
        //apply a paragraph of text for the current line
        /*if (runStartingOptions(key, lord, targetLord, targetMarket, textPanel, null, options, dialog, markersReplaced)){
            return;
        }*/
        if (shouldHide(key,textPanel,options,lord,targetLord,targetMarket)) return;
        String line = this.getLine(key);
        if (line != null && !line.equals("") && !shouldHide(key, textPanel, options, lord,targetLord,targetMarket) && !forceHide) {
            log.info("running dialog of line: "+line);
            //if (!((key.equals("greeting") || key.equals(LordInteractionDialogPluginImpl.startingDialog)) && LordInteractionDialogPluginImpl.isHasGreeted())) {
            line = insertCustomData(key, line, lord, targetLord, targetMarket);
            line = insertDefaltData(line, lord, targetLord, targetMarket);
            line = insertAdditionalData(line, markersReplaced);
            String[] highlightsTemp = new String[0];
            if (colorHighlight.containsKey(key)) {
                highlightsTemp = new String[highlight.get(key).length];
                for (int a = 0; a < highlightsTemp.length; a++) {
                    highlightsTemp[a] = insertDefaltData(highlight.get(key)[a], lord, targetLord, targetMarket, true);
                }
            }
            if (colorOverride.containsKey(key)) {
                if (colorHighlight.containsKey(key)) {
                    textPanel.addPara(line, colorOverride.get(key), colorHighlight.get(key), highlightsTemp);
                } else {
                    textPanel.addPara(line, colorOverride.get(key));
                }
            } else {
                if (colorHighlight.containsKey(key)) {
                    textPanel.addPara(line, colorHighlight.get(key), highlightsTemp);
                } else {
                    textPanel.addPara(line);
                }
            }
            //}
        }
        //add on all addons.
        if (addons.containsKey(key)){
            //log.info("attempting to run addons for line...");
            for (DialogAddon_Base a : addons.get(key)){
                //log.info("  running addon of class: "+a.getClass().getName());
                a.apply(textPanel,options,dialog,lord,targetLord,targetMarket);
            }
        }
        //log.info("attempting to add options from line" + key);
        //add on all options
        boolean builtOptions = false;
        if (additionalOptions.containsKey(key)){
            //log.info("ADDITIONAL OPTIONS: adding options from key of: " + key);
            if (additionalOptions.get(key).size() != 0){
                options.clearOptions();
                builtOptions = true;
            }
            for (String a : additionalOptions.get(key)){
                log.info("  ADDITIONAL OPTIONS: a single option of key: "+a);
                //if (runStartingOptions(a,lord,targetLord,targetMarket,textPanel,null,options,dialog,markersReplaced)) continue;
                addOptionWithInserts(a,lord,targetLord,targetMarket,textPanel,options,dialog,markersReplaced);
            }
        }else if (defaltOptionSets.containsKey(key)){
            log.info("getting default options of: "+defaltOptionSets.get(key));
            addParaWithInserts(defaltOptionSets.get(key),lord,targetLord,targetMarket,textPanel,options,dialog,false,markersReplaced);
            builtOptions = true;
        }
        if (!builtOptions){
            log.info("got no options from key of: "+key);
            //???????? unknown process.
            //what I would attempt to do here is run the optionData of the line. effectively, just changing lines directly after. this can be done with chained lines though?
        }

    }
    /*public void applyOption(String key, Lord lord, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog,HashMap<String,String> markersReplaced){
        String optionID = null;
        if (dialogOptionData.containsKey(key)) optionID = dialogOptionData.get(key);
        DialogOption optionData = new DialogOption(optionID,addons.get(key));
        applyOption(key,lord,textPanel,optionData,options,dialog,markersReplaced);
    }*/
    public void applyOption(String key, Lord lord, Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel, OptionPanelAPI options, InteractionDialogAPI dialog, HashMap<String,String> markersReplaced){
        String optionID = null;
        if (dialogOptionData.containsKey(key)) optionID = dialogOptionData.get(key);
        DialogOption optionData = new DialogOption(optionID,addons.get(key),targetLord,targetMarket);
        applyOption(key,lord,targetLord,targetMarket,textPanel,optionData,options,dialog,markersReplaced);
    }
    public void applyOption(String key, Lord lord,Lord targetLord, MarketAPI targetMarket, TextPanelAPI textPanel,Object optionData,OptionPanelAPI options, InteractionDialogAPI dialog,HashMap<String,String> markersReplaced){
        Logger log = Global.getLogger(StoredSettings.class);
        log.info("  checking option of key: "+key);
        if (advancedDialogOptionData.containsKey(key)){
            log.info("  attempting to add advanced option for: "+key);
            advancedDialogOptionData.get(key).applyOptions(lord,targetLord,targetMarket,textPanel,options,dialog,markersReplaced);
        }else{
            log.info("  attempting to add basic option for: "+key);
            applyOptionSingle(key, lord, targetLord,targetMarket, textPanel, optionData, options, dialog, markersReplaced);
        }
    }
    public void applyOptionSingle(String key, Lord lord,Lord targetLord,MarketAPI targetMarket, TextPanelAPI textPanel,Object optionData,OptionPanelAPI options, InteractionDialogAPI dialog,HashMap<String,String> markersReplaced){
        Logger log = Global.getLogger(DialogSet.class);
        String line = this.getLine(key);
        log.info("running option of key: "+key);
        if (optionData != null){
            DialogOption temp = ((DialogOption)optionData);
            temp.optionID = attemptKeyRedirection(temp.optionID);
        }
        /*if (runStartingOptions(key, lord, targetLord, targetMarket, textPanel, optionData, options, dialog, markersReplaced)){
            return;
        }*/
        if (!shouldHide(key, textPanel, options, lord,targetLord,targetMarket) && (line != null)) {
            log.info("adding option key: " + key);
            line = insertCustomData(key, line, lord, targetLord, targetMarket);
            line = insertDefaltData(line, lord, targetLord, targetMarket);
            line = insertAdditionalData(line, markersReplaced);
            if (hint.containsKey(key)) {
                String line2 = hint.get(key);
                line2 = insertDefaltData(line2, lord, targetLord, targetMarket);
                line2 = insertAdditionalData(line2, markersReplaced);
                if (colorOverride.containsKey(key)) {
                    options.addOption(line, optionData, colorOverride.get(key), line2);
                } else {
                    options.addOption(line, optionData, line2);
                }
                //is this even required????
                options.setTooltip(optionData, line2);
            } else {
                if (colorOverride.containsKey(key)) {
                    options.addOption(line, optionData, colorOverride.get(key), "");
                } else {
                    options.addOption(line, optionData);
                }
            }
            if (shotcut.containsKey(key)) {
                switch (shotcut.get(key)) {
                    case "ESCAPE":
                        options.setShortcut(optionData, 1, false, false, false, false);
                        break;
                    case "CONTROL":
                        //I am unsure how to do this, tbh.
                        break;
                    case "ALT":
                        break;
                    case "SHIFT":
                        break;
                }
            }
            if (enable.containsKey(key) && !shouldEnable(key, textPanel, options, lord, targetLord, targetMarket)){
                options.setEnabled(optionData, false);
            }
        }else if(line == null){
            log.info("attempting to run option as optionSet.");
            DialogOption optionDataTemp = (DialogOption)optionData;
            //note: target lord and market set in internal data of dialogOption. not needed for input.
            optionDataTemp.applyAddons(textPanel, options, dialog, lord);
        }
        //log.info("attempting to add options from options" + key);
        boolean builtOptions = false;
        if (additionalOptions.containsKey(key)) {
            log.info("running additional options from option...");
            //log.info("ADDITIONAL OPTIONS: adding options from key of: " + key);
            if (additionalOptions.get(key).size() != 0) {
                options.clearOptions();
                builtOptions = true;
            }
            for (String a : additionalOptions.get(key)) {
                //log.info("  ADDITIONAL OPTIONS: a single option of key: " + a);
                addOptionWithInserts(a, lord,targetLord,targetMarket, textPanel, options, dialog, markersReplaced);
            }
        } else if (defaltOptionSets.containsKey(key)) {
            log.info("running default option sets of key "+key+" from options...");
            //log.info("getting default options from, to key: " + key + ", " + defaltOptionSets.get(key));
            addParaWithInserts(defaltOptionSets.get(key), lord,targetLord,targetMarket, textPanel, options, dialog, false, markersReplaced);
            builtOptions = true;
        }
        if (!builtOptions) {
            //log.info("got no options from key of: " + key);
            //???????? unknown process.
            //what I would attempt to do here is run the optionData of the line. effectively, just changing lines directly after. this can be done with chained lines though?
        }

    }
    private boolean runStartingOptions(String key, Lord lord,Lord targetLord,MarketAPI targetMarket, TextPanelAPI textPanel,Object optionData,OptionPanelAPI options, InteractionDialogAPI dialog,HashMap<String,String> markersReplaced){
        //String optionIDTemp = null;
        //if (optionData != null) optionIDTemp = ((DialogOption)optionData).optionID;
        String redirectKey = "optionSet_beginning";
        if (key.equals(redirectKey)){// || (optionIDTemp != null && (key.equals(LordInteractionDialogPluginImpl.startingDialogSet) || optionIDTemp.equals(redirectKey) || optionIDTemp.equals(LordInteractionDialogPluginImpl.startingDialogSet)))){
            addOptionWithInserts(LordInteractionDialogPluginImpl.startingDialogSet,lord,targetLord,targetMarket,textPanel,options,dialog,markersReplaced);
            //addParaWithInserts(LordInteractionDialogPluginImpl.startingDialogSet, lord, targetLord, targetMarket, textPanel, options, dialog);
            return true;
        }
        return false;
    }
    private String insertCustomData(String key,String line, Lord lord,Lord targetLord,MarketAPI targetMarket){
        if(!customInserts.containsKey(key))return line;
        return customInserts.get(key).insertData(line, lord, targetLord, targetMarket);
    }
    private boolean shouldHide(String key,TextPanelAPI textPanel,OptionPanelAPI options,Lord lord,Lord targetLord, MarketAPI targetMarket){
        if (!hide.containsKey(key) || hide.get(key).size() == 0) return false;
        for (DialogRule_Base a : hide.get(key)){
            if (!a.condition(lord,targetLord,targetMarket)) return true;
        }
        return false;
    }
    private boolean shouldEnable(String key,TextPanelAPI textPanel,OptionPanelAPI options,Lord lord,Lord targetLord, MarketAPI targetMarket){
        for (DialogRule_Base a : enable.get(key)){
            if (!a.condition(lord,targetLord,targetMarket)) return false;
        }
        return true;
    }
    public void addLine(String key,String line){
        dialog.put(key,line);
    }
    public String getLine(String key){
        return dialog.get(key);
    }
    public boolean hasLine(String key){
        return dialog.containsKey(key);
    }
    @SneakyThrows
    public void getLineAsObjectFromJSon(String key, JSONObject line) {
        if (line.has("line")) {
            addLine(key, line.getString("line"));
        }else{
            addLine(key,null);
        }
        if (line.has("addons")) {
            JSONObject addons = line.getJSONObject("addons");
            this.addons.put(key, getDialogAddonsFromJSon(addons));
        }
        if (line.has("color")){
            colorOverride.put(key,getColorFromJson(line));
        }
        if (line.has("highlight")){
            colorHighlight.put(key,getColorFromJson(line));
            if (line.getJSONObject("highlight").get("highlight") instanceof JSONArray){
                String[] temp = new String[line.getJSONObject("highlight").getJSONArray("highlight").length()];
                for (int a = 0; a < line.getJSONObject("highlight").getJSONArray("highlight").length(); a++){
                    temp[a] = (line.getJSONObject("highlight").getJSONArray("highlight").getString(a));
                }
                highlight.put(key,temp);
            }else{
                String[] temp = {line.getJSONObject("highlight").getString("highlight")};
                highlight.put(key,temp);
            }
        }
        if (line.has("show")){
            JSONObject rulesTemp = line.getJSONObject("show");
            hide.put(key, getDialogRulesFromJSon(rulesTemp));
        }
        if (line.has("enable")){
            JSONObject rulesTemp = line.getJSONObject("enable");
            enable.put(key, getDialogRulesFromJSon(rulesTemp));
        }
        if (line.has("options")){
            //JSONObject options = line.getJSONObject("options");
            JSONArray options = line.getJSONArray("options");
            ArrayList<String> optionsTemp = new ArrayList<>();
            for (int a = 0; a < options.length(); a++){
                optionsTemp.add(options.getString(a));
            }
            additionalOptions.put(key,optionsTemp);
        }
        if (line.has("optionData")){
            boolean isObject = true;
            try{
                line.getJSONObject("optionData");
            }catch (Exception e){
                isObject = false;
            }
            if (isObject){
                JSONObject optionData = line.getJSONObject("optionData");
                advancedDialogOptionData.put(key,new DialogGroupOption(optionData));
                /*
                ArrayList<DialogRule_Base> tempa = getDialogRulesFromJSon(optionData.getJSONObject("rules"));
                String tempb = optionData.getString("optionData");
                advancedDialogOptionData.put(key,new DialogGroupOption(tempa,tempb));
                */
            }else {
                dialogOptionData.put(key, line.getString("optionData"));
            }
        }
        if (line.has("hint")){
            hint.put(key,line.getString("hint"));
        }
        if (line.has("shortcut")){
            shotcut.put(key,line.getString("shortcut"));
        }
        if (line.has("customInserts")){
            customInserts.put(key,new DialogInsertList(line,"customInserts"));
        }
    }
    @SneakyThrows
    private Color getColorFromJson(JSONObject line){
        boolean isArray = true;
        try {
            line.getJSONObject("color");
        }catch (Exception e){
            isArray = false;
        }
        if (isArray){
            JSONObject color = line.getJSONObject("color");
            int r=0,g=0,b=0,a=255;
            if (color.has("r")) r = color.getInt("r");
            if (color.has("g")) g = color.getInt("g");
            if (color.has("b")) b = color.getInt("b");
            if (color.has("a")) a = color.getInt("a");
            return new Color(r,g,b,a);
        }else{
            return getColorDefault(line.getString("color"));
        }
    }
    private Color getColorDefault(String color){
        switch (color){
            case "RED":{
                return Color.RED;
            }
            case "GREEN":{
                return Color.GREEN;
            } case "YELLOW":{
                return Color.YELLOW;
            } case "ORANGE":{
                return Color.ORANGE;
            }
        }
        return Color.WHITE;//blank color because I am to lazy to write anti crash code
    }

    @SneakyThrows
    public static ArrayList<DialogAddon_Base> getDialogAddonsFromJSon(JSONObject addons){
        ArrayList<DialogAddon_Base> newAddons = new ArrayList<DialogAddon_Base>();
        for (Iterator it = addons.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            DialogAddon_Base addon = null;
            switch (key2) {
                case "repChange":
                    addon = addAddon_repChange(addons, key2);
                    break;
                case "creditsChange":
                    addon = addAddon_creditsChange(addons, key2);
                    break;
                case "exchangeCreditsWithLord":
                    addon = addAddon_exchangeCreditsWithLord(addons, key2);
                    break;
                case "changeLordCredits":
                    addon = addAddon_changeLordCredits(addons, key2);
                    break;
                case "romanceChange":
                    addon = addAddon_romanceChange(addons, key2);
                    break;
                case "changeCommoditysInPlayersFleet":
                    addon = addAddon_changeCommoditysInPlayersFleet(addons, key2);
                    break;
                case "additionalText":
                    addon = addAddon_additionalText(addons,key2);
                    break;
                case "startWedding":
                    addon = addAddon_startWedding(addons,key2);
                    break;
                case "dedicateTournamentVictoryToLord":
                    addon = addAddon_dedicateTournamentVictoryToLord(addons,key2);
                    break;
                case "startTournament":
                    addon = addAddon_startTournament(addons,key2);
                    break;
                case "setHeldDate":
                    addon = addAddon_setHeldDate(addons,key2);
                    break;
                case "setProfessedAdmiration":
                    addon = addAddon_setProfessedAdmirationThisFeast(addons,key2);
                    break;
                case "setCourted":
                    addon = addAddon_setCourted(addons,key2);
                    break;
                case "wedPlayerToLord":
                    addon = addAddon_wedPlayerToLord(addons,key2);
                    break;
                case "wedPlayerToWeddingTarget":
                    addon = addAddon_wedPlayerToWeddingTarget(addons,key2);
                    break;
                case "setInPlayerFleet":
                    addon = addAddon_setInPlayerFleet(addons,key2);
                    break;
                case "setPledgedFor_CurrentProposal":
                    addon = addAddon_setPledgedFor_CurrentProposal(addons,key2);
                    break;
                case "setPledgedAgainst_CurrentProposal":
                    addon = addAddon_setPledgedAgainst_CurrentProposal(addons,key2);
                    break;
                case "setPledgedFor_PlayerProposal":
                    addon = addAddon_setPledgedFor_PlayerProposal(addons,key2);
                    break;
                case "setPledgedAgainst_PlayerProposal":
                    addon = addAddon_setPledgedAgainst_PlayerProposal(addons,key2);
                    break;
                case "setSwayed":
                    addon = addAddon_setSwayed(addons,key2);
                    break;
                case "setPlayerSupportForLordProposal":
                    addon = addAddon_setPlayerSupportForLordProposal(addons,key2);
                    break;
                case "setPlayerSupportForCurProposal":
                    addon = addAddon_setPlayerSupportForCurProposal(addons,key2);
                    break;
                case "targetLord":
                    addon = addAddon_targetLord(addons,key2);
                    break;
                case "setPersonalityKnown":
                    addon = addAddon_setPersonalityKnown(addons,key2);
                    break;
                case "setDialogData":
                    addon = addAddon_setDialogData(addons,key2);
                    break;
                case "setMemoryData":
                    addon = addAddon_setMemoryData(addons,key2);
                    break;
                case "setLordTags":
                    addon = addAddon_setLordTags(addons,key2);
                    break;
                case "setLordMemoryData":
                    addon = addAddon_setLordMemoryData(addons,key2);
                    break;
                case "defectLordToFaction":
                    addon = addAddon_defectLordToFaction(addons,key2);
                    break;
                case "playSound":
                    addon = addAddon_playSound(addons,key2);
                    break;
                case "setLordRank":
                    addon = addAddon_setLordRank(addons,key2);
                    break;
                case "setPlayerRank":
                    addon = addAddon_setPlayerRank(addons,key2);
                    break;
                case "attemptToAddRandomQuest":
                    addon = addAddon_attemptToAddRandomQuest(addons,key2);
                    break;
                case "AICommand":
                    addon = addAddon_AICommand(addons,key2);
                    break;
                case "preventAttacksOnPlayer":
                    addon = addAddon_preventAttacksOnPlayer(addons,key2);
                    break;
                case "preventAttacksOnTargetLord":
                    addon = addAddon_preventAttacksOnTargetLord(addons,key2);
                    break;
                case "conditionalAddon":
                    addon = addAddon_conditionalAddon(addons,key2);
                    break;
                case "captureLord":
                    addon = addAddon_captureLord(addons,key2);
                    break;
                case "releaseLord":
                    addon = addAddon_releaseLord(addons,key2);
                    break;
                case "customDialogAddon":
                    addon = addAddon_customDialogAddon(addons,key2);
                    break;
                case "adjustDialogData_Int":
                    newAddons.addAll(addAddon_setDialogData_Int(addons,key2));
                    break;
                case "adjustMemoryData_Int":
                    newAddons.addAll(addAddon_setMemoryData_Int(addons,key2));
                    break;
                case "adjustLordMemoryData_Int":
                    newAddons.addAll(addAddon_setLordMemoryData_Int(addons,key2));
                    break;
                case "setDialogData_Boolean":
                    newAddons.addAll(addAddon_setDialogData_Boolean(addons,key2));
                    break;
                case "setMemoryData_Boolean":
                    newAddons.addAll(addAddon_setMemoryData_Boolean(addons,key2));
                    break;
                case "setLordMemoryData_Boolean":
                    newAddons.addAll(addAddon_setLordMemoryData_Boolean(addons,key2));
                    break;
                case "setDialogData_String":
                    newAddons.addAll(addAddon_setDialogData_String(addons,key2));
                    break;
                case "setMemoryData_String":
                    newAddons.addAll(addAddon_setMemoryData_String(addons,key2));
                    break;
                case "setLordMemoryData_String":
                    newAddons.addAll(addAddon_setLordMemoryData_String(addons,key2));
                    break;
                case "applyAddonsToMultiple":
                    addon = addAddon_applyAddonsToMultiple(addons,key2);
                    break;
                case "setFeastInteracted":
                    addon = addAddon_setFeastInteracted(addons,key2);
                    break;

            }
            if (addon != null) newAddons.add(addon);
        }
        return newAddons;
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_repChange(JSONObject json, String key){
        return new DialogAddon_repChange(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_creditsChange(JSONObject json, String key){
        return new DialogAddon_creditsChange(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_changeLordCredits(JSONObject json, String key){
        return new DialogAddon_changeLordCredits(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_exchangeCreditsWithLord(JSONObject json, String key){
        return new DialogAddon_exchangeCreditsWithLord(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_romanceChange(JSONObject json, String key){
        return new DialogAddon_romanceChange(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_changeCommoditysInPlayersFleet(JSONObject json, String key){
        return new DialogAddon_changeCommoditysInPlayersFleet(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_additionalText(JSONObject json,String key){
        return new DialogAddon_additionalText(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_startWedding(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        if (!json2) return null;
        return new DialogAddon_startWedding();
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_dedicateTournamentVictoryToLord(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        if (!json2) return null;
        return new DialogAddon_dedicateTournamentVictoryToLord();
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_startTournament(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        if (!json2) return null;
        return new DialogAddon_startTournament();
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setHeldDate(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setHeldDate(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setProfessedAdmirationThisFeast(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setProfessedAdmirationThisFeast(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setCourted(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setCourted(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_wedPlayerToLord(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_wedPlayerToLord(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_wedPlayerToWeddingTarget(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_wedPlayerToWeddingTarget(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setInPlayerFleet(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        if (json2) return new DialogAddon_setInPlayerFleetTrue();
        return new DialogAddon_setInPlayerFleetFalse();
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPledgedFor_CurrentProposal(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPledgedFor_CurrentProposal(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPledgedAgainst_CurrentProposal(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPledgedAgainst_CurrentProposal(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPledgedFor_PlayerProposal(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPledgedFor_PlayerProposal(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPledgedAgainst_PlayerProposal(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPledgedAgainst_PlayerProposal(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setSwayed(JSONObject json, String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setSwayed(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPlayerSupportForLordProposal(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPlayerSupportForLordProposal(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPlayerSupportForCurProposal(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPlayerSupportForCurProposal(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_targetLord(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogAddon_targetLord(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPersonalityKnown(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogAddon_setPersonalityKnown(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setDialogData(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogAddon_setDialogData(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setMemoryData(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogAddon_setMemoryData(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setLordTags(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogAddon_setLordTags(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setLordMemoryData(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogAddon_setLordMemoryData(json2);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_defectLordToFaction(JSONObject json,String key){
        return new DialogAddon_defectLordToFaction(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_playSound(JSONObject json,String key){
        return new DialogAddon_playSound(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setLordRank(JSONObject json,String key){
        return new DialogAddon_setLordRank(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setPlayerRank(JSONObject json,String key){
        return new DialogAddon_setPlayerRank(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_attemptToAddRandomQuest(JSONObject json,String key){
        if (!json.getBoolean(key)) return null;
        return new DialogAddon_attemptToAddRandomQuest();
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_AICommand(JSONObject json,String key){
        return new DialogAddon_AICommand(json, key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_preventAttacksOnPlayer(JSONObject json,String key){
        return new DialogAddon_preventAttacksOnPlayer(json, key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_preventAttacksOnTargetLord(JSONObject json,String key){
        return new DialogAddon_preventAttacksOnTargetLord(json, key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_conditionalAddon(JSONObject json,String key){
        return new DialogAddon_conditionalAddon(json, key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_captureLord(JSONObject json,String key){
        if (!json.getBoolean(key)) return null;
        return new DialogAddon_captureLord();
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_releaseLord(JSONObject json,String key){
        if (!json.getBoolean(key)) return null;
        return new DialogAddon_releaseLord();
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setDialogData_Int(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setDialogData_Int(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setMemoryData_Int(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setMemoryData_Int(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setLordMemoryData_Int(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setLordMemoryData_Int(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setDialogData_Boolean(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setDialogData_Boolean(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setMemoryData_Boolean(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setMemoryData_Boolean(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setLordMemoryData_Boolean(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setLordMemoryData_Boolean(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setDialogData_String(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setDialogData_String(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setMemoryData_String(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setMemoryData_String(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static ArrayList<DialogAddon_Base> addAddon_setLordMemoryData_String(JSONObject json,String key){
        json = json.getJSONObject(key);
        ArrayList<DialogAddon_Base> addons = new ArrayList<>();
        for (Iterator it = json.keys(); it.hasNext(); ) {
            String key2 = (String) it.next();
            addons.add(new DialogAddon_setLordMemoryData_String(key2,json));
        }
        return addons;
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_customDialogAddon(JSONObject json,String key){
        return new DialogAddon_customList(json, key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_applyAddonsToMultiple(JSONObject json,String key){
        return new DialogAddon_applyAddonsToMultiple(json,key);
    }
    @SneakyThrows
    private static DialogAddon_Base addAddon_setFeastInteracted(JSONObject json,String key){
        return new DialogAddon_setFeastInteracted(json,key);
    }


    public static ArrayList<DialogRule_Base> getDialogRulesFromJSon(JSONObject rulesTemp){
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        for (Iterator it = rulesTemp.keys(); it.hasNext();) {
            String key = (String) it.next();
            switch (key){
                //have the different rule sets here.
                case "relationWithPlayer":
                    rules.add(addRule_relationWithPlayer(rulesTemp,key));
                    break;
                case "startingFaction":
                    rules.addAll(addRule_startingFaction(rulesTemp,key));
                    break;
                case "currentFaction":
                    rules.addAll(addRule_currentFaction(rulesTemp,key));
                    break;
                case "isMarriedToPlayer":
                    rules.add(addRule_isMarriedToPlayer(rulesTemp,key));
                    break;
                case "isMarried":
                    rules.add(addRule_isMarried(rulesTemp,key));
                    break;
                case "isPlayerMarried":
                    rules.add(addRule_isPlayerMarried(rulesTemp,key));
                    break;
                case "willEngage":
                    rules.add(addRule_willEngage(rulesTemp,key));
                    break;
                case "hostileFaction":
                    rules.add(addRule_hostileFaction(rulesTemp,key));
                    break;
                case "isAtFeast":
                    rules.add(addRule_isAtFeast(rulesTemp,key));
                    break;
                case "isHostingFeast":
                    rules.add(addRule_isHostingFeast(rulesTemp,key));
                    break;
                case "playerSubject":
                    rules.add(addRule_playerSubject(rulesTemp,key));
                    break;
                case "playerFactionMarital":
                    rules.add(addRule_playerFactionMarital(rulesTemp,key));
                    break;
                case "lordFactionMarital":
                    rules.add(addRule_lordFactionMarital(rulesTemp,key));
                    break;
                case "lordPersonality":
                    rules.add(addRule_personality(rulesTemp,key));
                    break;
                case "lordBattlerPersonality":
                    rules.add(addRule_battlerPersonaility(rulesTemp,key));
                    break;
                case "playerWealth":
                    rules.add(addRule_playerWealth(rulesTemp,key));
                    break;
                case "lordWealth":
                    rules.add(addRule_lordWealth(rulesTemp,key));
                    break;
                case "playerLevel":
                    rules.add(addRule_playerLevel(rulesTemp,key));
                    break;
                case "lordLevel":
                    rules.add(addRule_lordLevel(rulesTemp,key));
                    break;
                case "playerRank":
                    rules.add(addRule_playerRank(rulesTemp,key));
                    break;
                case "lordRank":
                    rules.add(addRule_lordRank(rulesTemp,key));
                    break;
                case "lordsCourted":
                    rules.add(addRule_lordsCourted(rulesTemp,key));
                    break;
                case "isLordCourtedByPlayer":
                    rules.add(addRule_isLordCourtedByPlayer(rulesTemp,key));
                    break;
                case "playerLordRomanceAction":
                    rules.add(addRule_playerLordRomanceAction(rulesTemp,key));
                    break;
                case "availableTournament":
                    rules.add(addRule_availableTournament(rulesTemp,key));
                    break;
                case "playerTournamentVictory":
                    rules.add(addRule_playerTournamentVictory(rulesTemp,key));
                    break;
                case "lordTournamentVictory":
                    rules.add(addRule_lordTournamentVictory(rulesTemp,key));
                    break;
                case "playerTournamentVictoryDedicated":
                    rules.add(addRule_playerTournamentVictoryDedicated(rulesTemp,key));
                    break;
                case "feastIsHostingWedding":
                    rules.add(addRule_feastIsHostingWedding(rulesTemp,key));
                    break;
                case "firstMeeting":
                    rules.add(addRule_firstMeeting(rulesTemp,key));
                    break;
                case "tournamentDedicatedToLord":
                    rules.add(addRule_tournamentDedicatedToLord(rulesTemp,key));
                    break;
                case "lordsInFeast":
                    rules.add(addRule_lordsInFeast(rulesTemp,key));
                    break;
                case "hasProfessedAdmirationThisFeast":
                    rules.add(addRule_hasProfessedAdmirationThisFeast(rulesTemp,key));
                    break;
                case "hasHeldDateThisFeast":
                    rules.add(addRule_hasHeldDateThisFeast(rulesTemp,key));
                    break;
                case "lordPledgedSupport_forActiveProposal":
                    rules.add(addRule_lordPledgedSupport_forActiveProposal(rulesTemp,key));
                    break;
                case "lordPledgedSupport_againstActiveProposal":
                    rules.add(addRule_lordPledgedSupport_againstActiveProposal(rulesTemp,key));
                    break;
                case "lordPledgedSupport_forPlayerProposal":
                    rules.add(addRule_lordPledgedSupport_forPlayerProposal(rulesTemp,key));
                    break;
                case "lordPledgedSupport_againstPlayerProposal":
                    rules.add(addRule_lordPledgedSupport_againstPlayerProposal(rulesTemp,key));
                    break;
                case "playerProposalExists":
                    rules.add(addRule_playerProposalExists(rulesTemp,key));
                    break;
                case "lordProposalExists":
                    rules.add(addRule_lordProposalExists(rulesTemp,key));
                    break;
                case "lordFactionHasActiveConsole":
                    rules.add(addRule_lordFactionHasActiveConsole(rulesTemp,key));
                    break;
                case "playerFactionHasActiveConsole":
                    rules.add(addRule_playerFactionHasActiveConsole(rulesTemp,key));
                    break;
                case "lordActingInPlayerFleet":
                    rules.add(addRule_lordActingInPlayerFleet(rulesTemp,key));
                    break;
                case "lordAndPlayerSameFaction":
                    rules.add(addRule_lordAndPlayerSameFaction(rulesTemp,key));
                    break;
                case "playerHasCommodity":
                    rules.addAll(addRule_playerHasCommodity(rulesTemp,key));
                    break;
                case "lordsFavItem":
                    rules.addAll(addRule_lordsFavItem(rulesTemp,key));
                    break;
                case "isWeddingTarget":
                    rules.add(addRule_isWeddingTarget(rulesTemp,key));
                    break;
                case "optionOfCurrProposal":
                    rules.add(addRule_optionOfCurrProposal(rulesTemp,key));
                    break;
                case "optionOfPlayerProposal":
                    rules.add(addRule_optionOfPlayerProposal(rulesTemp,key));
                    break;
                case "isSwayed":
                    rules.add(addRule_isSwayed(rulesTemp,key));
                    break;
                case "lordProposalSupporters":
                    rules.add(addRule_lordProposalSupporters(rulesTemp,key));
                    break;
                case "lordProposalOpposers":
                    rules.add(addRule_lordProposalOpposers(rulesTemp,key));
                    break;
                case "lordProposalPlayerSupports":
                    rules.add(addRule_lordProposalPlayerSupports(rulesTemp,key));
                    break;
                case "playerProposalSupporters":
                    rules.add(addRule_playerProposalSupporters(rulesTemp,key));
                    break;
                case "playerProposalOpposers":
                    rules.add(addRule_playerProposalOpposers(rulesTemp,key));
                    break;
                case "playerProposalLordSupports":
                    rules.add(addRule_playerProposalLordSupports(rulesTemp,key));
                    break;
                case "curProposalSupporters":
                    rules.add(addRule_curProposalSupporters(rulesTemp,key));
                    break;
                case "curProposalOpposers":
                    rules.add(addRule_curProposalOpposers(rulesTemp,key));
                    break;
                case "curProposalPlayerSupports":
                    rules.add(addRule_curProposalPlayerSupports(rulesTemp,key));
                    break;
                case "curProposalLordSupports":
                    rules.add(addRule_curProposalLordSupports(rulesTemp,key));
                    break;
                case "random":
                    rules.add(addRule_random(rulesTemp,key));
                    break;
                case "lordFleetIsAlive":
                    rules.add(addRule_lordFleetIsAlive(rulesTemp,key));
                    break;
                case "relationsBetweenLords":
                    rules.add(addRule_relationsBetweenLords(rulesTemp,key));
                    break;
                case "lordAndTargetSameFaction":
                    rules.add(addRule_lordAndTargetSameFaction(rulesTemp,key));
                    break;
                case "isInteractingLord":
                    rules.add(addRule_isInteractingLord(rulesTemp,key));
                    break;
                case "lordHasLiege":
                    rules.add(addRule_lordHasLiege(rulesTemp,key));
                    break;
                case "playerHasLiege":
                    rules.add(addRule_playerHasLiege(rulesTemp,key));
                    break;
                case "validLordNumbers":
                    rules.add(addRule_validLordNumbers(rulesTemp,key));
                    break;
                case "SECOND_LORD":
                    rules.add(addRule_targetLord(rulesTemp,key));
                    break;
                case "isPersonalityKnown":
                    rules.add(addRule_isPersonalityKnown(rulesTemp,key));
                    break;
                case "or":
                    rules.add(addRule_or(rulesTemp,key));
                    break;
                case "SECOND_LORD_SPOUSE":
                    rules.add(addRule_SECOND_LORD_SPOUSE(rulesTemp,key));
                    break;
                case "PLAYER_SPOUSE":
                    rules.add(addRule_PLAYER_SPOUSE(rulesTemp,key));
                    break;
                case "LORD_SPOUSE":
                    rules.add(addRule_LORD_SPOUSE(rulesTemp,key));
                    break;
                case "LORD_HOST":
                    rules.add(addRule_LORD_HOST(rulesTemp,key));
                    break;
                case "LORD_HOST_SPOUSE":
                    rules.add(addRule_LORD_HOST_SPOUSE(rulesTemp,key));
                    break;
                case "WEDDING_TARGET":
                    rules.add(addRule_WEDDING_TARGET(rulesTemp,key));
                    break;
                case "WEDDING_TARGET_SPOUSE":
                    rules.add(addRule_WEDDING_TARGET_SPOUSE(rulesTemp,key));
                    break;
                case "lordLoyalty":
                    rules.add(addRule_lordLoyalty(rulesTemp,key));
                    break;
                case "getDialogData":
                    rules.addAll(addRule_getDialogData(rulesTemp,key));
                    break;
                case "getMemoryData":
                    rules.addAll(addRule_getMemoryData(rulesTemp,key));
                    break;
                case "getLordMemoryData":
                    rules.addAll(addRule_getLordMemoryData(rulesTemp,key));
                    break;
                case "LordTags":
                    rules.addAll(addRule_LordTags(rulesTemp,key));
                    break;
                case "baseValue":
                    rules.add(addRule_baseValue(rulesTemp,key));
                    break;
                case "currentAction":
                    rules.add(addRule_currentAction(rulesTemp,key));
                    break;
                case "marketFactionRelationToLordsFaction":
                    rules.add(addRule_marketFactionRelationToLord(rulesTemp,key));
                    break;
                case "marketFactionRelationToPlayersFaction":
                    rules.add(addRule_marketFactionRelationToPlayer(rulesTemp,key));
                    break;
                case "marketFactionRelationToTargetLordsFaction":
                    rules.add(addRule_marketFactionRelationToTargetLord(rulesTemp,key));
                    break;
                case "marketSize":
                    rules.add(addRule_marketSize(rulesTemp,key));
                    break;
                case "marketStability":
                    rules.add(addRule_marketStability(rulesTemp,key));
                    break;
                case "marketPlayerFaction":
                    rules.add(addRule_marketPlayerFaction(rulesTemp,key));
                    break;
                case "marketLordFaction":
                    rules.add(addRule_marketLordFaction(rulesTemp,key));
                    break;
                case "marketTargetLordFaction":
                    rules.add(addRule_marketTargetLordFaction(rulesTemp,key));
                    break;
                case "marketHasFiefOwner":
                    rules.add(addRule_marketHasFiefOwner(rulesTemp,key));
                    break;
                case "marketFiefBelongsToPlayer":
                    rules.add(addRule_marketFiefBelongsToPlayer(rulesTemp,key));
                    break;
                case "marketFiefBelongsToLord":
                    rules.add(addRule_marketFiefBelongsToLord(rulesTemp,key));
                    break;
                case "marketFiefBelongsToTargetLord":
                    rules.add(addRule_marketFiefBelongsToTargetLord(rulesTemp,key));
                    break;
                case "marketIsValidTarget":
                    rules.add(addRule_marketIsValidTarget(rulesTemp,key));
                    break;
                case "dialogType":
                    rules.add(addRule_dialogType(rulesTemp,key));
                    break;
                case "customDialogRule":
                    rules.add(addRule_customDialogRule(rulesTemp,key));
                    break;
                case "LordAndTargetAtSameFeast":
                    rules.add(addRule_LordAndTargetAtSameFeast(rulesTemp,key));
                    break;
                case "hasInteractedThisFeast":
                    rules.add(addRule_hasInteractedThisFeast(rulesTemp,key));
                    break;
                case "compareStrings":
                    rules.add(addRule_compareStrings(rulesTemp,key));
                    break;
                case "isValidMarket":
                    rules.add(addRule_isValidMarket(rulesTemp,key));
                    break;
            }
        }
        return rules;
    }
    @SneakyThrows
    private static DialogRule_Base addRule_relationWithPlayer(JSONObject json,String key){
        return new DialogRule_relationWithPlayer(json,key);
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_startingFaction(JSONObject json,String key){
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new DialogRule_startingFaction_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new DialogRule_startingFaction_blacklist(blackList));
        return rules;
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_currentFaction(JSONObject json,String key){
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new DialogRule_currentFaction_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new DialogRule_currentFaction_blacklist(blackList));
        return rules;
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isMarriedToPlayer(JSONObject json, String key){
        return new DialogRule_isMarriedToPlayer(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isMarried(JSONObject json, String key){
        return new DialogRule_isMarried(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isPlayerMarried(JSONObject json, String key){
        return new DialogRule_isPlayerMarried(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_willEngage(JSONObject json, String key){
        return new DialogRule_willEngage(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_hostileFaction(JSONObject json, String key){
        return new DialogRule_hostileFaction(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isAtFeast(JSONObject json, String key){
        return new DialogRule_isAtFeast(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isHostingFeast(JSONObject json, String key){
        return new DialogRule_isHostingFeast(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerSubject(JSONObject json, String key){
        return new DialogRule_playerSubject(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerFactionMarital(JSONObject json, String key){
        return new DialogRule_playerFactionMarital(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordFactionMarital(JSONObject json, String key){
        return new DialogRule_lordFactionMarital(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_personality(JSONObject json,String key){
        ArrayList<String> whiteList = new ArrayList<>();
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
        }
        return new DialogRule_personaility(whiteList);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_battlerPersonaility(JSONObject json,String key){
        ArrayList<String> whiteList = new ArrayList<>();
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
        }
        return new DialogRule_battlePersonaility(whiteList);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordLevel(JSONObject json,String key){
        return new DialogRule_lordLevel(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordRank(JSONObject json,String key){
        return new DialogRule_lordRank(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordsCourted(JSONObject json,String key){
        return new DialogRule_lordsCourted(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordWealth(JSONObject json,String key){
        return new DialogRule_lordWealth(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerLevel(JSONObject json,String key){
        return new DialogRule_playerLevel(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerRank(JSONObject json,String key){
        return new DialogRule_playerRank(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerWealth(JSONObject json,String key){
        return new DialogRule_playerWealth(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isLordCourtedByPlayer(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_isLordCourtedByPlayer(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerLordRomanceAction(JSONObject json,String key){
        return new DialogRule_playerLordRomanceAction(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_availableTournament(JSONObject json, String key){
        return new DialogRule_availableTournament(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerTournamentVictory(JSONObject json, String key){
        return new DialogRule_playerTournamentVictory(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordTournamentVictory(JSONObject json, String key){
        return new DialogRule_lordTournamentVictroy(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerTournamentVictoryDedicated(JSONObject json, String key){
        return new DialogRule_playerTournamentVictroyDedicated(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_feastIsHostingWedding(JSONObject json, String key){
        return new DialogRule_feastIsHostingWedding(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_firstMeeting(JSONObject json, String key){
        return new DialogRule_firstMeeting(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_tournamentDedicatedToLord(JSONObject json, String key){
        return new DialogRule_tournamentDedicatedToLord(json.getBoolean(key));
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordsInFeast(JSONObject json,String key){
        return new DialogRule_lordsInFeast(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_hasProfessedAdmirationThisFeast(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_hasProfessedAdmirationThisFeast(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_hasHeldDateThisFeast(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_hasHeldDateThisFeast(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordPledgedSupport_forActiveProposal(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordPledgedSupport_forActiveProposal(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordPledgedSupport_againstActiveProposal(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordPledgedSupport_againstActiveProposal(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordPledgedSupport_forPlayerProposal(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordPledgedSupport_forPlayerProposal(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordPledgedSupport_againstPlayerProposal(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordPledgedSupport_againstPlayerProposal(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerProposalExists(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_playerProposalExists(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordProposalExists(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordProposalExists(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordFactionHasActiveConsole(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordFactionHasActiveConsole(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerFactionHasActiveConsole(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_playerFactionHasActiveConsole(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordActingInPlayerFleet(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordActingInPlayerFleet(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordAndPlayerSameFaction(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordAndPlayerSameFaction(json2);
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_playerHasCommodity(JSONObject json,String key){
        ArrayList<DialogRule_Base> output = new ArrayList<>();
        JSONObject json2 = json.getJSONObject(key);
        for (Iterator it = json2.keys(); it.hasNext();) {
            String key2 = (String) it.next();
            output.add(new DialogRule_playerHasCommodity(json2,key2));
        }
        return output;
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_lordsFavItem(JSONObject json,String key){
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new DialogRule_lordsFavItem_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new DialogRule_lordsFavItem_blacklist(blackList));
        return rules;
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isWeddingTarget(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_isWeddingTarget(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_optionOfCurrProposal(JSONObject json,String key){
        return new DialogRule_optionOfCurrProposal(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_optionOfPlayerProposal(JSONObject json,String key){
        return new DialogRule_optionOfPlayerProposal(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isSwayed(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_isSwayed(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordProposalSupporters(JSONObject json,String key){
        return new DialogRule_lordProposalSupporters(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordProposalOpposers(JSONObject json,String key){
        return new DialogRule_lordProposalOpposers(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordProposalPlayerSupports(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordProposalPlayerSupports(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerProposalSupporters(JSONObject json,String key){
        return new DialogRule_playerProposalSupporters(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerProposalOpposers(JSONObject json,String key){
        return new DialogRule_playerProposalOpposers(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerProposalLordSupports(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_playerProposalLordSupports(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_curProposalSupporters(JSONObject json,String key){
        return new DialogRule_curProposalSupporters(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_curProposalOpposers(JSONObject json,String key){
        return new DialogRule_curProposalOpposers(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_curProposalPlayerSupports(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_curProposalPlayerSupports(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_curProposalLordSupports(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_curProposalLordSupports(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_random(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_random(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordFleetIsAlive(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordFleetIsAlive(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_relationsBetweenLords(JSONObject json,String key){
        return new DialogRule_relationsBetweenLords(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordAndTargetSameFaction(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordAndTargetSameFaction(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isInteractingLord(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_isInteractingLord(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordHasLiege(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_lordHasLiege(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_playerHasLiege(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_playerHasLiege(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_validLordNumbers(JSONObject json,String key){
        return new DialogRule_validLordNumbers(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_targetLord(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_targetLord(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isPersonalityKnown(JSONObject json,String key){
        boolean json2 = json.getBoolean(key);
        return new DialogRule_isPersonalityKnown(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_or(JSONObject json,String key){
        JSONArray json2 = json.getJSONArray(key);
        return new DialogRule_or(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_SECOND_LORD_SPOUSE(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_SECOND_LORD_SPOUSE(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_PLAYER_SPOUSE(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_PLAYER_SPOUSE(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_LORD_SPOUSE(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_LORD_SPOUSE(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_LORD_HOST(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_LORD_HOST(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_LORD_HOST_SPOUSE(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_LORD_HOST_SPOUSE(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_WEDDING_TARGET(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_WEDDING_TARGET(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_WEDDING_TARGET_SPOUSE(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        return new DialogRule_WEDDING_TARGET_SPOUSE(json2);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_lordLoyalty(JSONObject json,String key){
        return new DialogRule_lordLoyalty(json,key);
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_getDialogData(JSONObject json,String key){
        /*OH BOY, i SURE HOPE THIS WORKS!!!!*/
        JSONObject json2 = json.getJSONObject(key);
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        for (Iterator it = json2.keys(); it.hasNext();) {
            String key2 = (String) it.next();
            boolean go = true;
            try {
                json2.getBoolean(key2);
            }catch (Exception e){
                go = false;
            }
            if (go){//(json2.get(key2) instanceof Boolean)){
                rules.add(new DialogRule_getDialogData_boolean(key2,json2.getBoolean(key2)));
                continue;
            }
            go = true;
            try {
                json2.getInt(key2);
            }catch (Exception e){
                go = false;
            }
            if (go || json2.getJSONObject(key2).has("min") || json2.getJSONObject(key2).has("max")){
                rules.add(new DialogRule_getDialogData_int(key2,json2));
                continue;
            }
            rules.add(new DialogRule_getDialogData_string(key2,json2.getJSONObject(key2)));
        }
        return rules;
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_getMemoryData(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        for (Iterator it = json2.keys(); it.hasNext();) {
            String key2 = (String) it.next();
            boolean go = true;
            try {
                json2.getBoolean(key2);
            }catch (Exception e){
                go = false;
            }
            if (go){//(json2.get(key2) instanceof Boolean)){
                rules.add(new DialogRule_getMemoryData_boolean(key2,json2.getBoolean(key2)));
                continue;
            }
            go = true;
            try {
                json2.getInt(key2);
            }catch (Exception e){
                go = false;
            }
            if (go || json2.getJSONObject(key2).has("min") || json2.getJSONObject(key2).has("max")){
                rules.add(new DialogRule_getMemoryData_int(key2,json2));
                continue;
            }
            rules.add(new DialogRule_getMemoryData_string(key2,json2.getJSONObject(key2)));
        }
        return rules;
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_getLordMemoryData(JSONObject json,String key){
        JSONObject json2 = json.getJSONObject(key);
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        for (Iterator it = json2.keys(); it.hasNext();) {
            String key2 = (String) it.next();
            boolean go = true;
            try {
                json2.getBoolean(key2);
            }catch (Exception e){
                go = false;
            }
            if (go){//(json2.get(key2) instanceof Boolean)){
                rules.add(new DialogRule_getLordMemoryData_boolean(key2,json2.getBoolean(key2)));
                continue;
            }
            go = true;
            try {
                json2.getInt(key2);
            }catch (Exception e){
                go = false;
            }
            if (go || json2.getJSONObject(key2).has("min") || json2.getJSONObject(key2).has("max")){
                rules.add(new DialogRule_getLordMemoryData_int(key2,json2));
                continue;
            }
            rules.add(new DialogRule_getLordMemoryData_string(key2, json2.getJSONObject(key2)));
        }
        return rules;
    }
    @SneakyThrows
    private static ArrayList<DialogRule_Base> addRule_LordTags(JSONObject json,String key){
        ArrayList<DialogRule_Base> rules = new ArrayList<>();
        ArrayList<String> whiteList = new ArrayList<>();
        ArrayList<String> blackList = new ArrayList<>();
        JSONObject ruleAdded = json.getJSONObject(key);
        for (Iterator it2 = ruleAdded.keys(); it2.hasNext();) {
            String key2 = (String) it2.next();
            if (ruleAdded.getBoolean(key2)){
                whiteList.add(key2);
                continue;
            }
            blackList.add(key2);
        }
        if (whiteList.size() != 0) rules.add(new DialogRule_LordTags_whitelist(whiteList));
        if (blackList.size() != 0) rules.add(new DialogRule_LordTags_blacklist(blackList));
        return rules;
    }
    @SneakyThrows
    private static DialogRule_Base addRule_baseValue(JSONObject json,String key){
        return new DialogRule_baseValue(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_currentAction(JSONObject json,String key){
        return new DialogRule_currentAction(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketFactionRelationToLord(JSONObject json,String key){
        return new DialogRule_marketFactionRelationToLord(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketFactionRelationToPlayer(JSONObject json,String key){
        return new DialogRule_marketFactionRelationToPlayer(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketFactionRelationToTargetLord(JSONObject json,String key){
        return new DialogRule_marketFactionRelationToTargetLord(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketSize(JSONObject json,String key){
        return new DialogRule_marketSize(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketStability(JSONObject json,String key){
        return new DialogRule_marketStability(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketPlayerFaction(JSONObject json,String key){
        return new DialogRule_marketPlayerFaction(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketLordFaction(JSONObject json,String key){
        return new DialogRule_marketLordFaction(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketTargetLordFaction(JSONObject json,String key){
        return new DialogRule_marketTargetLordFaction(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketHasFiefOwner(JSONObject json,String key){
        return new DialogRule_marketHasFiefOwner(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketFiefBelongsToPlayer(JSONObject json,String key){
        return new DialogRule_marketFiefBelongsToPlayer(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketFiefBelongsToLord(JSONObject json,String key){
        return new DialogRule_marketFiefBelongsToLord(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketFiefBelongsToTargetLord(JSONObject json,String key){
        return new DialogRule_marketFiefBelongsToTargetLord(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_marketIsValidTarget(JSONObject json,String key){
        return new DialogRule_marketIsValidTarget(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_dialogType(JSONObject json,String key){
        return new DialogRule_dialogType(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_customDialogRule(JSONObject json,String key){
        return new DialogRule_customList(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_LordAndTargetAtSameFeast(JSONObject json,String key){
        return new DialogRule_LordAndTargetAtSameFeast(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_hasInteractedThisFeast(JSONObject json,String key){
        return new DialogRule_hasInteractedThisFeast(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_compareStrings(JSONObject json,String key){
        return new DialogRule_compareStrings(json,key);
    }
    @SneakyThrows
    private static DialogRule_Base addRule_isValidMarket(JSONObject json,String key){
        return new DialogRule_isValidMarket(json,key);
    }
}
