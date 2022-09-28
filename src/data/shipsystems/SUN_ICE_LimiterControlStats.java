package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SUN_ICE_LimiterControlStats extends BaseShipSystemScript {
	private static final Color LIGHT_COLOR = new Color(255, 235, 100);
	private static final Color EXP_COLOR = new Color(255, 255, 120, 80);
	private static final Color GLOW_COLOR = new Color(255, 255, 120, 120);
	private static final Vector2f ZERO = new Vector2f();
	private static final String LC_KEY = "SUN_ICE_LimiterControl";

	private ShipAPI target;
	private float clock = 0f;

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "SUN_ICE_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null || Global.getCombatEngine().isPaused()) {
			return;
		}

		init(ship);
		ship.setJitter(ship, LIGHT_COLOR, effectLevel, 10, 0f, 18f);

		if (!isDownshifted(ship)) {
			ship.setPhased(true);
			ship.setExtraAlphaMult(1f - effectLevel);
			ship.getVelocity().scale(0.95f);
			ship.setAngularVelocity(ship.getAngularVelocity() * 0.9f);

			if (target == null) {
				target = ship.getShipTarget();
			}

			if (effectLevel == 1f) {
				if (target == null) {
					return; // maybe?
				}

				Vector2f jumpLoc = MathUtils.getPointOnCircumference(target.getLocation(), target.getCollisionRadius() + ship.getCollisionRadius() + 100f, target.getFacing() + 180f);
				ship.getLocation().set(jumpLoc);
				ship.setFacing(target.getFacing());
				target.getVelocity().scale(0.5f);
				target.setAngularVelocity(target.getAngularVelocity() * 0.5f);

				Global.getSoundPlayer().playSound("sun_ice_system_limitercontrol_jump", 1f, 1f, ship.getLocation(), ZERO);
				MagicLensFlare.createSmoothFlare(Global.getCombatEngine(), ship, ship.getLocation(), 30f, 3000f, 0f, LIGHT_COLOR, Color.CYAN);
				Global.getCombatEngine().spawnExplosion(ship.getLocation(), ZERO, EXP_COLOR, ship.getCollisionRadius() * 7f, 1f);
			} else if (state != State.IN) {
				if ((float) Math.random() < effectLevel) {
					Vector2f initLoc = MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 2f * (1f - effectLevel * 0.5f));
					Vector2f targetLoc = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * (1f - effectLevel * 0.8f), VectorUtils.getAngle(ship.getLocation(), initLoc) + 90f);
					Vector2f vel = Vector2f.sub(initLoc, targetLoc, null);

					float size = (float) Math.random() * 100f + 30f;
					vel.scale(-200f / size);
					int a = (int) (155f * (float) Math.random()) + 100;
					if ((float) Math.random() < 0.8f) {
						float brightness = (float) Math.random() * 0.5f + 0.5f;
						Global.getCombatEngine().addSmoothParticle(initLoc, vel, size, brightness, size * 0.01f, new Color(169, 200, 177, a));
					} else {
						a = a >> 2;
						Global.getCombatEngine().spawnExplosion(ship.getLocation(), vel, new Color(177, 169, 200, a), size, size * 0.007f);
					}
				}
			}
		} else {
			if (clock == 0f && effectLevel < 0.1f && state != State.OUT) {
				Global.getSoundPlayer().playSound("sun_ice_system_limitercontrol_charge", 1f, 1f, ship.getLocation(), ZERO);
			}

			stats.getTimeMult().modifyPercent(id, 500f * effectLevel);

			ship.setAngularVelocity(0f);
			ship.setCollisionClass(CollisionClass.FIGHTER);
			ship.giveCommand(ShipCommand.ACCELERATE, ship.getMouseTarget(), 0);
			ship.getEngineController().extendFlame(ship, 2f, 2f, 1f);
			CombatUtils.applyForce(ship, ship.getFacing(), 10000f * effectLevel);

			clock += Global.getCombatEngine().getElapsedInLastFrame();
			if (clock >= 0.2f) {
				ship.addAfterimage(GLOW_COLOR, 0f, 0f, -ship.getVelocity().x * 0.6f, -ship.getVelocity().y * 0.6f, effectLevel, effectLevel, 0.5f, 1.2f - effectLevel, true, false, false);
				clock = 0f;
			}

			for (ShipAPI target : AIUtils.getNearbyEnemies(ship, 100)) {
				if (target.isPhased()) {
					continue;
				}

				List<Vector2f> ats = new ArrayList<>();
				float angle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
				for (int i = -3; i < 4; i++) {
					Vector2f idealCollPoint = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, angle + i * 20f);
					if (CollisionUtils.isPointWithinBounds(idealCollPoint, target)) {
						ats.add(idealCollPoint);
					} else {
						Vector2f actualCollPoint = CollisionUtils.getCollisionPoint(ship.getLocation(), idealCollPoint, target);
						if (actualCollPoint != null) {
							ats.add(actualCollPoint);
						} else if (CollisionUtils.isPointWithinBounds(ship.getLocation(), target)) {
							ats.add(ship.getLocation());
						}
					}
				}

				for (Vector2f at : ats) { // originally 2500
					float damage = 2800f * Global.getCombatEngine().getElapsedInLastFrame();
					Global.getCombatEngine().applyDamage(target, at, damage, DamageType.HIGH_EXPLOSIVE, 0f, true, true, ship);

					CombatUtils.applyForce(target, ship.getVelocity(), 10f);
					Global.getCombatEngine().spawnExplosion(at, // Location
							target.getVelocity(), // Velocity
							LIGHT_COLOR, // Color
							50f + (float) Math.random() * 100f, // Size
							1f + (float) Math.random() * 1f); // Duration
					Global.getSoundPlayer().playSound("collision_ships", 1f, 1f, ship.getLocation(), target.getVelocity());
				}
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		if (stats.getEntity() == null) {
			return;
		}

		ShipAPI ship = (ShipAPI) stats.getEntity();

		ship.setCollisionClass(CollisionClass.SHIP);
		ship.getMutableStats().getTimeMult().unmodify(id);
		ship.setPhased(false);

		clock = 0f;
		target = null;

		if (isDownshifted(ship)) {
			setDownshifted(ship, false);
			ship.getVelocity().scale(0.5f);
		} else {
			setDownshifted(ship, true);
		}
	}

	public static boolean beforeInit(ShipAPI ship) {
		return !ship.getMutableStats().getDynamic().getMods().containsKey(LC_KEY);
	}

	public static void init(ShipAPI ship) {
		ship.getMutableStats().getDynamic().getMod(LC_KEY);
	}

	public static boolean isDownshifted(ShipAPI ship) {
		if (beforeInit(ship)) return false;
		return ship.getMutableStats().getDynamic().getMod(LC_KEY).computeEffective(0f) == 1f;
	}

	public static void setDownshifted(ShipAPI ship, boolean value) {
		if (beforeInit(ship)) return;
		ship.getMutableStats().getDynamic().getMod(LC_KEY).modifyFlat(LC_KEY, value ? 1f : 0f);
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (isDownshifted(ship)) {
			return true;
		}
		if (ship.getShipTarget() == null) {
			return false;
		}

		ShipAPI target = ship.getShipTarget();
		if (MathUtils.getDistance(ship, target) > 1500f) {
			return false;
		}

		if (!target.isDrone() && !target.isFighter() && target.getOwner() != ship.getOwner() && target.isAlive() && !target.isStation() && !target.isStationModule()) {
			Vector2f point = MathUtils.getPointOnCircumference(target.getLocation(), target.getCollisionRadius() + ship.getCollisionRadius() + 10f, target.getFacing() + 180f);
			return !isShipObstructingArea(point, ship.getCollisionRadius());
		}
		return false;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != ShipSystemAPI.SystemState.IDLE) {
			return "";
		}
		if (isDownshifted(ship)) {
			return strings.get("LimiterControlStats1");
		}
		if (ship.getShipTarget() == null) {
			return strings.get("LimiterControlStats3");
		}
		if (isUsable(system, ship)) {
			return strings.get("LimiterControlStats2");
		}
		return strings.get("LimiterControlStats4");
	}

	private boolean isShipObstructingArea(Vector2f at, float range) {
		for (ShipAPI s : CombatUtils.getShipsWithinRange(at, range)) {
			if (!s.isFighter()) {
				return true;
			}
		}
		return false;
	}
}