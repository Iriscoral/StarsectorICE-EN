package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;

import java.util.ArrayList;
import java.util.List;

public class SUN_ICE_ColonyFleet extends BaseMarketConditionPlugin {

	static List<String> toRemove = new ArrayList<>();

	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "SUN_ICE_" + key);
	}

	@Override
	public void advance(float amount) {
		if (market != null && market.getIndustries() != null) {
			for (Industry ind : market.getIndustries()) {
				if (ind.getId().contains("station_base") || ind.getId().contains("orbitalstation") || ind.getId().contains("battlestation") || ind.getId().contains("starfortress")) {
					toRemove.add(ind.getId());
				}
			}
			for (String id : toRemove) {
				market.removeIndustry(id, null, false);
			}
			toRemove.clear();
		}
	}

	@Override
	public void apply(String id) {
		float stabilityModification = 1;

		if (market.getPrimaryEntity() instanceof CampaignFleetAPI) {
			stabilityModification += ((CampaignFleetAPI)market.getPrimaryEntity()).getFleetPoints() * 0.01f;
		}

		market.getAccessibilityMod().modifyFlat(id, 1f, getName());
		market.getStability().modifyFlat(id, (int) stabilityModification, getString("fleetSize"));
		market.setImmigrationClosed(true);
	}

	@Override
	public void unapply(String id) {
		market.getAccessibilityMod().unmodifyFlat(id);
		market.getStability().unmodifyFlat(id);
	}
}