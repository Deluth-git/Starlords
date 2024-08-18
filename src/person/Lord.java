package person;

import ai.LordStrategicModule;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import controllers.LordController;
import controllers.PoliticsController;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.util.vector.Vector2f;
import util.LordTags;
import util.StringUtil;
import util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.Constants.LORD_TABLE_KEY;

@Getter
public class Lord {

    // Data stored in this dict will be persistent.
    @Getter(AccessLevel.NONE)
    private Map<String, Object> persistentData;

    private PersonAPI lordAPI;

    private int kills;

    private int ranking;

    private float wealth;

    private LordTemplate template;

    private List<SectorEntityToken> fiefs;

    private LordAction currAction;

    private SectorEntityToken target;

    private boolean actionComplete;

    private long assignmentStartTime;

    private boolean isPlayer;

    private boolean knownToPlayer;

    private boolean personalityKnown;

    private boolean feastInteracted;

    private boolean playerDirected;

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
        ranking = template.ranking;
        persistentData.put("wealth", wealth);
        persistentData.put("ranking", template.ranking);
        persistentData.put("knownToPlayer", false);
        persistentData.put("personalityKnown", false);
        persistentData.put("playerDirected", false);
        persistentData.put("feastInteracted", false);
        persistentData.put("fief", new ArrayList<String>());
        // What kind of parser maps null to the string null???
        if (template.fief != null && !template.fief.equals("null")) {
            fiefs.add(Global.getSector().getEconomy().getMarket(template.fief).getPrimaryEntity());
            ((List<String>) persistentData.get("fief")).add(template.fief);
        }
        String[] splitname = template.name.split(" ");
        String lastName = "";
        for (int i = 1; i < splitname.length; i++) {
            lastName += splitname[i] + " ";
        }
        FullName fullName = new FullName(splitname[0], lastName.trim(), gender);
        lord.setName(fullName);
        lord.setPortraitSprite("graphics/portraits/" + template.portrait + ".png");
        lord.addTag(LordTags.LORD);

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
    }

    /**
     * Recreates a lord object from an existing person. Used when loading save game.
     * @param lord existing person
     */
    public Lord(PersonAPI lord, LordTemplate template) {
        Map lordDataMap = (Map) Global.getSector().getPersistentData().get(LORD_TABLE_KEY);
        if (!lordDataMap.containsKey(lord.getId())) {
            lordDataMap.put(lord.getId(), new HashMap<String, Object>());
        }
        persistentData = (Map<String, Object>) lordDataMap.get(lord.getId());
        lordAPI = lord;
        wealth = (float) persistentData.get("wealth");
        String actionStr = (String) persistentData.get("currAction");
        ranking = (int) persistentData.get("ranking");
        personalityKnown = (boolean) persistentData.get("personalityKnown");
        knownToPlayer = (boolean) persistentData.get("knownToPlayer");
        playerDirected = (boolean) persistentData.get("playerDirected");
        feastInteracted = (boolean) persistentData.get("feastInteracted");
        if (actionStr != null) {
            currAction = LordAction.valueOf(actionStr);
        }
        if (persistentData.containsKey("kills")) {
            kills = (int) persistentData.get(kills);
        }
        if (persistentData.containsKey("assignmentStartTime")) {
            assignmentStartTime = (long) persistentData.get("assignmentStartTime");
        }
        if (persistentData.containsKey("actionComplete")) {
            actionComplete = (boolean) persistentData.get("actionComplete");
        }
        this.template = template;
        fiefs = new ArrayList<>();
        List<String> storedFiefs = (List<String>) persistentData.get("fief");
        for (String fiefStr : storedFiefs) {
            fiefs.add(Global.getSector().getEconomy().getMarket(fiefStr).getPrimaryEntity());
        }
        String targetStr = (String) persistentData.get("target");
        if (targetStr != null) {
            if (targetStr.startsWith("market_")) {
                target = Global.getSector().getEconomy().getMarket(targetStr.substring(7)).getPrimaryEntity();
            } else  {
                // assumes fleet target is either lord or player
                String targetId = targetStr.substring(6);
                if (targetId.equals(Global.getSector().getPlayerPerson().getId())) {
                    target = Global.getSector().getPlayerFleet();
                } else {
                    PersonAPI person = Global.getSector().getImportantPeople().getPerson(targetId);
                    if (person != null) {
                        target = person.getFleet();
                    }
                }
            }
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
            fiefs.add(Global.getSector().getEconomy().getMarket(fiefStr).getPrimaryEntity());
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

    public boolean isMarshal() {
        return lordAPI.getId().equals(PoliticsController.getLaws(getFaction()).getMarshal());
    }

    // Returns number between 0 and 2 representing lord's military strength relative to desired level
    // 1 is the expected level, 2 is higher, 0 is lower
    public float getMilitaryLevel() {
        return (float) (1 + Math.tanh((getLordAPI().getFleet().getFleetPoints() - 200f) / 100));
    }

    public void setCurrAction(LordAction action) {
        currAction = action;
        ModularFleetAIAPI lordAI = (ModularFleetAIAPI) lordAPI.getFleet().getAI();
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
        } else if (newTarget != null) {
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
        String titleStr = "title_" + getFaction().getId() + "_" + ranking;
        String ret = StringUtil.getString("starlords_title", titleStr);
        if (ret != null && ret.startsWith("Missing string")) {
            ret = StringUtil.getString("starlords_title", "title_default_" + ranking);
        }
        return ret;
    }

    // Returns closest owned fief, if any. If no fiefs, just return the closest friendly planet/station.
    public SectorEntityToken getClosestBase(boolean prioritizeFiefs) {
        CampaignFleetAPI lordFleet = lordAPI.getFleet();
        if (lordFleet == null) {
            // N/A if lord is currently defeated/captured
            return null;
        }
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

    public static Lord createPlayer() {
        Lord player = new Lord(Global.getSector().getPlayerPerson());
        player.isPlayer = true;
        return player;
    }

}
