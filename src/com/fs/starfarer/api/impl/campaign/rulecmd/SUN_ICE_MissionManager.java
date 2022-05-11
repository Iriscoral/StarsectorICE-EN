package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.SUN_ICE_ExileRemnantWarIntel;
import data.scripts.campaign.intel.SUN_ICE_GoToFleetIntel;
import data.scripts.tools.SUN_ICE_Data;
import data.scripts.world.SUN_ICE_ExileFleetFakeAI;
import data.scripts.world.SUN_ICE_ExileFleetManager;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

public class SUN_ICE_MissionManager extends BaseCommandPlugin implements EveryFrameScript {
	public static final String MISSION_MAIN_KEY = "$SUN_ICE_isMissionOnGoing";
	public static final String MISSION_GIVER_KEY = "$SUN_ICE_isMissionGiver";
	public static final String MISSION_PRIEST_KEY = "$SUN_ICE_isPriest";
	public static final String MISSION_STAGE_KEY = "$SUN_ICE_MissionManager_Stage";
	public static final String MISSION_SPECIAL_TAG_KEY = "$SUN_ICE_MissionManager_Special";
	public static final String STATE_WHEN_DIALOG_STARTED = "$SUN_ICE_MissionManager_State";
	public static final int DAYS_OF_WAIT_OF_MESSAGE = 60;

	public float messageDelay;
	public boolean messageDelivered = false;

	public RepLevel repLevel = null;
	public MissionStage nextStage = null;
	public InteractionDialogPlugin pluginForNextStage;

	@Override
	public boolean isDone() {
		return messageDelivered;
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	public SUN_ICE_MissionManager() {}

	public SUN_ICE_MissionManager(RepLevel repLevel, MissionStage nextStage, InteractionDialogPlugin pluginForNextStage) {
		this.messageDelay = DAYS_OF_WAIT_OF_MESSAGE;
		this.repLevel = repLevel;
		this.nextStage = nextStage;
		this.pluginForNextStage = pluginForNextStage;

		Global.getLogger(this.getClass()).info("New queued event " + repLevel + " " + nextStage);
	}

	public SUN_ICE_MissionManager(InteractionDialogPlugin pluginForNextStage) {
		this.messageDelay = DAYS_OF_WAIT_OF_MESSAGE;
		this.pluginForNextStage = pluginForNextStage;

		Global.getLogger(this.getClass()).info("New queued event direct");
	}

	@Override
	public void advance(float amount) {
		float days = Global.getSector().getClock().convertToDays(amount);

		FactionAPI playerFaction = Global.getSector().getPlayerFaction();
		FactionAPI targetFaction = SUN_ICE_Data.getICE();
		if (repLevel == null || targetFaction.isAtWorst(playerFaction, repLevel)) {
			messageDelay -= days;
		}

		if (messageDelay <= 0f) {
			if (repLevel != null) {
				SUN_ICE_ExileFleetManager manager = SUN_ICE_Data.getExileManager();
				if (manager.getCurrentState() != SUN_ICE_ExileFleetFakeAI.ExileState.STAY) {
					messageDelay += 10f;
					return;
				}
			}

			Global.getSector().getCampaignUI().showInteractionDialog(pluginForNextStage, null);
			if (nextStage != null) setStage(nextStage);

			messageDelivered = true;
		}
	}

	public void setMessageDelay(float messageDelay) {
		this.messageDelay = messageDelay;
	}

	private static String getString(String key) {
		return Global.getSettings().getString("Event", "SUN_ICE_mission_" + key);
	}

	public static String getCutLink() {
		return Global.getSettings().getString("Event", "SUN_ICE_cutlink");
	}

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		SUN_ICE_ExileFleetManager manager = SUN_ICE_Data.getExileManager();
		if (manager == null) return false;

		MemoryAPI memory = memoryMap.get(MemKeys.FACTION);
		MemoryAPI memoryLocal = memoryMap.get(MemKeys.LOCAL);
		if (memoryLocal.contains(STATE_WHEN_DIALOG_STARTED)) {
			memoryLocal.unset(STATE_WHEN_DIALOG_STARTED);
		}

		MissionStage stage = getStage();
		memoryLocal.set(MISSION_STAGE_KEY, stage);

		TextPanelAPI textPanel = dialog.getTextPanel();
		textPanel.addPara(getString("entrance"));

		OptionPanelAPI optionPanel = dialog.getOptionPanel();
		optionPanel.clearOptions();

		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		PersonAPI missionGiver = getMissionGiver();

		switch (stage) {
			case STAGE_NOT_STARTED: {
				if (fleetStateCheck(dialog, manager) && repStateCheck(dialog, RepLevel.NEUTRAL)) {
					optionPanel.addOption(getString("option"), "init");
					return true;
				}
				break;
			}
			case STAGE_ONGOING_SUPPLIES:
			case STAGE_ONGOING_CORE:
			case STAGE_ONGOING_SHIPS:
			case STAGE_ONGOING_GOODS: {
				optionPanel.addOption(getString("option"), "init");
				break;
			}
			case STAGE_PREVIEW_CORE:
			case STAGE_PREVIEW_SHIPS:
			case STAGE_PREVIEW_GOODS: {
				if (fleetStateCheck(dialog, manager) && messageStateCheck(dialog)) {
					optionPanel.addOption(getString("option"), "init");
					return true;
				}
				break;
			}
			case STAGE_AFTER_ALL:
			case STAGE_FINALE: {
				textPanel.addPara(getString("after_all_missions"));
				optionPanel.addOption(getCutLink(), "cutCommLink");
				optionPanel.setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
				return true;
			}
			default:
				dialog.getTextPanel().addPara(getString("no_enough_reputation"));
				dialog.getOptionPanel().addOption(getCutLink(), "cutCommLink");
				dialog.getOptionPanel().setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
				break;
		}

		return true;
	}

	public static void startMission(int daysToStay) {
		PersonAPI missionGiver = getMissionGiver();
		Misc.makeImportant(missionGiver, MISSION_MAIN_KEY);

		if (daysToStay > 0) SUN_ICE_Data.getExileManager().getFakeAI().increaseDaysToStay(daysToStay);
	}

	public static void finishMission() {
		PersonAPI missionGiver = getMissionGiver();
		Misc.makeUnimportant(missionGiver, MISSION_MAIN_KEY);
	}

	public static void endGoToFleetIntel(boolean success) {
		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_GoToFleetIntel.class)) {
			SUN_ICE_GoToFleetIntel goToFleetIntel = (SUN_ICE_GoToFleetIntel)intel;
			if (goToFleetIntel.isEnded() || goToFleetIntel.isEnding()) continue;

			if (success) goToFleetIntel.performDelivery();
			else goToFleetIntel.forceFailed();
		}
	}

	public static void doomStages() {
		finishMission();
		Global.getSector().getMemoryWithoutUpdate().set(MISSION_MAIN_KEY, false);

		endGoToFleetIntel(false);
		for (EveryFrameScript s : Global.getSector().getScripts()) {
			if (s instanceof SUN_ICE_MissionManager) {
				((SUN_ICE_MissionManager)s).messageDelivered = true;
			}
		}

		Global.getSector().getMemoryWithoutUpdate().unset(SUN_ICE_ExileRemnantWarIntel.REMNANT_WAR_UNFINISHED_KEY);
		Global.getSector().getMemoryWithoutUpdate().unset(SUN_ICE_ExileRemnantWarIntel.REMNANT_WAR_FINISHED_KEY);
		Global.getSector().getMemoryWithoutUpdate().unset(SUN_ICE_ExileRemnantWarIntel.REMNANT_WAR_MUTED_KEY);
	}

	public static MissionStage getStage() {
		FactionAPI targetFaction = SUN_ICE_Data.getICE();

		Object tryStage = targetFaction.getMemoryWithoutUpdate().get(MISSION_STAGE_KEY);
		return tryStage == null ? MissionStage.STAGE_NOT_STARTED : (MissionStage)tryStage;
	}

	public static void setStage(MissionStage stage) {
		FactionAPI targetFaction = SUN_ICE_Data.getICE();
		targetFaction.getMemoryWithoutUpdate().set(MISSION_STAGE_KEY, stage);
	}

	public static int getSpecialTagCompletedCount() {
		FactionAPI targetFaction = SUN_ICE_Data.getICE();
		Object tryAmount = targetFaction.getMemoryWithoutUpdate().get(MISSION_SPECIAL_TAG_KEY);
		int stage = tryAmount == null ? 0 : (Integer)tryAmount;

		return stage;
	}

	public static boolean isAllSpecialTagsCompleted() {
		return getSpecialTagCompletedCount() >= 4;
	}

	public static void evolveSpecialTag() {
		FactionAPI targetFaction = SUN_ICE_Data.getICE();
		int stage = getSpecialTagCompletedCount();

		int newStage = stage + 1;
		targetFaction.getMemoryWithoutUpdate().set(MISSION_SPECIAL_TAG_KEY, newStage);
	}

	public static void setKey(String key) {
		FactionAPI targetFaction = SUN_ICE_Data.getICE();
		targetFaction.getMemoryWithoutUpdate().set(key, true);
	}

	public static PersonAPI getMissionGiver() {
		return getMissionGiver(Ranks.AGENT, Ranks.POST_BASE_COMMANDER);
	}

	public static PersonAPI getMissionGiver(String rank, String post) {

		PersonAPI preload = Global.getSector().getImportantPeople().getPerson(MISSION_GIVER_KEY);
		if (preload != null) return preload;

		PersonAPI person = SUN_ICE_Data.getICE().createRandomPerson(FullName.Gender.MALE);
		person.setId(MISSION_GIVER_KEY);
		person.setName(new FullName("?", "", FullName.Gender.MALE));
		person.setPortraitSprite("graphics/sun_ice/portraits/jay.png");

		person.setRankId(rank);
		person.setPostId(post);

		person.setVoice(Voices.SPACER);
		person.setImportance(PersonImportance.MEDIUM);
		person.getStats().setSkillLevel("sun_ice_consumables_backcycling", 1);
		person.getStats().setSkillLevel("sun_ice_kermess_space", 1);

		person.getMemoryWithoutUpdate().set(MISSION_GIVER_KEY, true);
		Global.getSector().getImportantPeople().addPerson(person);
		// Global.getSector().getImportantPeople().excludeFromGetPerson(person);
		Global.getSector().getImportantPeople().checkOutPerson(person, "permanent_staff");

		return person;
	}

	public static PersonAPI getPriest() {

		PersonAPI preload = Global.getSector().getImportantPeople().getPerson(MISSION_PRIEST_KEY);
		if (preload != null) return preload;

		PersonAPI person = SUN_ICE_Data.getICE().createRandomPerson(FullName.Gender.MALE);
		person.setId(MISSION_PRIEST_KEY);
		person.setName(new FullName("Priest", "Silver", FullName.Gender.MALE));
		person.setPortraitSprite("graphics/sun_ice/portraits/priest.png");

		person.setRankId(Ranks.SPACE_ADMIRAL);
		person.setPostId(Ranks.POST_FLEET_COMMANDER);

		person.setPersonality(Personalities.AGGRESSIVE);
		person.setVoice(Voices.OFFICIAL);
		person.setImportance(PersonImportance.HIGH);
		person.getMemoryWithoutUpdate().set(MISSION_PRIEST_KEY, true);
		person.getStats().setLevel(7);
		person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
		person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
		person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
		person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
		person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
		person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
		person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);

		Global.getSector().getImportantPeople().addPerson(person);
		// Global.getSector().getImportantPeople().excludeFromGetPerson(person);
		Global.getSector().getImportantPeople().checkOutPerson(person, "permanent_staff");

		return person;
	}

	private boolean fleetStateCheck(InteractionDialogAPI dialog, SUN_ICE_ExileFleetManager manager) {
		if (manager.getCurrentState() != SUN_ICE_ExileFleetFakeAI.ExileState.STAY) {
			dialog.getTextPanel().addPara(getString("state_incorrect"));
			dialog.getOptionPanel().addOption(getCutLink(), "cutCommLink");
			dialog.getOptionPanel().setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
			return false;
		}

		return true;
	}

	private boolean messageStateCheck(InteractionDialogAPI dialog) {

		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(SUN_ICE_GoToFleetIntel.class)) {
			if (!intel.isEnded() && !intel.isEnding()) return true;
		}

		dialog.getTextPanel().addPara(getString("no_enough_reputation"));
		dialog.getOptionPanel().addOption(getCutLink(), "cutCommLink");
		dialog.getOptionPanel().setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
		return false;
	}

	private boolean repStateCheck(InteractionDialogAPI dialog, RepLevel repLevel) {
		FactionAPI playerFaction = Global.getSector().getPlayerFaction();
		FactionAPI targetFaction = SUN_ICE_Data.getICE();

		if (targetFaction.isAtWorst(playerFaction, repLevel)) {
			return true;
		}

		dialog.getTextPanel().addPara(getString("no_enough_reputation"));
		dialog.getOptionPanel().addOption(getCutLink(), "cutCommLink");
		dialog.getOptionPanel().setShortcut("cutCommLink", Keyboard.KEY_ESCAPE, false, false, false, false);
		return false;
	}

	public enum MissionStage {
		STAGE_NOT_STARTED(0),
		STAGE_ONGOING_SUPPLIES(0),
		STAGE_AFTER_SUPPLIES(1),
		STAGE_PREVIEW_CORE(1),
		STAGE_ONGOING_CORE(1),
		STAGE_AFTER_CORE(2),
		STAGE_PREVIEW_SHIPS(2),
		STAGE_ONGOING_SHIPS(2),
		STAGE_AFTER_SHIPS(3),
		STAGE_PREVIEW_GOODS(3),
		STAGE_ONGOING_GOODS(3),
		STAGE_AFTER_ALL(4),
		STAGE_FINALE(4); // means all special tag completed and remnant war finished

		private final int completedAmount;

		MissionStage(int completedAmount) {
			this.completedAmount = completedAmount;
		}

		public int getCompleted() {
			return completedAmount;
		}
	}
}