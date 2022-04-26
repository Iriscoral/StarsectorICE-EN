package data.weapons.decorative;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;

public class UmbraShellEffect implements EveryFrameWeaponEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		WeaponAPI bomb = weapon.getShip().getAllWeapons().get(0);

		float openness = Math.max(bomb.getChargeLevel(), bomb.getCooldownRemaining() / bomb.getCooldown());
		if (bomb.getChargeLevel() > 0f) { // Force it to follow throught with attack because I can't get "interruptibleBurst":false to work for some reason
			weapon.getShip().giveCommand(ShipCommand.FIRE, weapon.getShip().getMouseTarget(), 0);
		}

		weapon.setCurrAngle(openness * 105f * weapon.getRange() + weapon.getShip().getFacing());

		// the weapon.getRange() thing is a hack to distinguish between left and right shell halves

		if (weapon.getRange() > 0) {
			weapon.getShip().getMutableStats().getArmorDamageTakenMult().modifyPercent("sun_ice_umbra_shell", 500f * openness);
		}
	}
}