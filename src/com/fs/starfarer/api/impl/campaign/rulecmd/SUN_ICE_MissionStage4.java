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
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

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

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_mission4_");

	public static void backdoor() { // runcode com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionStage4.backdoor();
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

				textPanel.addPara(strings.get("response_1"), Misc.getHighlightColor(), String.valueOf(TIME_OUT));
				optionPanel.addOption(strings.get("accept"), "accept");
				optionPanel.addOption(strings.get("information"), "information");
				optionPanel.addOption(strings.get("refuse"), "refuse");
				break;
			case "accept":
				textPanel.addPara(strings.get("response_accept"));
				optionPanel.addOption(strings.get("get_hint"), "hint");
				setStage(MissionStage.STAGE_ONGOING_GOODS);

				startMission(TIME_OUT);
				new SUN_ICE_MissionGoodsProcurementIntel(dialog, REQUIRED_COMMODITY, TIME_OUT);

				endGoToFleetIntel(true);
				break;
			case "information":
				textPanel.addPara(strings.get("response_information"));
				optionPanel.addOption(strings.get("accept"), "accept");
				optionPanel.addOption(strings.get("refuse"), "refuse");
				break;
			case "refuse":
				textPanel.addPara(strings.get("response_refuse"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "hint":
				textPanel.addPara(strings.get("response_hint"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "init_re":
				textPanel.addPara(strings.get("init_re"));
				optionPanel.addOption(strings.get("accept"), "accept");
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