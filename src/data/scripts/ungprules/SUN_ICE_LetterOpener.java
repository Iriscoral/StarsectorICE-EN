package data.scripts.ungprules;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_LetterOpener extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

	private static final float ANGLE_THRESHOLD = 30f;
    private float damageBonus = 1f;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        damageBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(20f, 30f);
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
		if (index == 0) return getFactorString(ANGLE_THRESHOLD);
        if (index == 1) return getPercentString(getValueByDifficulty(0, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {}

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {}

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    	if (!ship.isAlive() || ship.isDrone() || ship.isFighter()) return;

    	if (!ship.hasListenerOfClass(OpenerListener.class)) {
			ship.addListener(new OpenerListener());
		}
	}

	private class OpenerListener implements DamageTakenModifier {

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

			float angle = VectorUtils.getAngle(target.getLocation(), point);
			float diff = Math.abs(MathUtils.getShortestRotation(target.getFacing(), angle));
			diff -= ANGLE_THRESHOLD;

			if (diff <= 0f) return null;

			float damageIncrease = diff / (180f - ANGLE_THRESHOLD) * damageBonus;
			damage.getModifier().modifyPercent(buffID, damageIncrease);

			return buffID;
		}
	}
}