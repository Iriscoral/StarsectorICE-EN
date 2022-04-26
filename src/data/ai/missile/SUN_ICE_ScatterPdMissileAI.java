package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;

import java.awt.*;
import java.util.Random;

public class SUN_ICE_ScatterPdMissileAI implements MissileAIPlugin {
	private final MissileAPI missile;
	private final ShipCommand direction;
	private final ShipCommand strafe;
	private float lifeTime;

	private static final Random rand = new Random();
	private static final Color PARTICLE_COLOR = new Color(160, 240, 220, 255);
	private static final Color EXPLOSION_COLOR = new Color(125, 123, 93, 155);

	public SUN_ICE_ScatterPdMissileAI(MissileAPI missile) {
		this.missile = missile;
		this.lifeTime = rand.nextFloat() * 1.3f + 0.2f;
		this.direction = (rand.nextFloat() < 0.5f) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
		this.strafe = (direction == ShipCommand.TURN_LEFT) ? ShipCommand.STRAFE_LEFT : ShipCommand.STRAFE_RIGHT;
	}

	@Override
	public void advance(float amount) {
		missile.giveCommand(direction);
		missile.giveCommand(strafe);
		missile.giveCommand(ShipCommand.ACCELERATE);

		lifeTime -= amount;

		if (missile.isFizzling() || lifeTime < 0f) {
			DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 80f, 45f, missile.getDamageAmount(), missile.getDamageAmount() * 0.5f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER, 3f, 3f, 2f, 5, PARTICLE_COLOR, EXPLOSION_COLOR);
			spec.setDamageType(missile.getDamageType());
			spec.setSoundSetId("explosion_missile");
			spec.setUseDetailedExplosion(false);
			Global.getCombatEngine().spawnDamagingExplosion(spec, missile.getSource(), missile.getLocation(), false);
			Global.getCombatEngine().removeEntity(missile);
		}
	}
}