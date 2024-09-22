package starlords.plugins;

import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import starlords.util.LordTags;

public class LordsCampaignPlugin extends BaseCampaignPlugin {

    @Override
    public void updateEntityFacts(SectorEntityToken entity, MemoryAPI memory) {
        // marks lord fleets so they can have custom dialog
        if (entity instanceof CampaignFleetAPI) {
            if (((CampaignFleetAPI) entity).getCommander().hasTag(LordTags.LORD)) {
                memory.set("$lords_isLord", true, 0);
            }
        }
    }
}
