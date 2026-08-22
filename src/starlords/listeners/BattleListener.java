package starlords.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import starlords.controllers.*;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;
import starlords.person.LordRequest;
import starlords.util.DefectionUtils;
import starlords.util.LordFleetFactory;
import starlords.util.StringUtil;
import starlords.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import static starlords.util.Constants.*;

public class BattleListener extends BaseCampaignEventListener {
    private static final Logger log = Global.getLogger(BattleListener.class);
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
	    int loserKills = 0;
	    int loserKillsFp = 0;
	    int loserTotalFP = 0;
	    boolean playerWon = battle.isOnPlayerSide(primaryWinner);

	    ArrayList<Lord> loserLords = new ArrayList<>();
	    HashSet<Lord> maybeCaptured = new HashSet<>();
	    for (CampaignFleetAPI loser : battle.getOtherSideSnapshotFor(primaryWinner)) {
		    killsFP += Misc.getSnapshotFPLost(loser);
		    kills += Misc.getSnapshotMembersLost(loser).size();
		    loserTotalFP += loser.getFleetPoints();
		    Lord lord = LordController.getLordOrPlayerById(loser.getCommander().getId());

		    if (lord != null) {
			    loserLords.add(lord);
			    // defeated lords that lose their flagship are set for capture
			    if (!lord.isPlayer()) {
				    if (loser.getFlagship() == null) {
					    maybeCaptured.add(lord);
				    } else {
					    if (loser.getFlagship().isFlagship() == false) {
						    maybeCaptured.add(lord);
					    }
				    }
			    }
		    }
	    }

	    int totalFP = 0;
	    for (CampaignFleetAPI winner : battle.getSnapshotSideFor(primaryWinner)) {
		    totalFP += winner.getFleetPoints();
	    }
	    totalFP = Math.max(1, totalFP);
	    loserTotalFP = Math.max(1, loserTotalFP);

	    ArrayList<Lord> winnerLords = new ArrayList<>();
	    ArrayList<Lord> maybeCaptor = new ArrayList<>();
	    for (CampaignFleetAPI winner : battle.getSnapshotSideFor(primaryWinner)) {
		    loserKills += Misc.getSnapshotMembersLost(winner).size();
		    loserKillsFp += Misc.getSnapshotFPLost(winner);
		    // record kills and level up officers
		    Lord lord = LordController.getLordOrPlayerById(winner.getCommander().getId());
		    if (lord == null) continue;
		    backupLordFleet(winner, lord);
		    lord.recordKills(kills * winner.getFleetPoints() / totalFP);
		    // maybe improve relations with other lords
		    for (Lord alliedLord : winnerLords) {
			    float denom = Math.max(1, (alliedLord.getFleet().getFleetPoints() + lord.getFleet().getFleetPoints()) / 2f);
			    if (lord.isPlayer()) {
				    int change = (int) (3 * battle.getPlayerInvolvementFraction()
						    * killsFP / Math.max(1, alliedLord.getFleet().getFleetPoints()));
				    Utils.adjustPlayerReputation(alliedLord.getLordAPI(), Math.min(4, change));
			    } else if (alliedLord.isPlayer()) {
				    int change = (int) (3 * battle.getPlayerInvolvementFraction()
						    * killsFP / Math.max(1, lord.getFleet().getFleetPoints()));
				    Utils.adjustPlayerReputation(lord.getLordAPI(), Math.min(4, change));
			    } else {
				    RelationController.modifyRelation(lord, alliedLord,
						    Math.min(5, Math.round(2 * killsFP / denom)));
			    }
		    }
		    winnerLords.add(lord);
		    if (!lord.getFleet().isEmpty() && !lord.isPlayer()) maybeCaptor.add(lord);

		    // level up if enough stuff is killed
		    if (!lord.isPlayer()) {
			    levelUpWithChance(winner, 200 * killsFP / totalFP);

			    // pirates get happier about being pirates when they kill stuff
				//ok, this is... somewhat interesting. so: pirate faction gain relations with there faction based on chance of all things.
				//possable solution.. give all lord rep gain? no, that feels wrong...
				//when leader is null give rep gain? use this calculation to also give wealth per kill (that sounds good for pirates to...)
				//arg... how do
				lord.addWealth((float) (LORD_MURDER_INCOME * ((float)killsFP / (float)totalFP) * lord.getCombatIncomeMulti()));
				float rep = ((float) killsFP) / ((float) totalFP);
				float remainder = rep % 1;
				rep = (int) (rep);
			    if (Utils.rand.nextFloat() < remainder) {
				    rep++;
			    }
			    rep = (int)(rep * lord.getRepGainFromKillsMulti());
			    if (rep >= 1){
					RelationController.modifyLoyalty(lord, (int) (rep));
				}
		    }
	    }
	    // record kills and levels up losers, but no relations increase
	    for (CampaignFleetAPI loser : battle.getOtherSideSnapshotFor(primaryWinner)) {
		    Lord lord = LordController.getLordOrPlayerById(loser.getCommander().getId());
		    if (lord == null) continue;
		    lord.recordKills(loserKills * loser.getFleetPoints() / loserTotalFP);
		    if (!lord.isPlayer()) levelUpWithChance(loser, 200 * loserKillsFp / loserTotalFP);
	    }

	    // update relations between winners and losers
	    for (Lord winner : winnerLords) {
		    for (Lord loser : loserLords) {
			    if (winner.isPlayer()) {
				    Utils.adjustPlayerReputation(loser.getLordAPI(), -1);
			    } else if (loser.isPlayer()) {
				    Utils.adjustPlayerReputation(winner.getLordAPI(), -1);
			    } else {
				    RelationController.modifyRelation(winner, loser, -1);
			    }
		    }
	    }

	    String beforePrisonerCalculation = "[Star Lords] Winner Lords:" + System.lineSeparator()
			    + Utils.printLordsWithPrisoners(winnerLords) + System.lineSeparator()
			    + "[Star Lords] Loser Lords:" + System.lineSeparator()
			    + Utils.printLordsWithPrisoners(loserLords) + System.lineSeparator();

	    // take prisoners here
	    boolean canCapture = !maybeCaptor.isEmpty() || playerWon;
	    for (Lord defeated : maybeCaptured) {
		    Random rand = new Random(
				    defeated.getLordAPI().getId().hashCode() * Global.getSector().getClock().getTimestamp());
		    Lord captor = null;
		    int captorFreeChance = 0;

		    // if lord still has a couple ships left, destroy them
		    if (defeated.getFleet() != null) for (FleetMemberAPI toDestroy : defeated.getFleet().getMembersWithFightersCopy()) if (!toDestroy.isFighterWing()) defeated.getFleet().removeFleetMemberWithDestructionFlash(toDestroy);
		    if (LifeAndDeathController.getInstance().attemptToKillStalord(defeated)) {
			    Global.getSector().getCampaignUI().addMessage(
					    StringUtil.getString(CATEGORY_UI, "lord_defeated_killed",
							    defeated.getTitle() + " " + defeated.getLordAPI().getNameString(),
							    primaryWinner.getFaction().getDisplayName()),
					    defeated.getFaction().getBaseUIColor());
			    continue;
		    }
		    if (canCapture && rand.nextInt(100) < LORD_CAPTURE_CHANCE) {
			    // capture lord. If player is winner, player captures all lords. Else allocates prisoners randomly
			    boolean freed = false;
			    if (playerWon) {
				    captor = LordController.getPlayerLord();
			    } else {
				    captor = maybeCaptor.get(rand.nextInt(maybeCaptor.size()));
				    // check if captor will free lord immediately
				    switch (captor.getPersonality()) {
					    case UPSTANDING:
						    captorFreeChance = 90;
						    break;
					    case MARTIAL:
						    captorFreeChance = 75;
						    break;
					    case CALCULATING:
						    captorFreeChance = 60 + Math.max(0, RelationController.getRelation(captor, defeated) / 5);
						    break;
					    case QUARRELSOME:
						    captorFreeChance = 20 + Math.max(0, RelationController.getRelation(captor, defeated) / 3);
						    break;
				    }
				    captorFreeChance += RelationController.getRelation(captor, defeated) / 5;
				    freed = rand.nextInt(100) < captorFreeChance;
			    }


			    if (freed) {
					String message = StringUtil.getString(CATEGORY_UI, "lord_defeated_released",
							defeated.getTitle() + " " + defeated.getLordAPI().getNameString(),
							captor.getFaction().getDisplayName());
					Utils.showUIMessageCaptureStatus(message,defeated.getFaction());
				    RelationController.modifyRelation(captor, defeated, defeated.getPersonality().releaseRepGain);
			    } else {
				    String message = StringUtil.getString(CATEGORY_UI, "lord_defeated_captured",
								    defeated.getTitle() + " " + defeated.getLordAPI().getNameString(),
								    captor.getFaction().getDisplayName());
				    Utils.showUIMessageCaptureStatus(message,defeated.getFaction());

				    defeated.setCurrAction(LordAction.IMPRISONED);
				    defeated.setCaptor(captor.getLordAPI().getId());
				    captor.addPrisoner(defeated.getLordAPI().getId());
			    }

		    } else {
			    String message = StringUtil.getString(CATEGORY_UI, "lord_defeated_escape",
							    defeated.getTitle() + " " + defeated.getLordAPI().getNameString());
			    Utils.showUIMessageCaptureStatus(message,defeated.getFaction());
		    }

		    // check if defeated lord had prisoners. These prisoners are either freed or transferred to new captor
		    for (String prisonerId : defeated.getPrisoners()) {
			    if (LordController.getLordById(prisonerId) == null) {
				    //todo: somehow, starlords are getting lords that don't exist in normal gameplay as prisoners. I dont know if its because the lords are being removed by mistake, or something else. but regardless, this helps prevent issues from missing lords being freed or taken prisoner.
				    log.info("[Star Lords] ERROR: failed to get prisoner from lords inventory. missing lords personID should have been: " + prisonerId);
				    continue;
			    }
			    boolean freed = true;
			    Lord existingPrisoner = LordController.getLordById(prisonerId);
			    if (captor != null && captor.getFaction().isHostileTo(existingPrisoner.getFaction())) {
				    if (captor.isPlayer()) {
					    freed = false;
				    } else {
					    switch (captor.getPersonality()) {
						    case UPSTANDING:
							    captorFreeChance = 90;
							    break;
						    case MARTIAL:
							    captorFreeChance = 75;
							    break;
						    case CALCULATING:
							    captorFreeChance = 60 + Math.max(0, RelationController.getRelation(captor, existingPrisoner) / 5);
							    break;
						    case QUARRELSOME:
							    captorFreeChance = 20 + Math.max(0, RelationController.getRelation(captor, existingPrisoner) / 3);
							    break;
					    }
					    captorFreeChance += RelationController.getRelation(captor, existingPrisoner) / 5;
					    freed = Utils.nextInt(100) < captorFreeChance;
				    }
			    }

			    LordRequest prisonerRequest = RequestController.getCurrentRequest(existingPrisoner, LordRequest.PRISON_BREAK);
			    if (prisonerRequest != null && prisonerRequest.hasPlayerAgreed() && playerWon) {
				    DefectionUtils.performDefection(existingPrisoner, Utils.getRecruitmentFaction(), true);
				    freed = true;
			    }

			    if (freed) {
				    if (captor != null) {
					    RelationController.modifyRelation(captor, existingPrisoner, existingPrisoner.getPersonality().releaseRepGain);
				    }

				    existingPrisoner.setCaptor(null);
				    String message = StringUtil.getString(CATEGORY_UI, "lord_freed_captivity",
								    existingPrisoner.getTitle() + " " + existingPrisoner.getLordAPI().getNameString());
				    Utils.showUIMessageCaptureStatus(message,existingPrisoner.getFaction());


			    } else {
				    existingPrisoner.setCaptor(captor.getLordAPI().getId());
				    captor.addPrisoner(existingPrisoner.getLordAPI().getId());
			    }
		    }
		    defeated.getPrisoners().clear();
		    EventController.removeFromAllEvents(defeated);

		    // penalize player for lords dying under their rule or command
		    if (defeated.isPlayerDirected()) {
			    Utils.adjustPlayerReputation(defeated.getLordAPI(), -5);
		    } else if (defeated.getFaction().isPlayerFaction()) {
			    int change = Math.min(3, 1 + defeated.getPersonality().ordinal());
			    Utils.adjustPlayerReputation(defeated.getLordAPI(), -change);
		    } else {
			    int change = Math.min(3, 1 + defeated.getPersonality().ordinal());
			    RelationController.modifyLoyalty(defeated, -change);
		    }

		    // Faction marshal gets controversy for lord being defeated
		    //if (!Utils.isMinorFaction(defeated.getFaction())) {
			    Lord marshal = LordController.getLordOrPlayerById(
					    PoliticsController.getLaws(defeated.getFaction()).getMarshal());
			    if (marshal != null) {
				    marshal.setControversy(Math.min(100, marshal.getControversy() + 1));
			    }
		    //}

		    //Check if lord was defeated in a battle that was leading a campaign, and if so, remove the campaign
		    LordEvent campaign = EventController.getCurrentCampaign(defeated.getFaction());
		    if (campaign != null) {
			    if (campaign.getOriginator() != null)
				    if (campaign.getOriginator().equals(defeated))
					    EventController.endCampaign(campaign);
		    }


		    String afterPrisonerCalculation = "[Star Lords] Winner Lords:" + System.lineSeparator()
				    + Utils.printLordsWithPrisoners(winnerLords) + System.lineSeparator()
				    + "[Star Lords] Loser Lords:" + System.lineSeparator()
				    + Utils.printLordsWithPrisoners(loserLords) + System.lineSeparator();

		    if (Utils.printLordsWithPrisonerProblems(false, false)) {
			    log.info(beforePrisonerCalculation);
			    log.info(afterPrisonerCalculation);
			    Utils.printLordsWithPrisonerProblems(false, true);
		    }
	    }
    }

    private void levelUpWithChance(CampaignFleetAPI fleet, int chance) {
        for (FleetMemberAPI ship : fleet.getFleetData().getMembersListCopy()) {
            if (ship.isFighterWing() || ship.getCaptain() == null || ship.isFlagship()) {
                continue;
            }
            int currLevel = ship.getCaptain().getStats().getLevel();
            if (currLevel < LordFleetFactory.OFFICER_MAX_LEVEL && Utils.nextInt(100) < chance) {
                ship.getCaptain().getStats().setLevel(currLevel + 1);
                LordFleetFactory.upskillOfficer(ship.getCaptain(), true);
            }
        }
    }

    private void backupLordFleet(CampaignFleetAPI fleet,Lord lord){
        lord.setBackupFleet(null);
        if (lord.getFleet() == null && !fleet.getFleetData().getMembersListCopy().isEmpty() && fleet.isAlive()){
            lord.setBackupFleet(fleet);
        }
    }
}
