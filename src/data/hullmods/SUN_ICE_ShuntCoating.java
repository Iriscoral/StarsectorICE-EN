package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SUN_ICE_ShuntCoating extends BaseHullMod {
	private static final Vector2f ZERO = new Vector2f();
	private static final Color COLOR = new Color(60, 60, 60, 150);

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new ShuntCoatingMod(ship));
	}

	public static class ShuntCoatingMod implements DamageTakenModifier, DamageListener {
		private final ShipAPI ship;
		private final float dpsDur = 0.1f;

		public ShuntCoatingMod(ShipAPI ship) {
			this.ship = ship;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (!(param instanceof BeamAPI)) {
				return null;
			}

			BeamAPI beam = (BeamAPI)param;
			float amount = Global.getCombatEngine().getElapsedInLastFrame();

			float beamDamage = damage.getDamage() + damage.getFluxComponent();
			if (beamDamage > 0f && (float) Math.random() < amount * 3f) {
				Global.getCombatEngine().addSmokeParticle(beam.getRayEndPrevFrame(), ZERO, beam.getWidth() * 5f, 0.5f, 0.5f, COLOR);
			}

			return null;
		}

		@Override
		public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
			if (!result.isDps()) {
				return;
			}

			float damageAmount = result.getDamageToHull() * 0.5f * dpsDur;
			damageAmount += result.getTotalDamageToArmor() * 0.5f * dpsDur;
			damageAmount += result.getEmpDamage() * 0.25f * dpsDur;

			ship.getFluxTracker().decreaseFlux(damageAmount);
		}
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "50%";
		if (index == 1) return "25%";
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && ship.getHullSpec().getHullId().startsWith("sun_ice_");
	}
}