package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.ai.ship.SUN_ICE_BraindeadTempAI;
import data.scripts.ICEModPlugin;
import data.scripts.tools.SUN_ICE_JauntSession;
import data.scripts.tools.SUN_ICE_RecallTracker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SUN_ICE_EveryFramePlugin extends BaseEveryFrameCombatPlugin {
	private static final String MODIFIER_KEY = "sun_ice_every_frame_modifiers";
	private CombatEngineAPI engine;
	private static final Set<ShipAPI> shipsToSolveShieldRefund = new HashSet<>();
	private static final Set<ShipAPI> shipsToClearBonusesFrom = new HashSet<>();

	static public void tagForShieldEffectRefund(ShipAPI ship) {
		shipsToSolveShieldRefund.add(ship);
	}

	private void clearBonuses() {
		for (ShipAPI ship : shipsToClearBonusesFrom) {
			ship.getMutableStats().getShieldUpkeepMult().unmodify(MODIFIER_KEY);
			ship.getMutableStats().getHardFluxDissipationFraction().unmodify(MODIFIER_KEY);
		}

		shipsToClearBonusesFrom.clear();
	}

	private void refundShieldEffect(float amount) {
		for (ShipAPI ship : shipsToSolveShieldRefund) {
			float arcReduction = 1f - Math.max(0f, ship.getShield().getActiveArc()) / ship.getShield().getArc();
			ship.getMutableStats().getShieldUpkeepMult().modifyMult(MODIFIER_KEY, 1f - Math.max(0f, arcReduction * 2f - 1f));
			ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(MODIFIER_KEY, Math.max(0f, arcReduction * 2f - 1f));
		}

		shipsToClearBonusesFrom.addAll(shipsToSolveShieldRefund);
		shipsToSolveShieldRefund.clear();
	}

	private boolean fissionDrillWeaponActivated = false;

	private void checkFissionDrillUsageByPlayer() {
		ShipAPI ship = engine.getPlayerShip();
		ShipSystemAPI sys = ship.getSystem();

		if (!ship.isAlive() || !ship.getHullSpec().getBaseHullId().contentEquals("sun_ice_athame") || ship.getShipAI() != null || ship.getAllWeapons() == null || ship.getAllWeapons().isEmpty()) {
			return;
		}

		WeaponAPI weapon = ship.getAllWeapons().get(2);
		if (weapon == null) {
			return;
		}

		if (!fissionDrillWeaponActivated && weapon.isFiring()) {
			if (!sys.isOn()) ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			fissionDrillWeaponActivated = true;
		} else if (fissionDrillWeaponActivated && !weapon.isFiring()) {
			if (sys.isOn()) ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			fissionDrillWeaponActivated = false;
		}
	}

	private static final List<SUN_ICE_RecallTracker> recalling = new LinkedList<>();
	private static final List<SUN_ICE_RecallTracker> recallingToRemove = new LinkedList<>();

	public static void beginRecall(SUN_ICE_RecallTracker tracker) {
		tracker.start();
		recalling.add(tracker);
	}

	private void advanceActiveRecalls(float amount) {

		for (SUN_ICE_RecallTracker t : recalling) {
			t.advance(amount);

			if (t.isComplete()) recallingToRemove.add(t);
		}

		for (SUN_ICE_RecallTracker t : recallingToRemove) {
			t.end();
			recalling.remove(t);
		}

		recallingToRemove.clear();
	}

	private boolean playerCloakPreviouslyCoolingDown = false;

	private void playPhaseCloakCooldownOverSoundForPlayer() {
		ShipAPI ship = Global.getCombatEngine().getPlayerShip();

		if (ship != null && ship.getShipAI() == null && ship.getPhaseCloak() != null && ship.getHullSpec().getHullId().startsWith("sun_ice_")) {

			if (playerCloakPreviouslyCoolingDown != ship.getPhaseCloak().isCoolingDown() && !ship.getPhaseCloak().isCoolingDown()) {
				Global.getSoundPlayer().playSound("sun_ice_right_click_cooldown", 1f, 1f, ship.getLocation(), ship.getVelocity());
			}

			playerCloakPreviouslyCoolingDown = ship.getPhaseCloak().isCoolingDown();
		} else {
			playerCloakPreviouslyCoolingDown = false;
		}
	}

	private void makeAllShipsFaceNE() {
		for (ShipAPI ship : engine.getShips()) {
			ship.setFacing(45f);
			ship.giveCommand(ShipCommand.ACCELERATE, null, 0);

			if (ship.getShipAI() != null && !(ship.getAI() instanceof SUN_ICE_BraindeadTempAI)) {
				ship.setShipAI(new SUN_ICE_BraindeadTempAI(ship));
			}
		}
	}

	@Override
	public void advance(float amount, List events) {
		clearBonuses();

		if (engine == null || engine.isPaused()) {
			return;
		}

		checkFissionDrillUsageByPlayer();
		playPhaseCloakCooldownOverSoundForPlayer();

		refundShieldEffect(amount);
		advanceActiveRecalls(amount);
		SUN_ICE_JauntSession.advanceAll(amount);

		if (ICEModPlugin.SMILE_FOR_CAMERA) {
			makeAllShipsFaceNE();
		}
	}

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;

		shipsToSolveShieldRefund.clear();
		shipsToClearBonusesFrom.clear();
		recalling.clear();

		SUN_ICE_JauntSession.clearStaticData();
		SUN_ICE_RecallTracker.clearStaticData();

		Global.getCombatEngine().addLayeredRenderingPlugin(new SUN_ICE_RenderPlugin());
	}
}