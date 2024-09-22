package starlords.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import lombok.Getter;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.util.Utils;

import java.awt.*;
import java.util.Set;

// intel entry to notify player of hostile raids and campaigns targeting their faction
public class HostileEventIntelPlugin extends BaseIntelPlugin {

    @Getter
    private final SectorEntityToken target;
    @Getter
    private final LordEvent event;
    private static final float pad = 3;
    private static final float opad = 10;

    public HostileEventIntelPlugin(LordEvent event) {
        setImportant(true);
        this.event = event;
        target = event.getTarget();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color uiColor = Global.getSettings().getBasePlayerColor();
        Lord originator = event.getOriginator();
        FactionAPI faction = originator.getFaction();
        info.addImages(256, 128, pad, 0, faction.getCrest(),
                originator.getLordAPI().getPortraitSprite());
        info.addPara("You've received reports of an imminent hostile incursion led by "
                + originator.getLordAPI().getNameString() + ".", opad);
        info.addPara("Target: " + target.getName(), uiColor, opad);
        info.addPara("Hostile fleets: " + (event.getParticipants().size() + 1), uiColor, pad);
        if (event.getOpposition().size() > 0) {
            info.addPara("Nearby lords are assisting in the defense.", opad);
            info.addPara("Defenders:", uiColor, pad);
            String[] portraits = new String[event.getOpposition().size()];
            for (int i = 0; i < portraits.length; i++) {
                portraits[i] = event.getOpposition().get(i).getLordAPI().getPortraitSprite();
            }
            info.addImages(48 * portraits.length, 48, pad, 0, portraits);
        }
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
        return isDone();
    }

    @Override
    public boolean isDone() {
        if (event.getTarget() == null) return true;
        return !event.isAlive() || !playerTargeted()
                || !event.getTarget().equals(target);
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return target;
    }

    @Override
    public boolean shouldRemoveIntel() {
        return isDone();
    }

    @Override
    public String getName() {
        return "Hostile Alert: " + target.getName();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return event.getOriginator().getFaction();
    }

    @Override
    public String getIcon() {
        return event.getOriginator().getLordAPI().getPortraitSprite();
    }

    public Color getTitleColor(ListInfoMode mode) {
        return event.getOriginator().getFaction().getBaseUIColor();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Military");
        return tags;
    }

    private boolean playerTargeted() {
        // warning: we don't do null check here
        FactionAPI faction = event.getTarget().getFaction();
        return faction.equals(Utils.getRecruitmentFaction()) || faction.equals(Global.getSector().getPlayerFaction());
    }
}