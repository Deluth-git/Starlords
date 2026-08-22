package starlords.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import starlords.controllers.*;
import org.apache.log4j.Logger;
import starlords.person.Lord;
import starlords.person.LordAction;
import starlords.person.LordRequest;
import starlords.util.*;
import starlords.util.factionUtils.FactionTemplateController;

import java.util.List;

public class MonthlyUpkeepListener extends BaseCampaignEventListener {

    public static Logger log = Global.getLogger(MonthlyUpkeepListener.class);

    /**
     * @param permaRegister: if true, automatically sets this listener to always be running
     */
    public MonthlyUpkeepListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportEconomyMonthEnd() {
        if (Utils.nexEnabled()) NexerlinUtilitys.calculateInvasionsEnabled();
        // Give all lords their base monthly wage and pay fleet upkeep.
        List<Lord> lords = LordController.getLordsList();
        for (Lord lord : lords) {
            // make sure mercenary lords dont expire every month
            if (Misc.isMercenary(lord.getLordAPI())) {
                Misc.setMercHiredNow(lord.getLordAPI());
            }
            if (!lord.isMarshal()) {
                lord.setControversy(Math.max(0, lord.getControversy() - 2));
            }

            Pair<Float, Float> result = PoliticsController.getBaseIncomeMultipliers(lord.getFaction());
            // give pirates some more base money since they can't own fiefs
            result.one *= (float) lord.getCommissionedIncomeMulti();
            result.two *= (float) lord.getCommissionedIncomeMulti();
            //if (Utils.isMinorFaction(lord.getFaction())) result.one *= 2f;
            lord.addWealth(result.one * Constants.LORD_MONTHLY_INCOME
                    + result.two * lord.getRanking() * Constants.LORD_MONTHLY_INCOME);
            CampaignFleetAPI fleet = lord.getLordAPI().getFleet();
            if (fleet == null || lord.getCurrAction() == LordAction.COMPANION) {
                continue;
            }
            // maintenance cost is 15% of purchase cost, also use FP instead of DP for simplicity
            float cost = LordFleetFactory.COST_MULT * fleet.getFleetPoints() * 0.15f;
            lord.addWealth((float) (-1 * cost * lord.getFleetUpkeepMulti()));
            //log.info("DEBUG: Lord " + lord.getLordAPI().getNameString() + " incurred expenses of " + cost);
        }
        LifeAndDeathController.getInstance().runMonth();
        FiefController.onMonthPass();
        QuestController.getInstance().resetQuests();
		FiefController.playerAssignFiefs();
        // check for lord betrayal
        calculateLordsBetrayal();
    }

	public void calculateLordsBetrayal() {

		for (Lord lord : LordController.getLordsList()) {
		    //if a lord is 'not allow to defect' or if a lords faction does not allow lords to join / leave.
		    if (!lord.isAllowedToDefect() || !FactionTemplateController.getTemplate(lord.getFaction()).isCanStarlordsJoin()) continue;
			LordRequest existingRequest = RequestController.getCurrentRequest(lord, LordRequest.FIEF_FOR_DEFECTION);
			if (existingRequest != null) {
				RequestController.endRequest(existingRequest);
			}
			else if (lord.wantsToDefect())
				if (lord.shouldRequestFiefForDefection())
					RequestController.addRequest(new LordRequest(LordRequest.FIEF_FOR_DEFECTION, lord));
				else
					DefectionUtils.performDefection(lord);


			// player faction cant have lords if player is not leading the faction
			if (lord.getFaction().isPlayerFaction() && Misc.getCommissionFaction() != null) {
				DefectionUtils.performDefection(lord, Misc.getCommissionFaction(), true);
			}
		}
	}
}
