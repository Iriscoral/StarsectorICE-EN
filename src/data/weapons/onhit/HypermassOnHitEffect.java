package data.weapons.onhit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class HypermassOnHitEffect implements OnHitEffectPlugin {
	public static final float FORCE = 1200f;

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

		CombatUtils.applyForce(target, projectile.getVelocity(), FORCE / 4);
		if (target instanceof ShipAPI) {
			ShipAPI enemy = (ShipAPI)target;
			if (enemy.isDrone() || enemy.isFighter()) {
				engine.applyDamage(enemy, enemy.getLocation(), projectile.getDamageAmount(), projectile.getDamageType(), projectile.getEmpAmount(), true, false, projectile.getSource());
			}
		}

		//
		//        engine.spawnEmpArc((ShipAPI)projectile.getSource(),
		//                new Vector2f(target.getLocation().x + 1, target.getLocation().y),
		//                target, target, DamageType.KINETIC, FORCE, FORCE, 1000, null, 20, Color.orange, Color.red);
	}
}
