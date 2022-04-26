package data.scripts.ungprules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

import java.util.ArrayList;
import java.util.List;

import static data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel.RuleMessage;

public class SUN_ICE_AbnormalAnchor extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

	private static final float STRIKE_CHANCE_PER_DAY = 0.15f;

    private float crReduceMult;
    private float damageChanceMult;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        crReduceMult = getValueByDifficulty(0, difficulty);
        damageChanceMult = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.2f, 0.4f);
        if (index == 1) return difficulty.getLinearValue(0.5f, 1f);
        return 0f;
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneDayPassed()) {

        	if (Math.random() > STRIKE_CHANCE_PER_DAY) return;

            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

			List<FleetMemberAPI> phaseMembers = new ArrayList<>();
			List<FleetMemberAPI> nonPhaseMembers = new ArrayList<>();
			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				if (member.isFighterWing()) continue;

				if (SUN_ICE_IceUtils.isConsideredPhaseShip(member)) {
					if (member.canBeDeployedForCombat()) phaseMembers.add(member);
				} else {
					nonPhaseMembers.add(member);
				}
			}

			if (phaseMembers.isEmpty() || nonPhaseMembers.isEmpty()) return;

			float totalReduced = 0f;
			for (FleetMemberAPI damageSource : phaseMembers) {

				float deployPoint = damageSource.getDeploymentPointsCost();
				if (Math.random() * damageChanceMult > deployPoint / 75f) continue;

				for (FleetMemberAPI nonPhaseMember : nonPhaseMembers) {

					float victimPoint = nonPhaseMember.getDeploymentPointsCost();
					if (Math.random() > victimPoint / 50f) continue;

					float crToReduce = crReduceMult * deployPoint / victimPoint;
					crToReduce = Math.min(crToReduce, nonPhaseMember.getRepairTracker().getCR());

					nonPhaseMember.getRepairTracker().applyCREvent(-crToReduce, rule.getName());
					totalReduced += crToReduce;
				}
			}

			if (totalReduced <= 0f) return;

			RuleMessage message = new RuleMessage(rule, rule.getExtra1(), getFactorString(totalReduced * 100f));
			message.send();
        }
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getPercentString(STRIKE_CHANCE_PER_DAY * 100f);
		if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }
}