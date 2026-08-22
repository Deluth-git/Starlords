package starlords.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import starlords.controllers.LordController;
import starlords.controllers.PoliticsController;
import starlords.faction.LawLevel;
import starlords.faction.LawProposal;
import starlords.faction.Lawset;
import starlords.person.Lord;
import starlords.util.StringUtil;
import starlords.util.Utils;

import java.awt.*;
import java.util.*;
import java.util.List;

import static starlords.controllers.PoliticsController.RELATION_CHANGE_LAW_IMPORTANT;
import static starlords.controllers.PoliticsController.RELATION_CHANGE_LAW_NORMAL;
import static starlords.util.Constants.*;

public class CouncilIntelPlugin extends BaseIntelPlugin {

    private static Object SUPPORT_BUTTON = new Object();
    private static Object OPPOSE_BUTTON = new Object();
    private static Object VETO_BUTTON = new Object();
    private static Object FORCE_PASS_BUTTON = new Object();
    private static int NUM_LORDS_PER_ROW = 3;
    private static float pad = 3;
    private static float opad = 10;

    private static CouncilIntelPlugin instance;

    private FactionAPI faction;
    private LawProposal currProposal;
    private ArrayList<Lord> supporters;
    private ArrayList<Lord> opposition;

    private CouncilIntelPlugin(){
        setNew(false);
        faction = Utils.getRecruitmentFaction();
        supporters = new ArrayList<>();
        opposition = new ArrayList<>();
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float headerHeight = 25;
        faction = Utils.getRecruitmentFaction();
        currProposal = PoliticsController.getCurrProposal(faction);
        Color uiColor = Global.getSettings().getBasePlayerColor();

        TooltipMakerAPI header = panel.createUIElement(width, headerHeight, false);
        header.addSectionHeading(getName(),
                faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
        if (currProposal != null) {
            Lord proposer = LordController.getLordOrPlayerById(currProposal.getOriginator());
            TooltipMakerAPI imageText = header.beginImageWithText(proposer.getLordAPI().getPortraitSprite(), 64);
            imageText.addPara("Under Consideration: " + currProposal.getSummary(), uiColor, pad);
            imageText.addPara("Proposer: " + proposer.getTitle() + " " + proposer.getLordAPI().getNameString(), uiColor, pad);
            imageText.addPara("Time remaining: " + Math.max(0, PoliticsController.getTimeRemainingDays(faction)) + " Day(s)", uiColor, pad);
            if (currProposal.isPlayerSupports()) {
                imageText.addPara("Your vote: Aye", pad, uiColor, LIGHT_GREEN, "Aye");
            } else {
                imageText.addPara("Your vote: Nay", pad, uiColor, LIGHT_RED, "Nay");
            }
            header.addImageWithText(pad);
        } else {
            header.addPara("Council in recess.", uiColor, pad);
            header.addPara("Time remaining: " + Math.max(0, PoliticsController.getTimeRemainingDays(faction)) + " Day(s)", uiColor, pad);
        }
        panel.addUIElement(header).inTL(0, 0);

        if (currProposal == null) {
            // summarize last vote, then exit
            LawProposal prevProposal = PoliticsController.getPrevProposal(faction);
            if (prevProposal != null) {
                TooltipMakerAPI prevHeader = panel.createUIElement(width, height - header.getHeightSoFar(), true);
                prevHeader.addSectionHeading("Last Council Results",
                        faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
                if (prevProposal.isPassed()) {
                    prevHeader.addPara("Proposal passed: " + prevProposal.getTitle(), LIGHT_GREEN, opad);
                } else  {
                    prevHeader.addPara("Proposal failed: " + prevProposal.getTitle(), LIGHT_RED, opad);
                }
                Pair<Lord, Lord> result = PoliticsController.getBeneficiaryVictim(prevProposal);
                Lord beneficiary = result.one;
                Lord victim = result.two;
                boolean playerImportant = (prevProposal.originator.equals(Global.getSector().getPlayerPerson().getId())
                        || (beneficiary != null && beneficiary.isPlayer())) && prevProposal.isPlayerSupports();
                playerImportant |= (victim != null && victim.isPlayer() && !prevProposal.isPlayerSupports());
                for (String supporterStr : prevProposal.getSupporters()) {
                    Lord supporter = LordController.getLordById(supporterStr);
                    boolean important = supporter.equals(beneficiary)
                            || supporter.getLordAPI().getId().equals(prevProposal.originator);
                    int delta = (prevProposal.isPlayerSupports() ? 1 : -1) *
                            ((important || playerImportant) ? RELATION_CHANGE_LAW_IMPORTANT : RELATION_CHANGE_LAW_NORMAL);
                    TooltipMakerAPI imageText = prevHeader.beginImageWithText(supporter.getLordAPI().getPortraitSprite(), 32);
                    if (delta < 0) {
                        imageText.addPara("Relations with " + supporter.getLordAPI().getNameString() + " decreased by " + (-1 * delta), LIGHT_RED, pad);
                    } else {
                        imageText.addPara("Relations with " + supporter.getLordAPI().getNameString() + " increased by " + delta, LIGHT_GREEN, pad);

                    }
                    prevHeader.addImageWithText(pad);

                }
                for (String opposerStr : prevProposal.getOpposers()) {
                    Lord opposer = LordController.getLordById(opposerStr);
                    boolean important = opposer.equals(victim);
                    int delta = (prevProposal.isPlayerSupports() ? -1 : 1) *
                            ((important || playerImportant) ? RELATION_CHANGE_LAW_IMPORTANT : RELATION_CHANGE_LAW_NORMAL);
                    TooltipMakerAPI imageText = prevHeader.beginImageWithText(opposer.getLordAPI().getPortraitSprite(), 32);
                    if (delta < 0) {
                        imageText.addPara("Relations with " + opposer.getLordAPI().getNameString() + " decreased by " + (-1 * delta), LIGHT_RED, pad);
                    } else {
                        imageText.addPara("Relations with " + opposer.getLordAPI().getNameString() + " increased by " + delta, LIGHT_GREEN, pad);

                    }
                    prevHeader.addImageWithText(pad);
                }
                panel.addUIElement(prevHeader).belowLeft(header, opad);
            }
            return;
        }

        PersonAPI ruler = Utils.getLeader(faction);
        TooltipMakerAPI voteHeader = panel.createUIElement(width, headerHeight, false);
        voteHeader.addSectionHeading("Voting Chamber",
                faction.getBrightUIColor(), faction.getDarkUIColor(), Alignment.LMID, opad);
        panel.addUIElement(voteHeader).belowLeft(header, 0);

        // this is pretty inefficient
        supporters.clear();
        opposition.clear();
        Pair<Integer, Integer> votes = PoliticsController.countVotes(currProposal, supporters, opposition);
        int totalSupport = votes.one;
        int totalOpposition = votes.two;

        TooltipMakerAPI supportPanel = panel.createUIElement(75, headerHeight, false);
        TooltipMakerAPI opposePanel = panel.createUIElement(75, headerHeight, false);
        TooltipMakerAPI supportButtonPanel = panel.createUIElement(100, headerHeight, false);
        TooltipMakerAPI opposeButtonPanel = panel.createUIElement(100, headerHeight, false);
        supportPanel.setParaInsigniaLarge();
        opposePanel.setParaInsigniaLarge();
        LabelAPI supportLabel = supportPanel.addPara(Integer.toString(totalSupport), LIGHT_GREEN, 0);
        LabelAPI opposeLabel = opposePanel.addPara(Integer.toString(totalOpposition), LIGHT_RED, 0);
        supportLabel.setAlignment(Alignment.MID);
        opposeLabel.setAlignment(Alignment.MID);
        // add tooltip for political weight breakdown
        Pair<List<String>, List<String>> result = generatePoliticalWeightBreakdown();
        supportPanel.addTooltipToPrevious(new ToolTip(200, result.one, true),
                TooltipMakerAPI.TooltipLocation.BELOW);
        opposePanel.addTooltipToPrevious(new ToolTip(200, result.two, true),
                TooltipMakerAPI.TooltipLocation.BELOW);

        supportButtonPanel.addButton("Support", SUPPORT_BUTTON, faction.getBrightUIColor(), faction.getDarkUIColor(), 100, 20, opad);
        ButtonAPI opposeButton = opposeButtonPanel.addButton("Oppose", OPPOSE_BUTTON, uiColor, LIGHT_RED, 100, 20, opad);
        if (currProposal.getPledgedFor().contains(LordController.getPlayerLord().getLordAPI().getId())) {
            opposeButton.setEnabled(false);
            opposeButtonPanel.addTooltipToPrevious(new ToolTip(200,
                    "You pledged to support this proposal."), TooltipMakerAPI.TooltipLocation.BELOW);
        }
        panel.addUIElement(supportPanel).belowLeft(voteHeader, opad);
        panel.addUIElement(supportButtonPanel).rightOfTop(supportPanel, opad);

        int progress = 0;
        if (totalSupport + totalOpposition != 0) {
            progress = (100 * totalSupport / (totalSupport + totalOpposition));
        }
        CustomPanelAPI debateBar = createDebateBar(panel, width * 0.8f, 40, progress);
        panel.addComponent(debateBar).belowLeft(supportPanel, 2 * opad);
        populateLords(panel, debateBar);

        panel.addUIElement(opposePanel).aboveRight(debateBar, 2 * opad);
        panel.addUIElement(opposeButtonPanel).leftOfTop(opposePanel, opad);
        if (ruler != null) {
            TooltipMakerAPI rulerPanel = panel.createUIElement(36, headerHeight, false);
            rulerPanel.addImage(ruler.getPortraitSprite(), 36, -5);
            if (currProposal.isLiegeSupports()) {
                panel.addUIElement(rulerPanel).rightOfTop(supportButtonPanel, opad);
            } else {
                panel.addUIElement(rulerPanel).leftOfTop(opposeButtonPanel, opad);
            }
            // add ruler reasons
            // create tooltip
            ArrayList<String> tooltip = new ArrayList<>();
            tooltip.add(ruler.getRank() + " " + ruler.getNameString());
            if (!faction.equals(Global.getSector().getPlayerFaction())) {
                tooltip.addAll(currProposal.getLiegeReasons());
                String total = Integer.toString(currProposal.getLiegeVal());
                if (currProposal.getLiegeVal() > 0) total = "+" + total;
                tooltip.add(total + " Total");
            }
            rulerPanel.addTooltipToPrevious(new ToolTip(250,
                    tooltip, true), TooltipMakerAPI.TooltipLocation.BELOW);
        }

        if (faction.equals(Global.getSector().getPlayerFaction())) {
            LawLevel crownAuthority = PoliticsController.getLaws(faction).getCrownAuthority();
            TooltipMakerAPI rulerPanel = panel.createUIElement(100, headerHeight, false);
            ButtonAPI vetoButton = rulerPanel.addButton("Veto", VETO_BUTTON, faction.getBrightUIColor(), faction.getDarkUIColor(), 100, 20, opad);
            if (!crownAuthority.isAtLeast(LawLevel.MEDIUM)) {
                vetoButton.setEnabled(false);
                rulerPanel.addTooltipToPrevious(new ToolTip(200, "Requires medium " + Lawset.LawType.CROWN_AUTHORITY.lawName), TooltipMakerAPI.TooltipLocation.BELOW);
            }
            ButtonAPI forcePassButton = rulerPanel.addButton("Force Pass", FORCE_PASS_BUTTON, faction.getBrightUIColor(), faction.getDarkUIColor(), 100, 20, opad);
            if (!crownAuthority.isAtLeast(LawLevel.HIGH)) {
                forcePassButton.setEnabled(false);
                rulerPanel.addTooltipToPrevious(new ToolTip(200, "Requires high " + Lawset.LawType.CROWN_AUTHORITY.lawName), TooltipMakerAPI.TooltipLocation.BELOW);;
            }
            panel.addUIElement(rulerPanel).rightOfTop(debateBar, opad);
        }
    }

    // progress is [0, 100]
    public CustomPanelAPI createDebateBar(CustomPanelAPI panel, float width, float height, int progress) {
        CustomPanelAPI ret = panel.createCustomPanel(width, height, new BaseCustomUIPanelPlugin());
        TooltipMakerAPI proBarPanel = ret.createUIElement(
                width * progress / 100, height, false);
        TooltipMakerAPI antiBarPanel = ret.createUIElement(
                width * (100 - progress) / 100, height, false);
        ButtonAPI button = proBarPanel.addButton("", new Object(), LIGHT_GREEN, LIGHT_GREEN,
                Alignment.MID, CutStyle.NONE, width * progress / 100, height - opad, 0);
        button.setClickable(false);
        button.setMouseOverSound(null);
        button.setHighlightBrightness(0);
        button = antiBarPanel.addButton("", new Object(), LIGHT_RED, LIGHT_RED, Alignment.MID,
                CutStyle.NONE, width * (100 - progress) / 100, height - opad, 0);
        button.setClickable(false);
        button.setMouseOverSound(null);
        button.setHighlightBrightness(0);
        ret.addUIElement(proBarPanel).inTL(0, opad);
        ret.addUIElement(antiBarPanel).rightOfTop(proBarPanel, 0);
        return ret;
    }

    // creates UI displaying whether lords support or oppose the current proposal
    private void populateLords(CustomPanelAPI panel, UIComponentAPI prev) {
        int totalPro = 0;
        int totalAgainst = 0;
        ArrayList<UIComponentAPI> prevPro = new ArrayList<>();
        ArrayList<UIComponentAPI> prevAgainst = new ArrayList<>();
        prevPro.add(prev);
        prevAgainst.add(prev);
        for (int i = 0; i < supporters.size(); i++) {
            Lord lord = supporters.get(i);
            TooltipMakerAPI lordPanel = panel.createUIElement(120, 48, false);
            TooltipMakerAPI imagePanel = lordPanel.beginImageWithText(lord.getLordAPI().getPortraitSprite(), 48);
            imagePanel.addPara("Aye", LIGHT_GREEN, 3);
            lordPanel.addImageWithText(3);
            // create tooltip
            ArrayList<String> tooltip = new ArrayList<>();
            tooltip.add(lord.getLordAPI().getNameString());
            if (!lord.isPlayer()) {
                int idx = i - (supporters.size() - currProposal.getSupporters().size()); // assumes player is first in list
                tooltip.addAll(currProposal.getSupporterReasons().get(idx));
                tooltip.add("+" + currProposal.getSupporterVals().get(idx) + " Total");
            }
            lordPanel.addTooltipToPrevious(new ToolTip(200, tooltip, true), TooltipMakerAPI.TooltipLocation.BELOW);
            if (totalPro % NUM_LORDS_PER_ROW == 0) {
                panel.addUIElement(lordPanel).belowLeft(prevPro.get(Math.max(0, prevPro.size() - NUM_LORDS_PER_ROW)), pad);
            } else {
                panel.addUIElement(lordPanel).rightOfTop(prevPro.get(prevPro.size() - 1), 0);
            }
            totalPro++;
            prevPro.add(lordPanel);
        }
        for (int i = 0; i < opposition.size(); i++) {
            Lord lord = opposition.get(i);
            TooltipMakerAPI lordPanel = panel.createUIElement(120, 48, false);
            TooltipMakerAPI imagePanel = lordPanel.beginImageWithText(lord.getLordAPI().getPortraitSprite(), 48);
            imagePanel.addPara("Nay", LIGHT_RED, 3);
            lordPanel.addImageWithText(3);
            // create tooltip
            ArrayList<String> tooltip = new ArrayList<>();
            tooltip.add(lord.getLordAPI().getNameString());
            if (!lord.isPlayer()) {
                int idx = i - (opposition.size() - currProposal.getOpposers().size()); // assumes player is first in list
                tooltip.addAll(currProposal.getOpposerReasons().get(idx));
                tooltip.add(currProposal.getOpposersVals().get(idx) + " Total");
            }
            lordPanel.addTooltipToPrevious(new ToolTip(200, tooltip, true), TooltipMakerAPI.TooltipLocation.BELOW);
            if (totalAgainst % NUM_LORDS_PER_ROW == 0) {
                panel.addUIElement(lordPanel).belowRight(prevAgainst.get(Math.max(0, prevAgainst.size() - NUM_LORDS_PER_ROW)), pad);
            } else {
                panel.addUIElement(lordPanel).leftOfTop(prevAgainst.get(prevAgainst.size() - 1), 0);
            }
            totalAgainst++;
            prevAgainst.add(lordPanel);
        }
    }

    private Pair<List<String>, List<String>> generatePoliticalWeightBreakdown() {
        List<String> supporterTooltip = new ArrayList<>();
        List<String> oppositionTooltip = new ArrayList<>();
        for (Lord supporter : supporters) {
            supporterTooltip.add("+" + PoliticsController.getPoliticalWeight(supporter)
                    + " " + supporter.getLordAPI().getNameString());
        }
        for (Lord opposer : opposition) {
            oppositionTooltip.add("+" + PoliticsController.getPoliticalWeight(opposer)
                    + " " + opposer.getLordAPI().getNameString());
        }
        PersonAPI ruler = Utils.getLeader(currProposal.faction);
        if (ruler != null) {
            if (currProposal.isLiegeSupports()) {
                if (currProposal.getFaction() == LordController.getPlayerLord().getFaction() && Misc.getCommissionFaction() == null)
                    supporterTooltip.add("+" + PoliticsController.PLAYER_EXTRA_COUNCIL_WEIGHT
                            + " Extra Player Weight");
                supporterTooltip.add("x" + String.format("%.1f", PoliticsController.getLiegeMultiplier(currProposal.faction))
                        + " " + ruler.getNameString());
            } else {
                if (currProposal.getFaction() == LordController.getPlayerLord().getFaction() && Misc.getCommissionFaction() == null)
                    oppositionTooltip.add("+" + PoliticsController.PLAYER_EXTRA_COUNCIL_WEIGHT
                            + " Extra Player Weight");
                oppositionTooltip.add("x" + String.format("%.1f",PoliticsController.getLiegeMultiplier(currProposal.faction))
                        + " " + ruler.getNameString());

            }
        }
        return new Pair<>(supporterTooltip, oppositionTooltip);
    }

    @Override
    public void createConfirmationPrompt(Object buttonId, TooltipMakerAPI prompt) {
        if (buttonId == VETO_BUTTON) {
            prompt.addPara("This will automatically fail the proposal and anger its supporters. Continue?", pad);
        } else if (buttonId == FORCE_PASS_BUTTON) {
            prompt.addPara("This will automatically pass the proposal and greatly anger its opposition. Continue?", pad);
        }
    }

    @Override
    public boolean doesButtonHaveConfirmDialog(Object buttonId) {
        if (buttonId == VETO_BUTTON || buttonId == FORCE_PASS_BUTTON) return true;
        return super.doesButtonHaveConfirmDialog(buttonId);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId == SUPPORT_BUTTON) {
            currProposal.setPlayerSupports(true);
            PoliticsController.updateProposal(currProposal);
            ui.updateUIForItem(this);
        } else if (buttonId == OPPOSE_BUTTON) {
            currProposal.setPlayerSupports(false);
            PoliticsController.updateProposal(currProposal);
            ui.updateUIForItem(this);
        } else if (buttonId == VETO_BUTTON) {
            // TODO fix these sounds
            Global.getSoundPlayer().playUISound("hit_glancing", 1, 1);
            PoliticsController.vetoProposal(faction);
            ui.updateUIForItem(this);
        } else if (buttonId == FORCE_PASS_BUTTON) {
            Global.getSoundPlayer().playUISound("hit_glancing", 1, 1);
            PoliticsController.forcePassProposal(faction);
            ui.updateUIForItem(this);
        }
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public String getName() {
        return Utils.getRecruitmentFaction().getDisplayName() + " Council";
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return Utils.getRecruitmentFaction();
    }

    @Override
    public String getIcon() {
        return getFactionForUIColors().getCrest();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(StringUtil.getString(CATEGORY_UI, "politics_category"));
        return tags;
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_2;
    }

    public static CouncilIntelPlugin getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(CouncilIntelPlugin.class);
            if (intel.isEmpty()) {
                instance = new CouncilIntelPlugin();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one CouncilIntelPlugin intel registered");
                }
                instance = (CouncilIntelPlugin) intel.get(0);
            }
        }
        return instance;
    }

    public static CouncilIntelPlugin getInstance() {
        return getInstance(false);
    }
}
