package data.console.commands;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.world.SUN_ICE_ExileFleetFakeAI;
import data.scripts.world.SUN_ICE_ExileFleetManager;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_ExileArriveIfInMove implements BaseCommand {

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
			if (manager.getCurrentState() == SUN_ICE_ExileFleetFakeAI.ExileState.TRAVELING
			|| manager.getCurrentState() == SUN_ICE_ExileFleetFakeAI.ExileState.START) {
				StarSystemAPI target = manager.getCurrentSystemDestination();
				SectorEntityToken destination = target.getStar();
				CampaignFleetAPI exile = manager.getExiledFleet();

				if (destination == null) {
					destination = target.createToken(0f, 0f);
				} else {
					PlanetAPI star = (PlanetAPI)destination;
					float distance = 2f * (star.getRadius() + star.getSpec().getCoronaSize()) + 200f;
					Vector2f offset = MathUtils.getRandomPointOnCircumference(null, distance);
					destination = target.createToken(offset.x, offset.y);
				}

				exile.getContainingLocation().removeEntity(exile);
				target.addEntity(exile);
				exile.setLocation(destination.getLocation().x, destination.getLocation().y);
				manager.getFakeAI().hasEnteredHyperspace = true;

				Console.showMessage("Exile will teleport to next location.");
				return CommandResult.SUCCESS;
			}

			Console.showMessage("Exile is not moving!");
			return CommandResult.ERROR;
		}

		Console.showMessage("Unable to find exile entity!");
		return CommandResult.ERROR;
	}
}