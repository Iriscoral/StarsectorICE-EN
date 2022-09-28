package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.awt.Color;
import java.util.Map;

public class SUN_ICE_MissionRemnantWarPreview implements InteractionDialogPlugin {

	private enum OptionID {
		NEXT, CANCEL
	}

	private final CampaignFleetAPI fleet;
	private InteractionDialogAPI dialog;
	private OptionPanelAPI options;

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_");

	public SUN_ICE_MissionRemnantWarPreview(CampaignFleetAPI fleet) {
		this.fleet = fleet;
	}

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		options = dialog.getOptionPanel();

		dialog.setOptionOnEscape(null, OptionID.CANCEL);
		dialog.setBackgroundDimAmount(0.4f);
		dialog.getVisualPanel().showImagePortion("illustrations", "fly_away", 400f, 300f, 0f, 0f, 400f, 300f);

		Color h = Misc.getHighlightColor();
		Color factionColor = fleet.getFaction().getBaseUIColor();

		TextPanelAPI textPanel = dialog.getTextPanel();
		textPanel.addPara(strings.get("war_preview"), factionColor, fleet.getName());

		options.addOption(strings.get("war_preview_next"), OptionID.NEXT);
		options.addOption(strings.get("war_preview_cancel"), OptionID.CANCEL);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		options.clearOptions();
		dialog.dismiss();
	}

	@Override
	public void optionMousedOver(String optionText, Object optionData) {
	}

	@Override
	public void advance(float amount) {
	}

	@Override
	public void backFromEngagement(EngagementResultAPI battleResult) {
	}

	@Override
	public Object getContext() {
		return null;
	}

	@Override
	public Map<String, MemoryAPI> getMemoryMap() {
		return null;
	}
}