package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import data.scripts.ICEModPlugin;
import data.scripts.tools.SUN_ICE_Data;

import java.util.List;

public class SUN_ICE_CampaignPlugin extends BaseCampaignPlugin {

	@Override
	public String getId() {
		return null;
	}

	@Override
	public boolean isTransient() {
		return false;
	}

	@Override
	public PluginPick<BattleAutoresolverPlugin> pickBattleAutoresolverPlugin(BattleAPI battle) {
		List<CampaignFleetAPI> one = battle.getSideOne();
		List<CampaignFleetAPI> two = battle.getSideTwo();

		CampaignFleetAPI exiledFleet = SUN_ICE_Data.getExileManager().getExiledFleet();
		if (exiledFleet == null) return null;

		boolean hasExiles = false;
		boolean hasPlayer = false;

		for (CampaignFleetAPI tmp : one) {
			if (tmp == exiledFleet) {
				hasExiles = true;
			} else if (tmp == Global.getSector().getPlayerFleet()) {
				hasPlayer = true;
			}
		}

		for (CampaignFleetAPI tmp : two) {
			if (tmp == exiledFleet) {
				hasExiles = true;
			} else if (tmp == Global.getSector().getPlayerFleet()) {
				hasPlayer = true;
			}
		}

		if (hasExiles && hasPlayer) {
			battle.leave(Global.getSector().getPlayerFleet(), false);
			battle.finish(BattleAPI.BattleSide.NO_JOIN, false);
			Global.getLogger(this.getClass()).info("ICE resolve fake called.");
			//return new PluginPick<BattleAutoresolverPlugin>(new SUN_ICE_BattleResolver(battle), PickPriority.MOD_SPECIFIC);
		}

		return null;
	}

	@Override
	public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
		CampaignFleetAPI exiledFleet = SUN_ICE_Data.getExileManager().getExiledFleet();
		if (interactionTarget instanceof CampaignFleetAPI && exiledFleet != null && Global.getSector().getPlayerFleet() != null && interactionTarget == exiledFleet) {
			if (exiledFleet.isHostileTo(Global.getSector().getPlayerFleet())) {
				return new PluginPick<InteractionDialogPlugin>(new SUN_ICE_ExileFleetHostileInteractionDialog(), PickPriority.MOD_SPECIFIC);
			} else if (ICEModPlugin.EXERELIN_ENABLED) {
				return new PluginPick<InteractionDialogPlugin>(new SUN_ICE_ExileFleetMarketInteractionDialog(), PickPriority.MOD_SPECIFIC);
			}
		}
		return null;
	}

	public static class SUN_ICE_ExileFleetHostileInteractionDialog extends FleetInteractionDialogPluginImpl {

		@Override
		public void advance(float amount) {
			if ((otherFleet != null && playerFleet != null && context.getBattle() != null && context.getBattle().isPlayerInvolved())) {
				context.getBattle().leave(playerFleet, false);
				context.getBattle().finish(BattleAPI.BattleSide.NO_JOIN, false);
				Global.getSector().getPlayerFleet().getFleetData().setSyncNeeded();
				Global.getSector().getPlayerFleet().getFleetData().syncIfNeeded();
				Global.getLogger(this.getClass()).info("Interaction changed.");
			}
		}
	}

	public static class SUN_ICE_ExileFleetMarketInteractionDialog extends FleetInteractionDialogPluginImpl {

		@Override
		public void advance(float amount) {
			Global.getSector().getCampaignUI().showInteractionDialog(SUN_ICE_Data.getFakeMarketEntity());
		}
	}
}