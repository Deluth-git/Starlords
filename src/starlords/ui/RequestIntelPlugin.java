package starlords.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import starlords.controllers.LordController;
import starlords.controllers.RequestController;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordRequest;
import org.apache.log4j.Logger;
import starlords.util.Utils;

import javax.tools.Tool;
import java.awt.*;
import java.util.*;
import java.util.List;

// Small intel entry to remind player of existing Lord requests
public class RequestIntelPlugin extends BaseIntelPlugin {

	public static Logger log = Global.getLogger(RequestIntelPlugin.class);

	private LordRequest request;
	private CampaignFleetAPI location;
	private long createdTime;

	private final Object ABANDON = new Object();
	private final Object ACCEPT = new Object();
	private Object buttonPressed = null;

	private static final float pad = 3;
	private static final float opad = 10;

	public RequestIntelPlugin(LordRequest request) {
		setImportant(true);
		this.request = request;
		this.createdTime = Global.getSector().getClock().getTimestamp();
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		if (request.isAlive()) {
			this.updateLocation();
			Lord originator = request.getOriginator();
			FactionAPI faction = originator.getFaction();
			info.addImages(256, 128, pad, 0, faction.getCrest(),
					originator.getLordAPI().getPortraitSprite());

			switch (request.getType()) {
				case LordRequest.PRISON_BREAK:
					Lord captor = LordController.getLordById(originator.getCaptor());
					Color captorUiColor = captor.getFaction().getBaseUIColor();
					log.info("[Star Lords] Request Intel Plugin rendering text");
					String captorString = captor.getLordAPI().getNameString() + " of " + captor.getFaction().getDisplayNameWithArticle();
					info.addPara("Hello, " + Global.getSector().getPlayerPerson().getNameString() + ". We don't know each other very well. "
							+ "I'm " + originator.getLordAPI().getNameString() + " and have been imprisoned by ", opad);
					info.addPara(captorString, captorUiColor, opad);
					info.addPara("I've been rotting in this prison for far too long. The other lords have abandoned me, left me here to rot."
							+ "If you defeat " + captor.getLordAPI().getNameString() + " and release me I will join you as a loyal lord of the realm. "
							+ ". So what do you say?", opad);

					Utils.addShipHullsWithCodex(info, originator, pad);

					if (request.hasPlayerAgreed() == true) {

						info.addPara("Accepted", Color.GREEN, opad);
						info.addPara("Excellent! See you soon!", opad);
						info.addPara(captorString + " current location: " + Utils.getNearbyDescription(this.location), opad);
						info.addPara(captorString + " current orders: " + Utils.getLordCurrOrders(captor, -100), opad);

					}

					if (request.hasPlayerAgreed() == false) {
						ButtonAPI button = info.addButton("Accept", ACCEPT, getFactionForUIColors().getBaseUIColor(),
								getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
						button.setShortcut(Keyboard.KEY_T, true);
						ButtonAPI button2 = info.addButton("Reject", ABANDON, getFactionForUIColors().getBaseUIColor(),
								getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
						button2.setShortcut(Keyboard.KEY_U, true);
					}
					break;
				case LordRequest.FIEF_FOR_DEFECTION:
					log.info("[Star Lords] Request Intel Plugin rendering text");
					info.addPara("Hello, " + Global.getSector().getPlayerPerson().getNameString() + ". We don't know each other very well. "
							+ "I'm " + originator.getLordAPI().getNameString() + ". I've been serving " + originator.getFaction().getDisplayNameWithArticle()
							+ " honourably for quite a while now and what do I have to show for my sacrifices and efforts? Nothing. And my ambition grows together with my discontent."
							+ "If you agree to gift one of your fiefs I will join you as a loyal lord of the realm."
							+ ". So what do you say?", opad);

					Utils.addShipHullsWithCodex(info, originator, pad);

					if (request.hasPlayerAgreed() == true) {

						info.addPara("Accepted", Color.GREEN, opad);
						info.addPara("Excellent! Which shall be my new demesne?", opad);
						for (SectorEntityToken fief : LordController.getPlayerLord().getFiefs()) {
							info.addButton(fief.getName(), fief.getMarket(), getFactionForUIColors().getBaseUIColor(),
									getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
						}
					}

					if (request.hasPlayerAgreed() == false) {
						ButtonAPI button = info.addButton("Accept", ACCEPT, getFactionForUIColors().getBaseUIColor(),
								getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
						button.setShortcut(Keyboard.KEY_T, true);
						ButtonAPI button2 = info.addButton("Reject", ABANDON, getFactionForUIColors().getBaseUIColor(),
								getFactionForUIColors().getDarkUIColor(), (int) width, 20, opad);
						button2.setShortcut(Keyboard.KEY_U, true);
					}
					break;
			}
		} else {
			switch (request.getType()) {
				case LordRequest.PRISON_BREAK:
					endImmediately();
					break;
				case LordRequest.FIEF_FOR_DEFECTION:
					endImmediately();
					break;
			}
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
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate,
	                               Color tc, float initPad) {
		float pad = 0;
		switch (request.getType()) {
			case LordRequest.FIEF_FOR_DEFECTION:

				String days = String.valueOf(Math.round(getTimeRemainingFraction()));
				String remaining = days + " days remaining";

				info.addPara(remaining, pad, tc, Misc.getHighlightColor(), days);
				break;
			case LordRequest.PRISON_BREAK:
				break;
		}

		String str = "Status: ";
		String status = "";
		if (request.hasPlayerAgreed())
			status = "Accepted";

		if (request.hasPlayerAgreed())
			info.addPara(str + status, pad, tc, Misc.getHighlightColor(), status);

	}

	@Override
	public boolean isDone() {
		return !request.isAlive();
	}

	@Override
	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		this.buttonPressed = buttonId;
		switch (request.getType()) {
			case LordRequest.PRISON_BREAK:
				if (buttonId == ABANDON) {
					RequestController.endRequest(request);
					ui.updateIntelList();
				}
				if (buttonId == ACCEPT) {
					request.setPlayerAgreed(true);
					ui.updateUIForItem(this);
				}
				break;
			case LordRequest.FIEF_FOR_DEFECTION:
				if (buttonId == ABANDON) {
					RequestController.endRequest(request);
					ui.updateIntelList();
				}
				if (buttonId == ACCEPT) {
					request.setPlayerAgreed(true);
					ui.updateUIForItem(this);
					break;
				}
				if (buttonId instanceof MarketAPI) {
					request.setFief((MarketAPI) buttonId);
					RequestController.endRequest(request);
					ui.updateIntelList();
				}
				break;
		}
	}

	@Override
	public void endImmediately() {
		super.endImmediately();
		Global.getSector().getIntelManager().removeIntel(this);
		if (this.buttonPressed == null)
			notifyExpiration();
		else if (this.buttonPressed.equals(ACCEPT))
			notifyExpiration();

		this.request = null;

	}

	private void notifyExpiration() {
		Global.getSector().getCampaignUI().addMessage(this.request.getRequestCapitalized() + " Request from "
				+ this.request.getOriginator().getLordAPI().getNameString()
				+ " HAS EXPIRED.", Color.LIGHT_GRAY);
	}

	private void updateLocation() {
		if (this.request.isAlive()) {
			CampaignFleetAPI newlocation;
			switch (request.getType()) {
				case LordRequest.PRISON_BREAK:
					newlocation = LordController.getLordById(request.getOriginator().getCaptor()).getFleet();
					break;
				default:
					newlocation = request.getOriginator().getFleet();
					if (newlocation == null)
						newlocation = Global.getSector().getPlayerFleet();
			}
			this.location = newlocation;
		} else
			endImmediately();
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		this.updateLocation();
		return this.location;
	}

	@Override
	public boolean shouldRemoveIntel() {
		return !request.isAlive();
	}

	@Override
	public String getName() {
		String name = "Lord Request - " + request.getRequestCapitalized() + " from " + request.getOriginator().getLordAPI().getNameString();
		return name;
	}

	@Override
	public FactionAPI getFactionForUIColors() {
		return request.getOriginator().getFaction();
	}

	public float getTimeRemainingFraction() {
		float daysRemaining = 0;
		switch (request.getType()) {
			case LordRequest.FIEF_FOR_DEFECTION:
				daysRemaining = 30 - Utils.getDaysSince(createdTime);
				break;
		}
		return daysRemaining;
	}

	@Override
	public String getIcon() {
		return request.getOriginator().getLordAPI().getPortraitSprite();
	}

	public Color getTitleColor(ListInfoMode mode) {
		return request.getOriginator().getFaction().getBaseUIColor();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = new LinkedHashSet<String>();
		if (isImportant()) {
			tags.add(Tags.INTEL_IMPORTANT);
		}
		if (isNew()) {
			tags.add(Tags.INTEL_NEW);
		}
		tags.add("Lord Requests");
		if (request.hasPlayerAgreed() == true)
			tags.add(Tags.INTEL_ACCEPTED);
		return tags;
	}

}