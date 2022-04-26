package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionGoodsProcurementIntel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage4 extends SUN_ICE_MissionManager {
	private static final String AFTER_INIT_KEY = "$SUN_ICE_MissionStage4_Init";
	public static final Map<String, Integer> REQUIRED_COMMODITY = new HashMap<>();
	public static final int TIME_OUT = 180;

	static {
		REQUIRED_COMMODITY.put(Commodities.DOMESTIC_GOODS, 1500);
		REQUIRED_COMMODITY.put(Commodities.FOOD, 1500);
		REQUIRED_COMMODITY.put(Commodities.SUPPLIES, 3500);
		REQUIRED_COMMODITY.put(Commodities.FUEL, 2000);
		REQUIRED_COMMODITY.put(Commodities.HEAVY_MACHINERY, 500);
		REQUIRED_COMMODITY.put(Commodities.VOLATILES, 500);
	}

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_mission4_" + key);
	}

	public static void backdoor() {
		for (String id : REQUIRED_COMMODITY.keySet()) {
			int amount = REQUIRED_COMMODITY.get(id);
			Global.getSector().getPlayerFleet().getCargo().addCommodity(id, amount);
		}
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

		if (stage != MissionStage.STAGE_PREVIEW_GOODS) return false;

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
				PersonAPI missionGiver = SUN_ICE_MissionManager.getMissionGiver();
				missionGiver.getName().setFirst("Carobin");
				missionGiver.getName().setLast("Kaiser");

				textPanel.addPara(getString("response_1"), Misc.getHighlightColor(), String.valueOf(TIME_OUT));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("information"), "information");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "accept":
				textPanel.addPara(getString("response_accept"));
				optionPanel.addOption(getString("get_hint"), "hint");
				setStage(MissionStage.STAGE_ONGOING_GOODS);

				startMission(TIME_OUT);
				new SUN_ICE_MissionGoodsProcurementIntel(dialog, REQUIRED_COMMODITY, TIME_OUT);

				endGoToFleetIntel(true);
				break;
			case "information":
				textPanel.addPara(getString("response_information"));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "refuse":
				textPanel.addPara(getString("response_refuse"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "hint":
				textPanel.addPara(getString("response_hint"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "init_re":
				textPanel.addPara(getString("init_re"));
				optionPanel.addOption(getString("accept"), "accept");
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