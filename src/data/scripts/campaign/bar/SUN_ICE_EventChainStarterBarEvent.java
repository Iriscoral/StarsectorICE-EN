package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.world.SUN_ICE_ExileFleetFakeAI;
import data.scripts.world.SUN_ICE_ExileFleetManager;

import java.awt.*;
import java.util.Map;

public class SUN_ICE_EventChainStarterBarEvent extends BaseBarEventWithPerson {

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_" + key);
	}

	@Override
	public boolean isAlwaysShow() {
		return true;
	}

	@Override
	public boolean shouldShowAtMarket(MarketAPI market) {
		Global.getLogger(this.getClass()).info("Spawning ICE bar event stage 1");
		if (!super.shouldShowAtMarket(market)) {
			return false;
		}

		Global.getLogger(this.getClass()).info("Spawning ICE bar event stage 2");
		if (!market.getFactionId().contentEquals("sun_ice")) {
			return false;
		}

		Global.getLogger(this.getClass()).info("Spawning ICE bar event stage 3");
		SUN_ICE_ExileFleetManager manager = SUN_ICE_Data.getExileManager();
		if (manager == null || manager.getCurrentState() != SUN_ICE_ExileFleetFakeAI.ExileState.STAY) {
			return false;
		}

		Global.getLogger(this.getClass()).info("Spawning ICE bar event stage 4");
		if (manager.getExiledFleet().getMarket() != market) {
			return false;
		}

		Global.getLogger(this.getClass()).info("Spawning ICE bar event stage 5");
		return market.getFaction().isAtWorst(Global.getSector().getPlayerFaction(), RepLevel.FAVORABLE);
	}

	@Override
	public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.init(dialog, memoryMap);

		done = false;
		dialog.getVisualPanel().showPersonInfo(person, true);
		optionSelected(null, OptionId.AFTER_ACT_1);
	}

	@Override
	public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		regen(dialog.getInteractionTarget().getMarket());
		TextPanelAPI text = dialog.getTextPanel();
		text.addPara(String.format(getString("prompt"), getManOrWoman(), getHeOrShe()));

		Color c = Global.getSector().getFaction(getPersonFaction()).getColor();
		dialog.getOptionPanel().addOption(String.format(getString("contact_act_1"), getManOrWoman()), this, c, null);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		if (!(optionData instanceof OptionId)) {
			return;
		}
		OptionId option = (OptionId) optionData;
		OptionPanelAPI options = dialog.getOptionPanel();
		TextPanelAPI text = dialog.getTextPanel();
		options.clearOptions();

		switch (option) {
			case AFTER_ACT_1:
				text.addPara(getString("contact_response_1"));
				options.addOption(getString("contact_act_2_progress_1"), OptionId.AFTER_ACT_2_A);
				options.addOption(getString("contact_act_2_progress_2"), OptionId.AFTER_ACT_2_B);
				options.addOption(getString("contact_act_2_leave"), OptionId.LEAVE);
				break;
			case AFTER_ACT_2_A:
			case AFTER_ACT_2_B:
				text.addPara(getString("contact_response_2"));
				options.addOption(getString("contact_act_3_progress_1"), OptionId.AFTER_ACT_3_A);
				options.addOption(getString("contact_act_3_progress_2"), OptionId.AFTER_ACT_3_B);
				break;
			case AFTER_ACT_3_A:
			case AFTER_ACT_3_B:
				text.addPara(getString("contact_response_3"));
				options.addOption(getString("contact_act_4_progress_1"), OptionId.AFTER_ACT_4_A);
				options.addOption(getString("contact_act_4_progress_2"), OptionId.AFTER_ACT_4_B);
				options.addOption(getString("contact_act_4_progress_3"), OptionId.AFTER_ACT_4_C);
				break;
			case AFTER_ACT_4_A:
			case AFTER_ACT_4_B:
			case AFTER_ACT_4_C:
				person.getName().setFirst("Jay");
				text.addPara(getString("contact_response_4"));
				options.addOption(getString("contact_act_5_progress_1"), OptionId.AFTER_ACT_5_A);
				options.addOption(getString("contact_act_5_progress_2"), OptionId.AFTER_ACT_5_B);
				break;
			case AFTER_ACT_5_A:
			case AFTER_ACT_5_B:
				text.addPara(getString("contact_response_5"));
				options.addOption(getString("contact_act_end"), OptionId.END);
				break;
			case END:
				doConfirmActions();
				noContinue = true;
				done = true;
				BarEventManager.getInstance().notifyWasInteractedWith(this);
				break;
			case LEAVE:
				noContinue = true;
				done = true;
				BarEventManager.getInstance().notifyWasInteractedWith(this);
				BarEventManager.getInstance().setTimeout(SUN_ICE_EventChainStarterBarEventCreator.class, 60f);
				break;
		}
	}

	private void doConfirmActions() {
		Global.getSector().getMemoryWithoutUpdate().set(SUN_ICE_MissionManager.MISSION_MAIN_KEY, true);

		market.addPerson(person);
		market.getCommDirectory().addPerson(person);
		Misc.makeImportant(person, SUN_ICE_MissionManager.MISSION_MAIN_KEY);

		SUN_ICE_ExileFleetManager manager = SUN_ICE_Data.getExileManager();
		if (manager != null) {
			manager.getFakeAI().increaseDaysToStay(10f);
		}
	}

	@Override
	protected String getPersonFaction() {
		return "sun_ice";
	}

	@Override
	protected PersonAPI createPerson() {
		return SUN_ICE_MissionManager.getMissionGiver(getPersonRank(), getPersonPost());
	}

	@Override
	protected String getPersonRank() {
		return Ranks.AGENT;
	}

	@Override
	protected String getPersonPost() {
		return Ranks.POST_BASE_COMMANDER;
	}

	public enum OptionId {
		AFTER_ACT_1, AFTER_ACT_2_A, AFTER_ACT_2_B, AFTER_ACT_3_A, AFTER_ACT_3_B, AFTER_ACT_4_A, AFTER_ACT_4_B, AFTER_ACT_4_C, AFTER_ACT_5_A, AFTER_ACT_5_B, END, LEAVE,
	}
}