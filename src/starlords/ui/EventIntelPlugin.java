package starlords.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.util.Utils;

import java.awt.*;
import java.util.Set;

// Small intel entry to remind player of ongoing feasts and campaigns
public class EventIntelPlugin extends BaseIntelPlugin {

    private final LordEvent event;
    private static final float pad = 3;
    private static final float opad = 10;
    private static final int MAX_PORTRAITS_PER_ROW = 6;

    public EventIntelPlugin(LordEvent event) {
        setImportant(true);
        this.event = event;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color uiColor = Global.getSettings().getBasePlayerColor();
        Lord originator = event.getOriginator();
        FactionAPI faction = originator.getFaction();
        info.addImages(256, 128, pad, 0, faction.getCrest(),
                originator.getLordAPI().getPortraitSprite());
        switch (event.getType()) {
            case LordEvent.CAMPAIGN:
                info.addPara("Marshal " + originator.getLordAPI().getNameString()
                        + " has raised the navy of the realm. All loyal subjects should rally to the banners!", opad);
                info.addPara("Location: " + Utils.getNearbyDescription(originator.getFleet()), uiColor, opad);
                String orderStr = "Assembling Navy";
                if (event.getTarget() != null) {
                    MarketAPI target = event.getTarget().getMarket();
                    if (target.getFaction().isHostileTo(faction)) {
                        orderStr = "Attacking " + target.getName();
                    } else {
                        orderStr = "Defending " + target.getName();
                    }
                }
                info.addPara("Orders: " + orderStr, uiColor, pad);
                break;
            case LordEvent.FEAST:
                info.addPara("You are cordially invited to the feast of " + originator.getLordAPI().getNameString()
                        + ". Feasts are an opportunity to meet lords and build rapport.", opad);
                info.addPara("Location: " + Utils.getNearbyDescription(originator.getFleet()), uiColor, opad);
                break;
        }
        LabelAPI supportText = info.addPara("Expected Attendees:", uiColor, pad);
        int numPortraits = 0;
        int totalPortraits = 1 + event.getParticipants().size();
        String[] portraits = new String[Math.min(MAX_PORTRAITS_PER_ROW, totalPortraits - numPortraits)];
        portraits[0] = event.getOriginator().getLordAPI().getPortraitSprite();
        for (int i = 1; i < portraits.length; i++) {
            portraits[i] = event.getParticipants().get(numPortraits + i - 1).getLordAPI().getPortraitSprite();
        }
        info.addImages(48 * portraits.length, 48, pad, 0, portraits);
        numPortraits += portraits.length;
        while (numPortraits < totalPortraits) {
            portraits = new String[Math.min(MAX_PORTRAITS_PER_ROW, totalPortraits - numPortraits)];
            for (int i = 0; i < portraits.length; i++) {
                portraits[i] = event.getParticipants().get(numPortraits + i - 1).getLordAPI().getPortraitSprite();
            }
            info.addImages(48 * portraits.length, 48, pad, 0, portraits);
            numPortraits += portraits.length;
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
        return !Utils.getRecruitmentFaction().equals(event.getOriginator().getFaction());
    }

    @Override
    public boolean isDone() {
        return !event.isAlive();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        switch (event.getType()) {
            case LordEvent.FEAST:
                return event.getTarget();
            default:
                return event.getOriginator().getFleet();
        }
    }

    @Override
    public boolean shouldRemoveIntel() {
        return !event.isAlive();
    }

    @Override
    public String getName() {
        return "Ongoing " + Character.toUpperCase(event.getType().charAt(0)) + event.getType().substring(1);
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
        tags.add("Missions");
        return tags;
    }
}