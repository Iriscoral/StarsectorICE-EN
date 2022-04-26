package data.weapons.decorative;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.ICEModPlugin;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_JauntSession;
import data.shipsystems.SUN_ICE_LimiterControlStats;
import org.lazywizard.lazylib.FastTrig;

import java.util.Map;
import java.util.WeakHashMap;

public class LightsVisualEffect implements EveryFrameWeaponEffectPlugin {

	private static final float ACTIVATE_SPEED = 5f;
	private static final float DEACTIVATE_SPEED = 1f;
	private static final float STATIC_ALPHA = 0.35f;
	private static final Map<ShipAPI, Float> offsets = new WeakHashMap<>();

	private float alpha = STATIC_ALPHA;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

		//if (engine.isPaused()) {
		//	return;
		//}

		ShipAPI ship = weapon.getShip();

		boolean downshifted = SUN_ICE_LimiterControlStats.isDownshifted(ship);
		boolean usingSystem = ship.getSystem() != null && ship.getSystem().isActive();
		boolean usingPhase = ship.getPhaseCloak() != null && (ship.getPhaseCloak().isActive() || ship.getPhaseCloak().isCoolingDown());
		boolean on = ship.isAlive() && !ship.getFluxTracker().isOverloadedOrVenting() && !SUN_ICE_JauntSession.hasSession(ship) && !usingSystem && !usingPhase && !downshifted;

		if (alpha == 0f && !on) {
			return;
		}

		weapon.getSprite().setAdditiveBlend();

		if (SUN_ICE_IceUtils.isInRefit() || ICEModPlugin.SMILE_FOR_CAMERA) {
			//String id = weapon.getId();
			weapon.getAnimation().setAlphaMult(STATIC_ALPHA);
		} else {
			if (!offsets.containsKey(ship)) {
				offsets.put(ship, (float) (Math.random() * 1000f));
			}

			float wave = (float) FastTrig.cos(engine.getTotalElapsedTime(false) * Math.PI + offsets.get(ship));
			wave *= (float) FastTrig.cos(engine.getTotalElapsedTime(false) * Math.E / 3);
			alpha += amount * (on ? ACTIVATE_SPEED : -DEACTIVATE_SPEED);
			alpha = Math.max(Math.min(alpha, 1f), 0f);
			weapon.getAnimation().setAlphaMult(alpha * (wave / 3f + 0.66f));
		}
	}
}