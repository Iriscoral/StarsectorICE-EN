package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.hullmods.HighScatterAmp;

import java.util.ArrayList;
import java.util.List;

public class SUN_ICE_ReverbDampeners extends BaseHullMod {

	private static final List<String> BLACK_LIST = new ArrayList<>();

	static {
		BLACK_LIST.add(HullMods.ADVANCED_TARGETING_CORE);
		BLACK_LIST.add(HullMods.DEDICATED_TARGETING_CORE);
		BLACK_LIST.add(HullMods.INTEGRATED_TARGETING_UNIT);
		BLACK_LIST.add(HullMods.UNSTABLE_INJECTOR);
		BLACK_LIST.add(HullMods.ADVANCEDOPTICS);
		BLACK_LIST.add("high_scatter_amp");
		BLACK_LIST.add("diableavionics_mount");
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getKineticDamageTakenMult().modifyMult(id, 0.5f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		boolean valid = true;

		for (String ban : BLACK_LIST) {
			if (ship.getVariant().getHullMods().contains(ban)) {
				ship.getVariant().removeMod(ban);
				valid = false;
			}
		}

		if (valid) {
			ship.addListener(new HighScatterAmp.HighScatterAmpDamageDealtMod(ship));
		} else {
			Global.getSoundPlayer().playUISound("cr_allied_warning", 1f, 1f);
		}
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "50%";
		return null;
	}
}