package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import data.shipsystems.SUN_ICE_JauntHeavyStats;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;
import java.util.WeakHashMap;

public class SUN_ICE_JauntAI implements ShipSystemAIScript {
	private static final float USE_THRESHHOLD = 100.0f;
	private static final float ACTIVATION_SECONDS = 3f;
	//static final float MAX_AMMO = 4.0f;

	private CombatEngineAPI engine;
	private ShipSystemAPI system;
	private ShipAPI ship;
	private final SUN_ICE_IntervalTracker timer = new SUN_ICE_IntervalTracker(0.1f, 0.7f);
	private static final Map<ShipAPI, Vector2f> origins = new WeakHashMap<>();
	private int ticksWithoutDissipation = 0;

	public static void setOrigin(ShipAPI ship, Vector2f origin) {
		origins.put(ship, origin);
	}

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		FluxTrackerAPI reactor = ship.getFluxTracker();

		if (!timer.intervalElapsed(engine) || ship == null || system.getAmmo() == 0 || system.isCoolingDown() || reactor.isOverloadedOrVenting() || ship.getCollisionClass() == CollisionClass.NONE) {
			return;
		}

		float flux, phaseNecessity = 0f;
		float damage = SUN_ICE_IceUtils.estimateIncomingDamage(ship, 1) * 1f;
		float armor = (float)Math.pow(SUN_ICE_IceUtils.getArmorPercent(ship), 3);
		//flux = (float)Math.sqrt(reactor.getFluxLevel());
		boolean noSoftFlux = reactor.getCurrFlux() == reactor.getHardFlux();
		boolean canTurn = SUN_ICE_IceUtils.getEngineFractionDisabled(ship) > 0f;

		if (system.isOn()) {
			flux = 0f;
			phaseNecessity += reactor.getFluxLevel() * USE_THRESHHOLD * 1.25f;
			//flux = (float)Math.pow(reactor.getFluxLevel(), 1);
			damage *= 0.5f;

			// No reason to stay here if we're not shooting anything or dissipating soft flux
			if (noSoftFlux) {
				++ticksWithoutDissipation;
			} else {
				ticksWithoutDissipation = 0;
			}

			phaseNecessity += 20f * ticksWithoutDissipation;

			// Don't want to return if it's dangerous
			Vector2f temp = new Vector2f(ship.getLocation());
			ship.getLocation().set(origins.get(ship));
			damage -= SUN_ICE_IceUtils.estimateIncomingDamage(ship, 2);
			ship.getLocation().set(temp);
		} else {
			flux = (float) Math.sqrt(reactor.getHardFlux() / reactor.getMaxFlux());
			ticksWithoutDissipation = 0;

			// Prevent from using when it doesn't have enough flux to use
			if (reactor.getCurrFlux() > reactor.getMaxFlux() - system.getFluxPerSecond() * ACTIVATION_SECONDS) {
				return;
			}

			// Check if we're in a good position to attack a distant target remotely
			if (reactor.getFluxLevel() < 0.7f) {
				int enemy = (ship.getOwner() + 1) % 2;
				float range = SUN_ICE_IceUtils.estimateOptimalRange(ship) * 0.8f;
				float fp = SUN_ICE_IceUtils.getFPStrength(ship);
				float hostilityInEminentRange = SUN_ICE_IceUtils.getStrengthInArea(ship.getLocation(), range, enemy);
				float hostilityInRemoteRange = Math.min(fp, SUN_ICE_IceUtils.getStrengthInArea(ship.getLocation(), range + SUN_ICE_JauntHeavyStats.getRange(ship), enemy) - hostilityInEminentRange);
				boolean canJauntToBetterTargets = hostilityInEminentRange < fp / 6f && hostilityInEminentRange < hostilityInRemoteRange;

				if (canJauntToBetterTargets || !canTurn) {
					phaseNecessity += USE_THRESHHOLD * 1.6f * (1f - reactor.getFluxLevel());
				}
			}

			//phaseNecessity -= USE_THRESHHOLD * (1 - system.getAmmo() / MAX_AMMO);
		}

		phaseNecessity += (damage * (1.2f - armor)) * (1f - flux);

		if (phaseNecessity >= USE_THRESHHOLD) {
			//SunUtils.print(ship, "" + phaseNecessity);
			ship.useSystem();
		}
	}
}