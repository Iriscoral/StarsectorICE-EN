package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.impl.hullmods.PhaseAnchor;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.ai.ship.SUN_ICE_PhaseCruiseTempAI;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class SUN_ICE_GravimetricSensors extends BaseHullMod {

	public static final String id = "sun_ice_gravitonic_sensors_mod";
	private static final Map<HullSize, Integer> PHASE_FACTORS = new HashMap<>();
	static {
		PHASE_FACTORS.put(HullSize.DEFAULT, 0);
		PHASE_FACTORS.put(HullSize.FIGHTER, 0);
		PHASE_FACTORS.put(HullSize.FRIGATE, 15);
		PHASE_FACTORS.put(HullSize.DESTROYER, 30);
		PHASE_FACTORS.put(HullSize.CRUISER, 45);
		PHASE_FACTORS.put(HullSize.CAPITAL_SHIP, 75);
	}

	private static final float INSTA_REPAIR = 0.5f;
	private static final float MIN_REFRESH = 0.1f;
	private static final float MAX_REFRESH = 0.9f;
	private static final float CR_LOSS_MULT_FOR_EMERGENCY_DIVE = 1f;//1.5f;

	private void doSkimAiHack(CombatEngineAPI engine, ShipAPI ship, float amount) {
		String id = ship.getHullSpec().getBaseHullId();
		ShipSystemAPI cloak = id.contentEquals("sun_ice_athame") ? ship.getSystem() : ship.getPhaseCloak();

		if (cloak == null || cloak.getId().contentEquals("sun_ice_phaseshift")) {
			return;
		}

		SUN_ICE_IntervalTracker tracker;
		if (!ship.getCustomData().containsKey(SUN_ICE_GravimetricSensors.id)) {
			tracker = new SUN_ICE_IntervalTracker(MIN_REFRESH, MAX_REFRESH);
			ship.setCustomData(SUN_ICE_GravimetricSensors.id, tracker);
		} else {
			tracker = (SUN_ICE_IntervalTracker)ship.getCustomData().get(SUN_ICE_GravimetricSensors.id);
		}

		if (!tracker.intervalElapsed(engine)) {
			return;
		}

		float speed = ship.getVelocity().length();
		AssignmentInfo task = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);
		float riskRange = SUN_ICE_PhaseCruiseTempAI.getRiskRange(ship);

		if (!cloak.isActive() && ship.getShipAI() != null && ship.getAngularVelocity() < 0.2f && !ship.getTravelDrive().isActive() && ship.getFluxTracker().getFluxLevel() == 0 && speed >= ship.getMutableStats().getMaxSpeed().getModifiedValue() - 2f && AIUtils.getNearbyEnemies(ship, riskRange + 800f).isEmpty() && (task == null || task.getTarget() == null || MathUtils.getDistance(ship, task.getTarget().getLocation()) > 800f)) {

			if (ship.getPhaseCloak() == cloak) {
				ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
			} else {
				ship.useSystem();
			}

			ship.setShipAI(new SUN_ICE_PhaseCruiseTempAI(ship));
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.isPaused() && ship.isAlive()) {
			doSkimAiHack(engine, ship, amount);
		}
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.INSTA_REPAIR_FRACTION).modifyFlat(id, INSTA_REPAIR);

		stats.getBallisticWeaponRangeBonus().modifyFlat(id, 300);
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, -50f); // not mult 0.5
		stats.getEnergyWeaponRangeBonus().modifyFlat(id, 300);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, -50f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new SUN_ICE_PhaseAnchorScript(ship));
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "600";
		if (index == 1) return "50%";
		if (index == 2) return "300";
		//if (index == 3) return (int) (CR_LOSS_MULT_FOR_EMERGENCY_DIVE * 100) + "%";
		if (index == 3) return (int) (INSTA_REPAIR * 100) + "%";
		return null;
	}

	public static final I18nSection strings = I18nSection.getInstance("HullMod", "SUN_ICE_");

	public static class SUN_ICE_PhaseAnchorScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {

		public static final Color DEFAULT_DIVE_COLOR = new Color(160, 240, 220, 255);

		private final ShipAPI ship;
		private boolean emergencyDive = false;
		private float diveProgress = 0f;
		private final FaderUtil diveFader = new FaderUtil(1f, 1f);

		public SUN_ICE_PhaseAnchorScript(ShipAPI ship) {
			this.ship = ship;
		}

		@Override
		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {

			if (Global.getCombatEngine().isMission()) return false;

			if (!emergencyDive) {

				float depCost = 0f;
				if (ship.getFleetMember() != null) {
					depCost = ship.getFleetMember().getDeployCost();
				}

				float crLoss = CR_LOSS_MULT_FOR_EMERGENCY_DIVE * depCost;
				if (ship.getVariant().hasHullMod(HullMods.PHASE_ANCHOR)) {
					crLoss = PhaseAnchor.CR_LOSS_MULT_FOR_EMERGENCY_DIVE * depCost;
				}

				boolean phase = ship.getFleetMember() != null && SUN_ICE_IceUtils.isConsideredPhaseShip(ship.getFleetMember(), false);
				boolean canDive = ship.getCurrentCR() >= crLoss && phase;
				float hull = ship.getHitpoints();
				if (damageAmount >= hull && canDive) {
					ship.setHitpoints(1f);

					if (ship.getFleetMember() != null) { // fleet member is fake during simulation, so this is fine
						ship.getFleetMember().getRepairTracker().applyCREvent(-crLoss, strings.get("GravimetricSensorsTEXT"));
					}

					emergencyDive = true;
					Global.getCombatEngine().getCustomData().put("phaseAnchor_canDive", true); // avoid conflict

					if (!ship.isPhased()) {
						Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
				}
			}

			return emergencyDive;
		}

		@Override
		public void advance(float amount) {
			String id = "phase_anchor_modifier";
			if (emergencyDive) {
				Color c = DEFAULT_DIVE_COLOR;
				if (ship.getPhaseCloak() != null) {
					c = ship.getPhaseCloak().getSpecAPI().getEffectColor2();
					c = Misc.setAlpha(c, 255);
					c = Misc.interpolateColor(c, Color.white, 0.5f);
				}

				if (diveProgress == 0f) {
					if (ship.getFluxTracker().showFloaty()) {
						float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
						Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(), strings.get("GravimetricSensorsTEXT2"),
								NeuralLinkScript.getFloatySize(ship), c, ship, 16f * timeMult, 3.2f / timeMult, 1f / timeMult, 0f, 0f,
								1f);
					}
				}

				diveFader.advance(amount);
				ship.setRetreating(true, false);

				ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
				ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
				if (ship.getPhaseCloak() != null) {
					diveProgress += amount * ship.getPhaseCloak().getChargeUpDur();
					float curr = ship.getExtraAlphaMult();
					ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.IN, Math.min(1f, Math.max(curr, diveProgress)));
				} else {
					diveProgress += amount;
					ship.setPhased(true);
					ship.setAlphaMult(Math.max(1f - diveProgress, 0f));
				}

				if (diveProgress >= 1f) {
					if (diveFader.isIdle()) {
						Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}

					diveFader.fadeOut();
					diveFader.advance(amount);
					float b = diveFader.getBrightness();
					ship.setExtraAlphaMult2(b);

					float r = ship.getCollisionRadius() * 5f;
					ship.setJitter(this, c, b, 20, r * (1f - b));

					if (diveFader.isFadedOut()) {
						ship.getLocation().set(0, -1000000f);
					}
				}
			}
		}
	}
}