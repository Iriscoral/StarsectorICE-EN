package data.ai.missile.abandoned;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.ai.missile.SUN_ICE_BaseMissileAI;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class SUN_ICE_AttackDroneMissileAI extends SUN_ICE_BaseMissileAI {
	private static final String WEAPON_ID = "sun_ice_pulserepeater";
	private static final String MISSILE_ID = "sun_ice_attackdrone";
	private static final float WEAPON_RANGE = 400f;
	private static final float WEAPON_RANGE_SQUARED = WEAPON_RANGE * WEAPON_RANGE;
	private static final float POTENTIAL_TARGET_RANGE = WEAPON_RANGE * 2f;
	private static final float WEAPON_COOLDOWN = 0.2f;
	private static final int MAX_AMMO = 1200;
	private static final int MAX_ACTIVE_DRONES = 3;

	private final Vector2f destOffset = new Vector2f();
	private final Vector2f dest = new Vector2f();
	private final List<CombatEntityAPI> potentialTargets = new ArrayList<>();
	private int ammo = MAX_AMMO;
	private float weaponCooldown = WEAPON_COOLDOWN;

	public SUN_ICE_AttackDroneMissileAI(MissileAPI missile) {
		super(missile);

		findTarget();
		this.circumstanceEvaluationTimer.setInterval(0.6f, 1.4f);

		int count = 0;

		Object[] missiles = Global.getCombatEngine().getMissiles().toArray();

		for (int i = missiles.length - 1; i >= 0; --i) {
			MissileAPI m = (MissileAPI) missiles[i];

			if (m.getProjectileSpecId().equals(MISSILE_ID) && m.getWeapon() == missile.getWeapon()) {
				++count;

				if (count >= MAX_ACTIVE_DRONES) {
					SUN_ICE_IceUtils.destroy(m);
				}
			}
		}

		//missile.setCollisionClass(CollisionClass.FIGHTER);
	}

	//    @Override
	//    public void findTarget() {
	//        target = missile.getSource().getShipTarget();
	//
	//        if(target == null || target.getOwner() != missile.getOwner())
	//            target = missile.getSource();
	//
	//        if(!target.isAlive())  target = AIUtils.getNearestAlly(missile);
	//    }

	@Override
	public CombatEntityAPI findTarget() {
		target = missile.getSource().getShipTarget();

		if (target == null || !((ShipAPI) target).isAlive() || target.getOwner() == missile.getOwner()) {
			target = AIUtils.getNearestEnemy(missile);
		}

		if (target == null) {
			target = missile.getSource();
		}

		return target;
	}

	@Override
	public void evaluateCircumstances() {
		super.evaluateCircumstances();

		//        if(target == null
		//                || (target instanceof ShipAPI && !((ShipAPI)target).isAlive())) {
		//            findTarget();
		//            return;
		//        }

		Vector2f.sub(MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius()), target.getLocation(), destOffset);

		if (missile.isFading() || ammo <= 0 || !missile.getSource().isAlive()) {
			SUN_ICE_IceUtils.destroy(missile);
		}

		potentialTargets.clear();
		potentialTargets.addAll(AIUtils.getNearbyEnemies(missile, POTENTIAL_TARGET_RANGE));
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		weaponCooldown = Math.max(0, weaponCooldown - amount);

		if (target == null) {
			return;
		}

		Vector2f.add(destOffset, target.getLocation(), dest);

		accelerate();
		turnToward(dest);

		if (ammo > 0 && weaponCooldown == 0) {
			CombatEntityAPI nearest = null;
			float record = Float.MAX_VALUE;

			for (CombatEntityAPI potentialTarget : potentialTargets) {

				float dist2 = MathUtils.getDistanceSquared(missile, potentialTarget);

				if (dist2 < record && dist2 <= WEAPON_RANGE_SQUARED) {
					record = dist2;
					nearest = potentialTarget;
				}
			}

			if (nearest != null) {
				Global.getCombatEngine().spawnProjectile(missile.getSource(), null, WEAPON_ID, missile.getLocation(), VectorUtils.getAngle(missile.getLocation(), nearest.getLocation()), new Vector2f());

				--ammo;
				weaponCooldown = WEAPON_COOLDOWN;
			}
		}
	}
}