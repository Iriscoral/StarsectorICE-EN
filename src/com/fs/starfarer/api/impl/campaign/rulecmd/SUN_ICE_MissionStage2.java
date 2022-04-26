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
import data.scripts.campaign.intel.SUN_ICE_MissionSalvageAICoreIntel;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage2 extends SUN_ICE_MissionManager {
	private static final String AFTER_INIT_KEY = "$SUN_ICE_MissionStage2_Init";
	private static final String AFTER_INFORMATION_KEY = "$SUN_ICE_MissionStage2_Information";
	private static final String AFTER_DELAY_KEY = "$SUN_ICE_MissionStage2_Delay";
	public static final String REQUIRED_COMMODITY = Commodities.SUPPLIES;
	public static final int REQUIRED_AMOUNT = 1;
	public static final int EXTRA_AMOUNT = 1;
	public static final int TIME_OUT = 40;

	private static final int DELAY = 10;

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_mission2_" + key);
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

		if (stage != MissionStage.STAGE_PREVIEW_CORE) return false;

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
				textPanel.addPara(getString("response_1"));
				optionPanel.addOption(getString("act_2_progress_1"), "after_act_2");
				break;
			case "after_act_2":
				textPanel.addPara(getString("response_2"), Misc.getHighlightColor(), String.valueOf(TIME_OUT));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("ask"), "ask");
				optionPanel.addOption(getString("delay"), "delay");
				break;
			case "accept":
				textPanel.addPara(getString("response_accept"));
				optionPanel.addOption(getString("get_hint"), "hint");
				setStage(MissionStage.STAGE_ONGOING_CORE);

				int timeOut = TIME_OUT;
				if (memory.getBoolean(AFTER_DELAY_KEY)) timeOut += DELAY;
				startMission(timeOut);
				new SUN_ICE_MissionSalvageAICoreIntel(dialog, REQUIRED_AMOUNT, EXTRA_AMOUNT, timeOut);

				endGoToFleetIntel(true);
				break;
			case "ask":
				textPanel.addPara(getString("response_ask"));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("information"), "information");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "information":
				textPanel.addPara(getString("response_information"));

				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("delay"), "delay");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "delay":
				textPanel.addPara(getString("response_delay"), Misc.getPositiveHighlightColor(), String.valueOf(DELAY));
				setKey(AFTER_DELAY_KEY);

				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "refuse":
				textPanel.addPara(getString("response_refuse"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				SUN_ICE_MissionManager.doomStages();
				break;
			case "hint":
				textPanel.addPara(getString("response_hint"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "init_re":
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