package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SUN_ICE_ExileFleetFinalDialog implements InteractionDialogPlugin {

	boolean displayOption = false;
	public enum OptionID {
		CONTINUE_1, CONTINUE_2, CONTINUE_3,
		AGREE, SELECT, DISAGREE, LEAVE, ADVICE
	}

	public static final String EXILE_FLEET_FINAL_DIALOG_AGREE_KEY = "playerAgreedSettle";
	public static final String EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY = "playerSelectedSettle";
	public static final String EXILE_FLEET_FINAL_DIALOG_ASKED_KEY = "askedSettle";
	private final CampaignFleetAPI fleet;
	private final SUN_ICE_ExileFleetFakeAI fakeAI;
	private final PlanetAPI planet;
	public PlanetAPI planetOverride = null;
	private InteractionDialogAPI dialog;
	private TextPanelAPI textPanel;
	private OptionPanelAPI options;

	public static final I18nSection strings = I18nSection.getInstance("Misc", "SUN_ICE_");

	public SUN_ICE_ExileFleetFinalDialog(CampaignFleetAPI fleet, SUN_ICE_ExileFleetFakeAI fakeAI) {
		this.fleet = fleet;
		this.fakeAI = fakeAI;
		this.planet = fakeAI.tmpSettle;
	}

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		textPanel = dialog.getTextPanel();
		options = dialog.getOptionPanel();

		dialog.setBackgroundDimAmount(0.4f);
		dialog.getVisualPanel().showImagePortion("illustrations", "vacuum_colony", 400f, 300f, 0f, 0f, 400f, 300f);

		Color h = Misc.getHighlightColor();
		Color factionColor = fleet.getFaction().getBaseUIColor();

		textPanel.addPara(strings.get("helpInfo1"), factionColor, fleet.getFaction().getDisplayNameLongWithArticle());
		textPanel.addPara(strings.get("helpInfo2"));
		options.addOption(strings.get("continue"), OptionID.CONTINUE_1);

		Global.getSoundPlayer().playUISound("exiles_intel_call", 1f, 1f);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {

		if (displayOption) {
			textPanel.addPara(optionText, Misc.getButtonTextColor());
		}

		options.clearOptions();
		OptionID selectedOption = (OptionID) optionData;
		switch (selectedOption) {
			case CONTINUE_1:
				textPanel.addPara(strings.get("helpInfo3"));
				options.addOption(strings.get("continue"), OptionID.CONTINUE_2);
				break;
			case CONTINUE_2:
				textPanel.addPara(strings.get("helpInfo4"));
				options.addOption(strings.get("continue"), OptionID.CONTINUE_3);
				break;
			case CONTINUE_3:
				displayOption = true;
				Color h = Misc.getHighlightColor();
				textPanel.addPara(strings.get("helpInfo5"));
				textPanel.addPara(strings.get("helpInfo6"), h, planet.getStarSystem().getName() + " " + planet.getName());

				options.addOption(strings.get("agree"), OptionID.AGREE);
				options.addOption(strings.get("select"), OptionID.SELECT);
				options.addOption(strings.get("disagree"), OptionID.DISAGREE);
				options.setShortcut(OptionID.DISAGREE, Keyboard.KEY_ESCAPE, false, false, false, false);
				break;
			case AGREE:
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_AGREE_KEY, true);
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY, true);
				dialog.dismiss();
				break;
			case SELECT:
				List<SectorEntityToken> targetData = new ArrayList<>();
				for (StarSystemAPI starSystem : Global.getSector().getStarSystems()) {
					for (PlanetAPI planet : starSystem.getPlanets()) {
						if (planet.isStar()) continue;
						if (planet.getMarket() == null) continue;
						if (!planet.getMarket().isPlanetConditionMarketOnly()) continue;
						if (planet.getMarket().getSurveyLevel() != MarketAPI.SurveyLevel.FULL) continue;

						targetData.add(planet);
					}
				}

				dialog.showCampaignEntityPicker(strings.get("selectplanet"), strings.get("selected"), strings.get("confirm"),
						Global.getSector().getPlayerFaction(), targetData,
						new SUN_ICE_ColonyPickerListener(dialog, this));
				break;
			case ADVICE:
				fakeAI.tmpSettle = planetOverride;
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_AGREE_KEY, true);
				SUN_ICE_Data.put(EXILE_FLEET_FINAL_DIALOG_SELECTED_KEY, true);
				dialog.dismiss();
				break;
			case DISAGREE:
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