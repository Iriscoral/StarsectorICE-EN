package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionCacheText extends BaseCommandPlugin {

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_" + key);
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		MemoryAPI memory = memoryMap.get(MemKeys.FACTION);

		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		TextPanelAPI textPanel = dialog.getTextPanel();
		textPanel.addPara(getString("cache_text"));

		optionPanel.addOption(getString("salvage"), "SUN_ICE_mission_cache_salvage");
		optionPanel.addOption(getString("leave"), "defaultLeave");
		optionPanel.setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
		return true;
	}
}