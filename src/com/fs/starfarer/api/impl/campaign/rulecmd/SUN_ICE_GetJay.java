package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.Map;

public class SUN_ICE_GetJay implements InteractionDialogPlugin {

	private enum OptionID {
		AGREE, CANCEL, LEAVE
	}

	private final PersonAPI person;
	private InteractionDialogAPI dialog;
	private OptionPanelAPI options;

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_");

	public SUN_ICE_GetJay() {
		this.person = SUN_ICE_MissionManager.getMissionGiver();
		person.setFaction(Global.getSector().getPlayerFaction().getId());
	}

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		options = dialog.getOptionPanel();

		dialog.setBackgroundDimAmount(0.4f);
		dialog.getVisualPanel().showPersonInfo(person);

		TextPanelAPI textPanel = dialog.getTextPanel();
		textPanel.addPara(String.format(strings.get("income"), person.getNameString()));
		options.addOption(strings.get("jay_act_1"), OptionID.AGREE);
		options.addOption(strings.get("jay_act_2"), OptionID.CANCEL);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		options.clearOptions();
		TextPanelAPI textPanel = dialog.getTextPanel();

		OptionID selectedOption = (OptionID) optionData;
		switch (selectedOption) {
			case AGREE:
				textPanel.addPara(strings.get("jay_response_1"));
				options.addOption(strings.get("cutlink"), OptionID.LEAVE);

				Global.getSector().getCharacterData().addAdmin(person);
				AddRemoveCommodity.addAdminGainText(person, dialog.getTextPanel());
				break;
			case CANCEL:
				textPanel.addPara(strings.get("jay_response_2"));
				options.addOption(strings.get("cutlink"), OptionID.LEAVE);
				break;
			case LEAVE:
				dialog.dismiss();
				break;
		}
	}

	@Override
	public void optionMousedOver(String optionText, Object optionData) {}

	@Override
	public void advance(float amount) {}

	@Override
	public void backFromEngagement(EngagementResultAPI battleResult) {}

	@Override
	public Object getContext() {
		return null;
	}

	@Override
	public Map<String, MemoryAPI> getMemoryMap() {
		return null;
	}
}