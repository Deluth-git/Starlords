package starlords.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lombok.Setter;
import org.apache.log4j.Logger;
import starlords.person.Lord;

import java.util.List;

public class LordCampaignFleetAI implements CampaignFleetAIAPI {
    //todo: remove this class, as its never used? as far as I can tell?
    private static Logger log = Global.getLogger(LordCampaignFleetAI.class);
    private Lord lord;
    private CampaignFleetAIAPI base;
    @Setter
    private boolean transitMode;

    public LordCampaignFleetAI(Lord lord, CampaignFleetAIAPI base) {
        this.lord = lord;
        this.base = base;
    }

    @Override
    public void advance(float amount) {
        base.advance(amount);
    }

    @Override
    public boolean isHostileTo(CampaignFleetAPI other) {
        return base.isHostileTo(other);
    }

    @Override
    public EncounterOption pickEncounterOption(FleetEncounterContextPlugin context, CampaignFleetAPI otherFleet) {
        return base.pickEncounterOption(context, otherFleet);
    }

    @Override
    public boolean wantsToJoin(BattleAPI battle, boolean playerInvolved) {
        return base.wantsToJoin(battle, playerInvolved);
    }

    @Override
    public PursuitOption pickPursuitOption(FleetEncounterContextPlugin context, CampaignFleetAPI otherFleet) {
        return base.pickPursuitOption(context, otherFleet);
    }

    @Override
    public InitialBoardingResponse pickBoardingResponse(FleetEncounterContextPlugin context, FleetMemberAPI toBoard, CampaignFleetAPI otherFleet) {
        return base.pickBoardingResponse(context, toBoard, otherFleet);
    }

    @Override
    public List<FleetMemberAPI> pickBoardingTaskForce(FleetEncounterContextPlugin context, FleetMemberAPI toBoard, CampaignFleetAPI otherFleet) {
        return base.pickBoardingTaskForce(context, toBoard, otherFleet);
    }

    @Override
    public BoardingActionDecision makeBoardingDecision(FleetEncounterContextPlugin context, FleetMemberAPI toBoard, CrewCompositionAPI maxAvailable) {
        return base.makeBoardingDecision(context, toBoard, maxAvailable);
    }

    @Override
    public void performCrashMothballingPriorToEscape(FleetEncounterContextPlugin context, CampaignFleetAPI playerFleet) {
        base.performCrashMothballingPriorToEscape(context, playerFleet);
    }

    @Override
    public void reportNearbyAction(ActionType type, SectorEntityToken actor, SectorEntityToken target, String responseVariable) {
        base.reportNearbyAction(type, actor, target, responseVariable);
    }

    @Override
    public String getActionTextOverride() {
        return base.getActionTextOverride();
    }

    @Override
    public void setActionTextOverride(String actionTextOverride) {
        base.setActionTextOverride(actionTextOverride);
    }

    @Override
    public FleetAssignmentDataAPI getCurrentAssignment() {
        return base.getCurrentAssignment();
    }

    @Override
    public List<FleetAssignmentDataAPI> getAssignmentsCopy() {
        return base.getAssignmentsCopy();
    }

    @Override
    public void addAssignmentAtStart(FleetAssignment assignment, SectorEntityToken target, float maxDurationInDays, Script onCompletion) {
        addAssignmentAtStart(assignment, target, maxDurationInDays, null, onCompletion);
    }

    @Override
    public void removeFirstAssignment() {
        base.removeFirstAssignment();
    }

    @Override
    public void addAssignmentAtStart(FleetAssignment assignment, SectorEntityToken target, float maxDurationInDays, String actionText, Script onCompletion) {
//        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
//        for (int i = 1; i < elements.length; i++) {
//            StackTraceElement s = elements[i];
//            log.info("at " + s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
//        }
        base.addAssignmentAtStart(assignment, target, maxDurationInDays, actionText, onCompletion);
    }

    @Override
    public void removeFirstAssignmentIfItIs(FleetAssignment assignment) {
        base.removeFirstAssignmentIfItIs(assignment);
    }

    @Override
    public boolean isCurrentAssignment(FleetAssignment assignment) {
        return base.isCurrentAssignment(assignment);
    }

    @Override
    public void addAssignment(FleetAssignment assignment, SectorEntityToken target, float maxDurationInDays, Script onCompletion) {
        addAssignment(assignment, target, maxDurationInDays, null, false, null, onCompletion);
    }

    @Override
    public void addAssignment(FleetAssignment assignment, SectorEntityToken target, float maxDurationInDays, String actionText, Script onCompletion) {
        addAssignment(assignment, target, maxDurationInDays, actionText, false, null, onCompletion);
    }

    @Override
    public boolean isFleeing() {
        return base.isFleeing();
    }

    @Override
    public void removeAssignment(FleetAssignmentDataAPI assignment) {
        base.removeAssignment(assignment);
    }

    @Override
    public void clearAssignments() {
        base.clearAssignments();
    }

    @Override
    public void dumpResourcesIfNeeded() {
        base.dumpResourcesIfNeeded();
    }

    @Override
    public void notifyInteractedWith(CampaignFleetAPI otherFleet) {
        base.notifyInteractedWith(otherFleet);
    }

    @Override
    public FleetAssignment getCurrentAssignmentType() {
        return base.getCurrentAssignmentType();
    }

    @Override
    public void doNotAttack(SectorEntityToken other, float durDays) {
        base.doNotAttack(other, durDays);
    }

    @Override
    public EncounterOption pickEncounterOption(FleetEncounterContextPlugin context, CampaignFleetAPI otherFleet, boolean pureCheck) {
        return base.pickEncounterOption(context, otherFleet, pureCheck);
    }

    @Override
    public FleetActionTextProvider getActionTextProvider() {
        return base.getActionTextProvider();
    }

    @Override
    public void setActionTextProvider(FleetActionTextProvider actionTextProvider) {
        base.setActionTextProvider(actionTextProvider);
    }

    @Override
    public void addAssignment(FleetAssignment assignment, SectorEntityToken target, float maxDurationInDays, String actionText, boolean addTimeToNext, Script onStart, Script onCompletion) {
        base.addAssignment(assignment, target, maxDurationInDays, actionText, addTimeToNext, onStart, onCompletion);
    }

    @Override
    public boolean isMaintainingContact() {
        return base.isMaintainingContact();
    }
}
