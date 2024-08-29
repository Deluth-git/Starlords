package ai;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.StrategicModulePlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import controllers.EventController;
import controllers.LordController;
import lombok.Setter;
import person.Lord;
import person.LordEvent;
import util.Utils;

public class LordStrategicModule implements StrategicModulePlugin {

    private StrategicModulePlugin base;
    private String lordId;
    @Setter
    private boolean inTransit;
    @Setter
    private boolean escort;  // they are leading/following a campaign OR escorting a fleet

    public LordStrategicModule(Lord lord, StrategicModulePlugin base) {
        lordId = lord.getLordAPI().getId();
        this.base = base;
    }

    @Override
    public void advance(float days) {
        base.advance(days);
    }

    @Override
    public boolean isAllowedToEngage(SectorEntityToken other) {
        Lord lord = LordController.getLordById(lordId);
        boolean ret = base.isAllowedToEngage(other);
        if (inTransit) {
            boolean engageInTransit = false;
            if (other instanceof CampaignFleetAPI) {
                CampaignFleetAPI otherFleet = (CampaignFleetAPI) other;
                CampaignFleetAPI lordFleet = lord.getLordAPI().getFleet();
                boolean canReach = (otherFleet.getFleetData().getMinBurnLevel() < lordFleet.getFleetData().getMinBurnLevel())
                        || Misc.getDistance(otherFleet.getLocation(), lordFleet.getLocation()) < 300;
                boolean interesting = otherFleet.getFleetPoints() > Math.min(lordFleet.getFleetPoints() / 2, 100);
                boolean inBattle = otherFleet.getBattle() != null;
                engageInTransit = (inBattle || canReach) && interesting;
            }
            ret = ret && engageInTransit;
        }
        if (escort) {
            if (other instanceof CampaignFleetAPI) {
                CampaignFleetAPI otherFleet = (CampaignFleetAPI) other;
                CampaignFleetAPI lordFleet = lord.getLordAPI().getFleet();
                boolean isPrincipal = false;
                CampaignFleetAPI principal = null;
                if (lord.getTarget() instanceof CampaignFleetAPI) {
                    principal = (CampaignFleetAPI) lord.getTarget();
                }
                LordEvent campaign = EventController.getCurrentCampaign(lord.getLordAPI().getFaction());
                if (campaign != null && campaign.getOriginator().equals(lord)) {
                    isPrincipal = true;
                    principal = lordFleet;
                }
                if (principal == null) {
                    LordController.log.info("ERROR: PRINCIPAL IS NULL. " + lord.getLordAPI().getNameString() + ", " + lord.getCurrAction());
                    if (lord.getTarget() != null) {
                        LordController.log.info(lord.getTarget().getName());
                    }
                }

                boolean interesting = otherFleet.getFleetPoints() > Math.min(lordFleet.getFleetPoints() / 2, 100);
                boolean near = Utils.isClose(principal, otherFleet);
                boolean necessary = isPrincipal || lordFleet.getFleetData().getMinBurnLevel() > principal.getFleetData().getMinBurnLevel();
                boolean inBattle = otherFleet.getBattle() != null;
                ret = ret && (inBattle || (interesting && near && necessary));
            }
        }
        return ret;
    }

    @Override
    public boolean isAllowedToEvade(SectorEntityToken other) {
        return base.isAllowedToEvade(other);
    }

    @Override
    public void dumpExcessCargoIfNeeded() {
        base.dumpExcessCargoIfNeeded();
    }

    @Override
    public TimeoutTracker<SectorEntityToken> getDoNotAttack() {
        return base.getDoNotAttack();
    }
}
