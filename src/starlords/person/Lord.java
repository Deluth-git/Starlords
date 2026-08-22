package starlords.person;

import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.alliances.Alliance;
import jdk.jshell.execution.Util;
import lombok.Setter;
import starlords.ai.LordStrategicModule;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import starlords.controllers.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.lwjgl.util.vector.Vector2f;
import starlords.ui.PrisonerIntelPlugin;
import starlords.util.*;
import starlords.util.factionUtils.FactionTemplateController;
import starlords.util.memoryUtils.DataHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static starlords.util.Constants.LORD_TABLE_KEY;
import static starlords.util.Constants.STARLORD_ADDITIONAL_MEMORY_KEY;

@Getter
public class Lord {
    // Data stored in this dict will be persistent.
    @Getter(AccessLevel.NONE)
    private Map<String, Object> persistentData;

    private PersonAPI lordAPI;

    private int kills;

    private int ranking;

    private float wealth;

    @Setter
    private LordTemplate template;

    private List<SectorEntityToken> fiefs;

    private LordAction currAction;

    private SectorEntityToken target;

    private ArrayList<String> prisoners;

    private Map<Alliance.Alignment, Float> alignments;

    private String captor;

	@Getter
	private int escapeAttempts;

	private boolean actionComplete;

    private long assignmentStartTime;

    private boolean isPlayer;

    // whether the player has been greeted by this lord
    private boolean knownToPlayer;

    // whether the player has spoken privately to this lord and learned their personality
    private boolean personalityKnown;

    // whether the player has already gotten a relation bonus with this lord at the latest feast
    private boolean feastInteracted;

    // whether their action is commanded by player. Player-commanded actions cannot be preempted
    private boolean playerDirected;

    // whether the player has attempted to sway this lord recently
    private boolean swayed;

    // whether the player has initiated courtship with this lord
    @Setter
    private boolean courted;

    // whether the player has already dedicated a tournament to this lord
    @Setter
    private boolean dedicatedTournament;

    // whether the player has married this lord
    @Setter
    private boolean married;

    @Setter
    private String spouse;

    // number of romantic actions the player has performed for this lord
    @Setter
    private int romanticActions;

    // for marshals, goes up when faction loses battles and fiefs. Makes people want to replace the marshal.
    @Setter
    private int controversy;

    // stores reference to lord's former fleet while lord is serving as an officer
    @Setter
    private CampaignFleetAPI oldFleet;

    // stores reference to lords fleet, so when the lord does not have there own flagship, they can still get there own fleet
    @Setter
    private CampaignFleetAPI backupFleet;

    //because it budged me that the god dammed way to decide weather or not you were a dress or not arg....
    @Setter
    private String formalWear;
    // Creates a lord from scratch, only run at campaign start
    public Lord(LordTemplate template) {
        FullName.Gender gender = template.isMale ? FullName.Gender.MALE : FullName.Gender.FEMALE;

        PersonAPI lord = Global.getSector().getFaction(template.factionId).createRandomPerson(gender);
        Global.getSector().getImportantPeople().addPerson(lord);
        Map lordDataMap = (Map) Global.getSector().getPersistentData().get(LORD_TABLE_KEY);
        if (!lordDataMap.containsKey(lord.getId())) {
            lordDataMap.put(lord.getId(), new HashMap<String, Object>());
        }
        persistentData = (Map<String, Object>) lordDataMap.get(lord.getId());
        this.template = template;
        lordAPI = lord;
        fiefs = new ArrayList<>();
        prisoners = new ArrayList<>();
        ranking = template.ranking;
        persistentData.put("wealth", wealth);
        persistentData.put("ranking", template.ranking);
        persistentData.put("knownToPlayer", false);
        persistentData.put("personalityKnown", false);
        persistentData.put("playerDirected", false);
        persistentData.put("feastInteracted", false);
        persistentData.put("swayed", false);
        persistentData.put("fief", new ArrayList<String>());
        persistentData.put("prisoners", prisoners);
        if (template.fief != null) {
            MarketAPI toAdd = Global.getSector().getEconomy().getMarket(template.fief);
            if (toAdd != null) {
                fiefs.add(toAdd.getPrimaryEntity());
                ((List<String>) persistentData.get("fief")).add(template.fief);
            }
        }

        if (Utils.nexEnabled()) {
            persistentData.put("alignments", new HashMap<Alliance.Alignment, Float>());
            if (template.alignments.isEmpty()) {
                this.alignments = NexerlinUtilitys.generateLordAlignments(this);
            }
            else {
                this.alignments = NexerlinUtilitys.generateLordAlignmentsFromTemplate(this);
            }
            persistentData.put("alignments", alignments);
        }

        String[] splitname = template.name.split(" ");
        String lastName = "";
        for (int i = 1; i < splitname.length; i++) {
            lastName += splitname[i] + " ";
        }
        FullName fullName = new FullName(splitname[0], lastName.trim(), gender);
        lord.setName(fullName);
        lord.addTag(LordTags.LORD);
        lord.addTag("coff_nocapture");  // prevents capture from take no prisoners
        String portraitPath;
        if (template.portrait.contains("/")) {
            portraitPath = template.portrait;
        } else {
            portraitPath = "graphics/portraits/" + template.portrait + ".png";
        }
        lord.setPortraitSprite(portraitPath);

        lord.getStats().setLevel(template.level);
        lord.setPersonality(template.battlePersonality);
        // base skills for level 8 lord
        lord.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        lord.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        lord.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        lord.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
        lord.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
        lord.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        lord.getStats().setSkillLevel(Skills.NAVIGATION, 1);
        lord.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        if (template.level >= 9) {
            lord.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            lord.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
            lord.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
        }
        if (template.level >= 10) {
            lord.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            lord.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
            lord.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);

        }
        if (template.level >= 11) {
            lord.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
            lord.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            lord.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);

        }
        if (template.level >= 12) {
            lord.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
            lord.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            lord.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);

        }
        if (template.level >= 13) {
            lord.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
            lord.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
            lord.getStats().setSkillLevel(Skills.CYBERNETIC_AUGMENTATION, 1);
            lord.getStats().setSkillLevel(Skills.SENSORS, 1);
        }
        //set custom skills
        if (!template.customSkills.isEmpty()) {
            try {
                template.customSkills.forEach((skillName, skillLevel) -> lord.getStats().setSkillLevel(skillName, skillLevel));
            } catch (Exception e) {
            }
        }

        //Nexerelin Alignments
        if (Utils.nexEnabled()) {

        }
    }


    // Creates special wrapper lords such as the player or lieges
    private Lord(PersonAPI player) {
        Map lordDataMap = (Map) Global.getSector().getPersistentData().get(LORD_TABLE_KEY);
        if (!lordDataMap.containsKey(player.getId())) {
            lordDataMap.put(player.getId(), new HashMap<String, Object>());
        }
        persistentData = (Map<String, Object>) lordDataMap.get(player.getId());
        lordAPI = player;
        if (persistentData.containsKey("ranking")) {
            ranking = (int) persistentData.get("ranking");
        }
        fiefs = new ArrayList<>();
        if (!persistentData.containsKey("fief")) {
            persistentData.put("fief", new ArrayList<String>());
        }
        List<String> storedFiefs = (List<String>) persistentData.get("fief");
        for (String fiefStr : storedFiefs) {
            if(Global.getSector().getEconomy().getMarket(fiefStr) == null) continue;
            fiefs.add(Global.getSector().getEconomy().getMarket(fiefStr).getPrimaryEntity());
        }
        if (persistentData.containsKey("prisoners")) {
            prisoners = (ArrayList<String>) persistentData.get("prisoners");
        } else {
            prisoners = new ArrayList<>();
            persistentData.put("prisoners", prisoners);
        }
    }

    public void addFief(MarketAPI fief) {
        fiefs.add(fief.getPrimaryEntity());
        ((List<String>) persistentData.get("fief")).add(fief.getId());
    }

    public void removeFief(MarketAPI fief) {
        fiefs.remove(fief.getPrimaryEntity());
        ((List<String>) persistentData.get("fief")).remove(fief.getId());
    }

    public void addPrisoner(String lordId) {
        if (isPlayer) {
            PrisonerIntelPlugin intel = new PrisonerIntelPlugin(lordId);
            Global.getSector().getIntelManager().addIntel(intel);
        }
        prisoners.add(lordId);
    }

    public void removePrisoner(String lordId) {
        prisoners.remove(lordId);
    }

    public void addWealth(float addend) {
        // clamp lord wealth between 100 million and 0
        wealth = Math.max(0, Math.min(1e8f, wealth + addend));
        persistentData.put("wealth", wealth);
    }

    // Returns number between 0 and 2 representing lord's economic strength relative to desired level
    // 1 is the expected level, 2 is higher, 0 is lower
    public float getEconLevel() {
        return (float) (1 + Math.tanh((wealth - 150000f) / 50000));
    }

    public LordPersonality getPersonality() {
        return template.personality;
    }

    public FactionAPI getFaction() {
        if (isPlayer) return Utils.getRecruitmentFaction();
        return lordAPI.getFaction();
    }

    public CampaignFleetAPI getFleet() {
        if (isPlayer) return checkAccuracyOfFleet(Global.getSector().getPlayerFleet());
        if (lordAPI.getFleet() != null) return checkAccuracyOfFleet(lordAPI.getFleet());

        if (backupFleet != null && backupFleet.isAlive()) return checkAccuracyOfFleet(backupFleet);
        return null;
    }
    private CampaignFleetAPI checkAccuracyOfFleet(CampaignFleetAPI fleet){
        //todo: make this find if the fleet is broken and fix it. and the fleet is proboly broken a lot. arg...
        return fleet;
    }
    public boolean isMarshal() {
        return lordAPI.getId().equals(PoliticsController.getLaws(getFaction()).getMarshal());
    }

    // Returns number between 0 and 2 representing lord's military strength relative to desired level
    // 1 is the expected level, 2 is higher, 0 is lower
    public float getMilitaryLevel() {
        return (float) (1 + Math.tanh((getFleet().getFleetPoints() - 200f) / 100));
    }

    public String getFormalWear(){
        if (formalWear != null) return formalWear;
        String CATEGORY = "starlords_lords_dialog";
        String a = StringUtil.getString(CATEGORY, "lordGenderDress");
        String b = StringUtil.getString(CATEGORY, "lordGenderSuit");
        String clothing = getLordAPI().getGender() == FullName.Gender.FEMALE ? a : b;
        return clothing;
    }

    public void setCurrAction(LordAction action) {
        if (isPlayer) return;
        try {
            currAction = action;
            ModularFleetAIAPI lordAI = (ModularFleetAIAPI) getFleet().getAI();
            if (!(lordAI.getStrategicModule() instanceof LordStrategicModule)) {
                // this seems to happen when lords are defeated
                lordAI.setStrategicModule(new LordStrategicModule(this, lordAI.getStrategicModule()));
                //LordController.log.info("WARNING: AI WAS RESET: " + getLordAPI().getNameString());
            }

            if (action != null) {
                persistentData.put("currAction", action.toString());
                ((LordStrategicModule) lordAI.getStrategicModule()).setInTransit(action.toString().contains("TRANSIT"));
                ((LordStrategicModule) lordAI.getStrategicModule()).setEscort(action == LordAction.CAMPAIGN || action == LordAction.FOLLOW);
            } else {
                persistentData.put("currAction", null);
                ((LordStrategicModule) lordAI.getStrategicModule()).setInTransit(false);
                ((LordStrategicModule) lordAI.getStrategicModule()).setEscort(false);
                setTarget(null);
            }
            setActionComplete(false);
            setAssignmentStartTime(Global.getSector().getClock().getTimestamp());
        }catch (Exception e){
            Utils.log.warn("failed to setCurrAction for lord "+lordAPI.getNameString()+" ("+lordAPI.getId()+")"+". error of: "+e);
        }
    }

    public void setActionComplete(boolean bool) {
        actionComplete = bool;
        persistentData.put("actionComplete", bool);
    }

    public void setTarget(SectorEntityToken newTarget) {
        target = newTarget;
        String saveStr = null;
        if (newTarget instanceof CampaignFleetAPI) {
            saveStr = "fleet_" + ((CampaignFleetAPI) newTarget).getCommander().getId();
        } else if (newTarget != null && newTarget.getMarket() != null) {
            saveStr = "market_" + newTarget.getMarket().getId();
        }
        persistentData.put("target", saveStr);
    }

    public void recordKills(int newKills) {
        kills += newKills;
        persistentData.put("kills", kills);
    }

    public void setAssignmentStartTime(long time) {
        assignmentStartTime = time;
        persistentData.put("assignmentStartTime", time);
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
        persistentData.put("ranking", ranking);
    }

    public void setFeastInteracted(boolean bool) {
        feastInteracted = bool;
        persistentData.put("feastInteracted", bool);
    }

    public void setSwayed(boolean bool) {
        swayed = bool;
        persistentData.put("swayed", bool);
    }

    public void setPlayerDirected(boolean bool) {
        playerDirected = bool;
        persistentData.put("playerDirected", bool);
    }

    public void setKnownToPlayer(boolean known) {
        knownToPlayer = known;
        persistentData.put("knownToPlayer", known);
    }

    public void setPersonalityKnown(boolean known) {
        personalityKnown = known;
        persistentData.put("personalityKnown", known);
    }

	public void setCaptor(String newCaptor) {

		LordRequest existingRequest = RequestController.getCurrentRequest(this,LordRequest.PRISON_BREAK);

		if (newCaptor == null) {
			this.resetEscapeAttempts();
			if (existingRequest != null)
				RequestController.endRequest(existingRequest);
		} else if (newCaptor.equals(LordController.getPlayerLord().getLordAPI().getId())) {
			if (existingRequest != null)
				RequestController.endRequest(existingRequest);
		}

		this.captor = newCaptor;
		persistentData.put("captor", captor);

	}

    public void setActionText(String text) {
        FleetAssignmentDataAPI assignment = getFleet().getCurrentAssignment();
        if (assignment != null) assignment.setActionText(text);
    }

    public boolean willSpeakPrivately() {
        int rel = lordAPI.getRelToPlayer().getRepInt();
        int offset = lordAPI.getId().hashCode() % 5;
        int baseRel = 0;
        switch(template.personality) {
            case UPSTANDING:
                baseRel = Utils.getThreshold(RepLevel.WELCOMING) + 5;
                break;
            case MARTIAL:
                baseRel = Utils.getThreshold(RepLevel.WELCOMING);
                break;
            case CALCULATING:
                baseRel = Utils.getThreshold(RepLevel.FAVORABLE) + 5;
                break;
            case QUARRELSOME:
                baseRel = Utils.getThreshold(RepLevel.FAVORABLE);
                break;
        }
        return rel >= baseRel + offset;
    }

    public String getLiegeName() {
        return Utils.getLiegeName(getFaction());
    }

    public int getPlayerRel() {
        return lordAPI.getRelToPlayer().getRepInt();
    }

    public int getOrderPriority() {
        if (currAction == null) return 10;
        if (playerDirected) return 1;
        return currAction.priority;
    }

    public SectorEntityToken getClosestBase() {
        return getClosestBase(true);
    }

    public String getTitle() {
        return Utils.getFactionTitle(getFaction().getId(),ranking);
    }

    // Returns closest owned fief, if any. If no fiefs, just return the closest friendly planet/station.
    public SectorEntityToken getClosestBase(boolean prioritizeFiefs) {
        CampaignFleetAPI lordFleet = getFleet();
        if (lordFleet == null) return null;
        Vector2f currLoc = lordFleet.getLocationInHyperspace(); // TODO also count in-sector loc
        if (!fiefs.isEmpty() && prioritizeFiefs) {
            int minIdx = 0;
            float minDist = Float.MAX_VALUE;
            for (int i = 0; i < fiefs.size(); i++) {
                Vector2f loc = fiefs.get(i).getLocationInHyperspace();
                float currDist = (float) (Math.pow(currLoc.x - loc.x, 2) + Math.pow(currLoc.y - loc.y, 2));
                if (currDist < minDist) {
                    minDist = currDist;
                    minIdx = i;
                }
            }
            return fiefs.get(minIdx);
        } else {
            List<MarketAPI> bases = Utils.getFactionMarkets(getFaction().getId());
            if (bases.isEmpty()) return null;
            int minIdx = 0;
            float minDist = Float.MAX_VALUE;
            for (int i = 0; i < bases.size(); i++) {
                Vector2f loc = bases.get(i).getLocationInHyperspace();
                float currDist = (float) (Math.pow(currLoc.x - loc.x, 2) + Math.pow(currLoc.y - loc.y, 2));
                if (currDist < minDist) {
                    minDist = currDist;
                    minIdx = i;
                }
            }
            return bases.get(minIdx).getPrimaryEntity();
        }
    }

	public void incrementEscapeAttempts() {
		this.escapeAttempts++;
		persistentData.put("escapeAttempts", this.escapeAttempts);
	}

	public void resetEscapeAttempts() {
		this.escapeAttempts = 0;
		persistentData.put("escapeAttempts", this.escapeAttempts);
	}

	public boolean shouldRequestPrisonBreak() {
//		log.info("[Star Lords] " + this.getLordAPI().getNameString() + " is checking Prison Break Request. "
//				+ " Attempts: " + this.getEscapeAttempts()
//				+ " Captor is player: " + LordController.getLordById(this.captor).isPlayer()
//				+ " Relationship with player: " + RelationController.getRelation(this, LordController.getPlayerLord())
//				+ " Player Commission: " + Misc.getCommissionFaction()
//				+ " Current Request: " + RequestController.getCurrentDefectionRequest(this)
//		);
        if (!FactionTemplateController.getTemplate(getFaction()).isCanStarlordsJoin()) return false;
        if (this.captor == null || LordController.getLordById(this.captor) == null){
            setCaptor(null);
            return false;
        }
		if (this.getEscapeAttempts() >= Constants.FAILED_PRISON_ESCAPES_ASK_ASSISTANCE
				&& !LordController.getLordById(this.captor).isPlayer()
				&& RelationController.getRelation(this, LordController.getPlayerLord()) >= Utils.getThreshold(RepLevel.SUSPICIOUS)
				&& Misc.getCommissionFaction() == null
				&& RequestController.getCurrentDefectionRequest(this) == null)
			return true;
		return false;
	}

	public boolean shouldRequestFiefForDefection() {
//		log.info("[Star Lords] " + this.getLordAPI().getNameString() + " is checking Fief for Defection Request. "
//				+ " Relationship with player: " + RelationController.getRelation(this, LordController.getPlayerLord())
//				+ " Player Commission: " + Misc.getCommissionFaction()
//				+ " Current Request: " + RequestController.getCurrentDefectionRequest(this)
//		);

        if (RelationController.getRelation(this, LordController.getPlayerLord()) >= Utils.getThreshold(RepLevel.SUSPICIOUS)
				&& Misc.getCommissionFaction() == null
				&& (LordController.getPlayerLord().fiefs.size() >= 3)
				&& RequestController.getCurrentDefectionRequest(this) == null)
			return true;
		return false;
	}

	public boolean wantsToDefect() {
        if (!isAllowedToDefect()) return false;
		int chance = DefectionUtils.getAutoBetrayalChance(this);
		if (chance > 0) {
			if (Utils.getRandomChance(this,100) < chance) {
				return true;
			}
		}
		return false;

	}

	public HashMap<String,Integer> getFleetComposition(){
        if (LordMemoryController.containsLord(getLordAPI().getId()) && LordMemoryController.getLordMemory(getLordAPI().getId()).overridingFleetComposition.size() != 0){
            return LordMemoryController.getLordMemory(getLordAPI().getId()).overridingFleetComposition;
        }
        return template.shipPrefs;
    }

    public boolean canRaid(){
        //todo: finish this.
        return true;
    }
    public boolean canTacticallyBomb(){
        return true;
    }
    public boolean canPreformInvasion(){
        //todo: finish this.
        return true;
    }
    public boolean canSatBomb(){
        if (getPersonality().equals(LordPersonality.QUARRELSOME)) return true;
        return false;
    }
    public boolean isAllowedToDefect(){
        return true;
    }

    public boolean canHoldFeast(){
        return FactionTemplateController.getTemplate(getFaction()).isCanPreformFeasts();
    }
    public double getFiefIncomeMulti(){
        return FactionTemplateController.getTemplate(getFaction()).getLordFiefIncomeMulti();
    }
    public double getTradeIncomeMulti(){
        return FactionTemplateController.getTemplate(getFaction()).getLordTradeIncomeMulti();
    }
    public double getCommissionedIncomeMulti(){
        return FactionTemplateController.getTemplate(getFaction()).getLordCommissionedIncomeMulti();
    }
    public double getCombatIncomeMulti(){
        return FactionTemplateController.getTemplate(getFaction()).getLordCombatIncomeMulti();
    }
    public double getFleetUpkeepMulti(){
        return FactionTemplateController.getTemplate(getFaction()).getLordFleetUpkeepCostMulti();
    }

    public double getRepGainFromKillsMulti(){
        return FactionTemplateController.getTemplate(getFaction()).getLordRepChangeFromKillsMulti();
    }

    private DataHolder DATA_HOLDER;
    public DataHolder getLordDataHolder(){
        DataHolder data_holder = DATA_HOLDER;
        if (DATA_HOLDER != null) return data_holder;
        String key = STARLORD_ADDITIONAL_MEMORY_KEY+getLordAPI().getId();
        if (Global.getSector().getMemory().contains(key)){
            data_holder = (DataHolder) Global.getSector().getMemory().get(key);
        }else{
            data_holder = new DataHolder();
        }
        DATA_HOLDER = data_holder;
        return data_holder;
    }
    public void saveLordDataHolder(){
        String key = STARLORD_ADDITIONAL_MEMORY_KEY+getLordAPI().getId();
        DataHolder data_holder = DATA_HOLDER;
        Global.getSector().getMemory().set(key,data_holder);
    }
    public static Lord createPlayer() {
        Lord player = new Lord(Global.getSector().getPlayerPerson());
        player.isPlayer = true;
        return player;
    }

    public Map<Alliance.Alignment, Float> getAlignments() {
        if (this.alignments == null) {
            persistentData.put("alignments", new HashMap<Alliance.Alignment, Float>());
            this.alignments = NexerlinUtilitys.generateLordAlignments(this);
            persistentData.put("alignments", alignments);
        }
        return this.alignments;
    }

}
