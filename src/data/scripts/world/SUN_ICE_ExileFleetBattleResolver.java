package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.BattleAutoresolverPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.EncounterOption;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;

public class SUN_ICE_ExileFleetBattleResolver implements BattleAutoresolverPlugin {
	private FleetEncounterContext context;
	private CampaignFleetAPI one;
	private CampaignFleetAPI two;
	private final BattleAPI battle;

	public SUN_ICE_ExileFleetBattleResolver(BattleAPI battle) {
		this.battle = battle;

		one = battle.getCombinedOne();
		two = battle.getCombinedTwo();
		if (battle.isPlayerInvolved()) {
			one = battle.getPlayerCombined();
			two = battle.getNonPlayerCombined();
		}
	}

	@Override
	public void resolve() {
		Global.getLogger(this.getClass()).info("ICE resolve called.");
		// debug
		battle.leave(one != null ? one : two, false);
		battle.finish(BattleAPI.BattleSide.NO_JOIN, false);

		context = new FleetEncounterContext();
		context.setAutoresolve(true);
		context.setBattle(battle);
		EncounterOption optionOne = one.getAI().pickEncounterOption(context, two);
		EncounterOption optionTwo = two.getAI().pickEncounterOption(context, one);

		if (optionOne == EncounterOption.DISENGAGE && optionTwo == EncounterOption.DISENGAGE) {
			return;
		}

		context.getDataFor(one).setDisengaged(true);
		context.getDataFor(one).setWonLastEngagement(false);
		context.getDataFor(one).setLastGoal(FleetGoal.ESCAPE);

		context.getDataFor(two).setDisengaged(true);
		context.getDataFor(two).setWonLastEngagement(false);
		context.getDataFor(two).setLastGoal(FleetGoal.ESCAPE);

		context.applyAfterBattleEffectsIfThereWasABattle();
	}

	@Override
	public FleetEncounterContextPlugin getContext() {
		return context;
	}
}