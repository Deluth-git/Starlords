package ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import controllers.LordController;
import controllers.PoliticsController;
import faction.LawProposal;
import person.Lord;
import util.StringUtil;
import util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Set;

import static util.Constants.CATEGORY_UI;
import static util.Constants.LIGHT_GREEN;

public class ProposalIntelPlugin extends BaseIntelPlugin {

    private LawProposal proposal;
    private boolean isPlayerProposal;

    private static float pad = 3;
    private static float opad = 10;
    private static Object SUPPORT_BUTTON = new Object();
    private static Object OPPOSE_BUTTON = new Object();

    public ProposalIntelPlugin(LawProposal proposal) {
        setNew(false);
        this.proposal = proposal;
        isPlayerProposal = proposal.getOriginator().equals(LordController.getPlayerLord().getLordAPI().getId());
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color uiColor = Global.getSettings().getBasePlayerColor();
        Lord originator = LordController.getLordOrPlayerById(proposal.getOriginator());
        FactionAPI faction = originator.getFaction();
        if (proposal.targetLord != null) {
            info.addImages(256, 128, pad, 0, originator.getLordAPI().getPortraitSprite(),
                    LordController.getLordOrPlayerById(proposal.getTargetLord()).getLordAPI().getPortraitSprite());
        } else {
            info.addImages(128, 128, pad, 0, originator.getLordAPI().getPortraitSprite());
        }
        info.addPara("Proposal: " + proposal.getSummary(), uiColor, opad);
        info.addPara("Proposed by: " + LordController.getLordOrPlayerById(
                proposal.getOriginator()).getLordAPI().getNameString(), uiColor, opad);
        info.addPara("Total Support: " + proposal.getTotalSupport(), uiColor, opad);

        LabelAPI supportText = info.addPara("Supporters: ", uiColor, opad);
        if (!proposal.getSupporters().isEmpty() || proposal.isPlayerSupports()) {
            int addend = 0;
            if (proposal.isPlayerSupports()) {
                addend = 1;
            }
            String[] portraits = new String[proposal.getSupporters().size() + addend];
            if (proposal.isPlayerSupports()) {
                portraits[0] = LordController.getPlayerLord().getLordAPI().getPortraitSprite();
            }
            for (int i = addend; i < portraits.length; i++) {
                portraits[i] = LordController.getLordById(proposal.getSupporters().get(i - addend)).getLordAPI().getPortraitSprite();
            }
            info.addImages(48 * portraits.length, 48, pad, 0, portraits);
        }
        info.addButton("Support", SUPPORT_BUTTON, faction.getBrightUIColor(), faction.getDarkUIColor(), 100, 20, opad);
        info.addButton("Oppose", OPPOSE_BUTTON, faction.getBrightUIColor(), faction.getDarkUIColor(), 100, 20, opad);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId == SUPPORT_BUTTON) {
            proposal.setPlayerSupports(true);
        } else if (buttonId == OPPOSE_BUTTON) {
            proposal.setPlayerSupports(false);
        }
        ui.updateUIForItem(this);
        ui.updateIntelList(true);
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }

    @Override
    public boolean hasLargeDescription() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return !Utils.getRecruitmentFaction().equals(proposal.faction);
    }

    @Override
    public boolean isDone() {
        return !proposal.isAlive();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return !proposal.isAlive();
    }

    @Override
    public String getName() {
        return proposal.getTitle();
    }

    @Override
    public String getSortString() {
        int ctr = 1 + proposal.getTotalSupport();
        return Integer.toString(10000000 - ctr);
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return proposal.faction;
    }

    @Override
    public String getIcon() {
        if (isPlayerProposal) return LordController.getPlayerLord().getLordAPI().getPortraitSprite();
        return LordController.getLordById(proposal.getOriginator()).getLordAPI().getPortraitSprite();
    }

    public Color getTitleColor(ListInfoMode mode) {
        if (isPlayerProposal) {
            return Color.YELLOW;
        }
        if (proposal.isPlayerSupports()) {
            return LIGHT_GREEN;
        }
        return super.getTitleColor(mode);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(StringUtil.getString(CATEGORY_UI, "politics_category"));
        return tags;
    }

}
