package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class FlameboltOnHitEffect implements OnHitEffectPlugin {

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target == null) {
			return;
		}
		if (!(target instanceof ShipAPI)) {
			return;
		}

		float coreRadius = projectile.getCollisionRadius() * 1.5f;
		float maxRadius = coreRadius * 2f;

		Color explosionColor = projectile.getProjectileSpec().getGlowColor();
		Color particleColor = projectile.getProjectileSpec().getCoreColor();

		DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, maxRadius, coreRadius, projectile.getDamageAmount(), projectile.getDamageAmount() * 0.5f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER, 5f, 10f, 2f, 20, particleColor, explosionColor);
		spec.setDamageType(projectile.getDamageType());
		spec.setSoundSetId("explosion_flak");
		spec.setUseDetailedExplosion(false);

		DamagingProjectileAPI proj = Global.getCombatEngine().spawnDamagingExplosion(spec, projectile.getSource(), projectile.getLocation(), false);
		proj.addDamagedAlready(target);
	}
}
