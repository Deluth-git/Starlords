package starlords.faction;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import lombok.Getter;
import lombok.Setter;
import starlords.person.Lord;
import starlords.util.Utils;

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

    public LawProposal(Lawset.LawType law, String originator,
                       String targetLord, String targetFief, String targetFaction, int targetLevel,boolean isTemp) {
        //this is a different function for the sole reason that I want to beable to get a crash report if LawProposal gets a null lord as its input.
        this.originator = originator;
        this.targetLord = targetLord;
        this.targetFief = targetFief;
        this.targetFaction = targetFaction;
        this.targetLevel = targetLevel;
        this.law = law;
        if (LordController.getLordOrPlayerById(originator) != null) this.faction = LordController.getLordOrPlayerById(originator).getFaction();
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
            Lord lord = LordController.getLordOrPlayerById(lordStr);
            if (lord == null) continue;
            ctr += PoliticsController.getPoliticalWeight(lord);
        }
        if (playerSupports) {
            if (!faction.equals(Global.getSector().getPlayerFaction())) {
                ctr += PoliticsController.getPoliticalWeight(LordController.getPlayerLord());
            } else  {
                ctr += PoliticsController.PLAYER_EXTRA_COUNCIL_WEIGHT;
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
        try {
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
        }catch (Exception e){
            return e.getMessage();
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
                if (lord == null){
                    return "Appoint Marshal [Error]";
                }else {
                    return "Appoint Marshal " + lord.getLordAPI().getNameString();
                }
            case AWARD_FIEF:
                lord = LordController.getLordOrPlayerById(targetLord);
                if(lord == null) return "Award Fief to "+"null";
                return "Award Fief to " + lord.getLordAPI().getNameString();
            case DECLARE_WAR:
                return "Declare war - " + Global.getSector().getFaction(targetFaction).getDisplayName();
            case SUE_FOR_PEACE:
                return "Sue for peace - " + Global.getSector().getFaction(targetFaction).getDisplayName();
            case REVOKE_FIEF:
                lord = LordController.getLordOrPlayerById(targetLord);
                if(lord == null) return "Revoke Fief from "+"null";
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
                if(lord == null) return "Exile "+"null";
                return "Exile " + lord.getLordAPI().getNameString();
        }
        return "ERROR: No Title";
    }
}
