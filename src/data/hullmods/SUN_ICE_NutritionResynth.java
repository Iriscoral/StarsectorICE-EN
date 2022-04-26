package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;

public class SUN_ICE_NutritionResynth extends BaseHullMod {

	@Override
	public void advanceInCampaign(FleetMemberAPI member, float amount) {
		CampaignFleetAPI fleet = member.getFleetData().getFleet();
		if (fleet == null || fleet.getLogistics() == null) {
			return;
		}

		float supplyCost = fleet.getLogistics().getPersonnelSuppliesPerDay();
		int i = DModManager.getNumDMods(member.getVariant());
		float effect = Math.max(0.35f - i * 0.1f, 0f);
		fleet.getCargo().addSupplies(supplyCost * effect * (amount / Global.getSector().getClock().getSecondsPerDay()));
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
		if (index == 0) return "35%";
		if (index == 1) return "10%";
		if (index == 2) return "0%";

		String s = "null";
		if (ship != null) {
			int i = DModManager.getNumDMods(ship.getVariant());
			s = Math.max(35 - i * 10, 0) + "%";
		}

		if (index == 3) return s;
		return null;
	}
}