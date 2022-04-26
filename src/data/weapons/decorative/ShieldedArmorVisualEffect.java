package data.weapons.decorative;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.tools.SUN_ICE_IceUtils;

import java.awt.Color;

public class ShieldedArmorVisualEffect implements EveryFrameWeaponEffectPlugin {

	private static final float ACTIVATION_SPEED = 3f;
	private static final float DEACTIVATION_SPEED = 3f;
	private static final String ID = "sun_ice_shielded_armor";
	//private static final Map<String> SHIELD_HIT_SOUNDS = "";
	private static final float SHIELD_HIT_SOUND_PITCH = 1f;
	private static final float SHIELD_HIT_SOUND_VOLUME = 1f;

	private float alpha = 0f, strength = 1f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();

		if (engine.isPaused() || ship.getShield() == null) {
			return;
		}

		strength = Math.min(1f, strength + amount);

		boolean on = ship.getShield().isOn() && ship.isAlive();

		if (alpha == 0f && !on) {
			weapon.getAnimation().setFrame(0);

			ship.getMutableStats().getShieldArcBonus().unmodify(ID);
			ship.getMutableStats().getShieldDamageTakenMult().unmodify(ID);
			ship.getMutableStats().getHighExplosiveDamageTakenMult().unmodify(ID);
			ship.getMutableStats().getKineticDamageTakenMult().unmodify(ID);
			ship.getMutableStats().getEnergyDamageTakenMult().unmodify(ID);
			ship.getMutableStats().getFragmentationDamageTakenMult().unmodify(ID);

			return;
		} else {
			ship.getShield().setActiveArc(0f);
			ship.getMutableStats().getShieldDamageTakenMult().modifyMult(ID, 0f);
			ship.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult(ID, 1f - strength);
			ship.getMutableStats().getKineticDamageTakenMult().modifyMult(ID, 1f - strength);
			ship.getMutableStats().getEnergyDamageTakenMult().modifyMult(ID, 1f - strength);
			ship.getMutableStats().getFragmentationDamageTakenMult().modifyMult(ID, 1f - strength);
		}

		for (DamagingProjectileAPI p : SUN_ICE_IceUtils.getProjectilesDamagedBy(ship)) {
			float damage = p.getDamageType().getShieldMult() * p.getDamageAmount() * ship.getMutableStats().getShieldAbsorptionMult().getModifiedValue();

			//ship.getFluxTracker().increaseFlux(damage, true);

			//            Global.getSoundPlayer().playSound(SHIELD_HIT_SOUND_ID,
			//                    SHIELD_HIT_SOUND_PITCH, SHIELD_HIT_SOUND_VOLUME,
			//                    p.getLocation(), ship.getVelocity());

			engine.addFloatingDamageText(p.getLocation(), damage, new Color(70, 200, 255), ship, p);
			//engine.removeEntity(p);
		}

		weapon.getSprite().setAdditiveBlend();
		weapon.getAnimation().setFrame(1);

		alpha += Global.getCombatEngine().getElapsedInLastFrame() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
		alpha = Math.max(Math.min(alpha, 1f), 0f);

		weapon.getAnimation().setAlphaMult(alpha * strength);
	}
}