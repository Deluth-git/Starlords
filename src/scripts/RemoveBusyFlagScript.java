package scripts;

import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import person.Lord;

public class RemoveBusyFlagScript implements Script {

    private Lord lord;

    public RemoveBusyFlagScript(Lord lord) {
        this.lord = lord;
    }

    @Override
    public void run() {
        Misc.clearFlag(lord.getLordAPI().getFleet().getMemoryWithoutUpdate(), MemFlags.FLEET_BUSY);
    }
}