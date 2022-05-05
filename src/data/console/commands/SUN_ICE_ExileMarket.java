package data.console.commands;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.tools.SUN_ICE_Data;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SUN_ICE_ExileMarket implements BaseCommand {

	@Override
	public CommandResult runCommand(@NotNull String args, CommandContext context) {
		if (!context.isInCampaign()) {
			Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
			return CommandResult.WRONG_CONTEXT;
		}

		SectorEntityToken token = SUN_ICE_Data.getFakeMarketEntity();
		if (token == null) {
			Console.showMessage("Unable to find market entity!");
			return CommandResult.ERROR;
		}

		Console.showDialogOnClose(token);
		Console.showMessage("Exile market will be shown when you next unpause on the campaign map.");
		return CommandResult.SUCCESS;
	}
}