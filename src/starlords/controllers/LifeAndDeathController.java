package starlords.controllers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import starlords.generator.LordGenerator;
import starlords.generator.support.LifeAndDeath_LordGeneratorListiner;
import starlords.listeners.LordGeneratorListener_base;
import starlords.lunaSettings.StoredSettings;
import starlords.person.Lord;
import starlords.util.Constants;
import starlords.util.LordTags;
import starlords.util.StringUtil;
import starlords.util.WeightedRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static starlords.util.Constants.CATEGORY_UI;

public class LifeAndDeathController extends BaseIntelPlugin{
    //no longer going to put this into the quest controler. it can stand on its own to feet. hopefully.
    private static LifeAndDeathController instance = null;
    private HashMap<MarketAPI,Double> markets = new HashMap<>();
    public static boolean ENABLE_LIFE;
    public static boolean ENABLE_DEATH;
    public static boolean ENABLE_EXTREMISTS;
    @Setter
    @Getter
    private static ArrayList<String> excludedFactions = new ArrayList<>();
    @Getter
    @Setter
    private static HashMap<String,Double> extremestFactions = new HashMap<>();
    @Setter
    private static double gainPerSizeMulti;
    @Setter
    private static double gainPerSizeExponent;
    @Setter
    private static double stabilityLossMulti;

    @Setter
    private static int maxLords;
    @Setter
    private static int minLords;

    @Setter
    private static int softMaxLords;
    @Setter
    private static int softMinLords;
    @Setter
    private static double slowDownPerExtraLord;
    @Setter
    private static double speedUpPerMissingLord;

    @Setter
    private static int stabilityReqForExtremists;
    @Setter
    private static double oddsOfExtremistsPerStabilityLoss;

    @Setter
    private static WeightedRandom pointsOnMarketCreation;

    @Setter
    private static double oddsOfDeath;
    @Setter
    private static int requiredPoints;

    private static Logger log = Global.getLogger(LifeAndDeathController.class);
    private LifeAndDeathController(){
        setHidden(true);
    }
    public boolean attemptToKillStalord(Lord lord){
        //todo: after a starlord is killed, make they who liked the dead dislike the killer.
        if (!ENABLE_DEATH ||
            !Constants.ENABLE_LIFE_AND_DEATH_SYSTEM ||
            LordController.getLordsList().size() <= minLords ||
            LordGenerator.getRandom().nextDouble() >= oddsOfDeath ||
            lord.getLordAPI().hasTag(LordTags.TAG_IMMORTAL)) return false;
        if (lord.isPlayer()) return false;
        log.info("DEBUG: killing a starlord. poor soul (lord ID, faction, name and remaining lords): "+lord.getLordAPI().getId()+", "+lord.getFaction()+", "+lord.getLordAPI().getNameString()+", "+LordController.getLordsList().size());
        LordController.removeLordMidGame(lord);
        log.info("DEBUG: is lord still alive somehow???? "+(LordController.getLordById(lord.getLordAPI().getId()) != null));
        return true;
    }
    public void runMonth(){
        //log.info("DEBUG: attempting to acquire additional starlords from time... with a value of: "+!ENABLE_LIFE +", "+!Constants.ENABLE_LIFE_AND_DEATH_SYSTEM+", "+(LordController.getLordsList().size()+" >= "+maxLords));
        if (!ENABLE_LIFE || !Constants.ENABLE_LIFE_AND_DEATH_SYSTEM || LordController.getLordsList().size() >= maxLords) return;
        //log.info("DEBUG: data enabled. continueing...");
        log.info("DEUBG: adding starlord spawn points to worlds....");
        //lord generator settings
        double multi = 1;
        if (LordController.getLordsList().size() > softMaxLords) multi *= Math.pow(slowDownPerExtraLord,LordController.getLordsList().size()-softMaxLords);
        //log.info("-temp: multi is now:"+multi+" with a a ^ b of: "+slowDownPerExtraLord+"^"+(LordController.getLordsList().size()-softMaxLords));
        if (LordController.getLordsList().size() < softMinLords) multi *= 1+(speedUpPerMissingLord*(softMinLords-LordController.getLordsList().size()));
        //log.info("-temp: multi is now:"+multi+" with a a ^ b of: "+speedUpPerMissingLord+"^"+(softMinLords-LordController.getLordsList().size()));
        for(MarketAPI a : Global.getSector().getEconomy().getMarketsCopy()){
            MarketAPI market = Global.getSector().getEconomy().getMarket(a.getId());
            if (excludedFactions.contains(market.getFactionId())) continue;
            if (markets.get(market) == null) addMarket(market);
            addMarketsMonthlyPonits( market,multi);
            attemptToSpawnLord( market);
            //log.info("  market "+market.getId()+" has "+getPonits(market)+" points so far...");
        }
    }
    public void attemptToSpawnLord(MarketAPI market){
        while(markets.get(market) >= requiredPoints){
            markets.put(market,markets.get(market)-requiredPoints);
            LifeAndDeath_LordGeneratorListiner listiner = new LifeAndDeath_LordGeneratorListiner();
            LordGenerator.createStarlord(getSpawnedLordFaction(market),market);
            Lord lord = LordController.getLordById(listiner.person.getId());
            LordGeneratorListener_base.removeListener(listiner);
            Global.getSector().getCampaignUI().addMessage(
                    StringUtil.getString(CATEGORY_UI, "lord_spawned",
                            lord.getTitle() + " " + lord.getLordAPI().getNameString(),
                            lord.getFaction().getDisplayName()),
                    lord.getFaction().getBaseUIColor());
        }
    }
    private String getSpawnedLordFaction(MarketAPI market){
        int stab = market.getStability().getModifiedInt();
        //short circlet this loop if lord is guaranteed to be part of the main faction.
        if (stab > stabilityReqForExtremists) return market.getFactionId();
        //get extremest faction if required
        if (ENABLE_EXTREMISTS && LordGenerator.getRandom().nextDouble() > ((stabilityReqForExtremists+1) - stab) * oddsOfExtremistsPerStabilityLoss){
            double totalWeight = 0;
            for (String a : extremestFactions.keySet()){
                totalWeight+=extremestFactions.get(a);
            }
            double target = LordGenerator.getRandom().nextDouble()*totalWeight;
            totalWeight=0;
            for (String a : extremestFactions.keySet()){
                totalWeight+=extremestFactions.get(a);
                if (totalWeight >= target){
                    return a;
                }
            }
        }
        return market.getFactionId();
    }
    public void addMarketsMonthlyPonits(MarketAPI market,double multi){
        addPonits(market,getGainedPonits(market,multi));
    }
    public double getGainedPonits(MarketAPI market,double multi){
        int stab = market.getStability().getModifiedInt();
        stab = Math.min(stab,10);
        int size = market.getSize();
        multi *= 1-((10-stab)*stabilityLossMulti);
        double output = (size*gainPerSizeMulti) + (Math.pow(gainPerSizeExponent,size));
        //log.info("      DEBUG: calculating gained points with a data stab, size, output(before multi), multi of: "+stab+", "+size+", "+output+", "+multi);
        //log.info("      DEBUG: exstra data: lords, softmax, softmin, slowdown, speedup"+LordController.getLordsList().size()+", "+softMaxLords+", "+softMinLords+", "+speedUpPerMissingLord+", "+slowDownPerExtraLord);
        output*=multi;

        return Math.max(0,output);
    }
    public void addMarket(MarketAPI market){
        markets.putIfAbsent(market,pointsOnMarketCreation.getRandom()*1d);
    }
    public void removeMarket(MarketAPI market){
        //todo: put this everywhere the fief controllers destroy market is.
        markets.remove(market);
    }
    public double getPonits(MarketAPI market){
        if (markets.get(market) == null) return 0;
        return markets.get(market);
    }
    public void setPonits(MarketAPI market, double ponits){
        markets.putIfAbsent(market,0d);
        markets.put(market,ponits);
    }
    public void addPonits(MarketAPI market, double ponits){
        markets.putIfAbsent(market,0d);
        setPonits(market,markets.get(market)+ponits);
    }
    public static LifeAndDeathController getInstance(boolean forceReset){
        if (forceReset || instance == null){
            List<IntelInfoPlugin> intel = Global.getSector().getIntelManager().getIntel(LifeAndDeathController.class);
            if (intel.isEmpty()) {
                instance = new LifeAndDeathController();
                Global.getSector().getIntelManager().addIntel(instance, true);
            } else {
                if (intel.size() > 1) {
                    throw new IllegalStateException("Should only be one LifeAndDeathController intel registered");
                }
                instance = (LifeAndDeathController) intel.get(0);
            }
        }
        return instance;
    }
    public static LifeAndDeathController getInstance(){
        return getInstance(false);
    }
}
