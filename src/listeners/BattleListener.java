package listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import controllers.LordController;
import controllers.RelationController;
import person.Lord;
import util.LordFleetFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleListener extends BaseCampaignEventListener {
    public BattleListener(boolean permaRegister) {
        super(permaRegister);
    }

    // The ai is dumb and sometimes goes to a different jump gate than the one it's sent to, so in this case
    // dynamically adjust its jump gate target, since these are usually rally points it rests at before acting
    @Override
    public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpPointAPI.JumpDestination to) {
        Lord lord = LordController.getLordById(fleet.getCommander().getId());
        if (lord == null || fleet.getCurrentAssignment() == null) return;
        SectorEntityToken target = fleet.getCurrentAssignment().getTarget();
        if (target instanceof JumpPointAPI && target.getContainingLocation().equals(fleet.getContainingLocation())) {
            fleet.removeFirstAssignment();
            fleet.addAssignmentAtStart(FleetAssignment.ORBIT_PASSIVE, to.getDestination(), 2, null);
            fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, to.getDestination(), 7, null);
        }
    }


    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        int killsFP = 0;
        int kills = 0;
        boolean playerWon = battle.isOnPlayerSide(primaryWinner);

        ArrayList<Lord> loserLords = new ArrayList<>();
        ArrayList<Lord> maybeCaptured = new ArrayList<>();
        for (CampaignFleetAPI loser : battle.getOtherSideSnapshotFor(primaryWinner)) {
            killsFP += Misc.getSnapshotFPLost(loser);
            kills += Misc.getSnapshotMembersLost(loser).size();
            Lord lord = LordController.getLordOrPlayerById(loser.getCommander().getId());
            if (lord != null) {
                loserLords.add(lord);
                if (!lord.isPlayer() && lord.getFleet().isEmpty()) maybeCaptured.add(lord);
            }
        }

        Random rand = new Random();
        int totalFP = 0;
        for (CampaignFleetAPI winner : battle.getSnapshotSideFor(primaryWinner)) {
            totalFP += winner.getFleetPoints();
        }

        ArrayList<Lord> winnerLords = new ArrayList<>();
        for (CampaignFleetAPI winner : battle.getSnapshotSideFor(primaryWinner)) {
            // record kills and level up officers
            Lord lord = LordController.getLordOrPlayerById(winner.getCommander().getId());
            if (lord == null) {
                continue;
            }
            lord.recordKills(kills * winner.getFleetPoints() / totalFP);
            // maybe improve relations with other lords
            for (Lord alliedLord : winnerLords) {
                RelationController.modifyRelation(lord, alliedLord,
                        Math.min(5, Math.round((float) killsFP / alliedLord.getLordAPI().getFleet().getFleetPoints())));
            }
            winnerLords.add(lord);

            // level up if enough stuff is killed
            int levelUpChance = 200 * killsFP / totalFP;
            for (FleetMemberAPI ship : winner.getFleetData().getMembersListCopy()) {
                if (ship.isFighterWing() || ship.getCaptain() == null || ship.isFlagship()) {
                    continue;
                }
                int currLevel = ship.getCaptain().getStats().getLevel();
                if (currLevel < LordFleetFactory.OFFICER_MAX_LEVEL && rand.nextInt(100) < levelUpChance) {
                    ship.getCaptain().getStats().setLevel(currLevel + 1);
                    LordFleetFactory.upskillOfficer(ship.getCaptain());
                }
            }
        }
        // record kills for losers

        // update relations between lords
        //for ()

        // TODO take prisoners here

    }
}
