package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unchecked")
public abstract class SUN_ICE_BaseMissileAI implements MissileAIPlugin, GuidedMissileAI {
	public static final float DEFAULT_FLARE_VULNERABILITY_RANGE = 500f;
	private static final float DEFAULT_FACING_THRESHHOLD = 5f;

	@Override
	public CombatEntityAPI getTarget() {
		return target;
	}

	@Override
	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}

	protected MissileAPI missile;
	protected CombatEntityAPI target;
	protected final SUN_ICE_IntervalTracker circumstanceEvaluationTimer = new SUN_ICE_IntervalTracker(0.05f, 0.15f);

	protected CombatEntityAPI findFlareTarget(float range) {
		List<MissileAPI> flares = new ArrayList<>();

		for (MissileAPI m : AIUtils.getNearbyEnemyMissiles(missile, range)) {
			if (m.isFlare()) {
				flares.add(m);
			}
		}

		return target = (flares.isEmpty()) ? target : flares.get((new Random()).nextInt(flares.size()));

	}

	protected CombatEntityAPI findTarget() {
		findFlareTarget(DEFAULT_FLARE_VULNERABILITY_RANGE);

		if (targetIsFlare()) {
			return target;
		}

		target = missile.getSource().getShipTarget();
		if (target == null || !((ShipAPI) target).isAlive() || target.getOwner() == missile.getOwner()) {
			target = AIUtils.getNearestEnemy(missile);
		}

		return target;
	}

	public boolean isFacing(CombatEntityAPI target) {
		return isFacing(target.getLocation(), DEFAULT_FACING_THRESHHOLD);
	}

	public boolean isFacing(CombatEntityAPI target, float threshholdDegrees) {
		return isFacing(target.getLocation(), threshholdDegrees);
	}

	protected boolean isFacing(Vector2f point) {
		return isFacing(point, DEFAULT_FACING_THRESHHOLD);
	}

	private boolean isFacing(Vector2f point, float threshholdDegrees) {
		return (Math.abs(getAngleTo(point)) <= threshholdDegrees);
	}

	protected float getAngleTo(CombatEntityAPI entity) {
		return getAngleTo(entity.getLocation());
	}

	protected float getAngleTo(Vector2f point) {
		float angleTo = VectorUtils.getAngle(missile.getLocation(), point);
		return MathUtils.getShortestRotation(missile.getFacing(), angleTo);
	}

	protected boolean targetIsFlare() {
		return (target instanceof MissileAPI) && ((MissileAPI) target).isFlare() && !((MissileAPI) target).isFizzling() && !((MissileAPI) target).isFading();
	}

	protected void evaluateCircumstances() {
		findTarget();
	}

	private ShipCommand strafe(float degreeAngle, boolean strafeAway) {
		float angleDif = MathUtils.getShortestRotation(missile.getFacing(), degreeAngle);

		if ((!strafeAway && Math.abs(angleDif) < DEFAULT_FACING_THRESHHOLD) || (strafeAway && Math.abs(angleDif) > 180f - DEFAULT_FACING_THRESHHOLD)) {
			return null;
		}

		ShipCommand direction = (angleDif > 0f ^ strafeAway) ? ShipCommand.STRAFE_LEFT : ShipCommand.STRAFE_RIGHT;
		missile.giveCommand(direction);

		return direction;
	}

	private ShipCommand strafe(Vector2f location, boolean strafeAway) {
		return strafe(VectorUtils.getAngle(missile.getLocation(), location), strafeAway);
	}

	public ShipCommand strafe(CombatEntityAPI entity, boolean strafeAway) {
		return strafe(entity.getLocation(), strafeAway);
	}

	private ShipCommand strafeToward(float degreeAngle) {
		return strafe(degreeAngle, false);
	}

	protected ShipCommand strafeToward(Vector2f location) {
		return strafeToward(VectorUtils.getAngle(missile.getLocation(), location));
	}

	protected ShipCommand strafeToward(CombatEntityAPI entity) {
		return strafeToward(entity.getLocation());
	}

	private ShipCommand strafeAway(float degreeAngle) {
		return strafe(degreeAngle, true);
	}

	private ShipCommand strafeAway(Vector2f location) {
		return strafeAway(VectorUtils.getAngle(missile.getLocation(), location));
	}

	protected ShipCommand strafeAway(CombatEntityAPI entity) {
		return strafeAway(entity.getLocation());
	}

	private ShipCommand turn(float degreeAngle, boolean turnAway) {
		float angleDif = MathUtils.getShortestRotation(missile.getFacing(), degreeAngle);

		if ((!turnAway && Math.abs(angleDif) < DEFAULT_FACING_THRESHHOLD) || (turnAway && Math.abs(angleDif) > 180f - DEFAULT_FACING_THRESHHOLD)) {
			return null;
		}

		ShipCommand direction = (angleDif > 0f ^ turnAway) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
		missile.giveCommand(direction);

		return direction;
	}

	private ShipCommand turn(Vector2f location, boolean strafeAway) {
		return turn(VectorUtils.getAngle(missile.getLocation(), location), strafeAway);
	}

	public ShipCommand turn(CombatEntityAPI entity, boolean strafeAway) {
		return turn(entity.getLocation(), strafeAway);
	}

	private ShipCommand turnToward(float degreeAngle) {
		return turn(degreeAngle, false);
	}

	protected ShipCommand turnToward(Vector2f location) {
		return turnToward(VectorUtils.getAngle(missile.getLocation(), location));
	}

	protected ShipCommand turnToward(CombatEntityAPI entity) {
		return turnToward(entity.getLocation());
	}

	private ShipCommand turnAway(float degreeAngle) {
		return turn(degreeAngle, true);
	}

	private ShipCommand turnAway(Vector2f location) {
		return turnAway(VectorUtils.getAngle(missile.getLocation(), location));
	}

	protected ShipCommand turnAway(CombatEntityAPI entity) {
		return turnAway(entity.getLocation());
	}

	protected void accelerate() {
		missile.giveCommand(ShipCommand.ACCELERATE);
	}

	public void accelerateBackward() {
		missile.giveCommand(ShipCommand.ACCELERATE_BACKWARDS);
	}

	protected void decelerate() {
		missile.giveCommand(ShipCommand.DECELERATE);
	}

	public void turnLeft() {
		missile.giveCommand(ShipCommand.TURN_LEFT);
	}

	public void turnRight() {
		missile.giveCommand(ShipCommand.TURN_RIGHT);
	}

	public void strafeLeft() {
		missile.giveCommand(ShipCommand.STRAFE_LEFT);
	}

	public void strafeRight() {
		missile.giveCommand(ShipCommand.STRAFE_RIGHT);
	}

	protected SUN_ICE_BaseMissileAI() {
	}

	protected SUN_ICE_BaseMissileAI(MissileAPI missile) {
		this.missile = missile;
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (circumstanceEvaluationTimer.intervalElapsed(engine)) {
			evaluateCircumstances();
		}
	}
}