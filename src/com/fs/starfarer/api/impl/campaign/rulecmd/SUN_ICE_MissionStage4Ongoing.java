package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionGoodsProcurementIntel;
import data.scripts.campaign.intel.SUN_ICE_MissionGoodsProcurementPickerListener;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage4Ongoing extends SUN_ICE_MissionManager {

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_mission4_ongoing_");

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
			case "check":
				CargoAPI toCheck = Global.getFactory().createCargo(false);
				for (CargoStackAPI stack : playerFleet.getCargo().getStacksCopy()) {
					if (stack.getCommodityId() == null) continue;
					if (SUN_ICE_MissionStage4.REQUIRED_COMMODITY.containsKey(stack.getCommodityId())) {
						toCheck.addCommodity(stack.getCommodityId(), stack.getSize());
					}
				}

				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionGoodsProcurementIntel.class)) {
					SUN_ICE_MissionGoodsProcurementIntel goodsProcurementIntel = (SUN_ICE_MissionGoodsProcurementIntel)intel;
					dialog.showCargoPickerDialog(strings.get("picker_title"), strings.get("picker_ensure"), strings.get("picker_cancle"),
							false, 320f, toCheck, new SUN_ICE_MissionGoodsProcurementPickerListener(goodsProcurementIntel, dialog, dialog.getPlugin()));

					break;
				}
				break;
			case "deliver":
				textPanel.addPara(strings.get("response_deliver"));
				optionPanel.addOption(strings.get("act_bye_1"), "hint_A");
				optionPanel.addOption(strings.get("act_bye_2"), "hint_B");
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
				textPanel.addPara(strings.get("response_deliver_extra"));
				optionPanel.addOption(strings.get("act_bye_extra_1"), "extra_end_A");
				optionPanel.addOption(strings.get("act_bye_extra_2"), "extra_end_B");
				optionPanel.addOption(strings.get("act_bye_extra_3"), "extra_end_C");
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
				textPanel.addPara(strings.get("extra_end"));
				optionPanel.addOption(strings.get("act_bye_extra_end"), "hint_A");
				break;
			case "wait":
				textPanel.addPara(strings.get("response_wait"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "hint_A":
			case "hint_B":
				textPanel.addPara(strings.get("response_hint"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "init":
			default:
				textPanel.addPara(strings.get("init"));
			case "checked":
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionGoodsProcurementIntel.class)) {
					SUN_ICE_MissionGoodsProcurementIntel goodsProcurementIntel = (SUN_ICE_MissionGoodsProcurementIntel)intel;
					if (goodsProcurementIntel.isAllDelivered()) {
						optionPanel.addOption(strings.get("deliver"), "deliver");
						optionPanel.addOption(strings.get("deliver_extra"), "deliver_extra", Misc.getPositiveHighlightColor(), null);
					} else {
						optionPanel.addOption(strings.get("check"), "check");
					}

					break;
				}
				optionPanel.addOption(strings.get("wait"), "wait");
				break;
		}

		return true;
	}
}