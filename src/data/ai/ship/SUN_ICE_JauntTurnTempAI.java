package data.ai.ship;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.tools.SUN_ICE_JauntSession;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_JauntTurnTempAI extends SUN_ICE_BaseShipAI {

	private final ShipAPI target;
	private final SUN_ICE_JauntSession jaunt;

	public SUN_ICE_JauntTurnTempAI(ShipAPI ship, ShipAPI target, SUN_ICE_JauntSession jaunt) {
		super(ship);
		this.target = target;
		this.jaunt = jaunt;

		//SunUtils.print(ship, "on");
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		if (ship == null) {
			return;
		}

		if ((!jaunt.isWarping() && !jaunt.isReturning()) || target == null || !SUN_ICE_JauntSession.hasSession(ship)) {
			//if(false) {
			ship.resetDefaultAI();
			//SunUtils.print(ship, "off");
		} else {
			Vector2f to = jaunt.isReturning() ? jaunt.getOrigin() : jaunt.getDestination();
			//            turnToward(VectorUtils.getAngle(to, target.getLocation()));
			//            ship.setFacing(VectorUtils.getAngle(to, target.getLocation()));
			//            turnToward(VectorUtils.getAngle(ship.getLocation(), Global.getCombatEngine().getPlayerShip().getMouseTarget()));

			fakeTurnToAngle(VectorUtils.getAngle(to, target.getLocation()), amount);

			//            IceUtils.blink(to);
			//            IceUtils.blink(target.getLocation());
		}
	}

	// Can't get ship to turn as quickly as it should during warp for some reason.
	// This is my hacky workaround.
	private float angleVel = Float.NaN;

	private void fakeTurnToAngle(float degrees, float amount) {
		if (Float.isNaN(angleVel)) {
			angleVel = ship.getAngularVelocity();
		}

		float angleDif = MathUtils.getShortestRotation(ship.getFacing(), degrees);
		float secondsTilDesiredFacing = angleDif / ship.getAngularVelocity();
		boolean goLeft = angleDif > 0f;
		if (secondsTilDesiredFacing > 0f) {
			float turnAcc = ship.getMutableStats().getTurnAcceleration().getModifiedValue();
			float rotValWhenAt = Math.abs(ship.getAngularVelocity()) - secondsTilDesiredFacing * turnAcc;
			if (rotValWhenAt > 0) {
				goLeft = !goLeft;
			}
		}

		float turnAcc = ship.getMutableStats().getTurnAcceleration().getModifiedValue();
		float maxTurn = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
		float dAngleVel = turnAcc * (goLeft ? 1f : -1f) * amount;
		float newAngleVel = angleVel + dAngleVel;

		angleVel = Math.max(-maxTurn, Math.min(maxTurn, newAngleVel));
		ship.setAngularVelocity(angleVel);
		ship.setFacing(ship.getFacing() + angleVel * amount);
	}
}