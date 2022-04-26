package data.scripts.campaign.intel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.raid.AssembleStage;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.SUN_ICE_MissionRemnantWarPreview;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.world.SUN_ICE_ExileFleetManager;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SUN_ICE_ExileRemnantWarIntel extends BaseIntelPlugin {

	private final SUN_ICE_ExileFleetManager manager;
	private final CampaignFleetAPI exiles;
	private final StarSystemAPI system;

	public static final String REMNANT_WAR_UNFINISHED_KEY = "$SUN_ICE_RemnantWarUnfinished";
	public static final String REMNANT_WAR_FINISHED_KEY = "$SUN_ICE_RemnantWarFinished";
	public static final String REMNANT_WAR_MUTED_KEY = "$SUN_ICE_RemnantWarMuted";
	public static final String REMNANT_WAR_ATTACKER_KEY = "$SUN_ICE_RemnantWarFleet";

	public final List<CampaignFleetAPI> remnantBattleGroup = new ArrayList<>();
	private float initialRemnantPoints = -1f;
	private float timeLeft = 60f;
	private float repGot = 0f;
	private boolean succeed = false;
	private boolean shownPreview = false;

	public SUN_ICE_ExileRemnantWarIntel(SUN_ICE_ExileFleetManager manager, CampaignFleetAPI exiles) {
		this.manager = manager;
		this.exiles = exiles;
		this.system = manager.getCurrentSystemDestination();

		setImportant(true);
		Global.getSector().addScript(this);
		Global.getSector().getIntelManager().addIntel(this, false);
		Global.getLogger(SUN_ICE_ExileRemnantWarIntel.class).info("Exile Fleet Remnant War Intel Active.");
	}

	private boolean isMissionValid() {
		return exiles != null && exiles.isAlive() && exiles.getMarket() != null && exiles.getFaction().getId().contentEquals("sun_ice") && exiles.getFaction().isAtWorst(Global.getSector().getPlayerFaction(), RepLevel.NEUTRAL) && timeLeft > 0f;
	}

	private boolean isMissionSucceed() {
		return succeed;
	}

	@Override
	public String getCommMessageSound() {
		return "ui_intel_distress_call";
	}

	@Override
	public void advanceImpl(float amount) {
		float days = Global.getSector().getClock().convertToDays(amount);
		CampaignFleetAPI player = Global.getSector().getPlayerFleet();

		if (!shownPreview && !exiles.isInHyperspace()) {
			Global.getSector().getCampaignUI().showInteractionDialog(new SUN_ICE_MissionRemnantWarPreview(exiles), null);
			shownPreview = true;
		}

		if (!isMissionSucceed() && player.getContainingLocation() != exiles.getContainingLocation()) {
			timeLeft -= days;
		}

		if (player.getContainingLocation() == exiles.getContainingLocation() && !player.getContainingLocation().isHyperspace()) {
			if (initialRemnantPoints < 0f) {
				initialRemnantPoints = spawnRemnantBattleGroup(exiles, 300f);

				exiles.getStats().getFleetwideMaxBurnMod().modifyMult("REM_WAR", 0.2f);
				exiles.getFaction().setRelationship("remnant", RepLevel.INHOSPITABLE);
				Global.getSector().getMemoryWithoutUpdate().set(REMNANT_WAR_UNFINISHED_KEY, true);
			}
		}

		if (!succeed && !remnantBattleGroup.isEmpty()) {
			float currentRemnantPoints = 0f;
			for (CampaignFleetAPI remnant : remnantBattleGroup) {
				currentRemnantPoints += remnant.getFleetPoints();
			}

			if (currentRemnantPoints < initialRemnantPoints * 0.25f) {
				succeed = true;

				Misc.makeImportant(exiles, "REM_WAR");
				SUN_ICE_MissionManager.startMission(-1);
				Global.getSector().getMemoryWithoutUpdate().set(REMNANT_WAR_UNFINISHED_KEY, false);
				Global.getSector().getMemoryWithoutUpdate().set(REMNANT_WAR_FINISHED_KEY, true);
				Global.getSector().getMemoryWithoutUpdate().set(REMNANT_WAR_MUTED_KEY, false);
			}
		}

		if (!isMissionValid()) {
			SUN_ICE_MissionManager.doomStages();
			createPenaltyResult();
			endAfterDelay();
		}
	}

	public void performDelivery(InteractionDialogAPI dialog) {
		float repAmount = 0.2f;
		CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_SUCCESS, completionRep, null, dialog.getTextPanel(), true, true), getFactionForUIColors().getId());

		exiles.getStats().getFleetwideMaxBurnMod().unmodifyMult("REM_WAR");

		manager.getFakeAI().forceMove();
		repGot = repAmount;
		endAfterDelay();
	}

	private void createPenaltyResult() {
		float repAmount = 0.05f;
		CoreReputationPlugin.MissionCompletionRep completionRep = new CoreReputationPlugin.MissionCompletionRep(repAmount, RepLevel.COOPERATIVE, -repAmount, RepLevel.INHOSPITABLE);
		Global.getSector().adjustPlayerReputation(new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_FAILURE, completionRep, null, null, true, false), getFactionForUIColors().getId());

		Global.getLogger(SUN_ICE_ExileRemnantWarIntel.class).info("Exile Fleet Remnant War Doomed.");
		manager.killExiledFleet();

		for (CampaignFleetAPI fleet : remnantBattleGroup) {
			fleet.clearAssignments();
		}
	}

	@Override
	public String getSortString() {
		return getString("title");
	}

	@Override
	public String getSmallDescriptionTitle() {
		if (isEnded() || isEnding()) {
			return getString("title_finished");
		}
		return getSortString();
	}

	@Override
	public String getName() {
		return getSmallDescriptionTitle();
	}

	@Override
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {

		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;

		if (mode == ListInfoMode.IN_DESC) {
			initPad = opad;
		}
		FactionAPI faction = getFactionForUIColors();

		bullet(info);
		if (isUpdate) {
			// 3 possible updates: de-posted/expired, failed, completed
			if (!isMissionValid()) {
				return;
			} else if (isEnding()) {
				if (repGot > 0f) {
					CoreReputationPlugin.addAdjustmentMessage(repGot, faction, null, null, null, info, tc, isUpdate, 0f);
				}
			}
		} else {
			// either in small description, or in tooltip/intel list
			if (isEnding()) {
				initPad = 0f;
				if (repGot > 0f) {
					CoreReputationPlugin.addAdjustmentMessage(repGot, faction, null, null, null, info, tc, isUpdate, initPad);
				}
			} else if (isMissionValid()) {
				if (mode != ListInfoMode.IN_DESC) {
					info.addPara(getString("location"), initPad, tc, h, system.getNameWithLowercaseType());
					initPad = 0f;
				}

				LabelAPI label = info.addPara(String.format(getString("brief"), system.getNameWithLowercaseType(), exiles.getName()), initPad, tc);
				label.setHighlight(system.getNameWithLowercaseType(), exiles.getName());
				label.setHighlightColors(h, exiles.getFaction().getBaseUIColor());
				info.addPara(getString("hint"), tc, 0f);

				CampaignFleetAPI player = Global.getSector().getPlayerFleet();
				if (player.getContainingLocation() != exiles.getContainingLocation()) {
					addDays(info, getString("to_respond"), timeLeft, tc, 0f);
				}
			}
		}

		unindent(info);
	}

	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.addPara(getSmallDescriptionTitle(), c, 0f);
		addBulletPoints(info, mode);
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;

		info.addImage(manager.getICE().getLogo(), width, 128, opad);

		addBulletPoints(info, ListInfoMode.IN_DESC);

		if (!isMissionValid()) {
			info.addPara(getString("failed"), opad);
		} else if (isEnding()) {
			info.addPara(getString("succeed"), opad);
		} else {

			FactionAPI remnant = Global.getSector().getFaction(Factions.REMNANTS);
			info.addPara(getString("information_1"), opad, tc, remnant.getBaseUIColor(), remnant.getDisplayName());
			info.addPara(getString("information_2"), opad, tc, exiles.getFaction().getBaseUIColor(), exiles.getName());
		}
	}

	private String getString(String key) {
		return Global.getSettings().getString("Intel", "SUN_ICE_remnant_war_" + key);
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return system.getHyperspaceAnchor();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_STORY);
		tags.add(getFactionForUIColors().getId());
		return tags;
	}

	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("intel", "sun_ice_exile");
	}

	@Override
	public FactionAPI getFactionForUIColors() {
		return exiles.getFaction();
	}

	private static float getSub(float x) {
		float res = x;
		if (x > 80) {
			res += getSub(x * 0.7f);
			res += getSub(x * 0.4f);
		}
		return res;
	}

	public static void maiin(String[] args) {
		float x = 300f;
		System.out.println(getSub(x));
	}

	private float spawnRemnantBattleGroup(CampaignFleetAPI victim, float fp) {
		float finalPoints = 0f;
		if (fp > 80f) {
			finalPoints += spawnRemnantBattleGroup(victim, fp * 0.7f);
			finalPoints += spawnRemnantBattleGroup(victim, fp * 0.4f);
		}

		FleetFactory.PatrolType type;
		if (fp < AssembleStage.FP_SMALL * 1.5f) {
			type = FleetFactory.PatrolType.FAST;
		} else if (fp < AssembleStage.FP_MEDIUM * 1.5f) {
			type = FleetFactory.PatrolType.COMBAT;
		} else {
			type = FleetFactory.PatrolType.HEAVY;
		}

		FleetParamsV3 params = new FleetParamsV3(null, victim.getLocation(), Factions.REMNANTS, null, type.getFleetType(), fp, // combatPts
				0f, // freighterPts
				0f, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0.5f // qualityMod
		);

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		if (fleet.isEmpty()) {
			fleet = null;
		}

		if (fleet != null) {
			fleet.setTransponderOn(false);
			fleet.getStats().getDetectedRangeMod().modifyFlat("REM", 2000f);

			fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, false); // let's see these 2's co-work
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_STICK_WITH_PLAYER_IF_ALREADY_TARGET, false);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
			fleet.getMemoryWithoutUpdate().set(REMNANT_WAR_ATTACKER_KEY, true);

			Vector2f loc = Misc.getPointAtRadius(victim.getLocation(), 400f + (float) Math.random() * 400f);
			victim.getContainingLocation().addEntity(fleet);
			fleet.setLocation(loc.x, loc.y);

			float timeForWait = 2f + (float)Math.random() * 2f;
			fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, victim, timeForWait, new AttackTargetScript(fleet, victim));
			fleet.addScript(new AttackTargetEveryFrameScript(fleet, victim));

			finalPoints += fleet.getFleetPoints();
			remnantBattleGroup.add(fleet);
		}

		Global.getLogger(this.getClass()).info("Remnant war fleet spawning, current stage: " + finalPoints);
		return finalPoints;
	}

	public static class AttackTargetScript implements Script {

		private final CampaignFleetAPI fleet;
		private final CampaignFleetAPI target;

		public AttackTargetScript(CampaignFleetAPI fleet, CampaignFleetAPI target) {
			this.fleet = fleet;
			this.target = target;
		}

		@Override
		public void run() {
			if (fleet == null || !fleet.isAlive()) return;
			if (target == null || !target.isAlive()) return;

			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, false);

			if (Global.getSector().getMemoryWithoutUpdate().getBoolean(REMNANT_WAR_UNFINISHED_KEY) &&
					!Global.getSector().getMemoryWithoutUpdate().getBoolean(REMNANT_WAR_FINISHED_KEY)) {

				Global.getSector().getMemoryWithoutUpdate().set(REMNANT_WAR_MUTED_KEY, true);
				fleet.getFaction().setRelationship(target.getFaction().getId(), RepLevel.VENGEFUL);
			}
		}
	}

	public static class AttackTargetEveryFrameScript implements EveryFrameScript {

		private final CampaignFleetAPI fleet;
		private final CampaignFleetAPI target;

		public AttackTargetEveryFrameScript(CampaignFleetAPI fleet, CampaignFleetAPI target) {
			this.fleet = fleet;
			this.target = target;
		}

		@Override
		public boolean isDone() {
			if (fleet == null || !fleet.isAlive()) return true;
			if (target == null || !target.isAlive()) return true;
			return false;
		}

		@Override
		public boolean runWhilePaused() {
			return false;
		}

		@Override
		public void advance(float amount) {

			if (isDone()) return;
			if (fleet.getBattle() != null) return;

			boolean attacking = true;
			if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE)) {
				attacking = false;
			}

			if (attacking) {
				fleet.clearAssignments();
				fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, target, 10000f);

				fleet.setInteractionTarget(target);
			}

			SectorEntityToken action = fleet.getInteractionTarget();
			if (action != null && action != target) {
				if (fleet.getAI() != null) {
					fleet.getAI().doNotAttack(action, 5f);
				}
			}
		}
	}
}