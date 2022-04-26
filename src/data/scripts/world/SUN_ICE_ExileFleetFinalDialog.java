package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_Data;

import java.awt.*;
import java.util.Map;

public class SUN_ICE_ExileFleetFinalDialog implements InteractionDialogPlugin {
	private enum OptionID {
		AGREE, DISAGREE, LEAVE
	}

	public static final String EXILE_FLEET_FINAL_DIALOG_AGREE_KEY = "playerAgreedSettle";
	public static final String EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY = "playerSelectedSettle";
	public static final String EXILE_FLEET_FINAL_DIALOG_ASKED_KEY = "askedSettle";
	private final CampaignFleetAPI fleet;
	private final PlanetAPI planet;
	private InteractionDialogAPI dialog;
	private TextPanelAPI textPanel;
	private OptionPanelAPI options;

	private static String getString(String key) {
		return Global.getSettings().getString("Misc", "SUN_ICE_" + key);
	}

	public SUN_ICE_ExileFleetFinalDialog(CampaignFleetAPI fleet, PlanetAPI planet) {
		this.fleet = fleet;
		this.planet = planet;
	}

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		textPanel = dialog.getTextPanel();
		options = dialog.getOptionPanel();

		dialog.setOptionOnEscape(null, OptionID.DISAGREE);
		dialog.setBackgroundDimAmount(0.4f);
		dialog.getVisualPanel().showImagePortion("illustrations", "vacuum_colony", 400f, 300f, 0f, 0f, 400f, 300f);

		Color h = Misc.getHighlightColor();
		Color factionColor = fleet.getFaction().getBaseUIColor();

		textPanel.addPara(getString("helpInfo1"), factionColor, fleet.getFaction().getDisplayNameLongWithArticle());
		textPanel.addPara(getString("helpInfo2"));
		textPanel.addPara(getString("helpInfo3"), h, planet.getStarSystem().getName() + " " + planet.getName());
		options.addOption(getString("agree"), OptionID.AGREE);
		options.addOption(getString("disagree"), OptionID.DISAGREE);

		Global.getSoundPlayer().playUISound("exiles_intel_call", 1f, 1f);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		options.clearOptions();
		OptionID selectedOption = (OptionID) optionData;
		switch (selectedOption) {
			case AGREE:
				textPanel.addPara(getString("agree"));
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_AGREE_KEY, true);
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY, true);
				dialog.dismiss();
				break;
			case DISAGREE:
				textPanel.addPara(getString("disagree"));
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY, true);
				dialog.dismiss();
				break;
			case LEAVE:
				dialog.dismiss();
				break;
		}

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