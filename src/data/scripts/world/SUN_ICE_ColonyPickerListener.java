package data.scripts.world;

import com.fs.starfarer.api.campaign.CampaignEntityPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.awt.Color;

public class SUN_ICE_ColonyPickerListener implements CampaignEntityPickerListener {
	private final InteractionDialogAPI dialog;
	private final SUN_ICE_ExileFleetFinalDialog plugin;

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

	public SUN_ICE_ColonyPickerListener(InteractionDialogAPI dialog, SUN_ICE_ExileFleetFinalDialog plugin) {
		this.dialog = dialog;
		this.plugin = plugin;
	}

	@Override
	public String getMenuItemNameOverrideFor(SectorEntityToken entity) {
		return null;
	}

	@Override
	public void pickedEntity(SectorEntityToken entity) {
		plugin.planetOverride = (PlanetAPI)entity;

		Color h = Misc.getHighlightColor();
		dialog.getTextPanel().addPara(strings.get("selectedplanet"), h, entity.getName());

		dialog.getOptionPanel().addOption(strings.get("agree"), SUN_ICE_ExileFleetFinalDialog.OptionID.AGREE);
		dialog.getOptionPanel().addOption(strings.get("advice"), SUN_ICE_ExileFleetFinalDialog.OptionID.ADVICE);
		dialog.getOptionPanel().addOption(strings.get("select"), SUN_ICE_ExileFleetFinalDialog.OptionID.SELECT);
		dialog.getOptionPanel().addOption(strings.get("disagree"), SUN_ICE_ExileFleetFinalDialog.OptionID.DISAGREE);
		SetStoryOption.set(dialog, 2, SUN_ICE_ExileFleetFinalDialog.OptionID.ADVICE, "sun_ice_select", Sounds.STORY_POINT_SPEND, null);
	}

	@Override
	public void cancelledEntityPicking() {
		dialog.getOptionPanel().addOption(strings.get("agree"), SUN_ICE_ExileFleetFinalDialog.OptionID.AGREE);
		dialog.getOptionPanel().addOption(strings.get("select"), SUN_ICE_ExileFleetFinalDialog.OptionID.SELECT);
		dialog.getOptionPanel().addOption(strings.get("disagree"), SUN_ICE_ExileFleetFinalDialog.OptionID.DISAGREE);
	}

	@Override
	public String getSelectedTextOverrideFor(SectorEntityToken entity) {
		return null;
	}

	@Override
	public void createInfoText(TooltipMakerAPI info, SectorEntityToken entity) {
		float pad = 3f;
		float opad = 10f;

		MarketAPI target = entity.getMarket();

		StringBuilder conditionsBuilder = new StringBuilder();
		for (MarketConditionAPI condition : target.getConditions()) {
			if (condition.isPlanetary()) {
				if (conditionsBuilder.length() != 0) {
					conditionsBuilder.append(", ");
				}
				conditionsBuilder.append(condition.getName());
			}
		}

		if (conditionsBuilder.length() == 0) {
			conditionsBuilder.append(strings.get("none"));
		}

		info.addPara(String.format(strings.get("conditions"), conditionsBuilder.toString()), opad);
	}

	@Override
	public boolean canConfirmSelection(SectorEntityToken entity) {
		return true;
	}

	@Override
	public float getFuelColorAlphaMult() {
		return 0f;
	}

	@Override
	public float getFuelRangeMult() {
		return 0f;
	}
}