package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_ExileRemnantWarIntel;
import data.scripts.tools.SUN_ICE_Data;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionRemnantWarEnds extends BaseCommandPlugin {

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_" + key);
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		MemoryAPI memory = memoryMap.get(MemKeys.FACTION);
		MemoryAPI memoryLocal = memoryMap.get(MemKeys.LOCAL);

		Global.getSector().getMemoryWithoutUpdate().set(SUN_ICE_ExileRemnantWarIntel.REMNANT_WAR_FINISHED_KEY, false);

		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		TextPanelAPI textPanel = dialog.getTextPanel();
		textPanel.addPara(getString("war_ends"));

		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_ExileRemnantWarIntel.class)) {
			SUN_ICE_ExileRemnantWarIntel remnantWarIntel = (SUN_ICE_ExileRemnantWarIntel)intel;
			remnantWarIntel.performDelivery(dialog);

			SUN_ICE_MissionManager.setStage(SUN_ICE_MissionManager.MissionStage.STAGE_FINALE);
			SUN_ICE_Data.getExileManager().getFakeAI().resetHomeCheckerDays();
			break;
		}

		Misc.makeUnimportant(dialog.getInteractionTarget(), "REM_WAR");
		SUN_ICE_MissionManager.finishMission();
		optionPanel.addOption(getString("continue"), "SUN_ICE_remnant_war_ends");
		return true;
	}
}