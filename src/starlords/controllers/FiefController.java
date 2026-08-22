package starlords.controllers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.util.Misc;
import starlords.ai.utils.TargetUtils;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.factionUtils.FactionTemplateController;

import java.util.*;

// Controls different properties of fiefs, e.g. tax and trade value available
public class FiefController extends BaseIntelPlugin {

    private static final float INIT_TAX_TRADE_VALUE = 10000;
    private HashMap<MarketAPI, Float> taxValue = new HashMap<>();
    private HashMap<MarketAPI, Float> tradeValue = new HashMap<>();
    private HashMap<MarketAPI, String> fiefOwner = new HashMap<>(); // maps market to lord id

    private static FiefController instance;

    private FiefController() {
        setHidden(true);
        // init all fief data
        List<MarketAPI> allMarkets = Global.getSector().getEconomy().getMarketsCopy();
        for (MarketAPI market : allMarkets) {
            taxValue.put(market, INIT_TAX_TRADE_VALUE);
            tradeValue.put(market, INIT_TAX_TRADE_VALUE);
        }

        for (Lord lord : LordController.getLordsList()) {
            for (SectorEntityToken fief : lord.getFiefs()) {
                fiefOwner.put(fief.getMarket(), lord.getLordAPI().getId());
            }
        }
    }

    public static void stripLord(Lord lord){
        String id = lord.getLordAPI().getId();
        for (int a = 0; a < instance.fiefOwner.size(); a++){
            if(instance.fiefOwner.values().toArray()[a] == null) continue;
            if(instance.fiefOwner.values().toArray()[a].toString().equals(id)){
                MarketAPI marketTemp = (MarketAPI)instance.fiefOwner.keySet().toArray()[a];
                setOwner(marketTemp,null);
            }
        }
    }

    public static float getTax(MarketAPI market) {
        if (!getInstance().taxValue.containsKey(market)) {
            return 0f;
        }
        return getInstance().taxValue.get(market);
    }

    public static float getTrade(MarketAPI market) {
        if (!getInstance().tradeValue.containsKey(market)) {
            return 0f;
        }
        return getInstance().tradeValue.get(market);
    }

    public static void setTax(MarketAPI market, float amt) {
        getInstance().taxValue.put(market, amt);
    }

    public static void setTrade(MarketAPI market, float amt) {
        getInstance().tradeValue.put(market, amt);
    }

    public static Lord getOwner(MarketAPI market) {
        return LordController.getLordOrPlayerById(getInstance().fiefOwner.get(market));
    }

    public static void setOwner(MarketAPI market, String newOwner) {
        if (market == null) return;
        Lord owner = getOwner(market);
        if (owner != null) owner.removeFief(market);
        // resets owner action if it involved going to their fief
        if (owner != null && market.getPrimaryEntity().equals(owner.getTarget())) {
            EventController.removeFromAllEvents(owner);
            owner.setCurrAction(null);
        }
        Lord newLord = LordController.getLordOrPlayerById(newOwner);
        if (newLord != null) {
            newLord.addFief(market);
        }
        getInstance().fiefOwner.put(market, newOwner);
    }

    public static void destroyMarket(MarketAPI market) {
        setOwner(market, null);
        getInstance().fiefOwner.remove(market);
        getInstance().taxValue.remove(market);
        getInstance().tradeValue.remove(market);
    }

    // perform monthly increment of tax/trade value and update fief statuses
    public static void onMonthPass() {
        HashMap<MarketAPI, String> fiefOwner = getInstance().fiefOwner;
        HashSet<MarketAPI> exists = new HashSet<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!getInstance().taxValue.containsKey(market)) {
                getInstance().taxValue.put(market, INIT_TAX_TRADE_VALUE);
                getInstance().tradeValue.put(market, INIT_TAX_TRADE_VALUE);
            }
            setTax(market, getTax(market) + 20000 + 2000 * market.getSize());
            setTrade(market, getTrade(market) + 4000 * market.getSize());
            if (!fiefOwner.containsKey(market)) {
                fiefOwner.put(market, null);
            }
            exists.add(market);
        }

        ArrayList<MarketAPI> toRemove = new ArrayList<>();
        for (MarketAPI market : fiefOwner.keySet()) {
            Lord owner = getOwner(market);
            if (!exists.contains(market)) {
                // this market was destroyed somehow and not caught
                toRemove.add(market);
                if (owner != null) {
                    owner.removeFief(market);
                }
            }
            if (owner != null && !owner.getFaction().equals(market.getFaction())) {
                // this market changed factions somehow and was not caught
                owner.removeFief(market);
                fiefOwner.put(market, null);
            }
        }
        for (MarketAPI market : toRemove) {
            fiefOwner.remove(market);
        }
    }

    public static MarketAPI chooseVentureTarget(Lord lord) {
        if (!FactionTemplateController.getTemplate(lord.getFaction()).isCanTrade()) return null;
        List<MarketAPI> allMarkets = Global.getSector().getEconomy().getMarketsCopy();
        List<MarketAPI> options = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = 0;
        for (MarketAPI market : allMarkets) {
            if (TargetUtils.canBeTradedWith(lord,market)){
                options.add(market);
                weights.add((int) getTrade(market));
                totalWeight += weights.get(weights.size() - 1);
            }
        }
        if (totalWeight <= 0) return null;
        Random random = new Random();
        int rand = random.nextInt(totalWeight);
        for (int i = 0; i < options.size(); i++) {
            if (rand < weights.get(i)) {
                return options.get(i);
            } else {
                rand -= weights.get(i);
            }
        }
        return null;
    }

    public static MarketAPI chooseNextFiefAward(FactionAPI faction) {
        HashMap<MarketAPI, String> fiefOwner = getInstance().fiefOwner;
        ArrayList<MarketAPI> candidates = new ArrayList<>();
        for (MarketAPI market : fiefOwner.keySet()) {
            if (market.getFaction().equals(faction) && fiefOwner.get(market) == null) {
                if (!TargetUtils.canBeFief(market)) continue;
                candidates.add(market);
            }
        }
        if (candidates.isEmpty()) return null;
        Collections.sort(candidates, new Comparator<MarketAPI>() {
            @Override
            public int compare(MarketAPI o1, MarketAPI o2) {
                if (o2.getSize() != o1.getSize()) return o2.getSize() - o1.getSize();
                return o1.getName().compareTo(o2.getName());
            }
        });
        return candidates.get(0);
    }

    public static MarketAPI getMarketByID(String id) {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()){
            if (market.getId().equals(id))
                return market;
        }
        return null;
    }

    public static List<MarketAPI> getFiefsOfFaction(FactionAPI faction) {
        List<MarketAPI> marketsOfFaction = new ArrayList<>();

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()){
            if (market.getFaction().equals(faction)) {
                if (TargetUtils.canBeFief(market)) continue;
                marketsOfFaction.add(market);
            }
        }

        return marketsOfFaction;
    }

    public static void playerTransferFief(Lord target,MarketAPI fief){
        FiefController.setOwner(fief,target.getLordAPI().getId());
        Utils.adjustPlayerReputation(target.getLordAPI(),10);
    }

    /**
     * Method to assign all markets in player faction as fiefs to player when no other lord is present
     */
    public static void playerAssignFiefs() {
        FactionAPI playerFaction = LordController.getPlayerLord().getFaction();
        String fiefList = "";

        if (LordController.getLordsOfFaction(playerFaction).size() > 0 || Misc.getCommissionFaction() != null)
            return;

        Lord playerLord = LordController.getPlayerLord();
        for (MarketAPI market : FiefController.getFiefsOfFaction(playerFaction)) {
            if (FiefController.getOwner(market) == null) {
                FiefController.setOwner(market, playerLord.getLordAPI().getId());
                fiefList += market.getName() + ", ";
            }
        }

        if (!fiefList.isEmpty()) {
            fiefList = fiefList.substring(0, fiefList.length() - 2);
            Global.getSector().getCampaignUI().addMessage("Added " + fiefList + " to your fiefs.", playerLord.getFaction().getBaseUIColor());
        }

    }


    public static FiefController getInstance(boolean forceReset) {
        if (instance == null || forceReset) {
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(FiefController.class);
            if (intel.isEmpty()) {
                instance = new FiefController();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one FiefController intel registered");
                }
                instance = (FiefController) intel.get(0);
            }
        }
        return instance;
    }

    public static FiefController getInstance() {
        return getInstance(false);
    }
}
