package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicRenderPlugin;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class SUN_ICE_MineAI extends SUN_ICE_BaseMissileAI {
	private static final float ATTACK_RANGE = 600f;
	private static final float MAX_TIME_TO_STOP = 5f;
	private static final float TIME_TO_GLOW = 2f;
	private static final float TTL_AFTER_BURN = 3f;
	private static final float BASE_FUEL = 0.5f;
	private static final float LEAD_TIME_PER_DISTANCE = 1.2f / ATTACK_RANGE; // in seconds
	private static final Color PING_COLOR = new Color(0, 250, 220, 255);

	private float fuel = BASE_FUEL;
	private float timeOfStageCheck;
	private float timeOfBurn = Float.MAX_VALUE;
	private boolean stopped = false;
	private boolean attacking = false;

	private void ping() {
		Global.getCombatEngine().addHitParticle(missile.getLocation(), missile.getVelocity(), 40, 0.8f, 0.1f, PING_COLOR);
	}

	private void glow() {
		SpriteAPI sprite = Global.getSettings().getSprite("fx", "sun_ice_mine_glow");
		sprite.setColor(PING_COLOR);
		sprite.setSize(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);

		simpleObjectBasedRender(sprite, missile, 0f, 0.2f, 0.3f, CombatEngineLayers.ABOVE_PARTICLES_LOWER);
	}

	public void simpleObjectBasedRender(SpriteAPI sprite, CombatEntityAPI anchor, float fadein, float full, float fadeout, CombatEngineLayers layer) {
		Vector2f loc = new Vector2f(anchor.getLocation());
		MagicRenderPlugin.addObjectspace(sprite, anchor, loc, new Vector2f(), null, null, 180f, 0f, true, fadein, fadein + full, fadein + full + fadeout, true, layer);
	}

	public SUN_ICE_MineAI(MissileAPI missile) {
		this.missile = missile;
		timeOfStageCheck = Global.getCombatEngine().getTotalElapsedTime(false);
	}

	@Override
	public CombatEntityAPI findTarget() {
		findFlareTarget(DEFAULT_FLARE_VULNERABILITY_RANGE);

		if (targetIsFlare()) {
			return target;
		}

		target = AIUtils.getNearestEnemy(missile);

		return target;
	}

	@Override
	public void advance(float amount) {
		float time = Global.getCombatEngine().getTotalElapsedTime(false);

		if (!stopped) {
			if (time - timeOfStageCheck > MAX_TIME_TO_STOP || (Math.abs(missile.getVelocity().x) < 0.00001 && Math.abs(missile.getVelocity().y) < 0.00001)) {
				stopped = true;
				timeOfStageCheck = time;

				missile.getVelocity().set(0f, 0f);
				ping();
			}

			decelerate();
			return;
		}

		super.advance(amount);

		if (Math.random() < amount * 0.9f) {
			ping();
		}

		if (time - timeOfStageCheck > TIME_TO_GLOW) {
			// glow();
			timeOfStageCheck = time;
		}

		if (target == null) {
			decelerate();
			return;
		}

		float distance = MathUtils.getDistance(missile, target);
		float leadTime = distance * LEAD_TIME_PER_DISTANCE;
		Vector2f leadPoint = (Vector2f) (new Vector2f(target.getVelocity()).scale(leadTime));
		Vector2f.add(leadPoint, target.getLocation(), leadPoint);
		boolean phased = target instanceof ShipAPI && ((ShipAPI)target).isPhased();

		if (attacking) {
			if (fuel > 0f) {
				accelerate();
				fuel -= amount;
			}
		} else if (!phased && (distance <= ATTACK_RANGE) && isFacing(leadPoint)) {
			attacking = true;
			timeOfBurn = time;
		} else {
			turnToward(leadPoint);
			decelerate();
		}

		if (time - timeOfBurn > TTL_AFTER_BURN) {
			SUN_ICE_IceUtils.destroy(missile);
		}
	}
}
