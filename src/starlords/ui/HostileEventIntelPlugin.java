package starlords.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import jdk.jfr.EventType;
import lombok.Getter;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// intel entry to notify player of hostile raids and campaigns targeting their faction and raids initiated by allied lords
public class HostileEventIntelPlugin extends BaseIntelPlugin {

    @Getter
    private final SectorEntityToken target;
    @Getter
    private final LordEvent event;
	private final boolean playerTargeted;
	private final boolean alliedOriginator;
	private final String eventType;
    private static final float pad = 3;
    private static final float opad = 10;

    public HostileEventIntelPlugin(LordEvent event) {
        setImportant(true);
        this.event = event;
        target = event.getTarget();
		playerTargeted = target.getFaction().equals(Utils.getRecruitmentFaction());
		if (event.getOriginator() != null)
			alliedOriginator = event.getOriginator().getFaction().equals(Utils.getRecruitmentFaction());
		else
			alliedOriginator = false;
		if (alliedOriginator) eventType = "Allied";
			else eventType = "Hostile";
    }

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color uiColor = Global.getSettings().getBasePlayerColor();
		Lord originator = event.getOriginator();
		FactionAPI faction = originator.getFaction();
		info.addImages(256, 128, pad, 0, faction.getCrest(),
				originator.getLordAPI().getPortraitSprite());
		info.addPara("You've received reports of an imminent " + eventType.toLowerCase() + " incursion led by "
				+ originator.getLordAPI().getNameString() + ".", opad);
		info.addPara("Target: " + target.getName(), uiColor, opad);
		info.addPara(eventType + " fleets: " + (event.getParticipants().size() + 1), uiColor, pad);

		String arrivalDays = Utils.getLordTravelTimeString(event.getOriginator(), event.getTarget());
		String arrivalStr = originator.getLordAPI().getNameString() + " - estimated arrival: " + arrivalDays;
		info.addPara(arrivalStr, opad, Misc.getHighlightColor(), arrivalDays);

		for (Lord lord : event.getParticipants()) {
			arrivalDays = Utils.getLordTravelTimeString(lord, event.getTarget());
			arrivalStr = lord.getLordAPI().getNameString() + " - estimated arrival:  " + arrivalDays;
			info.addPara(arrivalStr, opad, Misc.getHighlightColor(), arrivalDays);
		}

		if (event.getOpposition().size() > 0) {
			info.addPara("Nearby lords are assisting in the defense.", opad);
			info.addPara("Defenders:", uiColor, pad);
			String[] portraits = new String[event.getOpposition().size()];
			for (int i = 0; i < portraits.length; i++) {
				portraits[i] = event.getOpposition().get(i).getLordAPI().getPortraitSprite();
			}
			info.addImages(48 * portraits.length, 48, pad, 0, portraits);

			for (Lord lord : event.getOpposition()) {
				arrivalDays = Utils.getLordTravelTimeString(lord, event.getTarget());
				arrivalStr = lord.getLordAPI().getNameString() + " - estimated arrival:  " + arrivalDays;
				info.addPara(arrivalStr, opad, Misc.getHighlightColor(), arrivalDays);
			}
		}
	}

	@Override
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate,
	                               Color tc, float initPad) {
		float pad = 0;
		List<Lord> lords = new ArrayList<Lord>();
		lords.addAll(event.getParticipants());
		lords.add(event.getOriginator());

		String days = Utils.getMinArrivalTime(lords,event.getTarget());
		info.addPara("Attackers: " + days, pad, tc, Misc.getHighlightColor(), days);

		days = Utils.getMinArrivalTime(event.getOpposition(),event.getTarget());
		info.addPara("Defenders: " + days, pad, tc, Misc.getHighlightColor(), days);

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
        if (!event.isAlive() || !event.getTarget().equals(target))
			return true;
		if (playerTargeted || alliedOriginator)
			return false;
		return true;
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
        return eventType + " Action Alert: " + target.getName();
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
		tags.add("Lord Actions");
		if (playerTargeted)
			tags.add(Tags.INTEL_COLONIES);
		return tags;
	}

	@Override
	public void reportRemovedIntel() {
		if (playerTargeted || alliedOriginator)
			Global.getSector().getCampaignUI().addMessage(eventType + " Raid against "
					+ this.target.getName()
					+ " HAS ENDED.", Color.LIGHT_GRAY);
	}
}