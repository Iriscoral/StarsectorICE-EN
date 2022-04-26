package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.shipsystems.ai.SUN_ICE_EntropicInversionMatrixAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_MeleeTempAI extends SUN_ICE_BaseShipAI {

	private WeaponAPI tractorBeam;
	private int bladeGroup;
	private int tractorBeamGroup;
	private SUN_ICE_EntropicInversionMatrixAI systemAI;
	private final MeleeAIFlags AIFlags = new MeleeAIFlags();
	private final String bladeID = "sun_ice_fissionblade";

	private void checkIfShouldDefend() {
		if (ship.getShield() != null && ship.getShield().getType() != ShieldAPI.ShieldType.NONE && ship.getShield().isOff()) {
			ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
		}

		if (ship.getSystem().isActive() || ship.getShipTarget() == null) {
			return;
		}

		boolean targetDeathEminent = ship.getShipTarget().getHitpoints() <= SUN_ICE_IceUtils.estimateIncomingDamage(ship.getShipTarget(), 1f);
		boolean targetMayDieSoon = targetDeathEminent || ship.getShipTarget().getHitpoints() <= 3000f;
		boolean canPhase = ship.getPhaseCloak() != null && ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux() > ship.getPhaseCloak().getFluxPerUse() * 1.1f;

		float danger = SUN_ICE_IceUtils.estimateIncomingDamage(ship, 1f) / (ship.getHitpoints() + ship.getMaxHitpoints());
		if (danger > 0.12f || targetDeathEminent) {
			boolean systemUsed = useSystem();
			if (!systemUsed && canPhase) {
				toggleDefenseSystem();
			} else if (!systemUsed) {
				vent();
			}
		} else if (danger > 0.03f && !targetMayDieSoon) {
			useSystem();
		}
	}

	@Override
	public void evaluateCircumstances() {
		if (ship.getFluxTracker().isOverloadedOrVenting() || !tractorBeam.isFiring() || ship.getFluxTracker().getFluxLevel() > 0.9f || (ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive()) || ship.getShipTarget() == null || ship.getShipTarget().getOwner() == ship.getOwner() || tractorBeam.getRange() < MathUtils.getDistance(ship.getShipTarget(), tractorBeam.getLocation()) || !ship.getShipTarget().isAlive() || AIFlags.friendlyFire || !tractorBeam.isFiring()) {
			ship.resetDefaultAI();
		}

		checkIfShouldDefend();
	}

	public SUN_ICE_MeleeTempAI(ShipAPI ship, WeaponAPI tractorBeam) {
		super(ship);
		if (ship == null) {
			return;
		}
		this.tractorBeam = tractorBeam;
		this.systemAI = new SUN_ICE_EntropicInversionMatrixAI();
		for (int i = 0; i < ship.getWeaponGroupsCopy().size(); i++) {
			WeaponGroupAPI wg = ship.getWeaponGroupsCopy().get(i);
			if (wg == ship.getWeaponGroupFor(tractorBeam)) {
				tractorBeamGroup = i;
				wg.toggleOn();
			}
			for (WeaponAPI w : wg.getWeaponsCopy()) {
				if (w.getId().startsWith(bladeID)) {
					bladeGroup = i;
					wg.toggleOff();
					break;
				}
			}
		}

		systemAI.init(ship, ship.getSystem(), null, Global.getCombatEngine());
		circumstanceEvaluationTimer.setInterval(0.1f);
		//SunUtils.print("Gobble time!");
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (circumstanceEvaluationTimer.intervalElapsed(engine)) {
			evaluateCircumstances();
		}

		if (ship == null) {
			return;
		}

		systemAI.advance(amount, null, null, ship.getShipTarget());
		if (ship.getShipTarget() == null) {
			return;
		}

		Vector2f lead = AIUtils.getBestInterceptPoint(ship.getLocation(), ship.getMaxSpeedWithoutBoost(), ship.getShipTarget().getLocation(), ship.getShipTarget().getVelocity());
		if (lead == null) {
			lead = ship.getShipTarget().getLocation();
		}

		float angleToFace = VectorUtils.getAngle(ship.getLocation(), lead);
		if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(), angleToFace)) < 15f) {
			accelerate();
		}
		if (MathUtils.getDistance(ship, ship.getShipTarget()) <= 500f) {
			strafeToward(lead);
		}
		turnToward(angleToFace);

		float dist = MathUtils.getDistance(ship, ship.getShipTarget());
		if (!AIFlags.friendlyFire && (dist < 100f || bladeGroup == tractorBeamGroup)) {
			selectWeaponGroup(bladeGroup);
			fireSelectedGroup(ship.getShipTarget().getLocation());
		}
	}

	public static class MeleeAIFlags extends ShipwideAIFlags {
		public boolean friendlyFire = false;
	}
}