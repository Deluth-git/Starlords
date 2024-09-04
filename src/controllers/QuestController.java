package controllers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.DeliveryMissionIntel;
import com.fs.starfarer.api.impl.campaign.missions.DisruptHeavyIndustry;
import com.fs.starfarer.api.impl.campaign.missions.ExtractionMission;
import com.fs.starfarer.api.impl.campaign.missions.HijackingMission;
import com.fs.starfarer.api.impl.campaign.missions.RaidSecretOutpostMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.loading.PersonMissionSpec;
import org.apache.log4j.Logger;
import person.Lord;
import person.LordEvent;
import person.LordPersonality;
import ui.MissionPreviewIntelPlugin;

import java.util.*;

public class QuestController extends BaseIntelPlugin {

    private static QuestController instance = null;
    public static Logger log = Global.getLogger(QuestController.class);

    private HashMap<String, String> questMap;
    private HashMap<String, Boolean> questGivenMap;
    private ArrayList<String> tradeMissionIds;
    private ArrayList<String> militaryMissionIds;
    private ArrayList<String> underworldMissionIds;
    private Random random;

    private QuestController() {
        setHidden(true);
        random = new Random();
        questMap = new HashMap<>();
        questGivenMap = new HashMap<>();
        tradeMissionIds = new ArrayList<>();
        militaryMissionIds = new ArrayList<>();
        underworldMissionIds = new ArrayList<>();
        for (Lord lord : LordController.getLordsList()) {
            questMap.put(lord.getLordAPI().getId(), null);
            questGivenMap.put(lord.getLordAPI().getId(), false);
        }
        resetQuestList();
        resetQuests();
    }

    public void resetQuestList() {
        /* Good ones:
           cheapCom- trade
           proCom- trade
           ddro- trade
           ssat- military
           psb- military
           tabo- military
           smug- underworld
           rsom - underworld
           rdsm- underworld

           Unknown:
           dcom
           cpm - trade
           extr - military
           dhi- military
           dsp- military
           jabr - underworld

           Bad ones:
           sShip
           hmdf
           cpc
           mcb
           ucb
           hijack

           These two work but make no sense:
           seco
           sitm
        */
        tradeMissionIds.clear();
        militaryMissionIds.clear();
        underworldMissionIds.clear();
        tradeMissionIds.add("cheapCom");
        tradeMissionIds.add("proCom");
        tradeMissionIds.add("ddro");
        militaryMissionIds.add("ssat");
        militaryMissionIds.add("psb");
        militaryMissionIds.add("tabo");
        underworldMissionIds.add("smug");
        underworldMissionIds.add("rsom");
        underworldMissionIds.add("rdsm");
    }

    public void resetQuests() {
        HashSet<String> questLords = new HashSet<>();
        ArrayList<MissionPreviewIntelPlugin> toRemove = new ArrayList<>();
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel()) {
            if (intel instanceof BaseHubMission) {
                PersonAPI potentialLord = ((BaseHubMission) intel).getPersonOverride();
                if (potentialLord != null) questLords.add(potentialLord.getId());
            }
            if (intel instanceof MissionPreviewIntelPlugin) {
                questLords.add(((MissionPreviewIntelPlugin) intel).getMission().getPersonOverride().getId());
                toRemove.add((MissionPreviewIntelPlugin) intel);
            }
        }

        for (MissionPreviewIntelPlugin intel : toRemove) {
            intel.endImmediately();
            Global.getSector().getIntelManager().removeIntel(intel);
        }

        for (Lord lord : LordController.getLordsList()) {
            questMap.put(lord.getLordAPI().getId(), sampleQuestId(lord));
            if (questGivenMap.get(lord.getLordAPI().getId()) && !questLords.contains(lord.getLordAPI().getId())) {
                // reset if quest was already abandoned
                questGivenMap.put(lord.getLordAPI().getId(), false);
            }
        }
    }

    public static void setQuestGiven(Lord lord, boolean bool) {
        getInstance().questGivenMap.put(lord.getLordAPI().getId(), bool);
    }

    public static String getQuestId(Lord lord) {
        return getInstance().questMap.get(lord.getLordAPI().getId());
    }

    public static boolean isQuestGiven(Lord lord) {
        return getInstance().questGivenMap.get(lord.getLordAPI().getId());
    }

    private String sampleQuestId(Lord lord) {
        int pref = 0;
        switch(lord.getPersonality()) {
            case MARTIAL:
                pref = 1;
                break;
            case CALCULATING:
                pref = 2;
                break;
        }
        // with probability 50, some lords have a preferred category
        if (random.nextBoolean()) {
            if (pref == 1) {
                return militaryMissionIds.get(random.nextInt(militaryMissionIds.size()));
            } else if (pref == 2) {
                return tradeMissionIds.get(random.nextInt(tradeMissionIds.size()));
            }
        }
        int limit = 2;
        if (lord.getPersonality() == LordPersonality.QUARRELSOME) {
            limit = 3;
        }
        int category = random.nextInt(limit);
        switch(category) {
            case 0:
                return militaryMissionIds.get(random.nextInt(militaryMissionIds.size()));
            case 1:
                return tradeMissionIds.get(random.nextInt(tradeMissionIds.size()));
            default:
                return underworldMissionIds.get(random.nextInt(underworldMissionIds.size()));
        }
    }

    public static QuestController getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(QuestController.class);
            if (intel.isEmpty()) {
                instance = new QuestController();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one QuestController intel registered");
                }
                instance = (QuestController) intel.get(0);
                instance.resetQuestList();
            }
        }
        return instance;
    }

    public static QuestController getInstance() {
        return getInstance(false);
    }
}
