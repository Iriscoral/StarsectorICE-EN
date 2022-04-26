package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.world.SUN_ICE_ExileFleetManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SUN_ICE_ExileLocation implements BaseCommand {

	@Override
	public CommandResult runCommand(@NotNull String args, CommandContext context) {
		if (!context.isInCampaign()) {
			Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
			return CommandResult.WRONG_CONTEXT;
		}

		SUN_ICE_ExileFleetManager manager = SUN_ICE_Data.getExileManager();
		if (manager == null) {
			Console.showMessage("Unable to find exile script!");
			return CommandResult.ERROR;
		}

		if (manager.isFleetValid()) {
			CampaignFleetAPI player = Global.getSector().getPlayerFleet();
			CampaignFleetAPI exile = manager.getExiledFleet();

			Global.getSector().setCurrentLocation(exile.getContainingLocation());
			player.getContainingLocation().removeEntity(player);
			exile.getContainingLocation().addEntity(player);
			player.setLocation(exile.getLocation().getX(), exile.getLocation().getY());
			Console.showMessage("Teleport player to exile location.");
			return CommandResult.SUCCESS;
		}

		Console.showMessage("Unable to find exile entity!");
		return CommandResult.ERROR;
	}
}