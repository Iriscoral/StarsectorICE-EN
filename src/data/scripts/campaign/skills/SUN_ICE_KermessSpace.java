package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MarketSkillEffect;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

public class SUN_ICE_KermessSpace {

	private static final float ACCESS_BONUS = 0.35f;

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

	public static class Level1 implements MarketSkillEffect {

		@Override
		public void apply(MarketAPI market, String id, float level) {
			market.getAccessibilityMod().modifyFlat(id, ACCESS_BONUS, Global.getSettings().getSkillSpec("sun_ice_kermess_space").getName());
		}

		@Override
		public void unapply(MarketAPI market, String id) {
			market.getAccessibilityMod().unmodifyFlat(id);
		}
		
		@Override
		public String getEffectDescription(float level) {
			return String.format(strings.get("kermess_space_1"), (int)(ACCESS_BONUS * 100f));
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