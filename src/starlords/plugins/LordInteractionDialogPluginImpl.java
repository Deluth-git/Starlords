package starlords.plugins;

import starlords.ai.LordAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BeginMission;
import com.fs.starfarer.api.util.Misc;
import starlords.controllers.*;
import starlords.faction.LawProposal;
import org.apache.log4j.Logger;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordEvent;
import starlords.ui.MissionPreviewIntelPlugin;
import starlords.util.DefectionUtils;
import starlords.util.StringUtil;
import starlords.util.Utils;

import java.awt.*;
import java.util.*;

import static com.fs.starfarer.api.impl.campaign.rulecmd.BeginMission.TEMP_MISSION_KEY;
import static starlords.util.Constants.*;

public class LordInteractionDialogPluginImpl implements InteractionDialogPlugin {

    public static Logger log = Global.getLogger(LordInteractionDialogPluginImpl.class);
    static String CATEGORY = "starlords_lords_dialog";
    enum OptionId {
        INIT,
        ASK_CURRENT_TASK,
        ASK_QUESTION,
        ASK_QUEST,
        ASK_LOCATION,
        ASK_LOCATION_CHOICE,
        SWAY_PROPOSAL_PLAYER,
        SWAY_PROPOSAL_COUNCIL,
        SWAY_PROPOSAL_BARGAIN,
        SPEAK_PRIVATELY,
        ASK_WORLDVIEW,
        ASK_LIEGE_OPINION,
        SUGGEST_CEASEFIRE,
        SUGGEST_DEFECT,
        BARGAIN_DEFECT,
        JUSTIFY_DEFECT,
        CONFIRM_SUGGEST_DEFECT,
        SUGGEST_ACTION,
        FOLLOW_ME,
        STOP_FOLLOW_ME,
        LEAVE,
    }

    InteractionDialogPlugin prevPlugin;
    InteractionDialogAPI dialog;
    TextPanelAPI textPanel;
    OptionPanelAPI options;
    VisualPanelAPI visual;

    private HashMap<String, Lord> lordsReference;
    private CampaignFleetAPI lordFleet;
    Lord targetLord;
    private OptionId nextState;
    private boolean hasGreeted;

    // for defection
    private String justification;
    private String bargainAmount;
    private int claimStrength;

    // whether political sway is for or against the law
    private boolean swayFor;
    private LawProposal proposal;

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
        optionSelected(null, OptionId.INIT);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        OptionId option;
        if (optionData instanceof OptionId) {
            option = (OptionId) optionData;
        } else if (nextState != null) {
            option = nextState;
        } else {
            return;
        }

        // TODO
        boolean willEngage = false;
        boolean hostile = false;
        if (lordFleet.getFaction().isHostileTo(Global.getSector().getPlayerFleet().getFaction())) {
            hostile = true;
            SectorEntityToken target = ((ModularFleetAIAPI) lordFleet.getAI()).getTacticalModule().getTarget();
            if (lordFleet.isHostileTo(Global.getSector().getPlayerFleet()) && Global.getSector().getPlayerFleet().equals(target)) {
                willEngage = true;
            }
        }

        if (prevPlugin.equals(this) && !visual.isShowingPersonInfo(targetLord.getLordAPI())) {
            visual.showPersonInfo(targetLord.getLordAPI(), false, true);
        }

        PersonAPI player = Global.getSector().getPlayerPerson();
        FactionAPI faction;
        Random rand;
        switch (option) {
            case INIT:
                options.clearOptions();
                if (!hostile) {
                    boolean isFeast = targetLord.getCurrAction() == LordAction.FEAST;
                    LordEvent feast = null;
                    String greeting = "greeting_" + targetLord.getPersonality().toString().toLowerCase() + "_";
                    if (isFeast) {
                        feast = EventController.getCurrentFeast(targetLord.getLordAPI().getFaction());
                        if (feast.getOriginator().equals(targetLord)) {
                            greeting = "greeting_host_feast";
                        } else {
                            greeting += "feast";
                        }
                    } else if (targetLord.isKnownToPlayer()) {
                        greeting += relToString(targetLord.getLordAPI().getRelToPlayer().getRepInt());
                    } else {
                        greeting += "first";
                    }

                    if (!hasGreeted) {
                        hasGreeted = true;
                        textPanel.addParagraph(StringUtil.getString(CATEGORY, greeting, player.getNameString()));
                    }
                    if (isFeast) {
                        if (!feast.getOriginator().isFeastInteracted()) {
                            feast.getOriginator().setFeastInteracted(true);
                            feast.getOriginator().getLordAPI().getRelToPlayer().adjustRelationship(0.03f, null);
                            textPanel.addPara(StringUtil.getString(
                                    CATEGORY, "relation_increase",
                                    feast.getOriginator().getLordAPI().getNameString(), "3"), Color.GREEN);
                        }
                        for (Lord lord : feast.getParticipants()) {
                            if (!lord.isFeastInteracted()) {
                                lord.setFeastInteracted(true);
                                lord.getLordAPI().getRelToPlayer().adjustRelationship(0.02f, null);
                                textPanel.addPara(StringUtil.getString(
                                        CATEGORY, "relation_increase", lord.getLordAPI().getNameString(), "2"), Color.GREEN);
                            }
                        }
                    }
                    options.addOption(StringUtil.getString(CATEGORY, "option_ask_current_task"), OptionId.ASK_CURRENT_TASK);
                    options.addOption(StringUtil.getString(CATEGORY, "option_ask_question"), OptionId.ASK_QUESTION);
                    options.addOption(StringUtil.getString(CATEGORY, "option_suggest_action"), OptionId.SUGGEST_ACTION);
                } else {
                    String greeting = "battle_" + targetLord.getPersonality().toString().toLowerCase();
                    if (!hasGreeted) {
                        hasGreeted = true;
                        textPanel.addParagraph(StringUtil.getString(CATEGORY, greeting, player.getNameString()));
                    }
                    if (willEngage) {
                        options.addOption(StringUtil.getString(CATEGORY, "option_avoid_battle"), OptionId.SUGGEST_CEASEFIRE, Color.YELLOW, "-10 Relations if successful");
                        //options.setTooltip(OptionId.SUGGEST_CEASEFIRE, "-10 Relations if successful");
                    }
                }

                if (!targetLord.isKnownToPlayer()) {
                    textPanel.addPara("Added intel on " + targetLord.getLordAPI().getNameString(), Color.GREEN);
                    targetLord.setKnownToPlayer(true);
                }
                options.addOption(StringUtil.getString(CATEGORY, "option_speak_privately"), OptionId.SPEAK_PRIVATELY);
                options.addOption("Cut the comm link.", OptionId.LEAVE);
                break;
            case ASK_CURRENT_TASK:
                String id = "current_task_desc_none";
                String args = "";
                if (targetLord.getCurrAction() != null) {
                    id = "current_task_desc_" + targetLord.getCurrAction().base.toString().toLowerCase();
                }
                if (targetLord.getTarget() != null) {
                    args = targetLord.getTarget().getName();
                }
                textPanel.addParagraph(StringUtil.getString(CATEGORY, id, args));
                break;
            case ASK_QUESTION:
                options.clearOptions();
                options.addOption(StringUtil.getString(CATEGORY, "option_ask_location"), OptionId.ASK_LOCATION);
                options.addOption(StringUtil.getString(CATEGORY, "option_ask_quest"), OptionId.ASK_QUEST);
                faction = targetLord.getFaction();
                if (faction.equals(Utils.getRecruitmentFaction())) {
                    LawProposal councilProposal = PoliticsController.getCurrProposal(faction);
                    if (councilProposal != null
                            && !councilProposal.getPledgedAgainst().contains(targetLord.getLordAPI().getId())
                            && !councilProposal.getPledgedFor().contains(targetLord.getLordAPI().getId())) {
                        String arg;
                        if (councilProposal.isPlayerSupports()) {
                            arg = "support";
                            swayFor = true;
                        } else {
                            arg = "oppose";
                            swayFor = false;
                        }
                        options.addOption(StringUtil.getString(CATEGORY, "option_sway_council", arg), OptionId.SWAY_PROPOSAL_COUNCIL);
                    }
                    LawProposal playerProposal = PoliticsController.getProposal(LordController.getPlayerLord());
                    if (playerProposal != null && !playerProposal.equals(councilProposal)
                            && !playerProposal.getPledgedAgainst().contains(targetLord.getLordAPI().getId())
                            && !playerProposal.getPledgedFor().contains(targetLord.getLordAPI().getId())) {
                        options.addOption(StringUtil.getString(CATEGORY, "option_sway_player"), OptionId.SWAY_PROPOSAL_PLAYER);
                    }
                }
                options.addOption(StringUtil.getString(CATEGORY, "option_nevermind"), OptionId.INIT);
                break;
            case SWAY_PROPOSAL_PLAYER:
            case SWAY_PROPOSAL_COUNCIL:
                faction = targetLord.getFaction();
                if (option == OptionId.SWAY_PROPOSAL_PLAYER) {
                    proposal = PoliticsController.getProposal(LordController.getPlayerLord());
                    swayFor = true;
                } else {
                    proposal = PoliticsController.getCurrProposal(faction);
                }
                int opinion = PoliticsController.getApproval(targetLord, proposal, false).one;
                // lord can either agree/refuse outright, suggest a bribe, or suggest player support their proposal
                rand = new Random(targetLord.getLordAPI().getId().hashCode()
                        + Global.getSector().getClock().getTimestamp());
                LawProposal lordProposal = PoliticsController.getProposal(targetLord);
                int relation = RelationController.getRelation(targetLord, LordController.getPlayerLord());
                int agreeChance = 0;
                int bribeChance = 0;
                int bargainChance = 0;
                if (!swayFor) opinion *= -1;
                if (!targetLord.isSwayed() && opinion > -20) {
                    agreeChance = 10 * (relation / 10 + opinion + 12); // agree at -10 or above
                    bribeChance = 25 + relation;
                    switch (targetLord.getPersonality()) {
                        case UPSTANDING:
                            bribeChance /= 4;
                            break;
                        case MARTIAL:
                            bribeChance /= 2;
                            break;
                        case CALCULATING:
                            bribeChance *= 2;
                            break;
                    }
                    // lord needs a proposal worth supporting to bargain
                    if (lordProposal != null && lordProposal.getSupporters().size() > 1
                            && !lordProposal.isPlayerSupports()
                            && relation > Utils.getThreshold(RepLevel.SUSPICIOUS)) {
                        bargainChance = 100;
                    }
                }

                targetLord.setSwayed(true);
                if (rand.nextInt(100) < agreeChance) {
                    if (opinion > 0) {
                        textPanel.addPara(StringUtil.getString(CATEGORY, "option_accept_sway_redundant"));
                    } else {
                        textPanel.addPara(StringUtil.getString(CATEGORY, "option_accept_sway"));
                    }
                    if (swayFor) {
                        proposal.getPledgedFor().add(targetLord.getLordAPI().getId());
                    } else {
                        proposal.getPledgedAgainst().add(targetLord.getLordAPI().getId());
                    }
                    PoliticsController.updateProposal(proposal);
                    optionSelected(null, OptionId.ASK_QUESTION);
                } else if (rand.nextInt(100) < bargainChance) {
                    textPanel.addPara(StringUtil.getString(CATEGORY, "option_bargain_sway_support"));
                    options.clearOptions();
                    bargainAmount = "proposal"; // TODO make some flags
                    options.addOption("Pledge to support proposal: " + lordProposal.getTitle(), OptionId.SWAY_PROPOSAL_BARGAIN);
                    options.addOption("Refuse", OptionId.ASK_QUESTION);


                } else if (rand.nextInt(100) < bribeChance) {
                    textPanel.addPara(StringUtil.getString(CATEGORY, "option_bargain_sway_money"));
                    bargainAmount = "credits";
                    options.clearOptions();
                    options.addOption("Offer 100,000 credits", OptionId.SWAY_PROPOSAL_BARGAIN);
                    float playerWealth = Global.getSector().getPlayerFleet().getCargo().getCredits().get();
                    if (playerWealth < 100000) {
                        options.setEnabled(OptionId.SWAY_PROPOSAL_BARGAIN, false);
                        options.setTooltip(OptionId.SWAY_PROPOSAL_BARGAIN, "Insufficient funds.");
                    }
                    options.addOption("Refuse", OptionId.ASK_QUESTION);

                } else {
                    textPanel.addPara(StringUtil.getString(CATEGORY, "option_refuse_sway"));
                    optionSelected(null, OptionId.ASK_QUESTION);
                }
                break;
            case SWAY_PROPOSAL_BARGAIN:
                textPanel.addPara(StringUtil.getString(CATEGORY, "option_accept_sway_bargain"));
                if (bargainAmount.equals("credits")) {
                    Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(100000);
                    textPanel.addPara("Lost " + 100000 + " credits.", Color.RED);
                } else {
                    LawProposal bargainProposal = PoliticsController.getProposal(targetLord);
                    bargainProposal.getPledgedFor().add(LordController.getPlayerLord().getLordAPI().getId());
                    bargainProposal.setPlayerSupports(true);
                }
                if (swayFor) {
                    proposal.getPledgedFor().add(targetLord.getLordAPI().getId());
                } else {
                    proposal.getPledgedAgainst().add(targetLord.getLordAPI().getId());
                }
                PoliticsController.updateProposal(proposal);
                optionSelected(null, OptionId.ASK_QUESTION);
                break;
            case ASK_LOCATION:
                textPanel.addParagraph(StringUtil.getString(CATEGORY, "accept_ask_location"));
                options.clearOptions();
                lordsReference.clear();
                nextState = OptionId.ASK_LOCATION_CHOICE;
                ArrayList<Lord> toAdd = new ArrayList<>();
                for (Lord lord : LordController.getLordsList()) {
                    if (!lord.equals(targetLord)
                            && lord.getLordAPI().getFaction().equals(targetLord.getLordAPI().getFaction())) {
                        toAdd.add(lord);
                    }
                }
                Utils.canonicalLordSort(toAdd);
                for (Lord lord : toAdd) {
                    String desc = lord.getTitle() + " " + lord.getLordAPI().getNameString();
                    options.addOption(desc, new Object());
                    lordsReference.put(desc, lord);
                }
                options.addOption(StringUtil.getString(CATEGORY, "option_nevermind"), OptionId.ASK_QUESTION);
                break;
            case ASK_LOCATION_CHOICE:
                Lord lord = lordsReference.get(optionText);
                if (lord != null && lord.getLordAPI().getFleet().isAlive()) {
                    textPanel.addParagraph("I heard that " + optionText + " was last sighted around " + Utils.getNearbyDescription(
                            lord.getLordAPI().getFleet()));
                } else {
                    textPanel.addParagraph("I dont know where " + optionText + " is.");
                }
                optionSelected(null, OptionId.INIT);
                break;
            case SPEAK_PRIVATELY:
                if (targetLord.willSpeakPrivately()) {
                    options.clearOptions();
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "accept_speak_privately"));
                    options.addOption(StringUtil.getString(CATEGORY, "option_ask_worldview"), OptionId.ASK_WORLDVIEW);
                    if (!targetLord.getLordAPI().getFaction().equals(Global.getSector().getPlayerFaction())) {
                        if (targetLord.getLiegeName() != null) {
                            options.addOption(StringUtil.getString(CATEGORY, "option_ask_liege_opinion", targetLord.getLiegeName()), OptionId.ASK_LIEGE_OPINION);
                        } else {
                            options.addOption(StringUtil.getString(CATEGORY, "option_ask_liege_opinion_decentralized"), OptionId.ASK_LIEGE_OPINION);
                        }
                    }
                    options.addOption(StringUtil.getString(CATEGORY, "option_nevermind"), OptionId.INIT);
                } else {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "refuse_speak_privately_" +  targetLord.getPersonality().toString().toLowerCase()));
                }
                break;
            case ASK_WORLDVIEW:
                textPanel.addParagraph(StringUtil.getString(
                        CATEGORY, "worldview_" + targetLord.getPersonality().toString().toLowerCase()));
                if (!targetLord.isPersonalityKnown()) {
                    targetLord.setPersonalityKnown(true);
                    textPanel.addParagraph("Updated lord info!", Color.GREEN);
                }
                break;
            case ASK_LIEGE_OPINION:
                int loyalty = RelationController.getLoyalty(targetLord);
                if (targetLord.getLiegeName() == null) {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "liege_opinion_decentralized_" + relToString(loyalty)));
                } else {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "liege_opinion_" + relToString(loyalty), targetLord.getLiegeName()));
                }
                faction = Utils.getRecruitmentFaction();
                if (loyalty < Utils.getThreshold(RepLevel.FAVORABLE) && !faction.equals(targetLord.getLordAPI().getFaction())) {
                    options.clearOptions();
                    options.addOption(StringUtil.getString(CATEGORY, "suggest_defect", faction.getDisplayNameWithArticle()), OptionId.SUGGEST_DEFECT);
                    options.addOption("Interesting.", OptionId.INIT);
                }
                break;
            case SUGGEST_DEFECT:
                textPanel.addParagraph(StringUtil.getString(CATEGORY, "consider_defect"));
                nextState = OptionId.JUSTIFY_DEFECT;
                options.clearOptions();
                options.addOption(StringUtil.getString(CATEGORY, "suggest_defection_calculating"), OptionId.BARGAIN_DEFECT);
                options.addOption(StringUtil.getString(CATEGORY, "suggest_defection_upstanding"), new Object());
                options.addOption(StringUtil.getString(CATEGORY, "suggest_defection_martial"), new Object());
                options.addOption(StringUtil.getString(CATEGORY, "suggest_defection_quarrelsome"), new Object());
                options.addOption(StringUtil.getString(CATEGORY, "suggest_defection_abort"), OptionId.INIT);
                break;
            case JUSTIFY_DEFECT:
                // compute justification strength
                // upstanding checks player colony stability and count, marshal checks player level and fleet size,
                // calculating does bribes, quarrelsome auto-passes
                faction = Utils.getRecruitmentFaction();
                justification = CLAIM_CALCULATING; // calculating has an additional state so we don't know the option text for sure
                String justificationStr = "offer";
                if (optionText.equals(StringUtil.getString(CATEGORY, "suggest_defection_quarrelsome"))) {
                    justification = CLAIM_QUARRELSOME;
                    justificationStr = "history";
                } else if (optionText.equals(StringUtil.getString(CATEGORY, "suggest_defection_upstanding"))) {
                    justification = CLAIM_UPSTANDING;
                    justificationStr = "colonial stability and success";
                } else if (optionText.equals(StringUtil.getString(CATEGORY, "suggest_defection_martial"))) {
                    justification = CLAIM_MARTIAL;
                    justificationStr = "military and personal strength";
                }
                if (justification.equals(CLAIM_CALCULATING)) {
                    bargainAmount = optionText;
                    if (optionText.contains("500,000 Credits")
                            || optionText.contains(StringUtil.getString(CATEGORY_TITLES, "title_default_1"))) {
                        claimStrength = SOMEWHAT_JUSTIFIED;
                    } else if (optionText.contains("2,000,000 Credits")
                            || optionText.contains(StringUtil.getString(CATEGORY_TITLES, "title_default_2"))) {
                        claimStrength = FULLY_JUSTIFIED;
                    } else {
                        claimStrength = COMPLETELY_UNJUSTIFIED;
                    }
                } else {
                    bargainAmount = null;
                    claimStrength = DefectionUtils.computeClaimJustification(justification, faction);
                }
                String claimStr = "completely unjustified";
                Color claimColor = Color.RED;
                if (claimStrength == FULLY_JUSTIFIED) {
                    claimStr = "fully justified";
                    claimColor = Color.GREEN;
                } else if (claimStrength == SOMEWHAT_JUSTIFIED) {
                    claimStr = "partially justified";
                    claimColor = Color.YELLOW;
                }
                textPanel.addPara("Based on your " + justificationStr + ", your claim is seen as " + claimStr + ".", Color.ORANGE, claimColor, claimStr);
                // Breakdown defection factors - base personality, faction legitimacy, faction loyalty vs player relation, ties with subordinates, justification effect, and rng
                // use player loyalty here even if recruiting for other factions
                if (DefectionUtils.computeFactionLegitimacy(faction) > 7) {
                    textPanel.addPara("Hmm, your faction is well-established and has a legitimate claim to unite the sector.");
                } else {
                    textPanel.addPara("Hmm, your faction is still a minor player and would be a risk to join.");
                }
                if (DefectionUtils.computeRelativeFactionPreference(targetLord, Global.getSector().getPlayerFaction()) > 0) {
                    textPanel.addPara("It's true that I consider your faction more respectable than my own.");
                } else {
                    textPanel.addPara("Frankly, I prefer my faction over yours.");
                }
                if (DefectionUtils.computeRelativeLordPreference(targetLord, faction) > 0) {
                    textPanel.addPara("I would prefer working with your lords over my present company.");
                } else {
                    textPanel.addPara("I have good friends that I would not want to lose.");
                }
                if (justification.contains(targetLord.getPersonality().toString().toLowerCase()) && claimStrength > 0)  {
                    textPanel.addParagraph("Additionally, I must admit that your justification is not without merit.");
                } else {
                    textPanel.addParagraph("Additionally, I find your justification unconvincing.");
                }
                textPanel.addParagraph("Lastly, changing loyalties is no small decision. I must consider the consequences on my reputation carefully.");
                options.clearOptions();
                options.addOption(StringUtil.getString(CATEGORY, "confirm_suggest_defect"), OptionId.CONFIRM_SUGGEST_DEFECT);
                options.setTooltip(OptionId.CONFIRM_SUGGEST_DEFECT, "-10 relations if failed");
                options.addOption(StringUtil.getString(CATEGORY, "abort_suggest_defect"), OptionId.INIT);
                break;
            case BARGAIN_DEFECT:
                // 2 mil gold, 500k gold, or titles
                textPanel.addParagraph(StringUtil.getString(CATEGORY, "bargain_defect"));
                options.clearOptions();
                float playerWealth = Global.getSector().getPlayerFleet().getCargo().getCredits().get();
                if (playerWealth > SMALL_BRIBE) {
                    options.addOption("500,000 Credits", new Object());
                }
                if (playerWealth > LARGE_BRIBE) {
                    options.addOption("2,000,000 Credits", new Object());
                }
                if (Misc.getCommissionFaction() == null) {
                    int numRankTwo = 0;
                    int numRankOne = 0;
                    for (Lord lord2 : LordController.getLordsList()) {
                        if (lord2.getLordAPI().getFaction().equals(Global.getSector().getPlayerFaction())) {
                            if (lord2.getRanking() == 1) numRankOne++;
                            if (lord2.getRanking() == 2) numRankTwo++;
                        }
                    }
                    if (numRankOne < 3) {
                        options.addOption("The title of " + StringUtil.getString(CATEGORY_TITLES, "title_default_1"), new Object());
                    }
                    if (numRankTwo == 0) {
                        options.addOption("The title of " + StringUtil.getString(CATEGORY_TITLES, "title_default_2"), new Object());
                    }
                }
                options.addOption(StringUtil.getString(CATEGORY, "bargain_defect_nothing"), new Object());
                nextState = OptionId.JUSTIFY_DEFECT;
                break;
            case CONFIRM_SUGGEST_DEFECT:
                faction = Utils.getRecruitmentFaction();
                rand = new Random(targetLord.getLordAPI().getId().hashCode() + Global.getSector().getClock().getTimestamp());
                int claimConcern = 0;
                if (justification.contains(targetLord.getPersonality().toString().toLowerCase())) claimConcern = 100;
                int baseReluctance = DefectionUtils.getBaseReluctance(targetLord);
                int legitimacyFactor = DefectionUtils.computeFactionLegitimacy(faction);
                int loyaltyFactor = DefectionUtils.computeRelativeFactionPreference(targetLord, Global.getSector().getPlayerFaction());
                int companionFactor = DefectionUtils.computeRelativeLordPreference(targetLord, faction);
                int randFactor = rand.nextInt(10);
                //log.info("DEBUG defection: " + baseReluctance + ", " + legitimacyFactor + ", " + loyaltyFactor + ", " + companionFactor + ", " + claimStrength + ", " + randFactor);
                int totalWeight = baseReluctance + legitimacyFactor + loyaltyFactor + companionFactor + Math.min(claimConcern, claimStrength) + randFactor;
                if (totalWeight > 0) {
                    String liegeName = Utils.getLiegeName(faction);
                    textPanel.addPara(StringUtil.getString(CATEGORY, "accept_defect", player.getNameString(), liegeName), Color.GREEN);
                    Global.getSoundPlayer().playUISound("ui_char_level_up", 1, 1);
                    DefectionUtils.performDefection(targetLord, faction, false);
                    // resolve bribe effects
                    if (bargainAmount != null) {
                        if (bargainAmount.contains("Credits")) {
                            if (claimStrength == FULLY_JUSTIFIED) {
                                Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(LARGE_BRIBE);
                                textPanel.addPara("Lost " + LARGE_BRIBE + " credits.", Color.RED);
                            }
                            if (claimStrength == SOMEWHAT_JUSTIFIED) {
                                Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(SMALL_BRIBE);
                                textPanel.addPara("Lost " + SMALL_BRIBE + " credits.", Color.RED);
                            }
                        } else if (bargainAmount.contains("title")) {
                            if (claimStrength == FULLY_JUSTIFIED) targetLord.setRanking(2);
                            if (claimStrength == SOMEWHAT_JUSTIFIED) targetLord.setRanking(1);
                        }
                    }
                } else {
                    textPanel.addPara(StringUtil.getString(CATEGORY, "refuse_defect"));
                    Global.getSoundPlayer().playUISound("ui_rep_drop", 1, 1);
                    targetLord.getLordAPI().getRelToPlayer().adjustRelationship(-0.1f, null);
                    textPanel.addParagraph(StringUtil.getString(
                            CATEGORY, "relation_decrease", targetLord.getLordAPI().getNameString(), "10"), Color.RED);

                }
                optionSelected(null, OptionId.INIT);
                break;
            case ASK_QUEST:
                boolean questGiven = false;
                if (!QuestController.isQuestGiven(targetLord)) {
                    MarketAPI tmp = lordFleet.getMarket();
                    MarketAPI tmp2 = targetLord.getLordAPI().getMarket();
                    SectorEntityToken marketEntity = targetLord.getClosestBase();
                    MarketAPI newMarket;
                    if (marketEntity != null) {
                        newMarket = marketEntity.getMarket();
                    } else {  // last resort, just make it some market for now
                        newMarket = Global.getSector().getEconomy().getMarketsCopy().get(0);
                    }
                    lordFleet.setMarket(newMarket);
                    targetLord.getLordAPI().setMarket(newMarket);
                    ArrayList<Misc.Token> params = new ArrayList<>();
                    params.add(new Misc.Token(QuestController.getQuestId(targetLord), Misc.TokenType.LITERAL));
                    params.add(new Misc.Token("false", Misc.TokenType.LITERAL));
                    new BeginMission().execute("", dialog, params, new HashMap<String, MemoryAPI>());
                    //log.info("DEBUG: Creating quest of type " + params.get(0).toString());
                    BaseHubMission mission = (BaseHubMission) Global.getSector().getMemoryWithoutUpdate().get(TEMP_MISSION_KEY);
                    if (mission != null && !(mission instanceof BaseCustomBounty)) {  // TODO bounties dont seem to work
                        MissionPreviewIntelPlugin intel = new MissionPreviewIntelPlugin(mission);
                        Global.getSector().getIntelManager().addIntel(intel);
                        questGiven = true;
                        textPanel.addParagraph(StringUtil.getString(CATEGORY, "quest_available"));
                        textPanel.addPara("New intel added!", Color.GREEN);
                    }
                    QuestController.setQuestGiven(targetLord, true);
                    lordFleet.setMarket(tmp);
                    targetLord.getLordAPI().setMarket(tmp2);
                }

                if (!questGiven) {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "no_quest_available"));
                }
                break;
            case SUGGEST_ACTION:
                boolean isBusy = false;
                // check if lord is leading an event or dead
                LordEvent feast = EventController.getCurrentFeast(targetLord.getLordAPI().getFaction());
                LordEvent campaign = EventController.getCurrentCampaign(targetLord.getLordAPI().getFaction());
                if ((feast != null && feast.getOriginator().equals(targetLord))
                        || (campaign != null && campaign.getOriginator().equals(targetLord))
                        || lordFleet.isEmpty()) {
                    isBusy = true;
                }

                if (targetLord.getLordAPI().getRelToPlayer().isAtBest(RepLevel.NEUTRAL)) {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "refuse_suggest_action_relations"));
                } else if (isBusy) {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "refuse_suggest_action_busy"));
                } else {
                    // TODO relations tooltips
                    options.clearOptions();
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "consider_suggest_action"));
                    if (targetLord.getCurrAction() == LordAction.FOLLOW
                            && Global.getSector().getPlayerFleet().equals(targetLord.getTarget())) {
                        options.addOption(StringUtil.getString(CATEGORY, "option_stop_follow_me"), OptionId.STOP_FOLLOW_ME);

                    } else {
                        options.addOption(StringUtil.getString(CATEGORY, "option_follow_me"), OptionId.FOLLOW_ME);
                    }
                    options.addOption(StringUtil.getString(CATEGORY, "option_nevermind"), OptionId.INIT);
                }
                break;
            case FOLLOW_ME:
                LordAI.playerOrder(targetLord, LordAction.FOLLOW, Global.getSector().getPlayerFleet());
                textPanel.addParagraph(StringUtil.getString(CATEGORY, "accept_suggest_action"));
                if (!targetLord.getFaction().equals(Global.getSector().getPlayerFaction())) {
                    targetLord.getLordAPI().getRelToPlayer().adjustRelationship(-0.02f, null);
                    textPanel.addParagraph(StringUtil.getString(
                            CATEGORY, "relation_decrease", targetLord.getLordAPI().getNameString(), "2"), Color.RED);
                }
                optionSelected(null, OptionId.INIT);
                break;
            case STOP_FOLLOW_ME:
                targetLord.setCurrAction(null);
                lordFleet.clearAssignments();
                textPanel.addParagraph(StringUtil.getString(CATEGORY, "accept_suggest_action"));
                optionSelected(null, OptionId.INIT);
                break;
            case SUGGEST_CEASEFIRE:
                options.removeOption(OptionId.SUGGEST_CEASEFIRE);
                if (targetLord.getLordAPI().getRelToPlayer().isAtWorst(RepLevel.WELCOMING)) {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "accept_mercy"));
                    textPanel.addParagraph(StringUtil.getString(
                            CATEGORY, "relation_decrease", targetLord.getLordAPI().getNameString(), "10"), Color.RED);
                    targetLord.getLordAPI().getRelToPlayer().adjustRelationship(-0.1f, null);
                    lordFleet.getAI().doNotAttack(Global.getSector().getPlayerFleet(), 7);
                    Misc.setFlagWithReason(lordFleet.getMemoryWithoutUpdate(),
                            MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, "starlords", true, 7);
                } else {
                    textPanel.addParagraph(StringUtil.getString(CATEGORY, "refuse_mercy"));
                }
                break;
            case LEAVE:
                if (prevPlugin.equals(this)) {
                    dialog.dismiss();
                } else {
                    dialog.setPlugin(prevPlugin);
                    prevPlugin.optionSelected(null, FleetInteractionDialogPluginImpl.OptionId.CUT_COMM);
                }
                break;
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
}
