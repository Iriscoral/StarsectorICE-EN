package data.ai.drone;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI.DroneOrders;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import data.ai.ship.SUN_ICE_BaseShipAI;
import data.hullmods.SUN_ICE_MunitionsAutoFac;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SUN_ICE_ICEMxDroneAI extends SUN_ICE_BaseShipAI {

	private final ShipAPI mothership;
	private ShipAPI target;
	private final DroneLauncherShipSystemAPI system;
	private Vector2f targetOffset;
	private final Point cellToFix = new Point();
	private final Random rng = new Random();
	private ArmorGridAPI armorGrid;
	private float max;
	private float cellSize;
	private int gridWidth;
	private int gridHeight;
	private int cellCount;
	private int ticksLeftToMx = 0;
	private boolean doingMx = false;
	private boolean returning = false;
	private float dontRestoreAmmoUntil;
	private float targetFacingOffset = Float.MIN_VALUE;

	private static final HashMap<ShipAPI, Float> peakCrRecovery = new HashMap<>();
	private static final HashMap<ShipAPI, Integer> mxAssistTracker = new HashMap<>();
	private static final HashMap<ShipAPI, Float> mxPriorities = new HashMap<>();
	private static final float mxPriorityUpdateFrequency = 2f;
	private static float timeOfMxPriorityUpdate = 2f;

	private static final float REPAIR_RANGE = 200f;
	private static final float ROAM_RANGE = 3000f;
	private static final float REPAIR_AMOUNT = 0.6f;
	private static final float CR_PEAK_TIME_RECOVERY_RATE = 2.5f;
	private static final float FLUX_PER_MX_PERFORMED = 1f;
	private static final float COOLDOWN_PER_OP_OF_AMMO_RESTORED = 35f; // In seconds

	private static final Color SPARK_COLOR = new Color(255, 223, 128);
	private static final String SPARK_SOUND_ID = "system_emp_emitter_loop";
	private static final float SPARK_DURATION = 0.2f;
	private static final float SPARK_BRIGHTNESS = 1f;
	private static final float SPARK_MAX_RADIUS = 7f;
	private static final float SPARK_CHANCE = 0.17f;
	private static final float SPARK_SPEED_MULTIPLIER = 500f;
	private static final float SPARK_VOLUME = 1f;
	private static final float SPARK_PITCH = 1f;

	private static final List<String> MISSILE_BLACK_LIST = new ArrayList<>();
	private static final List<String> HULLMOD_BLACK_LIST = new ArrayList<>();

	static {
		MISSILE_BLACK_LIST.add("ii_titan_w");
		MISSILE_BLACK_LIST.add("TSC_Claymore-missile");
		MISSILE_BLACK_LIST.add("le_dram_w");

		HULLMOD_BLACK_LIST.add(HullMods.SAFETYOVERRIDES);
		HULLMOD_BLACK_LIST.add("Kantech_solstice");
	}

	private static void updateMxPriorities(ShipAPI mothership) {
		mxAssistTracker.clear();
		mxPriorities.clear();

		for (ShipAPI ship : Global.getCombatEngine().getShips()) {
			if (ship.isAlive() && ship.isDrone() && ship.getHullSpec().getHullId().contentEquals("sun_ice_drone_mx")) {
				addMxAssistance(ship.getShipTarget(), 1);
			}
		}

		for (ShipAPI ship : AIUtils.getNearbyAllies(mothership, ROAM_RANGE)) {
			if (!ship.isDrone() && !ship.isFighter()) {
				mxPriorities.put(ship, getMxPriority(ship));
				//Global.getCombatEngine().addFloatingText(s.getLocation(), ((Float)mxPriorities.get(s)).toString(), 40, Color.green, s, 1, 5);
			}
		}

		//        Utils.print(
		//                "   Priority:" + (Float)mxPriorities.get(Global.getCombatEngine().getPlayerShip()) +
		//                "   Armor:" + getArmorPercent(Global.getCombatEngine().getPlayerShip()) +
		//                "   Ordnance:" + getExpendedOrdnancePoints(Global.getCombatEngine().getPlayerShip()) +
		//                "   MxAssist:" + getMxAssistance(Global.getCombatEngine().getPlayerShip()) +
		//                "   PeakCR:" + getSecondsTilCrLoss(Global.getCombatEngine().getPlayerShip()));
		timeOfMxPriorityUpdate = mxPriorityUpdateFrequency + Global.getCombatEngine().getTotalElapsedTime(false);
	}

	private static void addMxAssistance(ShipAPI ship, int amount) {
		if (ship != null) {
			if (!mxAssistTracker.containsKey(ship)) {
				mxAssistTracker.put(ship, amount);
			} else {
				mxAssistTracker.put(ship, (mxAssistTracker.get(ship)) + amount);
			}
		}
	}

	private static int getMxAssistance(ShipAPI ship) {
		return (mxAssistTracker.containsKey(ship)) ? mxAssistTracker.get(ship) : 0;
	}

	private static float getSecondsTilCrLoss(ShipAPI ship) {
		float secondsTilCrLoss = 0f;

		if (ship.losesCRDuringCombat()) {
			if (peakCrRecovery.containsKey(ship)) {
				secondsTilCrLoss += peakCrRecovery.get(ship);
			}

			secondsTilCrLoss += ship.getHullSpec().getNoCRLossTime() - ship.getTimeDeployedForCRReduction();

		} else {
			secondsTilCrLoss = Float.MAX_VALUE;
		}

		return Math.max(0f, secondsTilCrLoss);
	}

	private static float getExpendedOrdnancePoints(ShipAPI ship) {
		float acc = 0f;

		for (WeaponAPI weapon : ship.getUsableWeapons()) {
			if (!weapon.usesAmmo()) continue;
			if (weapon.getAmmoPerSecond() > 0) continue;
			if (MISSILE_BLACK_LIST.contains(weapon.getId())) continue;

			acc += (1f - weapon.getAmmo() / (float) weapon.getMaxAmmo()) * weapon.getSpec().getOrdnancePointCost(null);
		}

		return acc;
	}

	private static float getMxPriority(ShipAPI ship) {
		float priority = getExpendedOrdnancePoints(ship);
		priority /= (2f + getMxAssistance(ship));

		float fp = SUN_ICE_IceUtils.getFPCost(ship);
		float peakCrLeft = getSecondsTilCrLoss(ship);

		if (ship.getHullSpec().getHullId().startsWith("sun_ice_")) {
			priority += 1f * (1f - SUN_ICE_IceUtils.getArmorPercent(ship)) * fp;
		}

		boolean considerCR = ship.losesCRDuringCombat();
		if (considerCR) for (String black : HULLMOD_BLACK_LIST) {
			if (ship.getVariant().hasHullMod(black)) {
				considerCR = false;
				break;
			}
		}

		if (considerCR) {
			priority += 1f * ((60f / (60f + peakCrLeft)) * (1 - peakCrLeft / ship.getHullSpec().getNoCRLossTime()) * fp);
		}

		if (ship == Global.getCombatEngine().getPlayerShip()) {
			priority *= 2f;
		}

		return priority;
	}

	@Override
	public void evaluateCircumstances() {
		ticksLeftToMx--;
		if (!mothership.isAlive()) {
			SUN_ICE_IceUtils.destroy(ship);
		}

		if (timeOfMxPriorityUpdate <= Global.getCombatEngine().getTotalElapsedTime(false) || timeOfMxPriorityUpdate > Global.getCombatEngine().getTotalElapsedTime(false) + mxPriorityUpdateFrequency) {
			updateMxPriorities(mothership);
		}

		ShipAPI previousTarget = target;
		setTarget(chooseTarget());

		if (returning) {
			targetOffset = SUN_ICE_IceUtils.toRelative(target, system.getLandingLocation(ship));
		} else if (target != previousTarget || ticksLeftToMx < 1) {
			ticksLeftToMx = 5;

			do {
				targetOffset = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
			} while (!CollisionUtils.isPointWithinBounds(targetOffset, target));

			targetOffset = SUN_ICE_IceUtils.toRelative(target, targetOffset);

			armorGrid = target.getArmorGrid();
			max = armorGrid.getMaxArmorInCell();
			cellSize = armorGrid.getCellSize();
			gridWidth = armorGrid.getGrid().length;
			gridHeight = armorGrid.getGrid()[0].length;
			cellCount = gridWidth * gridHeight;
		}

		if (!target.isPhased() && !returning && MathUtils.getDistance(ship, target) < REPAIR_RANGE && mxPriorities.containsKey(target) && mxPriorities.get(target) > 0) {
			performMaintenance();
		} else {
			doingMx = false;
		}

		if (doingMx == ship.getPhaseCloak().isOn()) {
			ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
		}
	}

	private void performMaintenance() {
		for (int i = 0; i < (1 + cellCount / 5); ++i) {
			cellToFix.x = rng.nextInt(gridWidth);
			cellToFix.y = rng.nextInt(gridHeight);

			if (armorGrid.getArmorValue(cellToFix.x, cellToFix.y) < max) {
				break;
			}
		}

		Vector2f at = armorGrid.getLocation(cellToFix.x, cellToFix.y);
		for (int i = 0; (i < 10) && !CollisionUtils.isPointWithinBounds(at, target); ++i) {
			at = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
		}

		if (dontRestoreAmmoUntil <= Global.getCombatEngine().getTotalElapsedTime(false)) {
			restoreAmmo();
		}

		ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux() + FLUX_PER_MX_PERFORMED);

		doingMx = true;
	}

	private void restoreAmmo() {

		WeaponAPI winner = null;
		float lowestAmmo = 1f;

		for (WeaponAPI weapon : target.getUsableWeapons()) {
			if (!weapon.usesAmmo()) continue;
			if (weapon.getAmmoPerSecond() > 0f) continue;
			if (MISSILE_BLACK_LIST.contains(weapon.getId())) continue;

			float ammo = weapon.getAmmo() / (float) weapon.getMaxAmmo();
			if (ammo < lowestAmmo) {
				lowestAmmo = ammo;
				winner = weapon;
			}
		}

		if (winner == null) {
			dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + 1f;
			return;
		}

		float op = winner.getSpec().getOrdnancePointCost(null);
		int ammoToRestore = (int) Math.max(1, Math.floor(winner.getMaxAmmo() / op));
		ammoToRestore = Math.min(ammoToRestore, winner.getMaxAmmo() - winner.getAmmo());
		winner.setAmmo(winner.getAmmo() + ammoToRestore);

		float basicCooldown = COOLDOWN_PER_OP_OF_AMMO_RESTORED * ((ammoToRestore / (float) winner.getMaxAmmo()) * op);
		float hullPenalty = target.getHullSpec().getHullId().startsWith("sun_ice_") ? 1f : 2f;
		float hullmodBonus = target.getVariant().hasHullMod(SUN_ICE_MunitionsAutoFac.id) ? 0.5f : 1f;
		dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + basicCooldown * hullPenalty * hullmodBonus;
	}

	private void repairArmor() {
		if (cellToFix == null) {
			return;
		}

		float totalRepaired = 0f;

		for (int x = cellToFix.x - 1; x <= cellToFix.x + 1; ++x) {
			if (x < 0 || x >= gridWidth) {
				continue;
			}

			for (int y = cellToFix.y - 1; y <= cellToFix.y + 1; ++y) {
				if (y < 0 || y >= gridHeight) {
					continue;
				}

				float mult = (3f - Math.abs(x - cellToFix.x) - Math.abs(y - cellToFix.y)) / 3f;

				totalRepaired -= armorGrid.getArmorValue(x, y);
				armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + REPAIR_AMOUNT * mult));
				totalRepaired += armorGrid.getArmorValue(x, y);
			}
		}//bug?
		//((Ship) target).syncWithArmorGridState();
		//((Ship) target).syncWeaponDecalsWithArmorDamage();
		SUN_ICE_IceUtils.showHealText(ship, ship.getLocation(), totalRepaired);
	}

	private void maintainCR(float amount) {
		if (target.losesCRDuringCombat()) {
			float peakTimeRecovered = 0f;

			if (!peakCrRecovery.containsKey(target)) {
				peakCrRecovery.put(target, 0f);
			} else {
				peakTimeRecovered = peakCrRecovery.get(target);
			}

			float t = target.getTimeDeployedForCRReduction() - peakTimeRecovered - target.getHullSpec().getNoCRLossTime();
			t *= target.getHullSpec().getHullId().startsWith("sun_ice_") ? 1f : 0.5f;

			peakTimeRecovered += Math.max(t, 0f);
			peakTimeRecovered += amount * (CR_PEAK_TIME_RECOVERY_RATE + target.getHullSpec().getCRLossPerSecond());
			peakTimeRecovered = Math.min(peakTimeRecovered, target.getTimeDeployedForCRReduction());

			target.getMutableStats().getPeakCRDuration().modifyFlat("sun_ice_drone_mx_repair", peakTimeRecovered);

			peakCrRecovery.put(target, peakTimeRecovered);
		}
	}

	private ShipAPI chooseTarget() {
		if (needsRefit() || system.getDroneOrders() == DroneOrders.RECALL) {
			returning = true;
			ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux());
			return mothership;
		} else {
			returning = false;
		}

		if (mothership.getShipTarget() != null && mothership.getOwner() == mothership.getShipTarget().getOwner() && !mothership.getShipTarget().isDrone() && !mothership.getShipTarget().isFighter()) {
			return mothership.getShipTarget();
		} else if (system.getDroneOrders() == DroneOrders.DEPLOY) {
			return mothership;
		}

		float record = 0f;
		ShipAPI leader = null;

		for (ShipAPI s : mxPriorities.keySet()) {

			if (s.getOwner() != this.ship.getOwner() || s.isDrone() || s.isFighter()) {
				continue;
			}

			float score = mxPriorities.get(s) / (500f + MathUtils.getDistance(this.ship, s));

			if (score > record) {
				record = score;
				leader = s;
			}
		}

		return (leader == null) ? mothership : leader;
	}

	private void setTarget(ShipAPI ship) {
		if (target == ship) {
			return;
		}
		this.ship.setShipTarget(target = ship);
	}

	private void goToDestination() {
		Vector2f to = SUN_ICE_IceUtils.toAbsolute(target, targetOffset);
		float distance = MathUtils.getDistance(ship, to);

		if (doingMx) {
			if (distance < 100f) {
				float f = (1f - distance / 100f) * 0.2f;
				ship.getLocation().x = (to.x * f + ship.getLocation().x * (2f - f)) / 2f;
				ship.getLocation().y = (to.y * f + ship.getLocation().y * (2f - f)) / 2f;
				ship.getVelocity().x = (target.getVelocity().x * f + ship.getVelocity().x * (2f - f)) / 2f;
				ship.getVelocity().y = (target.getVelocity().y * f + ship.getVelocity().y * (2f - f)) / 2f;
			}
		}

		if (doingMx && distance < 25f) {
			Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, ship, SPARK_PITCH, SPARK_VOLUME, ship.getLocation(), ship.getVelocity());

			if (targetFacingOffset == Float.MIN_VALUE) {
				targetFacingOffset = ship.getFacing() - target.getFacing();
			} else {
				ship.setFacing(MathUtils.clampAngle(targetFacingOffset + target.getFacing()));
			}

			if (Math.random() < SPARK_CHANCE) {
				Vector2f loc = new Vector2f(ship.getLocation());
				loc.x += cellSize * 0.5f - cellSize * (float) Math.random();
				loc.y += cellSize * 0.5f - cellSize * (float) Math.random();

				Vector2f vel = new Vector2f(ship.getVelocity());
				vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
				vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

				Global.getCombatEngine().addHitParticle(loc, vel, (SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS), SPARK_BRIGHTNESS, SPARK_DURATION * (float) Math.random() + SPARK_DURATION, SPARK_COLOR);
			}
		} else {
			targetFacingOffset = Float.MIN_VALUE;
			float angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), to));

			if (Math.abs(angleDif) < 30f) {
				accelerate();
			} else {
				turnToward(to);
				decelerate();
			}
			strafeToward(to);
		}
	}

	public SUN_ICE_ICEMxDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
		super(drone);
		this.mothership = mothership;
		this.system = system;

		circumstanceEvaluationTimer.setInterval(0.8f, 1.2f);

		float init = COOLDOWN_PER_OP_OF_AMMO_RESTORED * (0.3f + (float) Math.random() * 0.3f); // initial 10.5~21 CD
		dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + init;
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		if (target == null) {
			return;
		}

		if (doingMx) {
			if (target.getHullSpec().getHullId().startsWith("sun_ice_")) {
				repairArmor();
			}

			boolean considerCR = true;
			for (String black : HULLMOD_BLACK_LIST) {
				if (ship.getVariant().hasHullMod(black)) {
					considerCR = false;
					break;
				}
			}

			if (considerCR) {
				maintainCR(amount);
			}
		} else if (returning && !ship.isLanding() && MathUtils.getDistance(ship, mothership) < mothership.getCollisionRadius()) {
			ship.beginLandingAnimation(mothership);
		}

		goToDestination();
	}

	@Override
	public boolean needsRefit() {
		return ship.getFluxTracker().isOverloaded();
	}
}