package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionGoodsProcurementIntel;
import data.scripts.tools.SUN_ICE_Data;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage4Ongoing extends SUN_ICE_MissionManager {

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_mission4_ongoing_" + key);
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

		if (stage != MissionStage.STAGE_ONGOING_GOODS) return false;

		String string = params.get(0).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		if (string == null) return false;
		if (string.contentEquals("cutCommLink")) return false;

		TextPanelAPI textPanel = dialog.getTextPanel();
		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		switch (string) {
			case "deliver":
				textPanel.addPara(getString("response_deliver"));
				optionPanel.addOption(getString("act_bye_1"), "hint_A");
				optionPanel.addOption(getString("act_bye_2"), "hint_B");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionGoodsProcurementIntel.class)) {
					SUN_ICE_MissionGoodsProcurementIntel goodsProcurementIntel = (SUN_ICE_MissionGoodsProcurementIntel)intel;
					goodsProcurementIntel.performDelivery(dialog, false);

					setStage(MissionStage.STAGE_AFTER_ALL);
					finishMission();
					break;
				}

				SUN_ICE_Data.getExileManager().getFakeAI().setDaysToStay(60f);
				break;
			case "deliver_extra":
				textPanel.addPara(getString("response_deliver_extra"));
				optionPanel.addOption(getString("act_bye_extra_1"), "extra_end_A");
				optionPanel.addOption(getString("act_bye_extra_2"), "extra_end_B");
				optionPanel.addOption(getString("act_bye_extra_3"), "extra_end_C");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionGoodsProcurementIntel.class)) {
					SUN_ICE_MissionGoodsProcurementIntel goodsProcurementIntel = (SUN_ICE_MissionGoodsProcurementIntel)intel;
					goodsProcurementIntel.performDelivery(dialog, true);

					setStage(MissionStage.STAGE_AFTER_ALL);
					finishMission();
					evolveSpecialTag();
					if (isAllSpecialTagsCompleted()) {
						Global.getLogger(this.getClass()).info("All special tags completed.");
					}
					break;
				}

				SUN_ICE_Data.getExileManager().getFakeAI().setDaysToStay(60f);
				break;
			case "extra_end_A":
			case "extra_end_B":
			case "extra_end_C":
				textPanel.addPara(getString("extra_end"));
				optionPanel.addOption(getString("act_bye_extra_end"), "hint_A");
				break;
			case "wait":
				textPanel.addPara(getString("response_wait"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "hint_A":
			case "hint_B":
				textPanel.addPara(getString("response_hint"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				break;
			case "init":
			default:
				textPanel.addPara(getString("init"));
				optionPanel.addOption(getString("deliver"), "deliver");
				optionPanel.setEnabled("deliver", false);
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionGoodsProcurementIntel.class)) {
					SUN_ICE_MissionGoodsProcurementIntel goodsProcurementIntel = (SUN_ICE_MissionGoodsProcurementIntel)intel;
					if (goodsProcurementIntel.hasEnough()) {
						optionPanel.setEnabled("deliver", true);
						optionPanel.addOption(getString("deliver_extra"), "deliver_extra", Misc.getPositiveHighlightColor(), null);
					}

					break;
				}
				optionPanel.addOption(getString("wait"), "wait");
				break;
		}

		return true;
	}
}