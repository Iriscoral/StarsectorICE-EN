package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import data.shipsystems.SUN_ICE_FoFInverterStats;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class SUN_ICE_FoFInverterAI implements ShipSystemAIScript {
	private ShipSystemAPI system;
	private ShipAPI ship;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (system.isActive()) {
			return;
		}

		if (!AIUtils.canUseSystemThisFrame(ship)) {
			return;
		}

		float accumulator = 0f;
		MissileAPI missile;
		for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
			if (!(proj instanceof MissileAPI)) continue; // Ignore non-missiles
			if (proj.getOwner() == ship.getOwner()) continue; // Ignore friendly projectiles
			if (MathUtils.getDistance(proj, ship) > SUN_ICE_FoFInverterStats.getRange(ship) * 0.9f) continue; // Ignore too-distant projectiles

			missile = (MissileAPI) proj;

			float threat = missile.getDamageAmount();
			threat *= missile.getDamageType().getArmorMult();
			threat += missile.getEmpAmount();

			accumulator += threat;
		}

		if (accumulator > 1500f) {
			ship.useSystem();
		}
	}
}
