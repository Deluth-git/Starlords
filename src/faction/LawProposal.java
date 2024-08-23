package faction;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import controllers.LordController;
import controllers.PoliticsController;
import lombok.Getter;
import lombok.Setter;
import person.Lord;
import util.Utils;

import java.util.ArrayList;
import java.util.HashSet;

@Getter
public class LawProposal {

    public final String originator; // lord id
    public final String targetLord;
    public final String targetFief;
    public final String targetFaction;
    public final int targetLevel; // target law level or rank
    public final Lawset.LawType law;
    public final long creationTimestamp;
    @Setter
    public FactionAPI faction;

    @Setter
    private boolean shouldShow;
    private ArrayList<String> supportersCached; // stores supporters from last round of compute
    private ArrayList<String> supporters;
    private ArrayList<Integer> supporterVals; // stores each supporter's total opinion of proposal
    private ArrayList<ArrayList<String>> supporterReasons; // stores itemized breakdown of supporter reasons
    private ArrayList<String> opposersCached; // stores opposers from last round of compute
    private ArrayList<String> opposers;
    private ArrayList<Integer> opposersVals; // stores each opposer's total opinion of proposal
    private ArrayList<ArrayList<String>> opposerReasons; // stores itemized breakdown of opposer reasons
    private boolean playerSupports;
    @Setter
    private boolean liegeSupports;
    @Setter
    private int liegeVal;
    @Setter
    private ArrayList<String> liegeReasons;
    @Setter
    private boolean passed; // whether this proposal was ever passed
    @Setter
    private boolean forcePassed; // whether this proposal was ever force-passed
    private boolean alive;  // if not alive, intel entry is removed. Proposals in the council are considered not alive.
    @Getter
    private HashSet<String> pledgedFor; // stores lords who were swayed to support this proposal
    @Getter
    private HashSet<String> pledgedAgainst; // stores lords who were swayed to opposes this proposal


    public LawProposal(Lawset.LawType law, String originator,
                       String targetLord, String targetFief, String targetFaction, int targetLevel) {
        this.originator = originator;
        this.targetLord = targetLord;
        this.targetFief = targetFief;
        this.targetFaction = targetFaction;
        this.targetLevel = targetLevel;
        this.law = law;
        this.faction = LordController.getLordOrPlayerById(originator).getFaction();
        creationTimestamp = Global.getSector().getClock().getTimestamp();
        supporters = new ArrayList<>();
        opposers = new ArrayList<>();
        supporterReasons = new ArrayList<>();
        opposerReasons = new ArrayList<>();
        supporterVals = new ArrayList<>();
        opposersVals = new ArrayList<>();
        pledgedAgainst = new HashSet<>();
        pledgedFor = new HashSet<>();
        alive = true;
    }

    public void cacheSupporters() {
        supporterReasons.clear();
        opposerReasons.clear();
        supporterVals.clear();
        opposersVals.clear();
        supportersCached = supporters;
        opposersCached = opposers;
        supporters = new ArrayList<>();
        opposers = new ArrayList<>();
    }

    // ignores effect of non-player ruler. TODO this is a duplicate function
    public int getTotalSupport() {
        int ctr = 0;
        for (String lordStr : supporters) {
            ctr += PoliticsController.getPoliticalWeight(LordController.getLordOrPlayerById(lordStr));
        }
        if (playerSupports) {
            if (!faction.equals(Global.getSector().getPlayerFaction())) {
                ctr += PoliticsController.getPoliticalWeight(LordController.getPlayerLord());
            } else  {
                ctr *= PoliticsController.getLiegeMultiplier(Global.getSector().getPlayerFaction());
            }
        }
        return ctr;
    }

    public void setPlayerSupports(boolean supports) {
        playerSupports = supports;
        if (faction.equals(Global.getSector().getPlayerFaction())) {
            liegeSupports = supports;
        }
    }

    public void kill() {
        alive = false;
    }

    // 1-line summary of proposal
    public String getSummary() {
        Lord lord;
        switch (law) {
            case CROWN_AUTHORITY:
            case NOBLE_AUTHORITY:
            case TRADE_LAW:
            case FEAST_LAW:
                return "Change " + law.lawName + " to " + LawLevel.values()[targetLevel].displayName;
            case APPOINT_MARSHAL:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Appoint " + lord.getTitle() + " " + lord.getLordAPI().getNameString() + " to Marshal.";
            case AWARD_FIEF:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Award " + Global.getSector().getEconomy().getMarket(targetFief).getName()
                        + " to " + lord.getTitle() + " " + lord.getLordAPI().getNameString();
            case DECLARE_WAR:
                return "Declare war on " + Global.getSector().getFaction(targetFaction).getDisplayNameWithArticle();
            case SUE_FOR_PEACE:
                return "Sue for peace with " + Global.getSector().getFaction(targetFaction).getDisplayNameWithArticle();
            case REVOKE_FIEF:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Revoke " + Global.getSector().getEconomy().getMarket(targetFief)
                        + " from " + lord.getTitle() + " " + lord.getLordAPI().getNameString();
            case CHANGE_RANK:
                lord = LordController.getLordOrPlayerById(targetLord);
                String ret;
                if (lord.getRanking() > targetLevel) {
                    ret = "Demote ";
                } else {
                    ret = "Promote ";
                }
                ret += lord.getTitle() + " " + lord.getLordAPI().getNameString() + " to " + Utils.getTitle(
                        lord.getFaction(), targetLevel);
                return ret;
            case EXILE_LORD:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Exile " + lord.getTitle() + " " + lord.getLordAPI().getNameString() + " from the realm.";
        }
        return "";
    }

    public String getTitle() {
        int baseLevel;
        Lord lord;
        switch (law) {
            case CROWN_AUTHORITY:
                 baseLevel = PoliticsController.getLaws(
                         LordController.getLordOrPlayerById(originator).getFaction()).getCrownAuthority().ordinal();
                if (baseLevel > targetLevel) {
                    return "Decrease " + law.lawName;
                } else {
                    return "Increase " + law.lawName;
                }
            case NOBLE_AUTHORITY:
                baseLevel = PoliticsController.getLaws(
                        LordController.getLordOrPlayerById(originator).getFaction()).getNobleAuthority().ordinal();
                if (baseLevel > targetLevel) {
                    return "Decrease " + law.lawName;
                } else {
                    return "Increase " + law.lawName;
                }
            case TRADE_LAW:
                baseLevel = PoliticsController.getLaws(
                        LordController.getLordOrPlayerById(originator).getFaction()).getTradeFavor().ordinal();
                if (baseLevel > targetLevel) {
                    return "Decrease " + law.lawName;
                } else {
                    return "Increase " + law.lawName;
                }
            case FEAST_LAW:
                baseLevel = PoliticsController.getLaws(
                        LordController.getLordOrPlayerById(originator).getFaction()).getFeastLaw().ordinal();
                if (baseLevel > targetLevel) {
                    return "Decrease " + law.lawName;
                } else {
                    return "Increase " + law.lawName;
                }
            case APPOINT_MARSHAL:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Appoint Marshal " + lord.getLordAPI().getNameString();
            case AWARD_FIEF:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Award Fief to " + lord.getLordAPI().getNameString();
            case DECLARE_WAR:
                return "Declare war - " + Global.getSector().getFaction(targetFaction).getDisplayName();
            case SUE_FOR_PEACE:
                return "Sue for peace - " + Global.getSector().getFaction(targetFaction).getDisplayName();
            case REVOKE_FIEF:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Revoke Fief from " + lord.getLordAPI().getNameString();
            case CHANGE_RANK:
                lord = LordController.getLordOrPlayerById(targetLord);
                String ret;
                if (lord.getRanking() > targetLevel) {
                    ret = "Demote ";
                } else {
                    ret = "Promote ";
                }
                ret += lord.getLordAPI().getNameString();
                return ret;
            case EXILE_LORD:
                lord = LordController.getLordOrPlayerById(targetLord);
                return "Exile " + lord.getLordAPI().getNameString();
        }
        return "ERROR: No Title";
    }
}
