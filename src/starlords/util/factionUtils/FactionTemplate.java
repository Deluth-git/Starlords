package starlords.util.factionUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.People;
import com.fs.starfarer.api.util.Misc;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import starlords.ai.utils.TargetUtils;
import starlords.lunaSettings.StoredSettings;
import starlords.plugins.LordInteractionDialogPluginImpl;
import starlords.util.NexerlinUtilitys;
import starlords.util.StringUtil;
import starlords.util.Utils;

@Getter
public class FactionTemplate {
    /*what have I done?
    * (later)
    * starlords multi for lord 'costs'.
    *   - ship buying cost.
    *   - garrison buying cost.
    *   - Smod buying cost.
    *
    * starlord gain multi:
    *   - trade wealth gain
    *   - kill wealth gain.
    *   - fief wealth gain
    *   - XP gain
    * generator data:
    *   data for generator, so diffrent factions can have diffrent data in generator.
    *
    * */
    public static Logger log = Global.getLogger(TargetUtils.class);

    protected String factionID;

    //protected PersonAPI leader;
    protected String leaderID;
    protected String T0_title;
    protected String T1_title;
    protected String T2_title;

    protected boolean canAttack;
    protected boolean canBeAttacked;
    protected boolean canInvade;
    protected boolean canBeRaided;
    protected boolean canRaid;
    protected boolean canBeSatBomb;
    protected boolean canSatBomb;
    protected boolean canTacticalBomb;
    protected boolean canBeTacticalBomb;
    protected boolean canHaveCampaigns;

    protected boolean canStarlordsJoin;

    protected boolean canPreformDiplomacy;
    protected boolean canPreformPolicy;
    protected boolean canPreformFeasts;

    protected boolean canTrade;
    protected boolean canBeTradedWith;

    protected boolean canGiveFiefs;
    protected boolean canLordsTakeFiefsWithDefection;
    protected int maxNumberFiefsPerLord;

    protected double lordFiefIncomeMulti;
    protected double lordCombatIncomeMulti;
    protected double lordTradeIncomeMulti;
    protected double lordCommissionedIncomeMulti;

    protected double lordRepChangeFromKillsMulti;
    protected double lordFleetUpkeepCostMulti;
    //private double starlordsOnStartMulti = 1;
    //private double starlordsLifeMulti = 1;
    public FactionTemplate(String factionID){
        this.factionID = factionID;
        FactionTemplateController.addTemplate(this);
        init(factionID,new JSONObject());
    }
    public FactionTemplate(String factionID, JSONObject json){
        this.factionID = factionID;
        FactionTemplateController.addTemplate(this);
        init(factionID,json);
    }
    private void init(String factionID,JSONObject json){
        log.info("setting up faction template for: "+factionID);

        setLeaderID("leaderID",json);
        setT0_title("rank_0",json);
        setT1_title("rank_1",json);
        setT2_title("rank_2",json);

        setCanAttack("canBeAttacked",json);
        setCanBeAttacked("canAttack",json);
        setCanInvade("canInvade",json);
        setCanSatBomb("canSatBomb",json);
        setCanBeSatBomb("canBeSatBomb",json);
        setCanBeRaided("canBeRaided",json);
        setCanRaid("canRaid",json);
        setCanTacticalBomb("canTacticalBombed",json);
        setCanBeTacticalBomb("canBeTacticalBomb",json);
        setCanHaveCampaigns("canHaveCampaigns",json);

        setCanStarlordsJoin("canStarlordsJoin",json);

        setCanPreformDiplomacy("canPreformDiplomacy",json);
        setCanPreformPolicy("canPreformPolicy",json);
        setCanPreformFeasts("canHoldFeasts",json);

        setCanTrade("canTrade",json);
        setCanBeTradedWith("canBeTradedWith",json);

        setCanGiveFiefs("canGivesFiefs",json);
        setCanLordsTakeFiefsWithDefection("canLordsTakeFiefsWithDefection",json);
        setMaxNumberFiefsPerLord("maxNumberOfFiefs",json);

        setLordFiefIncomeMulti("fiefIncomeMulti",json);
        setLordCombatIncomeMulti("combatIncomeMulti",json);
        setLordTradeIncomeMulti("tradeIncomeMulti",json);
        setLordCommissionedIncomeMulti("commissionIncomeMulti",json);

        setLordFleetUpkeepCostMulti("lordFleetUpkeepCostMulti",json);

        setLordRepChangeFromKillsMulti("lordRepGainPerWin",json);

        insureAttackStatusCorrect();

        log.info("  leaderID "+this.leaderID);
        log.info("  rank0 "+this.T0_title);
        log.info("  rank1 "+this.T1_title);
        log.info("  rank2 "+this.T2_title);
        log.info("");
        log.info("  canBeAttacked: "+this.canBeAttacked);
        log.info("  canAttack: "+this.canAttack);
        log.info("  canInvade: "+this.canInvade);
        log.info("  canSatBomb: "+this.canSatBomb);
        log.info("  canBeSatBomb "+this.canBeSatBomb);
        log.info("  canBeRaided "+this.canBeRaided);
        log.info("  canRaid "+this.canRaid);
        log.info("  canTacticalBombed "+this.canTacticalBomb);
        log.info("  canHaveCompaings: "+this.canHaveCampaigns);
        log.info("");
        log.info("  canStarlordsJoin :"+this.canStarlordsJoin);
        log.info("  canPreformDeplomancy: "+this.canPreformDiplomacy);
        log.info("  canHoldFeasts: "+this.canPreformFeasts);
        log.info("  canTrade: "+this.canTrade);
        log.info("  canBeTradedWith: "+this.canBeTradedWith);
        log.info("  canGiveFiefs: "+this.canGiveFiefs);
        log.info("  canTakeFiefsWithWhenDefecting: "+this.canLordsTakeFiefsWithDefection);
        log.info("  (not yet working) max number of fiefs per lord: "+this.maxNumberFiefsPerLord);
        log.info("");
        log.info("  fiefIncomeMulti: "+this.lordFiefIncomeMulti);
        log.info("  combatIncomeMulti: "+this.lordCombatIncomeMulti);
        log.info("  tradeIncomeMulti: "+this.lordTradeIncomeMulti);
        log.info("  commissionIncomeMulti: "+this.lordCommissionedIncomeMulti);
        log.info("  fleetUpkeepCostMulti: "+this.lordFleetUpkeepCostMulti);
        log.info("  lordRepGainPerWinMulti: "+this.lordRepChangeFromKillsMulti);

    }
    /*@SneakyThrows
    protected void setLeader(String key, JSONObject json){
        if (json != null && json.has(key)){
            String temp = json.getString(key);
            leader = Global.getSector().getImportantPeople().getPerson(temp);
            LordInteractionDialogPluginImpl.log.info("got leader as a person of: "+leader);
            return;
        }
        LordInteractionDialogPluginImpl.log.info("got leader as null");
        leader = null;
    }*/
    public PersonAPI getLeader(){
        if (leaderID == null) return null;
        PersonAPI person = Global.getSector().getImportantPeople().getPerson(leaderID);
        return person;
    }
    @SneakyThrows
    protected void setLeaderID(String key, JSONObject json){
        if (json != null && json.has(key)){
            leaderID = json.getString(key);
            //leader = Global.getSector().getImportantPeople().getPerson(leaderID);
            return;
        }
        leaderID = null;
        //leader = null;
    }
    @SneakyThrows
    protected void setT0_title(String key, JSONObject json){
        if (json != null && json.has(key)){
            T0_title = json.getString(key);
            return;
        }
        T0_title = StringUtil.getString("starlords_title", "title_default_" + 0);
    }
    @SneakyThrows
    protected void setT1_title(String key, JSONObject json){
        if (json != null && json.has(key)){
            T1_title = json.getString(key);
            return;
        }
        T1_title = StringUtil.getString("starlords_title", "title_default_" + 1);
    }
    @SneakyThrows
    protected void setT2_title(String key, JSONObject json){
        if (json != null && json.has(key)){
            T2_title = json.getString(key);
            return;
        }
        T2_title = StringUtil.getString("starlords_title", "title_default_" + 2);
    }
    @SneakyThrows
    protected void setCanAttack(String key,JSONObject json){
        if (json != null && json.has(key)){
            canAttack = json.getBoolean(key);
            return;
        }
        canAttack = true;

    }
    @SneakyThrows
    protected void setCanBeAttacked(String key,JSONObject json){
        if (json != null && json.has(key)){
            canBeAttacked = json.getBoolean(key);
            return;
        }
        if (Utils.nexEnabled()){
            canBeAttacked = NexerlinUtilitys.canInvade(Global.getSector().getFaction(factionID));
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        canBeAttacked = !isPirate;
    }
    @SneakyThrows
    protected void setCanInvade(String key,JSONObject json){
        if (json != null && json.has(key)){
            canInvade = json.getBoolean(key);
            return;
        }
        if (Utils.nexEnabled()){
            canInvade = NexerlinUtilitys.canInvade(Global.getSector().getFaction(factionID));
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        canInvade = !isPirate;

    }
    @SneakyThrows
    protected void setCanSatBomb(String key,JSONObject json){
        if (json != null && json.has(key)){
            canSatBomb = json.getBoolean(key);
            return;
        }
        canSatBomb = true;

    }
    @SneakyThrows
    protected void setCanBeSatBomb(String key,JSONObject json){
        if (json != null && json.has(key)){
            canBeSatBomb = json.getBoolean(key);
            return;
        }
        canBeSatBomb = true;
    }
    @SneakyThrows
    protected void setCanRaid(String key,JSONObject json){
        if (json != null && json.has(key)){
            canRaid = json.getBoolean(key);
            return;
        }
        canRaid = true;
        /*if (Utils.nexEnabled()){
            canInvade = NexerlinUtilitys.canBeAttacked(Global.getSector().getFaction(factionID));
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        canInvade = !isPirate;*/

    }
    @SneakyThrows
    protected void setCanTacticalBomb(String key,JSONObject json){
        if (json != null && json.has(key)){
            canTacticalBomb = json.getBoolean(key);
            return;
        }
        canTacticalBomb = true;

    }
    @SneakyThrows
    protected void setCanBeTacticalBomb(String key,JSONObject json){
        if (json != null && json.has(key)){
            canBeTacticalBomb = json.getBoolean(key);
            return;
        }
        canBeTacticalBomb = true;
    }
    @SneakyThrows
    protected void setCanBeRaided(String key,JSONObject json){
        if (json != null && json.has(key)){
            canBeRaided = json.getBoolean(key);
            return;
        }
        /*if (Utils.nexEnabled()){
            canBeRaided = NexerlinUtilitys.canBeAttacked(Global.getSector().getFaction(factionID));
            return;
        }*/
        canBeRaided = true;

    }
    @SneakyThrows
    protected void setCanHaveCampaigns(String key,JSONObject json){
        if (json != null && json.has(key)){
            canHaveCampaigns = json.getBoolean(key);
            return;
        }
        if (Utils.nexEnabled()){
            canHaveCampaigns = NexerlinUtilitys.canInvade(Global.getSector().getFaction(factionID));
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        canHaveCampaigns = !isPirate;

    }

    @SneakyThrows
    protected void setCanStarlordsJoin(String key, JSONObject json){
        if (json != null && json.has(key)){
            canStarlordsJoin = json.getBoolean(key);
            return;
        }
        canStarlordsJoin = true;
    }
    @SneakyThrows
    protected void setCanPreformDiplomacy(String key, JSONObject json){
        if (json != null && json.has(key)){
            canPreformDiplomacy = json.getBoolean(key);
            return;
        }
        if (Utils.nexEnabled()){
            canPreformDiplomacy = NexerlinUtilitys.canPreformDiplomacy(Global.getSector().getFaction(factionID));
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        canPreformDiplomacy = !isPirate;

    }
    @SneakyThrows
    protected void setCanPreformPolicy(String key, JSONObject json){
        if (json != null && json.has(key)){
            canPreformPolicy = json.getBoolean(key);
            return;
        }
        if (Utils.nexEnabled()){
            canPreformPolicy = NexerlinUtilitys.canPreformDiplomacy(Global.getSector().getFaction(factionID));
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        canPreformPolicy = !isPirate;

    }
    @SneakyThrows
    protected void setCanPreformFeasts(String key, JSONObject json){
        if (json != null && json.has(key)){
            canPreformFeasts = json.getBoolean(key);
            return;
        }
        canPreformFeasts = true;
    }


    @SneakyThrows
    protected void setCanTrade(String key, JSONObject json){
        if (json != null && json.has(key)){
            canTrade = json.getBoolean(key);
            return;
        }
        canTrade = true;
    }
    @SneakyThrows
    protected void setCanBeTradedWith(String key, JSONObject json){
        if (json != null && json.has(key)){
            canBeTradedWith = json.getBoolean(key);
            return;
        }
        canBeTradedWith = true;
    }

    @SneakyThrows
    protected void setCanGiveFiefs(String key, JSONObject json){
        if (json != null && json.has(key)){
            canGiveFiefs = json.getBoolean(key);
            return;
        }
        canGiveFiefs = true;

    }
    @SneakyThrows
    protected void setCanLordsTakeFiefsWithDefection(String key, JSONObject json){
        if (json != null && json.has(key)){
            canLordsTakeFiefsWithDefection = json.getBoolean(key);
            return;
        }
        canLordsTakeFiefsWithDefection = canInvade;
    }
    @SneakyThrows
    protected void setMaxNumberFiefsPerLord(String key, JSONObject json){
        if (json != null && json.has(key)){
            maxNumberFiefsPerLord = json.getInt(key);
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        if (isPirate){
            maxNumberFiefsPerLord = 1;
            return;
        }
        maxNumberFiefsPerLord = Integer.MAX_VALUE;
    }

    @SneakyThrows
    protected void setLordFiefIncomeMulti(String key, JSONObject json){
        if (json != null && json.has(key)){
            lordFiefIncomeMulti = json.getDouble(key);
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        if (isPirate){
            lordFiefIncomeMulti = 0.5;
            return;
        }
        lordFiefIncomeMulti = 1;

    }
    @SneakyThrows
    protected void setLordCombatIncomeMulti(String key, JSONObject json){
        if (json != null && json.has(key)){
            lordCombatIncomeMulti = json.getDouble(key);
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        if (isPirate){
            lordCombatIncomeMulti = 2;
            return;
        }
        lordCombatIncomeMulti = 1;

    }
    @SneakyThrows
    protected void setLordTradeIncomeMulti(String key, JSONObject json){
        if (json != null && json.has(key)){
            lordTradeIncomeMulti = json.getDouble(key);
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        if (isPirate){
            lordTradeIncomeMulti = 1;
            return;
        }
        lordTradeIncomeMulti = 1;

    }
    @SneakyThrows
    protected void setLordCommissionedIncomeMulti(String key, JSONObject json){
        if (json != null && json.has(key)){
            lordCommissionedIncomeMulti = json.getDouble(key);
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        if (isPirate){
            lordCommissionedIncomeMulti = 2;
            return;
        }
        lordCommissionedIncomeMulti = 1;
    }

    @SneakyThrows
    protected void setLordFleetUpkeepCostMulti(String key, JSONObject json){
        if (json != null && json.has(key)){
            lordFleetUpkeepCostMulti = json.getDouble(key);
            return;
        }
        lordFleetUpkeepCostMulti = 1;
    }

    @SneakyThrows
    protected void setLordRepChangeFromKillsMulti(String key, JSONObject json){
        if (json != null && json.has(key)){
            lordRepChangeFromKillsMulti = json.getDouble(key);
            return;
        }
        boolean isPirate = Misc.isPirateFaction(Global.getSector().getFaction(factionID));
        if (isPirate){
            lordRepChangeFromKillsMulti = 1;
            return;
        }
        lordRepChangeFromKillsMulti = 0;
    }

    public boolean isCanBeAttacked(){
        return canBeAttacked;
        //if (!canBeAttacked) return false;
        //if (!canBeTacticalBomb && !canBeSatBomb && !canBeRaided && !canInvade) return false;
        //return true;
    }
    public boolean isCanAttack(){
        return canAttack;
        //if (!canAttack) return false;
        //if (!canTacticalBomb && !canSatBomb && !canRaid && !canInvade) return false;
        //return true;

    }
    private void insureAttackStatusCorrect(){
        if (canAttack){
            canAttack = canTacticalBomb || canSatBomb || canRaid || canInvade;
        }
        if (canBeAttacked){
            canBeAttacked = canTacticalBomb || canSatBomb || canRaid || canInvade;
        }
    }
}
