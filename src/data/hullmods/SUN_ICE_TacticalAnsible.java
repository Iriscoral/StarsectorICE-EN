package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.HashMap;

public class SUN_ICE_TacticalAnsible extends BaseHullMod {

	private static final String id = "sun_ice_tactical_ansible_mod";
	public static final I18nSection strings = I18nSection.getInstance("Hullmod", "SUN_ICE_");

	private static final float DAMAGE_EFFECT = 20f;
	private static final HashMap<HullSize, Float> DETECT_RADIUS = new HashMap<>();
	static {
		DETECT_RADIUS.put(HullSize.DEFAULT, 0f);
		DETECT_RADIUS.put(HullSize.FIGHTER, 0f);
		DETECT_RADIUS.put(HullSize.FRIGATE, 800f);
		DETECT_RADIUS.put(HullSize.DESTROYER, 1000f);
		DETECT_RADIUS.put(HullSize.CRUISER, 1200f);
		DETECT_RADIUS.put(HullSize.CAPITAL_SHIP, 1400f);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!ship.isAlive()) return;

		float expectedFactor = SUN_ICE_IceUtils.getFPCost(ship) * 3f;
		if (expectedFactor <= 0f) return;

		float dangerFactor = 0f;
		for (ShipAPI target : AIUtils.getNearbyEnemies(ship, DETECT_RADIUS.get(ship.getHullSize()))) {
			if (target.isFighter() || target.isDrone()) continue;
			dangerFactor += SUN_ICE_IceUtils.getFPCost(target);
		}

		for (ShipAPI target : AIUtils.getNearbyAllies(ship, DETECT_RADIUS.get(ship.getHullSize()))) {
			if (target.isFighter() || target.isDrone()) continue;
			dangerFactor -= SUN_ICE_IceUtils.getFPCost(target);
		}

		dangerFactor = Math.max(dangerFactor, 0f);
		float rate = Math.min(dangerFactor / expectedFactor, 1f);
		float damageIncreased = rate * DAMAGE_EFFECT;
		if (ship == engine.getPlayerShip())
			engine.maintainStatusForPlayerShip(this,
					Global.getSettings().getSpriteName("ui", "fleet_member_mothballed"),
					Global.getSettings().getHullModSpec(id).getDisplayName(),
					String.format(strings.get("TacticalAnsibleTEXT"), damageIncreased),
						damageIncreased <= 0f);

		// no break, thanks
		switch (ship.getHullSize()) {
			case CAPITAL_SHIP:
				ship.getMutableStats().getDamageToCapital().modifyPercent(id, damageIncreased);
			case CRUISER:
				ship.getMutableStats().getDamageToCruisers().modifyPercent(id, damageIncreased);
			case DESTROYER:
				ship.getMutableStats().getDamageToDestroyers().modifyPercent(id, damageIncreased);
			case FRIGATE:
				ship.getMutableStats().getDamageToFrigates().modifyPercent(id, damageIncreased);
			case FIGHTER:
			case DEFAULT:
				ship.getMutableStats().getDamageToFighters().modifyPercent(id, damageIncreased);
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "800";
		if (index == 1) return "1000";
		if (index == 2) return "1200";
		if (index == 3) return "1400";
		if (index == 4) return "20%";
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && ship.getHullSpec().getHullId().startsWith("sun_ice_");
	}
}