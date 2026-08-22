package starlords.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetActionTextProvider;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.missions.academy.GACelestialObject;
import com.fs.starfarer.api.util.Misc;
import com.sun.source.tree.CaseTree;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.alliances.Alliance;
import exerelin.utilities.*;
import lombok.Setter;
import starlords.person.Lord;

import javax.swing.text.ChangedCharSetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NexerlinUtilitys {
    private static boolean areInvasionsEnabled = true;
    public static void calculateInvasionsEnabled(){
        if (!NexConfig.enableInvasions) {
            areInvasionsEnabled = false;
            return;
        }
        if(NexConfig.invasionsOnlyAfterPlayerColony && NexUtilsFaction.getPlayerMarkets(true,false).size() == 0){
            areInvasionsEnabled = false;
            return;
        }
        if (NexUtils.getTrueDaysSinceStart() < NexConfig.invasionGracePeriod){
            areInvasionsEnabled = false;
            return;
        }
        //NexConfig.invasionGracePeriod;
        areInvasionsEnabled = true;
    }
    public static void declarePeace(FactionAPI proposer,FactionAPI propose){
        DiplomacyManager.adjustRelations(proposer,propose,0.51f,null,null,RepLevel.INHOSPITABLE);
    }
    public static void declareWar(FactionAPI proposer,FactionAPI propose){
        DiplomacyManager.adjustRelations(proposer,propose,-1.1f,null,null,RepLevel.HOSTILE);
    }

    public static boolean canPreformDiplomacy(FactionAPI faction){
        if (!NexConfig.enableDiplomacy) return false;
        NexFactionConfig factionConfig = NexConfig.getFactionConfig(faction.getId());
        if (factionConfig.disableDiplomacy) return false;
        if (!factionConfig.playableFaction) return false;
        if (factionConfig.pirateFaction && !NexConfig.allowPirateInvasions) return false;
        if (factionConfig.diplomacyTraits != null && factionConfig.diplomacyTraits.contains("foreverwar")) return false;

        return true;
    }
    public static boolean canBeInvaded(MarketAPI market){
        if (!canInvade(market.getFaction())) return false;
        //if (!NexConfig.enableInvasions) return false;
        return NexUtilsMarket.shouldTargetForInvasions(market,3);
    }
    public static boolean invasionsEnabled(){
        return areInvasionsEnabled;
    }
    public static boolean canInvade(FactionAPI factionAPI){
        NexFactionConfig factionConfig = NexConfig.getFactionConfig(factionAPI.getId());
        if (!factionConfig.canInvade) return false;
        if (factionConfig.pirateFaction && !NexConfig.allowPirateInvasions) return false;
        if (factionConfig.invasionPointMult == 0) return false;
        return true;
    }





    /*public static boolean canBeAttacked(FactionAPI faction){
        if (NexConfig.getFactionConfig(faction.getId()).pirateFaction) return false;
        if (!NexConfig.getFactionConfig(faction.getId()).canInvade) return false;
        if (!NexConfig.getFactionConfig(faction.getId()).playableFaction) return false;
        //NexConfig.
        //NexConfig has a lot of things here that might prove usefull. constants, but not functions.
        return true;
    }*/
    /*public static boolean canBeAttacked(MarketAPI market){
        return NexUtilsMarket.canBeInvaded(market,false);
    }*/
    /*public static boolean canChangeRelations(FactionAPI faction){
        //if (NexConfig.getFactionConfig(faction.getId()).hostileToAll || NexConfig.getFactionConfig(faction.getId()).playableFaction) return false;
        if (!NexConfig.getFactionConfig(faction.getId()).playableFaction) return false;
        if (NexConfig.getFactionConfig(faction.getId()).disableDiplomacy) return false;
        if (Misc.isPirateFaction(faction)) return false;
        return true;
    }*/
    /*public static boolean isMinorFaction(FactionAPI faction){
        //ok, so I have lined wether or not something is a minor faction and can be attacked together. this can be overwritten independently, but for now this makes sense I think?
        return !canBeAttacked(faction);
    }*/

    public static Map<Alliance.Alignment, Float> getFactionAlignments(String factionId) {
        return NexConfig.getFactionConfig(factionId).getAlignmentValues();
    }

    public static float generateCloseAlignment(float alignment) {
        if (alignment > 0.5f) return alignment - 0.5f;
        if (alignment < -0.5f) return alignment + 0.5f;

        return alignment + (Utils.getRandomChance(2) - 0.5f);
    }

    public static float generateRandomAlignment() {
        return ((float) Utils.getRandomChance(5) - 2f) / 2f;
    }

    public static Map<Alliance.Alignment, Float> generateLordAlignments(Lord lord) {
        Map<Alliance.Alignment, Float> factionAlignments =  getFactionAlignments(lord.getFaction().getId());
        Map<Alliance.Alignment, Float> lordAlignments = new HashMap<>();

        for (Map.Entry<Alliance.Alignment, Float> alignment : factionAlignments.entrySet()) {
            int chance = Utils.getRandomChance(100);
            float newAlignment = 0;
            if (chance < 50) newAlignment = alignment.getValue();
            else if (chance < 80) newAlignment = generateCloseAlignment(alignment.getValue());
            else newAlignment = generateRandomAlignment();

            lordAlignments.put(alignment.getKey(),newAlignment);
        }

    return lordAlignments;
    }

    public static Map<Alliance.Alignment, Float> generateLordAlignmentsFromTemplate(Lord lord) {
        Map<Alliance.Alignment, Float> lordAlignments = new HashMap<>();

        for (Alliance.Alignment alignment : Alliance.Alignment.getAlignments()) {
            Float value = Objects.requireNonNullElse(lord.getTemplate().alignments.get(alignment.toString()),0f);
            lordAlignments.put(alignment, value);
        }
        return lordAlignments;
    }

    public static float getAlignmentsComparison(Map<Alliance.Alignment, Float> a, Map<Alliance.Alignment, Float> b) {
        float difference = 0;
        for (Map.Entry<Alliance.Alignment, Float> alignment : a.entrySet()) {
            if (!b.containsKey(alignment.getKey())) continue;
            difference += Math.abs(alignment.getValue() - b.get(alignment.getKey()));
        }
        return difference;
    }
}
