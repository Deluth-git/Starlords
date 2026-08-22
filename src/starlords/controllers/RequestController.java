package starlords.controllers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionIntel;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import exerelin.campaign.intel.fleets.OffensiveFleetIntel;
import lombok.Getter;
import org.apache.log4j.Logger;
import starlords.person.*;

import java.util.*;
import java.util.List;

// Catalogues requests that Lords have made to the player
// such as prison breaks
// has nothing to do with java events
// also this isn't actually intel, it's only used for easy save/loading
public class RequestController extends BaseIntelPlugin {

	private final List<LordRequest> requests = new ArrayList<>();

	public static Logger log = Global.getLogger(RequestController.class);

	private static RequestController instance;

	private RequestController() {
		setHidden(true);
	}


	public static LordRequest getCurrentRequest(Lord lord, String requestType) {
		for (LordRequest request : getInstance().requests) {
			if (request.getOriginator().equals(lord) && requestType.equals(request.getType())) {
				return request;
			}
		}
		return null;
	}

	public static List<LordRequest> getRequestList() {
		return getInstance().requests;
	}

	public static LordRequest getCurrentDefectionRequest(Lord lord) {
		for (LordRequest request : getInstance().requests) {
			if (request.getOriginator().equals(lord) && (request.getType().equals(LordRequest.PRISON_BREAK) || request.getType().equals(LordRequest.FIEF_FOR_DEFECTION))) {
				return request;
			}
		}
		return null;
	}

	public static void addRequest(LordRequest request) {
		getInstance().requests.add(request);
	}

	public static void endRequest(LordRequest request) {
		getInstance().requests.remove(request);
		request.setAlive(false);


	}

//    @Override
//    public void advance(float amount) {
//
//    }

	public static RequestController getInstance(boolean forceReset) {
		if (instance == null || forceReset) {
			List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(RequestController.class);
			if (intel.isEmpty()) {
				instance = new RequestController();
				Global.getSector().getIntelManager().addIntel(instance, true);
			} else {
				if (intel.size() > 1) {
					throw new IllegalStateException("Should only be one EventController intel registered");
				}
				instance = (RequestController) intel.get(0);
				// update lord references
				for (LordRequest request : instance.requests) {
					request.updateReferences();
				}
			}

			if (!Global.getSector().getScripts().contains(instance)) {
				Global.getSector().addScript(instance);
			}
		}
		return instance;
	}

	public static RequestController getInstance() {
		return getInstance(false);
	}
}
