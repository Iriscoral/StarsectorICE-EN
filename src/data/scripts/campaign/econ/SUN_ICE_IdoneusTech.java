package data.scripts.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

public class SUN_ICE_IdoneusTech extends BaseMarketConditionPlugin {

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

	@Override
	public void apply(String id) {
		float mult = 1f;
		String string = getName();
		if (!market.getFactionId().contentEquals("sun_ici") && !market.getFactionId().contentEquals("sun_ice")) {
			mult = 0.25f;
			string = string + strings.get("marketLowEffect");
		}

		market.getAccessibilityMod().modifyFlat(id, 0.6f * mult, string);
		market.getStability().modifyFlat(id, 4f * mult, string);
	}

	@Override
	public void unapply(String id) {
		market.getAccessibilityMod().unmodify(id);
		market.getStability().unmodify(id);
	}
}