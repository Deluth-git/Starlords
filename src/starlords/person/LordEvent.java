package starlords.person;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import starlords.controllers.LordController;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static starlords.ai.LordAI.*;
import static starlords.util.Constants.DEBUG_MODE;

@Getter
public class LordEvent {
    public enum OffensiveType {
        HARASS(0, DEBUG_MODE ? 1 : 7),
        RAID_GENERIC(3, DEBUG_MODE ? 1 : 7),
        RAID_INDUSTRY(4, DEBUG_MODE ? 1 : 7),
        BOMBARD_TACTICAL(6, DEBUG_MODE ? 2 : 14),
        BOMBARD_SATURATION(6, DEBUG_MODE ? 2 : 14),
        NEX_GROUND_BATTLE(6, DEBUG_MODE ? 1 : 7);

        public final int violence;
        public final int chargeTime; // how long a lord has to hover around a target to initiate offensive

        OffensiveType(int violence, int chargeTime) {
            this.violence = violence;
            this.chargeTime = chargeTime;
        }
    }

    public static final String FEAST = "feast";
    public static final String RAID = "raid";
    public static final String CAMPAIGN = "campaign";

    @Setter
    private long offenseTimestamp;  // When an offensive action's charge time started
    @Setter
    private int totalViolence; // if a raid/campaign causes enough damage to a market, it will end / change target.
    private int totalCampaignViolence; //if a campaign causes a large amount of damage, it will end. if not, it will chose more targets.
    @Setter
    private OffensiveType offensiveType;
    @Setter
    private Object battle;
    private final long start;
    private final String type;
    // this is set if this event is actually a base game/nex raid
    private final RaidIntel baseRaidIntel;
    private Lord originator;
    @Setter
    private boolean alive;
    @Setter
    private boolean defensive; // factions are rate-limited in offensive campaigns, but not defensive ones
    @Setter
    private SectorEntityToken target;
    private List<Lord> participants;
    private List<Lord> opposition; // defenders in a raid/campaign, unused for feast
    private Set<Lord> pastParticipants; // for feasts, to track who has already had relation increases

    // flags for feasts
    @Setter
    private boolean heldTournament;
    @Setter
    private PersonAPI tournamentWinner;
    @Setter
    private boolean victoryDedicated;
    @Setter
    private boolean heldDate;
    @Setter
    private boolean professedAdmiration;
    @Setter
    private Lord weddingCeremonyTarget;


    public LordEvent(String type, RaidIntel intel, SectorEntityToken target) {
        this(type, null, target, intel);
    }

    public LordEvent(String type, Lord origin) {
        this(type, origin, null, null);
    }

    public LordEvent(String type, Lord origin, SectorEntityToken target) {
        this(type, origin, target, null);
    }

    private LordEvent(String type, Lord origin, SectorEntityToken target, RaidIntel intel) {
        originator = origin;
        this.type = type;
        this.target = target;
        alive = true;
        pastParticipants = new HashSet<>();
        participants = new ArrayList<>();
        opposition = new ArrayList<>();
        start = Global.getSector().getClock().getTimestamp();
        baseRaidIntel = intel;
    }


    public LordAction getAction() {
        switch(type) {
            case FEAST:
                return LordAction.FEAST;
            case RAID:
                return LordAction.RAID;
            case CAMPAIGN:
                return LordAction.CAMPAIGN;
        }
        return null;
    }

    // used on save load to remove outdated lord references
    public void updateReferences() {
        if (pastParticipants == null) pastParticipants = new HashSet<>();
        if (originator != null) {
            originator = LordController.getLordOrPlayerById(
                    originator.getLordAPI().getId());
        }
    }

    public float getTotalMarines() {
        if (originator == null) return 0;
        float marines = originator.getFleet().getCargo().getMarines();
        for (Lord supporter : participants) {
            if (originator.getFleet().getContainingLocation().equals(
                    supporter.getFleet().getContainingLocation())) {
                marines += supporter.getFleet().getCargo().getMarines();
            }
        }
        return marines;
    }

    public float getTotalFuel() {
        if (originator == null) return 0;
        float fuel = originator.getFleet().getCargo().getFuel();
        if (originator.getFleet().getContainingLocation() == null) return fuel;
        for (Lord supporter : participants) {
            if (supporter.getFleet().getContainingLocation() == null) continue;
            if (originator.getFleet().getContainingLocation().equals(
                    supporter.getFleet().getContainingLocation())) {
                fuel += supporter.getFleet().getCargo().getFuel();
            }
        }
        return fuel;
    }

    public float getTotalArms() {
        if (originator == null) return 0;
        float arms = originator.getFleet().getCargo().getCommodityQuantity(Commodities.HAND_WEAPONS);
        for (Lord supporter : participants) {
            if (originator.getFleet().getContainingLocation().equals(
                    supporter.getFleet().getContainingLocation())) {
                arms += supporter.getFleet().getCargo().getCommodityQuantity(Commodities.HAND_WEAPONS);
            }
        }
        return arms;
    }

    public FactionAPI getFaction() {
        if (originator != null) return originator.getFaction();
        if (baseRaidIntel != null) return baseRaidIntel.getFaction();
        for (Lord a : participants){
            if (a != null && a.getFaction() != null) return a.getFaction();
        }
        for (Lord a : opposition){
            if (a != null && a.getFaction() != null) return a.getFaction();
        }
        return Global.getSector().getFaction("independent");
    }
    public void increaseViolence(int value){
        totalViolence+=value;
        totalCampaignViolence+=value;
    }
    public int getMaxViolence(){
        boolean type = getType().equals(LordEvent.CAMPAIGN);
        if (type){
            return getMaxViolenceToWorld_campaign();
        }
        return getMaxViolenceToWorld_Lord();
    }
    public int getMaxViolenceToWorld_Lord(){
        int base = RAID_MAX_VIOLENCE;
        return base;
    }
    public int getMaxViolenceToWorld_campaign(){
        int base = CAMPAIGN_MAX_VIOLENCE;
        return base;
    }
    public int getMaxCampaignViolence(){
        int base = CAMPAIGN_MAX_VIOLENCE_TOTAL;
        return base;
    }
}
