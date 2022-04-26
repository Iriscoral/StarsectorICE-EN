package data.console.commands;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SUN_ICE_ExileEvolveMissionDelay implements BaseCommand {

	@Override
	public CommandResult runCommand(@NotNull String args, CommandContext context) {
		if (!context.isInCampaign()) {
			Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
			return CommandResult.WRONG_CONTEXT;
		}

		for (EveryFrameScript e : Global.getSector().getScripts()) {
			if (e instanceof SUN_ICE_MissionManager) {
				SUN_ICE_MissionManager missionManager = (SUN_ICE_MissionManager)e;
				if (missionManager.isDone()) continue;

				missionManager.setMessageDelay(0f);

				Console.showMessage("Message delay for current stage set to 0.");
				return CommandResult.SUCCESS;
			}
		}

		Console.showMessage("Unable to find script!");
		return CommandResult.ERROR;
	}
}