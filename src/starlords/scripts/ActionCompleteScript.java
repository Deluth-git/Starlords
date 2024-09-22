package starlords.scripts;

import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.FleetAssignment;
import starlords.person.Lord;

public class ActionCompleteScript implements Script {

    private Lord lord;

    public ActionCompleteScript(Lord lord) {
        this.lord = lord;
    }

    @Override
    public void run() {
        lord.setActionComplete(true);
        lord.getLordAPI().getFleet().addAssignmentAtStart(
                FleetAssignment.STANDING_DOWN, null, 1, null);
    }
}
