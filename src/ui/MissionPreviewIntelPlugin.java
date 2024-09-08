package ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.missions.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import lombok.Getter;
import org.lwjgl.input.Keyboard;
import person.LordEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.Set;

public class MissionPreviewIntelPlugin extends BaseIntelPlugin {

    private final Object ABANDON = new Object();
    private final Object ACCEPT = new Object();
    @Getter
    private BaseHubMission mission;

    public MissionPreviewIntelPlugin(BaseHubMission mission) {
        setImportant(true);
        this.mission = mission;
    }


    @Override
    public boolean hasSmallDescription() {
        return true;
    }


    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 3f;
        float opad = 10f;

        FactionAPI faction = mission.getFactionForUIColors();
        PersonAPI person = mission.getPerson();

        if (person != null) {
            info.addImages(width, 128, opad, opad, person.getPortraitSprite(), faction.getCrest());

            String post = "one";
            if (person.getPost() != null) post = person.getPost().toLowerCase();
            if (post == null && person.getRank() != null) post = person.getRank().toLowerCase();
            info.addPara("Mission offered by " + post + " " + person.getNameString() + ", affiliated with " +
                            faction.getDisplayNameWithArticle() + ".",
                    opad, faction.getBaseUIColor(), faction.getDisplayNameWithArticleWithoutArticle());
        }

        info.addPara("", opad);
        if (mission.getStartingStage() == null) {
            // this is insanely hacky
            if (mission instanceof RuinsDataSwapMission) mission.setStartingStage(RuinsDataSwapMission.Stage.GO_TO_RUINS);
            if (mission instanceof RaidSecretOutpostMission) mission.setStartingStage(RaidSecretOutpostMission.Stage.GO_TO_OUTPOST);
            if (mission instanceof PirateSystemBounty) mission.setStartingStage(PirateSystemBounty.Stage.BOUNTY);
            if (mission instanceof TacticallyBombardColony) mission.setStartingStage(TacticallyBombardColony.Stage.BOMBARD);
            if (mission instanceof DeadDropMission) mission.setStartingStage(DeadDropMission.Stage.DROP_OFF);
            if (mission instanceof SmugglingMission) mission.setStartingStage(SmugglingMission.Stage.SMUGGLE);
            if (mission instanceof SpySatDeployment) mission.setStartingStage(SpySatDeployment.Stage.DEPLOY);
            if (mission instanceof ProcurementMission) mission.setStartingStage(ProcurementMission.Stage.TALK_TO_PERSON);
            if (mission instanceof CheapCommodityMission) mission.setStartingStage(CheapCommodityMission.Stage.TALK_TO_PERSON);
        }
        try {
            mission.setCurrentStage(mission.getStartingStage(), null, null);
            mission.addDescriptionForCurrentStage(info, width, height);
        } catch (Exception e) {
            info.addPara("Error: Could not get mission description.", opad);
        }
        Object stage = mission.getStartingStage();
        try {
            mission.setStartingStage(null);
            mission.createIntelInfo(info, ListInfoMode.IN_DESC);
        } catch (Exception e) {

        } finally {
            mission.setStartingStage(stage);

        }
        ButtonAPI button = info.addButton("Accept", ACCEPT, getFactionForUIColors().getBaseUIColor(),
                getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
        button.setShortcut(Keyboard.KEY_T, true);
        ButtonAPI button2 = info.addButton("Reject", ABANDON, getFactionForUIColors().getBaseUIColor(),
                getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
        button2.setShortcut(Keyboard.KEY_U, true);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId == ABANDON) {
            Global.getSector().getIntelManager().removeIntel(this);
            endImmediately();
            mission.endImmediately();
            mission.abort();
            ui.updateIntelList();
        }
        if (buttonId == ACCEPT) {
            Global.getSector().getIntelManager().removeIntel(this);
            endImmediately();
            mission.accept(null, null);
            ui.updateIntelList();
        }
    }

    @Override
    public String getIcon() {
        return mission.getIcon();
    }

    @Override
    protected String getName() {
        return mission.getBaseName() + " - Mission Offer";
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return mission.getFactionForUIColors();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return mission.getMapLocation(map);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_MISSIONS);
        return tags;
    }
}
