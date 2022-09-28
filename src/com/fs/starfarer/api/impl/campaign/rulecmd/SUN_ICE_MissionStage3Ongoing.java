package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_MissionRecruitmentShipsIntel;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionStage3Ongoing extends SUN_ICE_MissionManager {

	private static final String SPECIAL_ACTION_KEY = "$SUN_ICE_MissionStage3_Special";
	public static final int DELAY_TIME = 30;
	public static final int EXTRA_AMOUNT = 40;
	public static final int EXTRA_CAP = 30;

	public static final I18nSection strings = I18nSection.getInstance("Event", "SUN_ICE_mission3_ongoing_");

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

		if (stage != MissionStage.STAGE_ONGOING_SHIPS) return false;

		String string = params.get(0).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		if (string == null) return false;
		if (string.contentEquals("cutCommLink")) return false;

		final TextPanelAPI textPanel = dialog.getTextPanel();
		final OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		List<FleetMemberAPI> selectableMembers = new ArrayList<>();
		for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
			if (member.isFighterWing()) continue;
			if (member.isCivilian()) continue;
			if (member.getVariant().isDHull()) continue;
			if (member.getVariant().getUnusedOP(Global.getSector().getPlayerPerson().getStats()) > 0) continue;
			if (member.getHullSpec().getDesignation().contentEquals("ICS")) continue;
			selectableMembers.add(member);
		}

		switch (string) {
			case "check":
				dialog.showFleetMemberPickerDialog(strings.get("picker_title"), strings.get("picker_ensure"), strings.get("picker_cancle"),
						5, 9, 96, // 3, 7, 58 or so
						true, true, selectableMembers,
						new FleetMemberPickerListener() {
							@Override
							public void pickedFleetMembers(List<FleetMemberAPI> members) {
								boolean able = false;
								for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionRecruitmentShipsIntel.class)) {
									SUN_ICE_MissionRecruitmentShipsIntel recruitmentShipsIntel = (SUN_ICE_MissionRecruitmentShipsIntel)intel;
									able = recruitmentShipsIntel.tryDeliver(members);
									break;
								}
								if (able) {
									textPanel.addPara(strings.get("check_able"));
									optionPanel.addOption(strings.get("check_continue"), "checked");
								} else {
									textPanel.addPara(strings.get("check_too_many"), Misc.getNegativeHighlightColor());
									optionPanel.addOption(strings.get("check_return"), "checked");
								}
							}

							@Override
							public void cancelledFleetMemberPicking() {
								optionPanel.addOption(strings.get("check_return"), "checked");
							}
						});
				break;
			case "deliver":
				boolean extra = false;
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionRecruitmentShipsIntel.class)) {
					SUN_ICE_MissionRecruitmentShipsIntel recruitmentShipsIntel = (SUN_ICE_MissionRecruitmentShipsIntel)intel;
					extra = recruitmentShipsIntel.performDelivery(dialog);

					setStage(MissionStage.STAGE_AFTER_SHIPS);
					finishMission();
					if (extra) evolveSpecialTag();

					Global.getSector().addScript(new SUN_ICE_MissionManager(RepLevel.COOPERATIVE, MissionStage.STAGE_PREVIEW_GOODS, new SUN_ICE_MissionStage4Preview()));
					break;
				}

				if (extra) {
					textPanel.addPara(strings.get("response_deliver_extra"));
					optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				} else {
					textPanel.addPara(strings.get("response_deliver"));
					optionPanel.addOption(cutlinkStrings.get(), "cutCommLink");
				}

				dialog.getVisualPanel().hideSecondPerson();
				break;
			case "deliver_extra":
				dialog.showFleetMemberPickerDialog(strings.get("picker_title"), strings.get("picker_ensure"), strings.get("picker_cancle"),
						5, 9, 96, // 3, 7, 58 or so
						true, true, selectableMembers,
						new FleetMemberPickerListener() {
							@Override
							public void pickedFleetMembers(List<FleetMemberAPI> members) {
								boolean able = false;
								for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionRecruitmentShipsIntel.class)) {
									SUN_ICE_MissionRecruitmentShipsIntel recruitmentShipsIntel = (SUN_ICE_MissionRecruitmentShipsIntel)intel;
									able = recruitmentShipsIntel.tryDeliverExtra(members, EXTRA_AMOUNT, EXTRA_CAP);
									break;
								}
								if (able) {
									textPanel.addPara(strings.get("check_able"));
									optionPanel.addOption(strings.get("check_continue"), "deliver");
								} else {
									textPanel.addPara(strings.get("check_too_many"), Misc.getNegativeHighlightColor());
									optionPanel.addOption(strings.get("check_return"), "checked");
								}
							}

							@Override
							public void cancelledFleetMemberPicking() {
								optionPanel.addOption(strings.get("check_return"), "checked");
							}
						});
				break;
			case "extra_end":
				dialog.getVisualPanel().hideSecondPerson();

				textPanel.addPara(strings.get("extra_end"));
				optionPanel.addOption(strings.get("act_bye_extra_end"), "hint");
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
				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionRecruitmentShipsIntel.class)) {
					SUN_ICE_MissionRecruitmentShipsIntel recruitmentShipsIntel = (SUN_ICE_MissionRecruitmentShipsIntel)intel;
					recruitmentShipsIntel.delayLimit(DELAY_TIME);
					memory.set(SPECIAL_ACTION_KEY, true);
					break;
				}
				break;
			case "init":
			default:
				dialog.getVisualPanel().showSecondPerson(getPriest());
				textPanel.addPara(strings.get("init"));
			case "checked":
				boolean canWait = true;

				for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_MissionRecruitmentShipsIntel.class)) {
					SUN_ICE_MissionRecruitmentShipsIntel recruitmentShipsIntel = (SUN_ICE_MissionRecruitmentShipsIntel)intel;
					if (recruitmentShipsIntel.checkedShipReadyToDeliver()) {
						optionPanel.addOption(strings.get("deliver"), "deliver");
						optionPanel.addOption(strings.get("deliver_extra"), "deliver_extra", Misc.getPositiveHighlightColor(), null);
						canWait = false;
					} else {
						optionPanel.addOption(strings.get("check"), "check");
					}

					break;
				}

				if (canWait) optionPanel.addOption(strings.get("wait"), "wait");
				break;
		}

		return true;
	}
}