package person;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import controllers.LordController;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LordEvent {

    public static final String FEAST = "feast";
    public static final String RAID = "raid";
    public static final String CAMPAIGN = "campaign";

    private long start;
    private String type;
    private Lord originator;
    @Setter
    private SectorEntityToken target;
    private List<Lord> participants;
    private List<Lord> opposition; // defenders in a raid/campaign, unused for feast

    public LordEvent(String type, Lord origin) {
        this(type, origin, null);
    }

    public LordEvent(String type, Lord origin, SectorEntityToken target) {
        originator = origin;
        this.type = type;
        this.target = target;
        participants = new ArrayList<>();
        opposition = new ArrayList<>();
        start = Global.getSector().getClock().getTimestamp();
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
        originator = LordController.getLordOrPlayerById(originator.getLordAPI().getId());
        List<Lord> newParticipants = new ArrayList<>();
        List<Lord> newOpposition = new ArrayList<>();
        for (Lord lord : participants) {
            newParticipants.add(LordController.getLordById(lord.getLordAPI().getId()));
        }
        for (Lord lord : opposition) {
            newOpposition.add(LordController.getLordById(lord.getLordAPI().getId()));
        }
        participants = newParticipants;
        opposition = newOpposition;
    }
}
