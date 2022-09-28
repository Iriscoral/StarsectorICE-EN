package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionRecruitmentShipsIntel;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage3 extends SUN_ICE_MissionManager {
	private static final String AFTER_INIT_KEY = "$SUN_ICE_MissionStage3_Init";
	public static final String REQUIRED_COMMODITY = Commodities.SUPPLIES;
	public static final int REQUIRED_AMOUNT = 90;
	public static final int REQUIRED_CAP = 20;
	public static final int TIME_OUT = 90;

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_mission3_");

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		MemoryAPI memory = memoryMap.get(MemKeys.FACTION);
		MemoryAPI memoryLocal = memoryMap.get(MemKeys.LOCAL);

		MissionStage stage = getStage();
		if (!memoryLocal.contains(STATE_WHEN_DIALOG_STARTED)) {
			memoryLocal.set(STATE_WHEN_DIALOG_STARTED, stage, 0f);
		} else {
			stage = (MissionStage) memoryLocal.get(STATE_WHEN_DIALOG_STARTED);
		}

		if (stage != MissionStage.STAGE_PREVIEW_SHIPS) return false;

		String string = params.get(0).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		if (string == null) return false;
		if (string.contentEquals("cutCommLink")) return false;
		if (string.contentEquals("init") && memory.getBoolean(AFTER_INIT_KEY)) string = "init_re";

		TextPanelAPI textPanel = dialog.getTextPanel();
		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		switch (string) {
			case "after_act_1_A":
			case "after_act_1_B":
				dialog.getVisualPanel().showSecondPerson(getPriest());

				textPanel.addPara(strings.get("response_1"));
				optionPanel.addOption(strings.get("act_2_progress_1"), "after_act_2_A");
				optionPanel.addOption(strings.get("act_2_progress_2"), "after_act_2_B");
				break;
			case "after_act_2_A":
			case "after_act_2_B":
				textPanel.addPara(strings.get("response_2"), Misc.getHighlightColor(), String.valueOf(REQUIRED_AMOUNT));
				optionPanel.addOption(strings.get("act_3_progress_1"), "after_act_3_A");
				optionPanel.addOption(strings.get("act_3_progress_2"), "after_act_3_B");
				break;
			case "after_act_3_A":
			case "after_act_3_B":
				textPanel.addPara(strings.get("response_3"), Misc.getHighlightColor(), String.valueOf(TIME_OUT));
				optionPanel.addOption(strings.get("accept"), "accept");
				optionPanel.addOption(strings.get("information"), "information");
				optionPanel.addOption(strings.get("delay"), "delay");
				optionPanel.addOption(strings.get("refuse"), "refuse");
				break;
			case "accept":
				dialog.getVisualPanel().hideSecondPerson();

				textPanel.addPara(strings.get("response_accept"));
				optionPanel.addOption(strings.get("get_hint"), "hint");
				setStage(MissionStage.STAGE_ONGOING_SHIPS);

				startMission(TIME_OUT);
				new SUN_ICE_MissionRecruitmentShipsIntel(dialog, REQUIRED_AMOUNT, REQUIRED_CAP, TIME_OUT);

				endGoToFleetIntel(true);
				break;
			case "information":
				textPanel.addPara(strings.get("response_information"));
				optionPanel.addOption(strings.get("accept"), "accept");
				optionPanel.addOption(strings.get("delay"), "delay");
				optionPanel.addOption(strings.get("refuse"), "refuse");
				break;
			case "delay":
				textPanel.addPara(strings.get("response_delay"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "refuse":
				dialog.getVisualPanel().hideSecondPerson();

				textPanel.addPara(strings.get("response_refuse"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				SUN_ICE_MissionManager.doomStages();
				break;
			case "hint":
				textPanel.addPara(strings.get("response_hint"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "init_re":
				dialog.getVisualPanel().showSecondPerson(getPriest());

				textPanel.addPara(strings.get("init_re"));
				optionPanel.addOption(strings.get("accept"), "accept");
				optionPanel.addOption(strings.get("delay"), "delay");
				optionPanel.addOption(strings.get("refuse"), "refuse");
			case "init":
			default:
				textPanel.addPara(strings.get("init"));
				optionPanel.addOption(strings.get("act_1_progress_1"), "after_act_1_A");
				optionPanel.addOption(strings.get("act_1_progress_2"), "after_act_1_B");
				setKey(AFTER_INIT_KEY);
				break;
		}

		return true;
	}
}