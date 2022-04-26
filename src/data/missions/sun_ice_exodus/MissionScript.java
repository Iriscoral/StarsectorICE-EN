package data.missions.sun_ice_exodus;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class MissionScript implements EveryFrameCombatPlugin {
	private boolean isFirstFrame = true;
	private CombatEngineAPI engine;

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
	}

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (isFirstFrame) {
			engine.getFleetManager(FleetSide.ENEMY).setSuppressDeploymentMessages(false);
			if (engine.getFleetManager(FleetSide.ENEMY).getReservesCopy().size() > 0) {
				float angle = 90f;
				float spawnX = 0f;
				float spawnY = -engine.getMapHeight() * 0.5f;
				angle *= -1f;

				for (FleetMemberAPI member : engine.getFleetManager(FleetSide.ENEMY).getReservesCopy()) {
					if (member.isFighterWing()) {
						continue;
					}
					Vector2f loc = new Vector2f(spawnX, spawnY);
					engine.getFleetManager(FleetSide.ENEMY).spawnFleetMember(member, loc, angle, 3f);

					if (spawnX > 0f) {
						spawnX *= -1f;
					} else {
						spawnX *= -1f;
						spawnX += 600f;
					}
					if (spawnX >= engine.getMapWidth() * 0.25f) {
						spawnX = 0f;
						spawnY -= 800f;
					}
				}
			}
			isFirstFrame = false;
		}
	}

	@Override
	public void processInputPreCoreControls(float f, List<InputEventAPI> list) {
	}

	@Override
	public void renderInWorldCoords(ViewportAPI viewport) {
	}

	@Override
	public void renderInUICoords(ViewportAPI viewport) {
	}
}