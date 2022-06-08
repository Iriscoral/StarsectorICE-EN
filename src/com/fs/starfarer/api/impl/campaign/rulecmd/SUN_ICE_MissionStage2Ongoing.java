package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionSalvageAICoreIntel;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage2Ongoing extends SUN_ICE_MissionManager {
	private static final String SPECIAL_ACTION_KEY = "$SUN_ICE_MissionStage2_Special";
	public static final int DELAY_TIME = 30;

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_mission2_ongoing_");

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

		if (stage != MissionStage.STAGE_ONGOING_CORE) return false;

		String string = params.get(0).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		if (string == null) return false;
		if (string.contentEquals("cutCommLink")) return false;

		TextPanelAPI textPanel = dialog.getTextPanel();
		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		switch (string) {
			case "deliver":
				textPanel.addPara(strings.get("response_deliver"));
				optionPanel.addOption(strings.get("act_bye_1"), "end_A");
				optionPanel.addOption(strings.get("act_bye_2"), "end_B");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionSalvageAICoreIntel.class)) {
					SUN_ICE_MissionSalvageAICoreIntel salvageAICoreIntel = (SUN_ICE_MissionSalvageAICoreIntel)intel;
					salvageAICoreIntel.performDelivery(dialog, false);

					setStage(MissionStage.STAGE_AFTER_CORE);
					finishMission();

					Global.getSector().addScript(new SUN_ICE_MissionManager(RepLevel.WELCOMING, MissionStage.STAGE_PREVIEW_SHIPS, new SUN_ICE_MissionStage3Preview()));
					break;
				}
				break;
			case "end_A":
			case "end_B":
				textPanel.addPara(strings.get("end"));
				optionPanel.addOption(strings.get("act_bye_end"), "cutCommLink");
				break;
			case "deliver_extra":
				textPanel.addPara(strings.get("response_deliver_extra"));
				optionPanel.addOption(strings.get("act_bye_extra_1"), "extra_end_A");
				optionPanel.addOption(strings.get("act_bye_extra_2"), "extra_end_B");
				optionPanel.addOption(strings.get("act_bye_extra_3"), "extra_end_C");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionSalvageAICoreIntel.class)) {
					SUN_ICE_MissionSalvageAICoreIntel salvageAICoreIntel = (SUN_ICE_MissionSalvageAICoreIntel)intel;
					salvageAICoreIntel.performDelivery(dialog, true);

					setStage(MissionStage.STAGE_AFTER_CORE);
					finishMission();
					evolveSpecialTag();

					Global.getSector().addScript(new SUN_ICE_MissionManager(RepLevel.WELCOMING, MissionStage.STAGE_PREVIEW_SHIPS, new SUN_ICE_MissionStage3Preview()));
					break;
				}
				break;
			case "extra_end_A":
			case "extra_end_B":
			case "extra_end_C":
				textPanel.addPara(strings.get("extra_end"));
				optionPanel.addOption(strings.get("act_bye_extra_end"), "cutCommLink");
				break;
			case "deliver_simple":
				textPanel.addPara(strings.get("response_deliver_simple"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionSalvageAICoreIntel.class)) {
					SUN_ICE_MissionSalvageAICoreIntel salvageAICoreIntel = (SUN_ICE_MissionSalvageAICoreIntel)intel;
					salvageAICoreIntel.simpleFinish(dialog);
					setStage(MissionStage.STAGE_AFTER_CORE);
					finishMission();

					Global.getSector().addScript(new SUN_ICE_MissionManager(RepLevel.WELCOMING, MissionStage.STAGE_PREVIEW_SHIPS, new SUN_ICE_MissionStage3Preview()));
					break;
				}
				break;
			case "doomed":
				textPanel.addPara(strings.get("response_doomed"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionSalvageAICoreIntel.class)) {
					SUN_ICE_MissionSalvageAICoreIntel salvageAICoreIntel = (SUN_ICE_MissionSalvageAICoreIntel)intel;
					salvageAICoreIntel.forceFailed();
					break;
				}
				break;
			case "wait":
				if (!memory.getBoolean(SPECIAL_ACTION_KEY)) {
					textPanel.addPara(strings.get("response_wait_special"), Misc.getHighlightColor(), String.valueOf(DELAY_TIME));
					optionPanel.addOption(strings.get("delay"), "delay");
					SetStoryOption.set(dialog, 1, "delay", "sun_ice_delay", Sounds.STORY_POINT_SPEND, null);

					optionPanel.addOption(strings.get("delay_refuse"), "wait2");
					break;
				}
			case "wait2":
				textPanel.addPara(strings.get("response_wait"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				break;
			case "delay":
				textPanel.addPara(strings.get("response_delay"));
				optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionSalvageAICoreIntel.class)) {
					SUN_ICE_MissionSalvageAICoreIntel salvageAICoreIntel = (SUN_ICE_MissionSalvageAICoreIntel)intel;
					salvageAICoreIntel.delayLimit(DELAY_TIME);
					memory.set(SPECIAL_ACTION_KEY, true);
					break;
				}
				break;
			case "init":
			default:
				textPanel.addPara(strings.get("init"));
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionSalvageAICoreIntel.class)) {
					SUN_ICE_MissionSalvageAICoreIntel salvageAICoreIntel = (SUN_ICE_MissionSalvageAICoreIntel)intel;
					if (salvageAICoreIntel.isLooted()) {
						optionPanel.addOption(strings.get("deliver"), "deliver");
						optionPanel.setEnabled("deliver", false);

						if (salvageAICoreIntel.hasEnough(false)) {
							optionPanel.setEnabled("deliver", true);
							if (salvageAICoreIntel.hasEnough(true)) { // initial hidden
								optionPanel.addOption(strings.get("deliver_extra"), "deliver_extra", Misc.getPositiveHighlightColor(), null);
								optionPanel.setEnabled("deliver_extra", true);
							}
						}

						optionPanel.addOption(strings.get("doomed"), "doomed");
					} else {
						optionPanel.addOption(strings.get("doomed"), "doomed");
					}

					break;
				}
				optionPanel.addOption(strings.get("wait"), "wait");
				break;
		}

		return true;
	}
}