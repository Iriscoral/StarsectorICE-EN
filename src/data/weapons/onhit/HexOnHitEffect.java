package data.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class HexOnHitEffect implements OnHitEffectPlugin {
	private static final Color PARTICLE_COLOR = new Color(59, 125, 94, 255);
	private static final Color EXPLOSION_COLOR = new java.awt.Color(128, 116, 71, 155);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target == null) {
			return;
		}

		DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 15f, 6f, projectile.getDamageAmount(), projectile.getDamageAmount() * 0.5f, CollisionClass.PROJECTILE_NO_FF, CollisionClass.PROJECTILE_FIGHTER, 2f, 3f, 2f, 5, PARTICLE_COLOR, EXPLOSION_COLOR);
		spec.setDamageType(projectile.getDamageType());
		spec.setSoundSetId("explosion_flak");
		spec.setUseDetailedExplosion(false);

		DamagingProjectileAPI proj = Global.getCombatEngine().spawnDamagingExplosion(spec, projectile.getSource(), projectile.getLocation(), false);
		proj.addDamagedAlready(target);
	}
}
