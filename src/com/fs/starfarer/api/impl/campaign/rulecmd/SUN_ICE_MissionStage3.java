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

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage3 extends SUN_ICE_MissionManager {
	private static final String AFTER_INIT_KEY = "$SUN_ICE_MissionStage3_Init";
	public static final String REQUIRED_COMMODITY = Commodities.SUPPLIES;
	public static final int REQUIRED_AMOUNT = 90;
	public static final int REQUIRED_CAP = 20;
	public static final int TIME_OUT = 90;

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_mission3_" + key);
	}

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

				textPanel.addPara(getString("response_1"));
				optionPanel.addOption(getString("act_2_progress_1"), "after_act_2_A");
				optionPanel.addOption(getString("act_2_progress_2"), "after_act_2_B");
				break;
			case "after_act_2_A":
			case "after_act_2_B":
				textPanel.addPara(getString("response_2"), Misc.getHighlightColor(), String.valueOf(REQUIRED_AMOUNT));
				optionPanel.addOption(getString("act_3_progress_1"), "after_act_3_A");
				optionPanel.addOption(getString("act_3_progress_2"), "after_act_3_B");
				break;
			case "after_act_3_A":
			case "after_act_3_B":
				textPanel.addPara(getString("response_3"), Misc.getHighlightColor(), String.valueOf(TIME_OUT));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("information"), "information");
				optionPanel.addOption(getString("delay"), "delay");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "accept":
				dialog.getVisualPanel().hideSecondPerson();

				textPanel.addPara(getString("response_accept"));
				optionPanel.addOption(getString("get_hint"), "hint");
				setStage(MissionStage.STAGE_ONGOING_SHIPS);

				startMission(TIME_OUT);
				new SUN_ICE_MissionRecruitmentShipsIntel(dialog, REQUIRED_AMOUNT, REQUIRED_CAP, TIME_OUT);

				endGoToFleetIntel(true);
				break;
			case "information":
				textPanel.addPara(getString("response_information"));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("delay"), "delay");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "delay":
				textPanel.addPara(getString("response_delay"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "refuse":
				dialog.getVisualPanel().hideSecondPerson();

				textPanel.addPara(getString("response_refuse"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				SUN_ICE_MissionManager.doomStages();
				break;
			case "hint":
				textPanel.addPara(getString("response_hint"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "init_re":
				dialog.getVisualPanel().showSecondPerson(getPriest());

				textPanel.addPara(getString("init_re"));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("delay"), "delay");
				optionPanel.addOption(getString("refuse"), "refuse");
			case "init":
			default:
				textPanel.addPara(getString("init"));
				optionPanel.addOption(getString("act_1_progress_1"), "after_act_1_A");
				optionPanel.addOption(getString("act_1_progress_2"), "after_act_1_B");
				setKey(AFTER_INIT_KEY);
				break;
		}

		return true;
	}
}