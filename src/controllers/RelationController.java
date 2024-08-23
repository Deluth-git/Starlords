package controllers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import person.Lord;

import java.util.HashMap;
import java.util.List;

import static util.Constants.DEBUG_MODE;

// tracks relations between all lords, lieges, and player
public class RelationController extends BaseIntelPlugin {

    private int[][] lordRelations;
    private int[][] factionRelations;
    private HashMap<String, Integer> factionIdxMap = new HashMap<>(); // This belongs somewhere else eventually

    private static RelationController instance;

    private RelationController() {
        setHidden(true);
        int numLords = LordController.getLordsList().size();
        List<FactionAPI> factions = Global.getSector().getAllFactions();
        int numLieges = factions.size();
        for (FactionAPI faction : factions) {
            factionIdxMap.put(faction.getId(), factionIdxMap.size());
        }
        lordRelations = new int[numLords][numLords];
        factionRelations = new int[numLieges][numLords];

        // set initial relations
        for (int i = 0; i < lordRelations.length; i++) {
            lordRelations[i][i] = 100; // lords love themselves!
        }
        for (Lord lord : LordController.getLordsList()) {
            int factionIdx = factionIdxMap.get(lord.getLordAPI().getFaction().getId());
            factionRelations[factionIdx][LordController.indexOf(lord)] = 25;
            if (DEBUG_MODE) {
                factionRelations[factionIdx][LordController.indexOf(lord)] = 0; // TODO DEBUG
            }
        }
    }

    public static void modifyRelation(Lord lord1, Lord lord2, int amount) {
        if (lord1.isPlayer())  {
            lord2.getLordAPI().getRelToPlayer().adjustRelationship(amount / 100f, null);
            return;
        }
        if (lord2.isPlayer()) {
            lord1.getLordAPI().getRelToPlayer().adjustRelationship(amount / 100f, null);
            return;
        }
        int idx1 = LordController.indexOf(lord1);
        int idx2 = LordController.indexOf(lord2);
        int newRel = Math.max(-100, Math.min(100, getInstance().lordRelations[Math.min(idx1, idx2)][Math.max(idx1, idx2)] + amount));
        getInstance().lordRelations[Math.min(idx1, idx2)][Math.max(idx1, idx2)] = newRel;
    }

    public static int getRelation(Lord lord1, Lord lord2) {
        if (lord1.isPlayer())  {
            return lord2.getLordAPI().getRelToPlayer().getRepInt();
        }
        if (lord2.isPlayer()) {
            return lord1.getLordAPI().getRelToPlayer().getRepInt();
        }
        int idx1 = LordController.indexOf(lord1);
        int idx2 = LordController.indexOf(lord2);
        return getInstance().lordRelations[Math.min(idx1, idx2)][Math.max(idx1, idx2)];
    }

    public static void modifyLoyalty(Lord lord, int amount) {
        modifyLoyalty(lord, lord.getLordAPI().getFaction().getId(), amount);
    }

    public static void modifyLoyalty(Lord lord, String factionId, int amount) {
        int newLoyalty = Math.min(100, Math.max(-100, amount +
                getInstance().factionRelations[getInstance().factionIdxMap.get(factionId)][LordController.indexOf(lord)]));
        getInstance().factionRelations[getInstance().factionIdxMap.get(factionId)][LordController.indexOf(lord)] = newLoyalty;
    }

    public static int getLoyalty(Lord lord) {
        return getLoyalty(lord, lord.getLordAPI().getFaction().getId());
    }

    public static int getLoyalty(Lord lord, String factionId) {
        if (Global.getSector().getPlayerFaction().getId().equals(factionId)) return lord.getPlayerRel();
        return getInstance().factionRelations[getInstance().factionIdxMap.get(factionId)][LordController.indexOf(lord)];
    }

    public static RelationController getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(RelationController.class);
            if (intel.isEmpty()) {
                instance = new RelationController();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one RelationController intel registered");
                }
                instance = (RelationController) intel.get(0);
            }
        }
        return instance;
    }

    public static RelationController getInstance() {
        return getInstance(false);
    }
}
