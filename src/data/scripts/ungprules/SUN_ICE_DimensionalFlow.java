package data.scripts.ungprules;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class SUN_ICE_DimensionalFlow extends UNGP_MemberBuffRuleEffect {

    private float phaseBonus = 1f;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        phaseBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(30f, 20f);
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

	@Override
	public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
		MutableShipStatsAPI stats = member.getStats();

		stats.getDynamic().getStat(Stats.PHASE_TIME_BONUS_MULT).modifyPercent(buffID, phaseBonus);
	}

	@Override
	public boolean canApply(FleetMemberAPI member) {
		return member.isFlagship();
	}
}