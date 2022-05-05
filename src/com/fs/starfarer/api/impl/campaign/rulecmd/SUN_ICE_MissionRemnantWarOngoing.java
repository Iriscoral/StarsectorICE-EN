package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_ExileRemnantWarIntel;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionRemnantWarOngoing extends BaseCommandPlugin {

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_" + key);
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		MemoryAPI memory = memoryMap.get(MemKeys.FACTION);
		MemoryAPI memoryLocal = memoryMap.get(MemKeys.LOCAL);

		TextPanelAPI textPanel = dialog.getTextPanel();
		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		textPanel.addPara(getString("war_ongoing"));
		optionPanel.addOption(getString("war_ongoing_act_1"), "SUN_ICE_remnant_war_init");
		Global.getSector().getMemoryWithoutUpdate().set(SUN_ICE_ExileRemnantWarIntel.REMNANT_WAR_MUTED_KEY, true);

		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_ExileRemnantWarIntel.class)) {
			SUN_ICE_ExileRemnantWarIntel exileRemnantWarIntel = (SUN_ICE_ExileRemnantWarIntel)intel;

			for (CampaignFleetAPI fleet : exileRemnantWarIntel.remnantBattleGroup) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, false);
			}

			break;
		}

		return true;
	}
}