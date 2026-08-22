package starlords.plugins;

import lombok.Getter;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import starlords.controllers.*;
import starlords.faction.LawProposal;
import org.apache.log4j.Logger;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;
import starlords.util.GenderUtils;
import starlords.util.StringUtil;
import starlords.util.Utils;
import starlords.util.dialogControler.DialogDataHolder;
import starlords.util.dialogControler.DialogOption;
import starlords.util.dialogControler.DialogSet;

import java.awt.*;
import java.util.*;

public class LordInteractionDialogPluginImpl implements InteractionDialogPlugin {
    public static DialogDataHolder DATA_HOLDER;
    public static Logger log = Global.getLogger(LordInteractionDialogPluginImpl.class);
    static String CATEGORY = "starlords_lords_dialog";

    InteractionDialogPlugin prevPlugin;
    InteractionDialogAPI dialog;
    TextPanelAPI textPanel;
    OptionPanelAPI options;
    VisualPanelAPI visual;

    private HashMap<String, Lord> lordsReference;
    private CampaignFleetAPI lordFleet;
    @Getter
    protected static Lord targetLord;
    @Getter
    private static boolean hasGreeted;
    @Getter
    private static String DialogType;

    // for defection
    private String justification;
    private String bargainAmount;
    private int claimStrength;

    // whether political sway is for or against the law
    private boolean swayFor;
    private LawProposal proposal;

    public static String startingDialogSet;
    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        prevPlugin = dialog.getPlugin();
        dialog.setPlugin(this);
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        // TODO unsafe cast
        lordFleet = (CampaignFleetAPI) dialog.getInteractionTarget();

        lordsReference = new HashMap<>();
        targetLord = LordController.getLordById(lordFleet.getCommander().getId());
        if (DATA_HOLDER == null || !DATA_HOLDER.getTargetID().equals(targetLord.getLordAPI().getId())){
            DATA_HOLDER = new DialogDataHolder();
            DATA_HOLDER.setTargetID(targetLord.getLordAPI().getId());
        }
        DialogType = setDialogType();
        startingDialogSet = setStartingDialogSet();
        hasGreeted = false;
        DialogOption option = new DialogOption(setStartingDialog(),new ArrayList<>());
        optionSelected(null, option);
    }
    protected String setDialogType(){
        return "default";
    }
    protected String setStartingDialogSet(){
        return "optionSet_greeting";
    }
    protected String setStartingDialog(){
        return "greeting";
    }
    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionSelected_NEW(optionText,optionData)){
            return;
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    private String relToString(int rel) {
        if (rel <= -100 * RepLevel.HOSTILE.getMax()) {
            return "hated";
        } else if (rel <= -100 * RepLevel.SUSPICIOUS.getMax()) {
            return "disliked";
        } else if (rel <= 100 * RepLevel.FAVORABLE.getMax()) {
            return "neutral";
        } else if (rel <= 100 * RepLevel.WELCOMING.getMax()) {
            return "friendly";
        } else {
            return "trusted";
        }
    }

    private void displayAcceptSuggestAction() {
        if (targetLord.getFaction().isPlayerFaction()) {
            textPanel.addParagraph(StringUtil.getString(
                    CATEGORY, "accept_suggest_action_subject",
                    GenderUtils.sirOrMaam(Global.getSector().getPlayerPerson(), false)));
        } else {
            textPanel.addParagraph(StringUtil.getString(CATEGORY, "accept_suggest_action"));
        }
    }

    private boolean optionSelected_NEW(String optionText, Object optionData){
        /*so... it is finaly time! time for me to do the finaly checks.
        * bug prevention:
        *   1) (DONE) add in a function that will be there until the next save compatible update. it will set anyone that is married to be married to the player.
        *   2) (DONE) merge this branch with the branch from fixes. its important ok?
        * dialog improvements
        *   ...
        *   OK: so I require some rules for this.
        *       -) process:
        *           I am going to go from the bottom of the dialogs, to the top, doing everything I can along the way to improve things.
        *           I have compleated the 'template'. now its time to apply the template to the other starlords...
        *               -I AM CURRENTLY 'DONE THE JSON. I need to fix issues though.
        *           lastly, I need to go into the defalt dialog, and remove all the lines (setting them to produce 'error'.)
        *       1) (done) fist of all, I am going to be forced to replace all the current personality dialogs with the template. KEEP THE OLD DIALOGS. I will needs them whenever I end up fing up.
        *       2) (I only needed to do this once.)[addons]. for many addons, complicated things happen (like ransoming prisoners.) this should be in the defalt dialog as like, [addons_textName_textType]. ([addons_marage_refusealHarsh] for example).
        *           -note: I need to deside how... many of the addons I am going to need.
        *                  I think for most of them I am NOT going to an [addon]. most dont need one.
        *       3) (done?) [options] will now be set to [optionSets]. this is so I can change every option all at once, if required.
        *       4) (done) items like [accept defection] should have the different options be set when you press it. not from options.
        *           -the reason for this is to allow users to have custom accept and refuse conditions.
        *               -although, this does have a disadvantage. I need a custom 'refuse' message for when the lord can never ever defect. I will add this as an rule? but only afterwards.
        *           -for things with advanced calculations (like defection) I should have a custom calculation that can be put into data instead of what im currently doing...
        *       5) (no.) lastly, I want to add additional dialogSets in the dialog. so its easyer to add conditions for things like (acsept marage) and what not.
        *
        *   notes:
        *       1) suggestDefectionCanConsider. this dialogSet might need to be moved, so its part of the line that you ask? then again, I think its fine?
        *       2) (DONE. untested )canDefectOption. the data here needs to be moved to the answer itself.
        *           -I also need to make it so the data that sets this happens in a set data.
        *       3) (done main changes. need to implement. untested.)for "swayProposal_forCounsel_bargain", "swayProposal_againstCounsel_bargain", "swayProposal_forPlayer_bargain" requies additional testing. to make sure they work.
        *           -NOTE: when I apply the rules, make sure the 'min' and 'max' values are set to the right value (all people have diffrent values for that.)
        *   1) (done) I need to go through all the dialog, and merge the dialog into a type of 'template' that I can add additional data to.
        *   2) (done) I need to go into all lines and imporve them just a bit. by adding in the new utility functions I added in the first place.
        *   3) (done) I need to go into anything that uses a diffrent option set, and make it so instead of setting all the options right there, it gets a different option set instead (because having a cenralized option set to change prevents modders from having to keep trake of every time I add a dialog option. it is important.)
        * dialog fixes:
        *   1) (done) speak privately right now, does not have an option that prevents it from working. I messed up somewhere. (or maybe just for certain lord types?)
        *       -(done) also, when you are in the prisoners dialog, runs the normal dialog list when it attmepts to return the player to 'greetings'
        *   2) (this can be done latter.) add in a custom defection refusal to the template, for when lords can never defect by the player hands.
        *   3) (done) many options are calling optionSet greetings. I need to do one of the following:
        *       -I made it so there is a 'backToStart' dialog set. (this in theory removes the need for custom support of greeting dialog)
        * dialog logs:
        *   1) remove most the logs. the ones i do chose to keep, should only be the most basic ones, and they should have the log class set currently.
        *
        * round of testing:
        *   I have done all the lines!?!!?!?! what????
        * */
        if (optionData instanceof DialogOption){
            if (prevPlugin.equals(this) && !visual.isShowingPersonInfo(targetLord.getLordAPI())) {
                visual.showPersonInfo(targetLord.getLordAPI(), false, true);
            }
            DialogOption data = (DialogOption) optionData;
            data.applyAddons(textPanel,options,dialog,targetLord);
            String selectedOption = data.optionID;

            Lord secondLord = data.getTargetLord();
            MarketAPI targetMarket = data.getTargetMarket();
            if (selectedOption.equals(setStartingDialog())){
                optionSelected_greetings(selectedOption,secondLord,targetMarket);
                return true;
            }
            switch (selectedOption){
                case "exitDialog":
                    optionSelected_exitDialog();
                    break;
                case "":
                    break;
                case "current_task_desc":
                    optionSelected_askCurrentTask(selectedOption,secondLord,targetMarket);
                    break;
                case "spend_time_together":
                    optionSelected_suggestDate(selectedOption,secondLord,targetMarket);
                    break;
                default:
                    DialogSet.addParaWithInserts(selectedOption,targetLord,secondLord,targetMarket,textPanel,options,dialog);
                    break;
            }
            return true;
        }
        return false;
    }


    private void optionSelected_greetings(String selectedOption,Lord secondLord,MarketAPI targetMarket){
        log.info("RUNNING THE CORRECT GREETING OPTION DATA");
        boolean isFeast = targetLord.getCurrAction() == LordAction.FEAST;
        LordEvent feast = isFeast ? EventController.getCurrentFeast(targetLord.getLordAPI().getFaction()) : null;
        //only run greetings if player has not yet heard them this conversation
        DialogSet.addParaWithInserts(selectedOption,targetLord,secondLord,targetMarket,textPanel,options,dialog);
        if (!hasGreeted){
            hasGreeted = true;
        }
        //if lord is newly met, add intil on lord.
        if (!targetLord.isKnownToPlayer()) {
            DialogSet.addParaWithInserts("addedIntel",targetLord,secondLord,targetMarket,textPanel,options,dialog);
            targetLord.setKnownToPlayer(true);
        }
        //if this is a feast, apply rep gained.
        /*if(feast != null) {
            log.info("APPLYING REP CHANGE DATA");
            if (!feast.getOriginator().isFeastInteracted()) {
                feast.getOriginator().setFeastInteracted(true);
                applyRepIncrease(textPanel, feast.getOriginator(),secondLord,targetMarket, 3);
            }
            for (Lord lord : feast.getParticipants()) {
                if (!lord.isFeastInteracted()) {
                    lord.setFeastInteracted(true);
                    applyRepIncrease(textPanel, lord,secondLord,targetMarket, 2);
                }
            }
        }*/
    }
    private void optionSelected_askCurrentTask(String selectedOption,Lord secondLord,MarketAPI targetMarket){
        //ok, so: the link to this needs to be set as 'current_task_desc'.
        //the options for this needs to be set to: 'greetings'.
        String args = "";
        if (targetLord.getCurrAction() != null) {
            selectedOption += "_" + targetLord.getCurrAction().base.toString().toLowerCase();
        }else{
            selectedOption += "_none";
        }
        if (targetLord.getTarget() != null) {
            args = targetLord.getTarget().getName();
        }
        HashMap<String,String> inserts = new HashMap<>();
        inserts.put("%c0",args);
        DialogSet.addParaWithInserts(selectedOption,targetLord,secondLord,targetMarket,textPanel,options,dialog,false,inserts);
    }
    private void optionSelected_suggestDate(String selectedOption,Lord secondLord,MarketAPI targetMarket){
        int dateType = 1 + Utils.rand.nextInt(36);
        DialogSet.addParaWithInserts(selectedOption+dateType,targetLord,secondLord,targetMarket,textPanel,options,dialog);
    }
    private void optionSelected_exitDialog(){
        if (prevPlugin.equals(this)) {
            dialog.dismiss();
        } else {
            dialog.setPlugin(prevPlugin);
            prevPlugin.optionSelected(null, FleetInteractionDialogPluginImpl.OptionId.CUT_COMM);
        }
    }
}
