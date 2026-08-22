package starlords.generator;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import starlords.controllers.FiefController;
import starlords.controllers.LordController;
import starlords.generator.support.LordGeneratorListinerTemp;
import starlords.listeners.LordGeneratorListener_base;
import starlords.person.Lord;
import starlords.util.Utils;
import starlords.util.factionUtils.FactionTemplateController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class NewGameLordPicker {
    public static NewGameLordPicker instance;
    @Setter
    @Getter
    private static ArrayList<String> excludeFactions = new ArrayList<>();
    @Getter
    @Setter
    private static HashMap<String,Double> bonusFactionLordSize = new HashMap<>();
    @Setter
    private double T0PerSize;
    @Setter
    private double T1PerSize;
    @Setter
    private double T2PerSize;
    @Setter
    private double T0Addition;
    @Setter
    private double T1Addition;
    @Setter
    private double T2Addition;

    @Setter
    private double T0oddsOfFief;
    @Setter
    private double T1oddsOfFief;
    @Setter
    private double T2oddsOfFief;

    @Setter
    private boolean allowAdditionalLords;

    @Setter
    private int maxLords;
    @Setter
    private boolean allowMaximumLords;
    Logger log = Global.getLogger(NewGameLordPicker.class);;
    public void addAll(){
        int totalLords = 0;
        //lord generator settings
        log.info("DEBUG: attempting to add new game lords");
        HashMap<String,ArrayList<MarketAPI>> factionMarkets = new HashMap<>();
        HashMap<String,Integer> factionSize = new HashMap<>();
        HashMap<String,ArrayList<Lord>> factionLords = new HashMap<>();
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        //getting faction sizes and possible fiefs.
        for (MarketAPI market : markets){
            String faction = market.getFactionId();
            factionMarkets.putIfAbsent(faction,new ArrayList<>());
            if (FiefController.getOwner(market) != null){
                factionMarkets.get(faction).add(market);
            }
            factionSize.putIfAbsent(faction, 0);
            factionSize.put(faction,factionSize.get(faction)+market.getSize());
        }
        //getting all already created lords
        for(Lord lord : LordController.getLordsList()){
            String factionID = lord.getFaction().getId();
            factionLords.putIfAbsent(factionID,new ArrayList<>());
            factionLords.get(factionID).add(lord);
            totalLords++;
        }
        //removing excluded factions
        for (String remove : excludeFactions){
            if (factionMarkets.get(remove) != null){
                log.info("DEBUG: excluding faction of ID: "+remove);
                factionMarkets.remove(remove);
                factionSize.remove(remove);
            }
        }
        //calculated addition lords per faction.
        int additonalLords = 0;
        HashMap<String,Integer> T0Lords = new HashMap<>();
        HashMap<String,Integer> T1Lords = new HashMap<>();
        HashMap<String,Integer> T2Lords = new HashMap<>();
        for (Object factionID : factionMarkets.keySet().toArray()) {
            int size = factionSize.get((String) factionID);
            int T0LordsNum = (int) ((size*T0PerSize)+T0Addition);
            int T1LordsNum = (int) ((size*T1PerSize)+T1Addition);
            int T2LordsNum = (int) ((size*T2PerSize)+T2Addition);
            additonalLords+=T0LordsNum;
            additonalLords+=T1LordsNum;
            additonalLords+=T2LordsNum;
            T0Lords.put((String)factionID,T0LordsNum);
            T1Lords.put((String)factionID,T1LordsNum);
            T2Lords.put((String)factionID,T2LordsNum);
        }
        log.info("DEBUG: got total lords to generate as: "+additonalLords+" in addition to the already in game lords "+totalLords);
        //reduce additional lords if over the maximum.
        if (allowMaximumLords && (additonalLords+totalLords > maxLords)){
            double multi = (0f+maxLords-totalLords) / (0f+additonalLords);
            log.info("DEBUG: reducing total number of starlords with a multiplayer of: "+multi);
            int total=0;
            for (Object factionID : factionMarkets.keySet().toArray()){
                String key = (String) factionID;
                int T0LordsTemp = (int) (T0Lords.get(key) * multi);
                int T1LordsTemp = (int) (T1Lords.get(key) * multi);
                int T2LordsTemp = T2Lords.get(key);
                if (T2LordsTemp > 1) T2LordsTemp = (int) (T2LordsTemp * multi);
                T0Lords.put(key,T0LordsTemp);
                T1Lords.put(key,T1LordsTemp);
                T2Lords.put(key,T2LordsTemp);
                total += (T0LordsTemp+T1LordsTemp+T2LordsTemp);
            }
            log.info("DEBUG: lords to generate modified to: "+total);
        }

        Random ran = new Random();
        LordGeneratorListinerTemp listiner = new LordGeneratorListinerTemp();
        for (Object factionID : factionMarkets.keySet().toArray()){
            int size = factionSize.get((String)factionID);
            log.info("DEBUG: considering adding lords to faction of "+(String) factionID+"... (size of: "+size+")");
            int T0 = T0Lords.get((String) factionID);
            int T1 = T1Lords.get((String) factionID);
            int T2 = T2Lords.get((String) factionID);
            addFaction((String) factionID, factionMarkets.get((String) factionID),factionLords.get((String)factionID) ,size,listiner,ran,T0,T1,T2);
        }
        LordGeneratorListener_base.removeListener(listiner);
    }
    public void addFaction(String factionID,ArrayList<MarketAPI> markets,ArrayList<Lord> lords, int size,LordGeneratorListinerTemp listiner,Random ran,int T0Lords,int T1Lords, int T2Lords){
        if (!allowAdditionalLords && (lords != null && lords.size() != 0)) return;
        log.info("DEBUG: attempting to add lords to "+(String)factionID+" with a size of "+size+" and "+markets.size()+" number of markets.");
        double multi = 1;
        if (bonusFactionLordSize.get((String)factionID) != null)multi= bonusFactionLordSize.get((String) factionID);
        boolean allowFiefs = FactionTemplateController.getTemplate(factionID).isCanGiveFiefs();//!Utils.isMinorFaction(Global.getSector().getFaction(factionID));
        T0Lords*=multi;
        T1Lords*=multi;
        //T2Lords*=multi;
        log.info("DEBUG: got t0,t1,t2 target lords: "+T0Lords+", "+T1Lords+", "+T2Lords);
        if (lords == null) lords = new ArrayList<>();
        for (Lord a: lords){
            switch (a.getRanking()){
                case 0:
                    T0Lords--;
                    break;
                case 1:
                    T1Lords--;
                    break;
                case 2:
                    T2Lords--;
                    break;
            }
        }
        log.info("DEBUG: after pruning the remaining lords to add are: t0,t1,t2: "+T0Lords+", "+T1Lords+", "+T2Lords);
        log.info("DEBUG: adding T2 lords... ");
        listiner.tier = 2;
        for (int a = 0; a < T2Lords; a++) {
            listiner.fief="null";
            if (allowFiefs && markets.size() > 0 && T2oddsOfFief < ran.nextDouble()){
                int id = ran.nextInt(markets.size());
                listiner.fief = markets.get(id).getId();
                markets.remove(id);
            }
            LordGenerator.createStarlord(factionID);
        }

        log.info("DEBUG: adding T1 lords... ");
        listiner.tier = 1;
        for (int a = 0; a < T1Lords; a++) {
            listiner.fief="null";
            if (allowFiefs && markets.size() > 0 && T1oddsOfFief < ran.nextDouble()){
                int id = ran.nextInt(markets.size());
                listiner.fief = markets.get(id).getId();
                markets.remove(id);
            }
            LordGenerator.createStarlord(factionID);
        }

        log.info("DEBUG: adding T0 lords... ");
        listiner.tier = 0;
        for (int a = 0; a < T0Lords; a++) {
            listiner.fief="null";
            if (allowFiefs && markets.size() > 0 && T0oddsOfFief < ran.nextDouble()){
                int id = ran.nextInt(markets.size());
                listiner.fief = markets.get(id).getId();
                markets.remove(id);
            }
            LordGenerator.createStarlord(factionID);
        }
    }
}
