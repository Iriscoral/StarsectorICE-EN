package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionSuppliesDeliveryIntel;
import data.scripts.tools.SUN_ICE_Data;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage1 extends SUN_ICE_MissionManager {
	private static final String AFTER_INIT_KEY = "$SUN_ICE_MissionStage1_Init";
	public static final String REQUIRED_COMMODITY = Commodities.SUPPLIES;
	public static final int REQUIRED_AMOUNT = 2000;
	public static final int EXTRA_AMOUNT = 500;
	public static final int TIME_OUT = 60;

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_mission1_" + key);
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

		if (stage != MissionStage.STAGE_NOT_STARTED) return false;

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
				textPanel.addPara(getString("response_1"), Misc.getHighlightColor(), String.valueOf(REQUIRED_AMOUNT), String.valueOf(TIME_OUT));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("information"), "information");
				optionPanel.addOption(getString("refuse"), "refuse");
				addCargoRequirementIcon(textPanel, REQUIRED_COMMODITY, REQUIRED_AMOUNT);
				break;
			case "accept":
				textPanel.addPara(getString("response_accept"));
				optionPanel.addOption(getString("get_hint"), "hint");
				setStage(MissionStage.STAGE_ONGOING_SUPPLIES);

				startMission(TIME_OUT);
				new SUN_ICE_MissionSuppliesDeliveryIntel(dialog, REQUIRED_COMMODITY, REQUIRED_AMOUNT, EXTRA_AMOUNT, TIME_OUT);
				break;
			case "information":
				textPanel.addPara(getString("response_information"));
				optionPanel.addOption(getString("accept"), "accept");
				optionPanel.addOption(getString("refuse"), "refuse");
				break;
			case "refuse":
				textPanel.addPara(getString("response_refuse"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				SUN_ICE_MissionManager.doomStages();
				break;
			case "hint":
				textPanel.addPara(getString("response_hint"), Misc.getHighlightColor(), String.valueOf(EXTRA_AMOUNT));
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

	private void addCargoRequirementIcon(TextPanelAPI textPanel, String requiredRes, int requiredAmount) {
		FactionAPI playerFaction = Global.getSector().getPlayerFaction();
		FactionAPI targetFaction = SUN_ICE_Data.getICE();
		CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();

		float costHeight = 67f;
		Color color = targetFaction.getColor();
		Color bad = Misc.getNegativeHighlightColor();

		ResourceCostPanelAPI cost = textPanel.addCostPanel(getString("item_list"), costHeight,
				color, playerFaction.getDarkUIColor());
		cost.setNumberOnlyMode(true);
		cost.setWithBorder(false);
		cost.setAlignment(Alignment.LMID);

		int available = (int)cargo.getCommodityQuantity(requiredRes);
		Color curr = color;
		if (requiredAmount > cargo.getQuantity(CargoAPI.CargoItemType.RESOURCES, requiredRes)) {
			curr = bad;
		}
		cost.addCost(requiredRes, "" + requiredAmount + " (" + available + ")", curr);

		cost.update();
	}
}