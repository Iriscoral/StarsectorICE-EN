package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import data.ai.weapon.*;

import java.util.ArrayList;
import java.util.List;

public class MobiusRayEffect implements EveryFrameWeaponEffectPlugin {
	private static final float MAX_ROTATION_PER_SECOND = 300f; // originally 270
	private final List<DamagingProjectileAPI> functionalProjs = new ArrayList<>();
	private final List<DamagingProjectileAPI> toRemove = new ArrayList<>();
	private float timeOfFiring;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (!weapon.isFiring() && functionalProjs.isEmpty()) {
			timeOfFiring = engine.getTotalElapsedTime(false);
			return;
		}

		//prevent weapon cooldown
		if (weapon.getChargeLevel() < 0.98f && !functionalProjs.isEmpty()) {
			weapon.setRemainingCooldownTo(weapon.getCooldown());
		}

		for (DamagingProjectileAPI proj : engine.getProjectiles()) {
			if (proj.getWeapon() != weapon) {
				continue;
			}
			if (proj.getProjectileSpecId().endsWith("hack")) {
				continue;
			}

			DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), proj.getWeapon(), "sun_ice_mobiusrayhack", proj.getLocation(), proj.getFacing(), null);
			functionalProjs.add(newProj);
			engine.removeEntity(proj);
		}

		ShipAPI ship = weapon.getShip();
		boolean disable = weapon.isDisabled() || !ship.isAlive() || ship.getFluxTracker().isOverloadedOrVenting();
		for (DamagingProjectileAPI proj : functionalProjs) {
			if (disable || proj == null || !engine.isEntityInPlay(proj) || proj.isFading() || MathUtils.getDistance(proj, weapon.getLocation()) > weapon.getRange()) {
				engine.removeEntity(proj);
				toRemove.add(proj);
			}
		}
		functionalProjs.removeAll(toRemove);
		toRemove.clear();

		advanceFunctionalProjs(amount, engine, weapon);
	}

	private void advanceFunctionalProjs(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();
		SUN_ICE_MobiusRayAutofireAIPlugin autofire = SUN_ICE_MobiusRayAutofireAIPlugin.get(weapon);
		Vector2f target = autofire.isOn() ? new Vector2f(autofire.getTarget()) : new Vector2f(ship.getMouseTarget());
		DamagingProjectileAPI previous = null;
		float maxRotation = MAX_ROTATION_PER_SECOND * amount;

		if (ship.getShipAI() != null || autofire.isOn()) {
			// Select exact target ship location rather than default AI cursor
			if (ship.getShipTarget() != null && !autofire.isOn()) {
				target = new Vector2f(ship.getShipTarget().getLocation());
			}

			float radius = Math.min(weapon.getRange() * 0.8f, MathUtils.getDistance(weapon.getLocation(), target));
			float scale = Math.max(0, 0.7f - (engine.getTotalElapsedTime(false) - timeOfFiring));
			target.x += Math.sin(engine.getTotalElapsedTime(false) * Math.PI) * radius * scale;
			target.y += Math.sin(engine.getTotalElapsedTime(false) * Math.E) * radius * scale;

			//SUN_ICE_IceUtils.blink(target);
		}

		for (DamagingProjectileAPI proj : functionalProjs) {
			if (previous != null) {
				target = previous.getLocation();
			}
			//SUN_ICE_IceUtils.blink(proj.getLocation());

			float d = VectorUtils.getAngle(proj.getLocation(), target);
			d = MathUtils.getShortestRotation(proj.getFacing(), d);
			d = Math.signum(d) * Math.min(maxRotation, Math.abs(d));
			proj.setFacing(MathUtils.clampAngle(proj.getFacing() + d));

			if (previous == null) {
				maxRotation *= 3f;
			}
			previous = proj;
		}

		autofire.setIsOn(false);
	}
}