package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.characters.MarketSkillEffect;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;

public class SUN_ICE_ConsumablesBackcycling {

	private static final String POPULATION_BONUS_ID = "SUN_ICE_ConsumablesBackcycling";
	private static final int POPULATION_BONUS = 30;
	private static final int DEMAND_REDUCTION = 1;

	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "SUN_ICE_" + key);
	}

	public static class Level1 implements MarketSkillEffect, MarketImmigrationModifier {

		@Override
		public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
			incoming.getWeight().modifyFlat(POPULATION_BONUS_ID, POPULATION_BONUS, Global.getSettings().getSkillSpec("sun_ice_consumables_backcycling").getName());
		}

		@Override
		public void apply(MarketAPI market, String id, float level) {
			market.addTransientImmigrationModifier(this);
		}

		@Override
		public void unapply(MarketAPI market, String id) {
			market.removeTransientImmigrationModifier(this);
		}
		
		@Override
		public String getEffectDescription(float level) {
			return String.format(getString("consumables_backcycling_1"), POPULATION_BONUS);
		}
		
		@Override
		public String getEffectPerLevelDescription() {
			return null;
		}

		@Override
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.GOVERNED_OUTPOST;
		}
	}
	
	public static class Level2 implements MarketSkillEffect {

		@Override
		public void apply(MarketAPI market, String id, float level) {
			market.getStats().getDynamic().getMod(Stats.DEMAND_REDUCTION_MOD).modifyFlat(id, DEMAND_REDUCTION);
		}
		
		@Override
		public void unapply(MarketAPI market, String id) {
			market.getStats().getDynamic().getMod(Stats.DEMAND_REDUCTION_MOD).unmodifyFlat(id);
		}
		
		@Override
		public String getEffectDescription(float level) {
			return String.format(getString("consumables_backcycling_2"), DEMAND_REDUCTION);
		}
		
		@Override
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		@Override
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.GOVERNED_OUTPOST;
		}
	}
}