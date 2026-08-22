package starlords.lunaSettings;

import com.fs.starfarer.api.Global;
import lombok.SneakyThrows;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.ai.utils.TargetUtils;
import starlords.controllers.LifeAndDeathController;
import starlords.controllers.PoliticsController;
import starlords.generator.LordGenerator;
import starlords.generator.NewGameLordPicker;
import starlords.generator.support.AvailableShipData;
import starlords.generator.types.flagship.LordFlagshipPickerBase;
import starlords.generator.types.flagship.LordFlagshipPicker_Cost;
import starlords.generator.types.flagship.LordFlagshipPicker_DP;
import starlords.generator.types.flagship.LordFlagshipPicker_HP;
import starlords.generator.types.fleet.LordFleetGeneratorBase;
import starlords.generator.types.fleet.LordFleetGenerator_Desing;
import starlords.generator.types.fleet.LordFleetGenerator_Hullmod;
import starlords.generator.types.fleet.LordFleetGenerator_System;
import starlords.ui.LordsIntelPlugin;
import starlords.util.*;
import starlords.util.SModSupport.SModSet;
import starlords.util.dialogControler.DialogSet;
import starlords.util.factionUtils.FactionTemplateController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class StoredSettings {
    public static void getSettings(){
        getUniversalSettings();
        if (Global.getSettings().getModManager().isModEnabled("lunalib")){
            getLunaSettings();
        }else {
            getConfigSettings();
        }
        FactionTemplateController.init();
    }
    public static void attemptEnableLunalib(){
        if (!Global.getSettings().getModManager().isModEnabled("lunalib")) return;
        LunaSettings.addSettingsListener(new ApplySettingsOnChange());
    }
    @SneakyThrows
    private static void getLunaSettings(){
        Logger log = Global.getLogger(StoredSettings.class);


        //get weather or not a given faction has a overridden possibility of lords being a minor faction, allowed diplomacy, or allowed to be attacked / preform campaigns
        JSONObject json = Global.getSettings().getMergedJSONForMod("data/config/starlords_factionSettings.json",Constants.MOD_ID);
        JSONObject jsonTemp = json.getJSONObject("canBeAttackedOverride");
        HashSet<String> hashSetTempForced = new HashSet<>();
        HashSet<String> hashSetTempNoForced = new HashSet<>();
        for (Iterator it = jsonTemp.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            boolean type = jsonTemp.getBoolean(key);
            if (type){
                hashSetTempForced.add(key);
                continue;
            }
            hashSetTempNoForced.add(key);
        }
        Utils.setForcedAttack(hashSetTempForced);
        Utils.setForcedNoAttack(hashSetTempNoForced);

        jsonTemp = json.getJSONObject("isMinorFactionOverride");
        hashSetTempForced = new HashSet<>();
        hashSetTempNoForced = new HashSet<>();
        for (Iterator it = jsonTemp.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            boolean type = jsonTemp.getBoolean(key);
            if (type){
                hashSetTempForced.add(key);
                continue;
            }
            hashSetTempNoForced.add(key);
        }
        Utils.setForcedMinorFaction(hashSetTempForced);
        Utils.setForcedNotMinorFaction(hashSetTempNoForced);

        jsonTemp = json.getJSONObject("haveRelationsOverride");
        hashSetTempForced = new HashSet<>();
        hashSetTempNoForced = new HashSet<>();
        for (Iterator it = jsonTemp.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            boolean type = jsonTemp.getBoolean(key);
            if (type){
                hashSetTempForced.add(key);
                continue;
            }
            hashSetTempNoForced.add(key);
        }
        Utils.setForcedRelations(hashSetTempForced);
        Utils.setForcedNoRelations(hashSetTempNoForced);


        //lord generator settings
        log.info("DEBUG: attempting to get lunaSettings");
        LordGenerator.setStarlordLevelRatio(getLunaWeightedRandom("generator_lordLevel"));
        LordGenerator.setSizeRatio(new WeightedRandom[]{
                getLunaWeightedRandom("generator_frigateRatio"),
                getLunaWeightedRandom("generator_destroyerRatio"),
                getLunaWeightedRandom("generator_cruiserRatio"),
                getLunaWeightedRandom("generator_captialRatio")
        });
        LordGenerator.setTypeRatio(new WeightedRandom[]{
                getLunaWeightedRandom("generator_warshipRatio"),
                getLunaWeightedRandom("generator_carrierRatio"),
                getLunaWeightedRandom("generator_phaseRatio")
        });
        LordGenerator.setPersonalityRatio(new double[]{
                LunaSettings.getDouble(Constants.MOD_ID,"generator_personalityRatio_Quarrelsome"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_personalityRatio_Calculating"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_personalityRatio_Martial"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_personalityRatio_Upstanding")
        });
        LordGenerator.setBattlePersonalityRatio(new double[]{
                LunaSettings.getDouble(Constants.MOD_ID,"generator_battlePersonalityRatio_Timid"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_battlePersonalityRatio_Cautious"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_battlePersonalityRatio_Steady"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_battlePersonalityRatio_Aggressive"),
                LunaSettings.getDouble(Constants.MOD_ID,"generator_battlePersonalityRatio_Reckless")
        });
        LordGenerator.setIsMaleChance(LunaSettings.getDouble(Constants.MOD_ID,"generator_isMaleChance"));
        String temp = LunaSettings.getString(Constants.MOD_ID,"generator_fleetAdjective");
        if (temp == null) temp = "";
        LordGenerator.setFleetAdjective(temp);

        LordGenerator.setOddsOfNonePriorityShips(LunaSettings.getDouble(Constants.MOD_ID,"generator_oddsOfNonePriorityShips"));
        LordGenerator.setOddsOfNoneSelectedFlagship(LunaSettings.getDouble(Constants.MOD_ID,"generator_oddsOfNoneSelectedFlagship"));

        LordGenerator.addFleetGenerator(new LordFleetGenerator_System("system"),LunaSettings.getDouble(Constants.MOD_ID,"generator_fleetTemplates_system"));
        LordGenerator.addFleetGenerator(new LordFleetGenerator_Desing("Desing"),LunaSettings.getDouble(Constants.MOD_ID,"generator_fleetTemplates_hullmod"));
        LordGenerator.addFleetGenerator(new LordFleetGenerator_Hullmod("Hullmod"),LunaSettings.getDouble(Constants.MOD_ID,"generator_fleetTemplates_manufacture"));
        LordGenerator.addFleetGenerator(new LordFleetGeneratorBase("random"),LunaSettings.getDouble(Constants.MOD_ID,"generator_fleetTemplates_random"));

        LordGenerator.addFlagshipPicker(new LordFlagshipPicker_Cost("cost"),LunaSettings.getDouble(Constants.MOD_ID,"generator_flagshipPicker_Cost"));
        LordGenerator.addFlagshipPicker(new LordFlagshipPickerBase("random"),LunaSettings.getDouble(Constants.MOD_ID,"generator_flagshipPicker_Random"));
        LordGenerator.addFlagshipPicker(new LordFlagshipPicker_HP("hp"),LunaSettings.getDouble(Constants.MOD_ID,"generator_flagshipPicker_HP"));


        LordGenerator.setShipSpawnRatio(getLunaWeightedRandom("generator_shipSpawnRatio"));
        LordGenerator.setMaxShipRatio(getLunaWeightedRandom("generator_maxShipSpawnRatio"));
        LordGenerator.setMinShipRatio(getLunaWeightedRandom("generator_minShipSpawnRatio"));


        //lord 'add additional lords to startup' settings
        NewGameLordPicker picker = new NewGameLordPicker();
        picker.setAllowAdditionalLords(LunaSettings.getBoolean(Constants.MOD_ID,"EL_EnableAdditionalLords"));

        picker.setAllowMaximumLords(LunaSettings.getBoolean(Constants.MOD_ID,"EL_EnableMaxLords"));
        picker.setMaxLords(LunaSettings.getInt(Constants.MOD_ID,"EL_MaxGeneratedLords"));

        picker.setT0PerSize(LunaSettings.getDouble(Constants.MOD_ID,"EL_T0PerSize"));
        picker.setT0Addition(LunaSettings.getDouble(Constants.MOD_ID,"EL_T0Additional"));
        picker.setT0oddsOfFief(LunaSettings.getDouble(Constants.MOD_ID,"EL_T0OddsOfFief"));

        picker.setT1PerSize(LunaSettings.getDouble(Constants.MOD_ID,"EL_T1PerSize"));
        picker.setT1Addition(LunaSettings.getDouble(Constants.MOD_ID,"EL_T1Additional"));
        picker.setT1oddsOfFief(LunaSettings.getDouble(Constants.MOD_ID,"EL_T1OddsOfFief"));

        picker.setT2PerSize(LunaSettings.getDouble(Constants.MOD_ID,"EL_T2PerSize"));
        picker.setT2Addition(LunaSettings.getDouble(Constants.MOD_ID,"EL_T2Additional"));
        picker.setT2oddsOfFief(LunaSettings.getDouble(Constants.MOD_ID,"EL_T2OddsOfFief"));

        json = Global.getSettings().getMergedJSONForMod("data/config/generator/starlord_generaterSettings.json",Constants.MOD_ID);
        JSONObject exscludedFactions = json.getJSONObject("excluded_factions");
        NewGameLordPicker.setExcludeFactions(new ArrayList<>());
        for (Iterator it = exscludedFactions.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            NewGameLordPicker.getExcludeFactions().add(exscludedFactions.getString(key));
        }
        JSONObject factionMultiplyer = json.getJSONObject("faction_multipliers");
        NewGameLordPicker.setBonusFactionLordSize(new HashMap<>());
        for (Iterator it = factionMultiplyer.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            NewGameLordPicker.getBonusFactionLordSize().put(key,factionMultiplyer.getDouble(key));
        }
        Constants.ENABLE_NEW_LORDS_ON_GAME_START = LunaSettings.getBoolean(Constants.MOD_ID,"fetures_newLordsInNewGame");
        NewGameLordPicker.instance = picker;


        //'life and death' settings
        Constants.ENABLE_LIFE_AND_DEATH_SYSTEM = LunaSettings.getBoolean(Constants.MOD_ID,"fetures_lifeAndDeathSystem");

        LifeAndDeathController.ENABLE_LIFE = LunaSettings.getBoolean(Constants.MOD_ID,"LAD_AllowLife");
        LifeAndDeathController.ENABLE_DEATH = LunaSettings.getBoolean(Constants.MOD_ID,"LAD_AllowDeath");
        LifeAndDeathController.ENABLE_EXTREMISTS = LunaSettings.getBoolean(Constants.MOD_ID,"LAD_AllowExstremests");

        LifeAndDeathController.setMaxLords(LunaSettings.getInt(Constants.MOD_ID,"LAD_hardMax"));
        LifeAndDeathController.setMinLords(LunaSettings.getInt(Constants.MOD_ID,"LAD_hardMin"));

        LifeAndDeathController.setSoftMaxLords(LunaSettings.getInt(Constants.MOD_ID,"LAD_softMax"));
        LifeAndDeathController.setSoftMinLords(LunaSettings.getInt(Constants.MOD_ID,"LAD_softMin"));
        LifeAndDeathController.setSlowDownPerExtraLord(LunaSettings.getDouble(Constants.MOD_ID,"LAD_softSlowdown"));
        LifeAndDeathController.setSpeedUpPerMissingLord(LunaSettings.getDouble(Constants.MOD_ID,"LAD_softSpeedup"));

        LifeAndDeathController.setRequiredPoints(LunaSettings.getInt(Constants.MOD_ID,"LAD_PonitsRequired"));
        LifeAndDeathController.setGainPerSizeMulti(LunaSettings.getDouble(Constants.MOD_ID,"LAD_PonitsSizeMulti"));
        LifeAndDeathController.setGainPerSizeExponent(LunaSettings.getDouble(Constants.MOD_ID,"LAD_PonitsSizeExscpoment"));
        LifeAndDeathController.setStabilityLossMulti(LunaSettings.getDouble(Constants.MOD_ID,"LAD_PontsLostPerStability"));

        LifeAndDeathController.setStabilityReqForExtremists(LunaSettings.getInt(Constants.MOD_ID,"LAD_StabilityforExstremests"));
        LifeAndDeathController.setOddsOfExtremistsPerStabilityLoss(LunaSettings.getDouble(Constants.MOD_ID,"LAD_OddsOfExstremestsPerMissingStability"));

        LifeAndDeathController.setOddsOfDeath(LunaSettings.getDouble(Constants.MOD_ID,"LAD_BaseOddsOfDeath"));

        LifeAndDeathController.setPointsOnMarketCreation(getLunaWeightedRandom("LAD_StartingPonits"));

        json = Global.getSettings().getMergedJSONForMod("data/config/generator/starlord_LifeAndDeathSettings.json",Constants.MOD_ID);
        exscludedFactions = json.getJSONObject("excluded_factions");
        LifeAndDeathController.setExcludedFactions(new ArrayList<>());
        for (Iterator it = exscludedFactions.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            LifeAndDeathController.getExcludedFactions().add(exscludedFactions.getString(key));
        }
        factionMultiplyer = json.getJSONObject("extremist_factions");
        LifeAndDeathController.setExtremestFactions(new HashMap<>());
        for (Iterator it = factionMultiplyer.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            LifeAndDeathController.getExtremestFactions().put(key,factionMultiplyer.getDouble(key));
        }



        LordsIntelPlugin.setRepForComs(LunaSettings.getInt(Constants.MOD_ID,"Other_requiredRepForDirectContact"));
        LordsIntelPlugin.setRepForLocation(LunaSettings.getInt(Constants.MOD_ID,"Other_requiredRepForKnowledgeOfLocation"));
        LordsIntelPlugin.setRepForShips(LunaSettings.getInt(Constants.MOD_ID,"Other_requiredRepForKnowledgeOfShips"));
        LordsIntelPlugin.setRepForWealth(LunaSettings.getInt(Constants.MOD_ID,"Other_requiredRepForKnowledgeOfWealth"));
        LordsIntelPlugin.setRepForCurAction(LunaSettings.getInt(Constants.MOD_ID,"Other_requiredRepForKnowledgeOfCurAction"));
        LordsIntelPlugin.setAllowLordsToBeViewed(LunaSettings.getBoolean(Constants.MOD_ID,"Other_showAllLordsIntelByDefault"));
        Utils.setShowMessageLordCaptureReleaseEscape(LunaSettings.getBoolean(Constants.MOD_ID,"Other_showMessageLordCaptureReleaseEscape"));
        PoliticsController.PLAYER_EXTRA_COUNCIL_WEIGHT = LunaSettings.getInt(Constants.MOD_ID,"Other_playerExtraCouncilWeight");
        DefectionUtils.MIN_STABILITY_TO_PREVENT_FIEF_DEFECTION = LunaSettings.getInt(Constants.MOD_ID,"Other_minStabilityToPreventFiefDefection");
        //player faction being attacked settings
        TargetUtils.setCanPlayerBeAttacked(LunaSettings.getBoolean(Constants.MOD_ID,"features_canPlayerFactionBeAttacked"));
        TargetUtils.setCanPlayerBeAttackedBeforeStarlords(LunaSettings.getBoolean(Constants.MOD_ID,"features_canPlayerFactionBeAttackedBeforeStarlords"));

        int maxSMods = LunaSettings.getInt(Constants.MOD_ID,"Other_maxSMods");
        if (LunaSettings.getBoolean(Constants.MOD_ID,"Other_exstraSModsForSpecalMods") && Global.getSettings().getModManager().isModEnabled("progressiveSMods")) maxSMods += 5;
        LordFleetFactory.setMaxSMods(maxSMods);

        log.info("DEBUG: luna settings loaded successfully.");

    }
    @SneakyThrows
    private static void getConfigSettings(){
        Logger log = Global.getLogger(StoredSettings.class);

        //get weather or not a given faction has a overridden possibility of lords being a minor faction, allowed diplomacy, or allowed to be attacked / preform campaigns
        JSONObject json = Global.getSettings().getMergedJSONForMod("data/config/starlords_factionSettings.json",Constants.MOD_ID);
        JSONObject jsonTemp = json.getJSONObject("canBeAttackedOverride");
        HashSet<String> hashSetTempForced = new HashSet<>();
        HashSet<String> hashSetTempNoForced = new HashSet<>();
        for (Iterator it = jsonTemp.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            boolean type = jsonTemp.getBoolean(key);
            if (type){
                hashSetTempForced.add(key);
                continue;
            }
            hashSetTempNoForced.add(key);
        }
        Utils.setForcedAttack(hashSetTempForced);
        Utils.setForcedNoAttack(hashSetTempNoForced);

        jsonTemp = json.getJSONObject("isMinorFactionOverride");
        hashSetTempForced = new HashSet<>();
        hashSetTempNoForced = new HashSet<>();
        for (Iterator it = jsonTemp.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            boolean type = jsonTemp.getBoolean(key);
            if (type){
                hashSetTempForced.add(key);
                continue;
            }
            hashSetTempNoForced.add(key);
        }
        Utils.setForcedMinorFaction(hashSetTempForced);
        Utils.setForcedNotMinorFaction(hashSetTempNoForced);

        jsonTemp = json.getJSONObject("haveRelationsOverride");
        hashSetTempForced = new HashSet<>();
        hashSetTempNoForced = new HashSet<>();
        for (Iterator it = jsonTemp.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            boolean type = jsonTemp.getBoolean(key);
            if (type){
                hashSetTempForced.add(key);
                continue;
            }
            hashSetTempNoForced.add(key);
        }
        Utils.setForcedRelations(hashSetTempForced);
        Utils.setForcedNoRelations(hashSetTempNoForced);


        //lord generator settings
         json = Global.getSettings().getMergedJSONForMod("data/config/generator/starlord_generaterSettings.json",Constants.MOD_ID);
        log.info("DEBUG: attempting to get normal config settings");
        LordGenerator.setStarlordLevelRatio(getConfigWeightedRandom("generator_lordLevel",json));
        LordGenerator.setSizeRatio(new WeightedRandom[]{
                getConfigWeightedRandom("generator_frigateRatio",json),
                getConfigWeightedRandom("generator_destroyerRatio",json),
                getConfigWeightedRandom("generator_cruiserRatio",json),
                getConfigWeightedRandom("generator_captialRatio",json),
        });
        LordGenerator.setTypeRatio(new WeightedRandom[]{
                getConfigWeightedRandom("generator_warshipRatio",json),
                getConfigWeightedRandom("generator_carrierRatio",json),
                getConfigWeightedRandom("generator_phaseRatio",json)
        });
        LordGenerator.setPersonalityRatio(new double[]{
                json.getDouble("generator_personalityRatio_Quarrelsome"),
                json.getDouble("generator_personalityRatio_Calculating"),
                json.getDouble("generator_personalityRatio_Martial"),
                json.getDouble("generator_personalityRatio_Upstanding")
        });
        LordGenerator.setBattlePersonalityRatio(new double[]{
                json.getDouble("generator_battlePersonalityRatio_Timid"),
                json.getDouble("generator_battlePersonalityRatio_Cautious"),
                json.getDouble("generator_battlePersonalityRatio_Steady"),
                json.getDouble("generator_battlePersonalityRatio_Aggressive"),
                json.getDouble("generator_battlePersonalityRatio_Reckless")
        });
        LordGenerator.setIsMaleChance(json.getDouble("generator_isMaleChance"));
        String temp = json.getString("generator_fleetAdjective");
        if (temp == null) temp = "";
        LordGenerator.setFleetAdjective(temp);

        LordGenerator.setOddsOfNonePriorityShips(json.getDouble("generator_oddsOfNonePriorityShips"));
        LordGenerator.setOddsOfNoneSelectedFlagship(json.getDouble("generator_oddsOfNoneSelectedFlagship"));

        LordGenerator.addFleetGenerator(new LordFleetGenerator_System("system"),json.getDouble("generator_fleetTemplates_system"));
        LordGenerator.addFleetGenerator(new LordFleetGenerator_Desing("Desing"),json.getDouble("generator_fleetTemplates_hullmod"));
        LordGenerator.addFleetGenerator(new LordFleetGenerator_Hullmod("Hullmod"),json.getDouble("generator_fleetTemplates_manufacture"));
        LordGenerator.addFleetGenerator(new LordFleetGeneratorBase("random"),json.getDouble("generator_fleetTemplates_random"));

        LordGenerator.addFlagshipPicker(new LordFlagshipPicker_Cost("cost"),json.getDouble("generator_flagshipPicker_Cost"));
        LordGenerator.addFlagshipPicker(new LordFlagshipPicker_DP("random"),json.getDouble("generator_flagshipPicker_Random"));
        LordGenerator.addFlagshipPicker(new LordFlagshipPicker_HP("hp"),json.getDouble("generator_flagshipPicker_HP"));

        LordGenerator.setMaxShipRatio(getConfigWeightedRandom("generator_maxShipSpawnRatio",json));
        LordGenerator.setMinShipRatio(getConfigWeightedRandom("generator_minShipSpawnRatio",json));

        LordGenerator.setShipSpawnRatio(getConfigWeightedRandom("generator_shipSpawnRatio",json));



        //lord 'add additional lords to startup' settings
        NewGameLordPicker picker = new NewGameLordPicker();
        picker.setAllowAdditionalLords(json.getBoolean("EL_EnableAdditionalLords"));

        picker.setAllowMaximumLords(json.getBoolean("EL_EnableMaxLords"));
        picker.setMaxLords(json.getInt("EL_MaxGeneratedLords"));

        picker.setT0PerSize(json.getDouble("EL_T0PerSize"));
        picker.setT0Addition(json.getDouble("EL_T0Additional"));
        picker.setT0oddsOfFief(json.getDouble("EL_T0OddsOfFief"));

        picker.setT1PerSize(json.getDouble("EL_T1PerSize"));
        picker.setT1Addition(json.getDouble("EL_T1Additional"));
        picker.setT1oddsOfFief(json.getDouble("EL_T1OddsOfFief"));

        picker.setT2PerSize(json.getDouble("EL_T2PerSize"));
        picker.setT2Addition(json.getDouble("EL_T2Additional"));
        picker.setT2oddsOfFief(json.getDouble("EL_T2OddsOfFief"));

        JSONObject exscludedFactions = json.getJSONObject("excluded_factions");
        NewGameLordPicker.setExcludeFactions(new ArrayList<>());
        for (Iterator it = exscludedFactions.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            NewGameLordPicker.getExcludeFactions().add(exscludedFactions.getString(key));
        }
        JSONObject factionMultiplyer = json.getJSONObject("faction_multipliers");
        NewGameLordPicker.setBonusFactionLordSize(new HashMap<>());
        for (Iterator it = factionMultiplyer.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            NewGameLordPicker.getBonusFactionLordSize().put(key,factionMultiplyer.getDouble(key));
        }
        Constants.ENABLE_NEW_LORDS_ON_GAME_START = json.getBoolean("fetures_newLordsInNewGame");
        NewGameLordPicker.instance = picker;

        //'life and death' settings
        json = Global.getSettings().getMergedJSONForMod("data/config/generator/starlord_LifeAndDeathSettings.json",Constants.MOD_ID);

        Constants.ENABLE_LIFE_AND_DEATH_SYSTEM = json.getBoolean("fetures_lifeAndDeathSystem");

        LifeAndDeathController.ENABLE_LIFE = json.getBoolean("LAD_AllowLife");
        LifeAndDeathController.ENABLE_DEATH = json.getBoolean("LAD_AllowDeath");
        LifeAndDeathController.ENABLE_EXTREMISTS = json.getBoolean("LAD_AllowExstremests");

        LifeAndDeathController.setMaxLords(json.getInt("LAD_hardMax"));
        LifeAndDeathController.setMinLords(json.getInt("LAD_hardMin"));

        LifeAndDeathController.setSoftMaxLords(json.getInt("LAD_softMax"));
        LifeAndDeathController.setSoftMinLords(json.getInt("LAD_softMin"));
        LifeAndDeathController.setSlowDownPerExtraLord(json.getDouble("LAD_softSpeedup"));
        LifeAndDeathController.setSpeedUpPerMissingLord(json.getDouble("LAD_softSpeedup"));

        LifeAndDeathController.setRequiredPoints(json.getInt("LAD_PonitsRequired"));
        LifeAndDeathController.setGainPerSizeMulti(json.getDouble("LAD_PonitsSizeMulti"));
        LifeAndDeathController.setGainPerSizeExponent(json.getDouble("LAD_PonitsSizeExscpoment"));
        LifeAndDeathController.setStabilityLossMulti(json.getDouble("LAD_PontsLostPerStability"));

        LifeAndDeathController.setStabilityReqForExtremists(json.getInt("LAD_StabilityforExstremests"));
        LifeAndDeathController.setOddsOfExtremistsPerStabilityLoss(json.getDouble("LAD_OddsOfExstremestsPerMissingStability"));

        LifeAndDeathController.setOddsOfDeath(json.getDouble("LAD_BaseOddsOfDeath"));

        LifeAndDeathController.setPointsOnMarketCreation(getConfigWeightedRandom("LAD_StartingPonits",json));

        exscludedFactions = json.getJSONObject("excluded_factions");
        LifeAndDeathController.setExcludedFactions(new ArrayList<>());
        for (Iterator it = exscludedFactions.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            LifeAndDeathController.getExcludedFactions().add(exscludedFactions.getString(key));
        }
        factionMultiplyer = json.getJSONObject("extremist_factions");
        LifeAndDeathController.setExtremestFactions(new HashMap<>());
        for (Iterator it = factionMultiplyer.keys(); it.hasNext(); ) {
            String key = (String) it.next();
            LifeAndDeathController.getExtremestFactions().put(key,factionMultiplyer.getDouble(key));
        }


        json = Global.getSettings().getMergedJSONForMod("data/config/starlord_settings.json",Constants.MOD_ID);
        LordsIntelPlugin.setRepForComs(json.getInt("requiredRepForDirectContact"));
        LordsIntelPlugin.setRepForLocation(json.getInt("requiredRepForKnowledgeOfLocation"));
        LordsIntelPlugin.setRepForShips(json.getInt("requiredRepForKnowledgeOfShips"));
        LordsIntelPlugin.setRepForWealth(json.getInt("requiredRepForKnowledgeOfWealth"));
        LordsIntelPlugin.setRepForCurAction(json.getInt("requiredRepForKnowledgeOfCurAction"));
        LordsIntelPlugin.setAllowLordsToBeViewed(json.getBoolean("showAllLordsIntelByDefault"));
        Utils.setShowMessageLordCaptureReleaseEscape(json.getBoolean("showMessageLordCaptureReleaseEscape"));
        PoliticsController.PLAYER_EXTRA_COUNCIL_WEIGHT = json.getInt("playerExtraCouncilWeight");
        DefectionUtils.MIN_STABILITY_TO_PREVENT_FIEF_DEFECTION = json.getInt("minStabilityToPreventFiefDefection");
        TargetUtils.setCanPlayerBeAttacked(json.getBoolean("features_canPlayerFactionBeAttacked"));
        TargetUtils.setCanPlayerBeAttackedBeforeStarlords(json.getBoolean("features_canPlayerFactionBeAttackedBeforeStarlords"));



        int maxSMods = json.getInt("Other_maxSMods");
        if (json.getBoolean("Other_exstraSModsForSpecalMods") && Global.getSettings().getModManager().isModEnabled("progressiveSMods")) maxSMods += 5;
        LordFleetFactory.setMaxSMods(maxSMods);
        log.info("DEBUG: normal config settings loaded successfully.");
    }

    @SneakyThrows
    private static void getUniversalSettings(){
        AvailableShipData.startup();
        JSONObject json = Global.getSettings().getMergedJSONForMod("data/lords/SMods.json",Constants.MOD_ID);
        SModSet.applySModSets(json);

        DialogSet.setup();
    }


    private static WeightedRandom getLunaWeightedRandom(String name){
        int min = LunaSettings.getInt(Constants.MOD_ID,name+"_min");
        int max = LunaSettings.getInt(Constants.MOD_ID,name+"_max");
        int target = LunaSettings.getInt(Constants.MOD_ID,name+"_target");
        double i = LunaSettings.getDouble(Constants.MOD_ID,name+"_i");
        return new WeightedRandom(max,min,i,target);
    }
    @SneakyThrows
    private static WeightedRandom getConfigWeightedRandom(String name, JSONObject object){
        //TODO test this please
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        object = object.getJSONObject(name);
        object.get("min");

        int min = object.getInt("min");
        int max = object.getInt("max");
        int target = object.getInt("target");
        double i = object.getDouble("i");
        return new WeightedRandom(max,min,i,target);
    }
}
