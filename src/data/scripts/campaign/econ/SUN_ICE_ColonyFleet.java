package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.ArrayList;
import java.util.List;

public class SUN_ICE_ColonyFleet extends BaseMarketConditionPlugin {

	transient static List<String> toRemove = new ArrayList<>();

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

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

		float effect = 0f;
		if (market.getPrimaryEntity() instanceof CampaignFleetAPI) {
			effect = ((CampaignFleetAPI)market.getPrimaryEntity()).getEffectiveStrength();
		}

		float stabilityModification = 1f + effect * 0.01f;
		float defenseModification = 500f + effect * 3f;

		market.getAccessibilityMod().modifyFlat(id, 1f, getName());
		market.getStability().modifyFlat(id, (int)stabilityModification, strings.get("fleetSize"));
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, defenseModification, strings.get("fleetSize"));

		market.setImmigrationClosed(true);
	}

	@Override
	public void unapply(String id) {
		market.getAccessibilityMod().unmodifyFlat(id);
		market.getStability().unmodifyFlat(id);
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(id);
	}
}