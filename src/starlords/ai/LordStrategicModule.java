package starlords.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.StrategicModulePlugin;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import org.apache.log4j.Logger;
import starlords.controllers.EventController;
import starlords.controllers.LordController;
import lombok.Setter;
import starlords.person.Lord;
import starlords.person.LordEvent;
import starlords.util.Utils;

import java.util.HashSet;

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
                boolean canReach = (otherFleet.getFleetData().getMinBurnLevel() <= lordFleet.getFleetData().getMinBurnLevel())
                        || Misc.getDistance(otherFleet.getLocation(), lordFleet.getLocation()) < 300;
                boolean interesting = otherFleet.getFleetPoints() > Math.min(lordFleet.getFleetPoints() / 2, 75);
                boolean inBattle = otherFleet.getBattle() != null;
                engageInTransit = (inBattle || canReach) && interesting;
            }
            if (other instanceof OrbitalStationAPI) {
                engageInTransit = false;
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
//                if (principal == null) {
//                    LordController.log.info("ERROR: PRINCIPAL IS NULL. " + lord.getLordAPI().getNameString() + ", " + lord.getCurrAction());
//                    if (lord.getTarget() != null) {
//                        LordController.log.info(lord.getTarget().getName());
//                    }
//                }
                boolean interesting = otherFleet.getFleetPoints() > Math.min(lordFleet.getFleetPoints() / 2, 75);
                boolean near = Utils.isSomewhatClose(principal, otherFleet);
                boolean adjacent = Utils.isClose(lordFleet, otherFleet, 100);
                boolean necessary = isPrincipal || adjacent || lordFleet.getFleetData().getMinBurnLevel() > principal.getFleetData().getMinBurnLevel();
                boolean inBattle = otherFleet.getBattle() != null;
                ret = ret && (inBattle || (interesting && near && necessary));
            }
        }
        return ret;
    }

    @Override
    public boolean isAllowedToEvade(SectorEntityToken other) {
        boolean ret =  base.isAllowedToEvade(other);
        Lord lord = LordController.getLordById(lordId);
        if (lord.getFleet().getContainingLocation() == null){
            for (CampaignFleetAPI fleet : Misc.getNearbyFleets(other, 600)){
                if (lord.getFleet().getCommander() == fleet.getCommander()){
                    lord.getLordAPI().setFleet(fleet);
                }
            }
        }
        FactionAPI faction = lord.getFaction();
        FactionAPI targetFaction = other.getFaction();
        if (!lord.getFleet().getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_IGNORES_OTHER_FLEETS)) {
            int alliedFp = lord.getFleet().getFleetPoints();
            int enemyFp = 0;
            HashSet<CampaignFleetAPI> seen = new HashSet<>();
            seen.add(lord.getFleet());
            if (other instanceof CampaignFleetAPI) {
                CampaignFleetAPI otherFleet = (CampaignFleetAPI) other;
                seen.add(otherFleet);
                enemyFp += otherFleet.getFleetPoints();
            }
            if ((lord.getFleet() != null && lord.getFleet().getContainingLocation() != null) && (other != null && other.getContainingLocation() != null)){
                //todo: this if statment is for when 'lord.getFleet()' or 'other' '.getContainingLocation()' is null, because that causes crashes
                //please note: this can cause some really bad issues. but hopefully this can.. just work.
                for (CampaignFleetAPI fleet : Misc.getNearbyFleets(lord.getFleet(), 600)) {
                    if (!seen.contains(fleet)) {
                        seen.add(fleet);
                        if (fleet.getFaction().isHostileTo(faction)
                                && !fleet.getFaction().isHostileTo(targetFaction)) {
                            enemyFp += fleet.getFleetPoints();
                        } else if (fleet.getFaction().isHostileTo(targetFaction)
                                && !fleet.getFaction().isHostileTo(faction)) {
                            alliedFp += fleet.getFleetPoints();
                        }
                    }
                }
                for (CampaignFleetAPI fleet : Misc.getNearbyFleets(other, 600)) {
                    if (!seen.contains(fleet)) {
                        seen.add(fleet);
                        if (fleet.getFaction().isHostileTo(faction)
                                && !fleet.getFaction().isHostileTo(targetFaction)) {
                            enemyFp += fleet.getFleetPoints();
                        } else if (fleet.getFaction().isHostileTo(targetFaction)
                                && !fleet.getFaction().isHostileTo(faction)) {
                            alliedFp += fleet.getFleetPoints();
                        }
                    }
                }
            }
            float thres;
            switch (lord.getPersonality()) {
                case MARTIAL:
                    thres = 2;
                    break;
                case CALCULATING:
                    thres = 1.25f;
                    break;
                default:
                    thres = 1.5f;
            }
            ret = ret || enemyFp > thres * alliedFp;
        }
        return ret;
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
