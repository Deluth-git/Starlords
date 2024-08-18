package listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;
import controllers.FiefController;
import controllers.LordController;
import controllers.PoliticsController;
import controllers.QuestController;
import org.apache.log4j.Logger;
import person.Lord;
import util.Constants;
import util.DefectionUtils;
import util.LordFleetFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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
        // Give all lords their base monthly wage and pay fleet upkeep.
        List<Lord> lords = LordController.getLordsList();
        for (Lord lord : lords) {
            Pair<Float, Float> result = PoliticsController.getBaseIncomeMultipliers(lord.getFaction());
            lord.addWealth(result.one * Constants.LORD_MONTHLY_INCOME
                    + result.two * lord.getRanking() * Constants.LORD_MONTHLY_INCOME);
            CampaignFleetAPI fleet = lord.getLordAPI().getFleet();
            if (fleet == null) {
                continue;
            }
            // maintenance cost is 20% of purchase cost, also use FP instead of DP for simplicity
            float cost = LordFleetFactory.COST_MULT * fleet.getFleetPoints() / 5;
            lord.addWealth(-1 * cost);
            //log.info("DEBUG: Lord " + lord.getLordAPI().getNameString() + " incurred expenses of " + cost);
        }
        FiefController.onMonthPass();
        QuestController.getInstance().resetQuests();
        // check for lord betrayal
        calculateLordBetrayal();
    }

    public void calculateLordBetrayal() {
        // increase betrayal chance if faction is wiped out
        HashSet<String> hasMarkets = new HashSet<>();
        for (MarketAPI marketAPI : Global.getSector().getEconomy().getMarketsCopy()) {
            hasMarkets.add(marketAPI.getFactionId());
        }

        for (Lord lord : LordController.getLordsList()) {
            int chance;
            if (!hasMarkets.contains(lord.getFaction().getId())) {
                chance = 50;
            } else {
                chance = DefectionUtils.getAutoBetrayalChance(lord);
            }
            if (chance > 0) {
                Random rand =  new Random(lord.getLordAPI().getId().hashCode() * Global.getSector().getClock().getTimestamp());
                if (rand.nextInt(100) < chance) {
                    DefectionUtils.performDefection(lord);
                }
            }
        }
    }
}
