package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_GoToFleetIntel;
import data.scripts.tools.SUN_ICE_Data;

import java.awt.*;
import java.util.Map;

public class SUN_ICE_MissionStage3Preview implements InteractionDialogPlugin {

	public static final int DAYS_OF_WAIT_OF_FLEET = 30;

	private final CampaignFleetAPI fleet;
	private InteractionDialogAPI dialog;
	private OptionPanelAPI options;

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_" + key);
	}

	public SUN_ICE_MissionStage3Preview() {
		this.fleet = SUN_ICE_Data.getExileManager().getExiledFleet();
	}

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		options = dialog.getOptionPanel();

		dialog.setOptionOnEscape(null, null);
		dialog.setBackgroundDimAmount(0.4f);
		dialog.getVisualPanel().showImagePortion("illustrations", "fly_away", 400f, 300f, 0f, 0f, 400f, 300f);

		Color h = Misc.getHighlightColor();
		Color factionColor = fleet.getFaction().getBaseUIColor();

		TextPanelAPI textPanel = dialog.getTextPanel();
		textPanel.addPara(getString("income"), factionColor, fleet.getName());
		textPanel.addPara(getString("mission3_preview"), h, "" + DAYS_OF_WAIT_OF_FLEET);
		textPanel.addPara(getString("location"), factionColor, fleet.getName(), fleet.getContainingLocation().getName());
		options.addOption(getString("close"), null);

		Global.getSoundPlayer().playUISound("exiles_intel_call", 1f, 1f);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		options.clearOptions();

		PersonAPI missionGiver = SUN_ICE_MissionManager.getMissionGiver();
		Misc.makeImportant(missionGiver, SUN_ICE_MissionManager.MISSION_MAIN_KEY);

		SUN_ICE_Data.getExileManager().getFakeAI().increaseDaysToStay(DAYS_OF_WAIT_OF_FLEET);
		new SUN_ICE_GoToFleetIntel(dialog, DAYS_OF_WAIT_OF_FLEET);

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