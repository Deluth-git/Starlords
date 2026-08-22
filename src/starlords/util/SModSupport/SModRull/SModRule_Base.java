package starlords.util.SModSupport.SModRull;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import starlords.person.Lord;

public class SModRule_Base {
    public boolean canAdd(FleetMemberAPI member, Lord lord){
        return true;
    }
}
