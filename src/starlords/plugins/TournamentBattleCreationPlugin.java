package starlords.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class TournamentBattleCreationPlugin extends BattleCreationPluginImpl {

    private List<FleetMemberAPI> members;
    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.fightToTheLast = true;
        context.aiRetreatAllowed = false;
        members = context.getPlayerFleet().getFleetData().getSnapshot();
        for (FleetMemberAPI member : members) {
            context.getPlayerFleet().getFleetData().removeFleetMember(member);
        }
        context.getPlayerFleet().getFleetData().clear();
        super.initBattle(context, api);
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                CombatFleetManagerAPI manager = engine.getFleetManager(engine.getPlayerShip().getOriginalOwner());
                manager.setSuppressDeploymentMessages(true);

                float offset = 300f;
                for (int i = 0; i < members.size(); i++) {
                    manager.spawnFleetMember(members.get(i),
                            new Vector2f(0f + (members.size() / 2 - i) * offset, -500f), 90f, 1.5f);
                }
                manager.setSuppressDeploymentMessages(false);
                engine.removePlugin(this);
            }
        });
    }
}
