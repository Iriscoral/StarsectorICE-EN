package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.Map;

public class SUN_ICE_GetJay implements InteractionDialogPlugin {

	private enum OptionID {
		AGREE, CANCEL, LEAVE
	}

	private final PersonAPI person;
	private InteractionDialogAPI dialog;
	private OptionPanelAPI options;

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_" + key);
	}

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
		textPanel.addPara(String.format(getString("income"), person.getNameString()));
		options.addOption(getString("jay_act_1"), OptionID.AGREE);
		options.addOption(getString("jay_act_2"), OptionID.CANCEL);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		options.clearOptions();
		TextPanelAPI textPanel = dialog.getTextPanel();

		OptionID selectedOption = (OptionID) optionData;
		switch (selectedOption) {
			case AGREE:
				textPanel.addPara(getString("jay_response_1"));
				options.addOption(getString("cutlink"), OptionID.LEAVE);

				Global.getSector().getCharacterData().addAdmin(person);
				AddRemoveCommodity.addAdminGainText(person, dialog.getTextPanel());
				break;
			case CANCEL:
				textPanel.addPara(getString("jay_response_2"));
				options.addOption(getString("cutlink"), OptionID.LEAVE);
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