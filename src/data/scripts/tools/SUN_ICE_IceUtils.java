package data.scripts.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.population.CoreImmigrationPluginImpl;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;

public class SUN_ICE_IceUtils {
	private static Map<String, Float> shieldUpkeeps;
	private static Map<String, String> systemIDs;
	private static final float SAFE_DISTANCE = 600f;
	private static final float DEFAULT_DAMAGE_WINDOW = 3f;
	private static final Map<HullSize, Float> baseOverloadTimes = new HashMap<>();

	static {
		baseOverloadTimes.put(HullSize.FIGHTER, 10f);
		baseOverloadTimes.put(HullSize.FRIGATE, 4f);
		baseOverloadTimes.put(HullSize.DESTROYER, 6f);
		baseOverloadTimes.put(HullSize.CRUISER, 8f);
		baseOverloadTimes.put(HullSize.CAPITAL_SHIP, 10f);
		baseOverloadTimes.put(HullSize.DEFAULT, 6f);
	}

	public static CampaignFleetAPI createFleet(float CombatPts, float FreighterPts, float TankerPts, float TransportPts, float LinerPts, float CivilianPts, float UtilityPts, MarketAPI source, String factionID, String swapFactionID, String fleetName, String memoryFleetType, String memorySpecial, boolean hasShalom) {

		FleetParamsV3 params = new FleetParamsV3(source, null, factionID, null, FleetTypes.PATROL_LARGE, CombatPts, FreighterPts, TankerPts, TransportPts, LinerPts, CivilianPts, UtilityPts);

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		if (fleet == null || fleet.getFleetPoints() < (CombatPts + FreighterPts) * 0.8f) {
			params.factionId = swapFactionID;
			params.doctrineOverride = Global.getSector().getFaction(factionID).getDoctrine();
			fleet = FleetFactoryV3.createFleet(params);
		}

		if (fleet == null) {
			return null;
		}

		fleet.setFaction(factionID);
		fleet.setName(fleetName);
		fleet.getMemoryWithoutUpdate().set("$fleetType", memoryFleetType);
		if (hasShalom) {
			Global.getLogger(SUN_ICE_IceUtils.class).info("Spawning shalom");
			fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_shalom_Standard"));
		}

		FleetFactoryV3.addCommanderAndOfficers(fleet, params, new Random());
		fleet.getCommander().setRankId("spaceMarshal");
		fleet.getCommanderStats().getCommandPoints().modifyMult("fleetMult", 300f);
		fleet.getMemoryWithoutUpdate().set(memorySpecial, Boolean.TRUE); // like $something

		fleet.getFleetData().sort();
		fleet.forceSync();
		return fleet;
	}

	public static float getEngineFractionDisabled(ShipAPI ship) {
		float maxThrust = 0;
		float onlineThrust = 0;

		for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
			if (engine.isSystemActivated()) {
				continue;
			} else if (!engine.isDisabled()) {
				onlineThrust += engine.getContribution();
			}

			maxThrust += engine.getContribution();
		}

		return onlineThrust / maxThrust;
	}

	public static void showHealText(ShipAPI anchor, Vector2f at, float repairAmount) {
		//Global.getCombatEngine().addFloatingDamageText(at, repairAmount,
		//		ICEModPlugin.HEAL_TEXT_COLOR, anchor, anchor);
	}

	public static void showText(CombatEntityAPI anchor, Vector2f at, float amount) {
		Global.getCombatEngine().addFloatingText(at, "" + amount, 25f, Color.RED, anchor, 1f, 1f);
	}

	public static void showText(CombatEntityAPI anchor, Vector2f at, String text) {
		Global.getCombatEngine().addFloatingText(at, text, 25f, Color.RED, anchor, 1f, 1f);
	}

	public static int getPlayerOrAllyOwner() {
		return 0;
	}

	public static boolean isPlayerOrAllyOwner(CombatEntityAPI entity) {
		return entity.getOwner() == getPlayerOrAllyOwner();
	}

	public static boolean isExactlyPlayerOwner(ShipAPI ship) {
		return ship.getOwner() == getPlayerOrAllyOwner() && !ship.isAlly();
	}

	public static boolean isPlayerShipOwner(ShipAPI ship) {
		return ship.getOwner() == Global.getCombatEngine().getPlayerShip().getOwner();
	}

	public static List<CombatEntityAPI> getCollideablesInRange(Vector2f at, float range) {
		List<CombatEntityAPI> retVal = new LinkedList<>(CombatUtils.getAsteroidsWithinRange(at, range));

		for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
			if (!ship.isFighter() && !ship.isDrone() && !ship.isPhased()) {
				retVal.add(ship);
			}
		}

		return retVal;
	}

	public static List<DamagingProjectileAPI> getProjectilesDamagedBy(ShipAPI ship) {
		List<DamagingProjectileAPI> retVal = new LinkedList<>();

		for (DamagingProjectileAPI p : CombatUtils.getProjectilesWithinRange(ship.getLocation(), ship.getCollisionRadius())) {
			if (p.didDamage() && p.getDamageTarget() == ship) {
				retVal.add(p);
			}
		}

		return retVal;
	}

	public static Vector2f getVectorFromDegrees(float degrees) {
		return new Vector2f((float) Math.cos(Math.toRadians(degrees)), (float) Math.sin(Math.toRadians(degrees)));
	}

	public static Vector2f getOffset(Vector2f from, float direction, float distance) {
		return getOffset(from, getVectorFromDegrees(direction), distance);
	}

	public static Vector2f getOffset(Vector2f from, Vector2f toward, float distance) {
		Vector2f retVal;
		retVal = VectorUtils.getDirectionalVector(from, toward);
		retVal.scale(distance);
		Vector2f.add(retVal, from, retVal);
		return retVal;
	}

	public static List<DamagingProjectileAPI> curveBullets(Vector2f at, float direction, float maxAngle, float forceMultiplier) {

		return curveBullets(at, direction, maxAngle, forceMultiplier, false, 1);
	}

	public static List<DamagingProjectileAPI> curveBullets(Vector2f at, float direction, float maxAngle, float forceMultiplier, boolean invert, float velocityMultiplier) {
		Collection<DamagingProjectileAPI> projectiles = Global.getCombatEngine().getProjectiles();

		List<DamagingProjectileAPI> undiflected = new LinkedList<>(projectiles);
		float amount = Global.getCombatEngine().getElapsedInLastFrame();

		for (DamagingProjectileAPI proj : projectiles) {
			// Make sure the projectile is moving in the opposite direction
			float angleDif = MathUtils.getShortestRotation(direction, MathUtils.clampAngle(proj.getFacing() + 180));
			if (Math.abs(angleDif) >= maxAngle) {
				continue;
			}

			// Make sure the projectile is within the cone of effect
			angleDif = MathUtils.getShortestRotation(direction, VectorUtils.getAngle(at, proj.getLocation()));
			if (Math.abs(angleDif) >= maxAngle) {
				continue;
			}

			// Deflected projectiles will not be checked during phase approval
			undiflected.remove(proj);

			// Calculate the angle by which to rotate the projectile
			float distance = MathUtils.getDistance(at, proj.getLocation());
			float force = (float) Math.pow(1 - Math.abs(angleDif) / maxAngle, 2) * (200 / (distance + 200)) * forceMultiplier;
			float dAngle = -Math.signum(angleDif) * force * amount;
			dAngle *= invert ? -1 : 1;

			// Rotate the facing and velocity of the projectile
			VectorUtils.rotate(proj.getVelocity(), dAngle, proj.getVelocity());
			proj.setFacing(MathUtils.clampAngle(proj.getFacing() + dAngle));
			proj.getVelocity().scale(velocityMultiplier);
		}

		return undiflected;
	}

	public static float estimateOptimalRange(ShipAPI ship) {
		float acc = 0, opAcc = 0;
		//        Map<WeaponType, Float> rangeBunuses = new HashMap();
		//        rangeBunuses.put(WeaponType.BALLISTIC, ship.getMutableStats().getBallisticWeaponRangeBonus().getBonusMult());
		//        rangeBunuses.put(WeaponType.ENERGY, ship.getMutableStats().getEnergyWeaponRangeBonus().getBonusMult());
		//        rangeBunuses.put(WeaponType.MISSILE, ship.getMutableStats().getMissileWeaponRangeBonus().getBonusMult());
		//        rangeBunuses.put(WeaponType., ship.getMutableStats().getBeamWeaponRangeBonus().getBonusMult());
		//        rangeBunuses.put(WeaponType.BALLISTIC, ship.getMutableStats().getBallisticWeaponRangeBonus().getBonusMult());

		for (WeaponAPI w : ship.getAllWeapons()) {
			float op = w.getSpec().getOrdnancePointCost(null);
			if (w.getDamageType() == DamageType.FRAGMENTATION) {
				op *= 0.2f;
			}
			opAcc += op;
			acc += op * w.getRange();

		}

		return acc / opAcc;
	}

	public static Vector2f getDirectionalVector(float degrees) {
		double radians = Math.toRadians(degrees);
		return new Vector2f((float) Math.cos(radians), (float) Math.sin(radians));
	}

	public static Vector2f getMidpoint(Vector2f from, Vector2f to, float d) {
		d *= 2;

		return new Vector2f((from.x * (2 - d) + to.x * d) / 2, (from.y * (2 - d) + to.y * d) / 2);
	}

	public static Vector2f getMidpoint(Vector2f from, Vector2f to) {
		return getMidpoint(from, to, 0.5f);
	}

	public static Vector2f toRelative(CombatEntityAPI entity, Vector2f point) {
		Vector2f retVal = new Vector2f(point);
		Vector2f.sub(retVal, entity.getLocation(), retVal);
		VectorUtils.rotate(retVal, -entity.getFacing(), retVal);
		return retVal;
	}

	public static Vector2f toAbsolute(CombatEntityAPI entity, Vector2f point) {
		Vector2f retVal = new Vector2f(point);
		VectorUtils.rotate(retVal, entity.getFacing(), retVal);
		Vector2f.add(retVal, entity.getLocation(), retVal);
		return retVal;
	}

	public static void blink(Vector2f at) {
		Global.getCombatEngine().addHitParticle(at, new Vector2f(), 30, 1, 0.1f, Color.RED);
	}

	public static List<ShipAPI> getShipsOnSegment(Vector2f from, Vector2f to) {
		float distance = MathUtils.getDistance(from, to);
		Vector2f center = new Vector2f();
		center.x = (from.x + to.x) / 2;
		center.y = (from.y + to.y) / 2;

		List<ShipAPI> list = new ArrayList<>();

		for (ShipAPI s : CombatUtils.getShipsWithinRange(center, distance / 2)) {
			if (CollisionUtils.getCollisionPoint(from, to, s) != null) {
				list.add(s);
			}
		}

		return list;
	}

	public static ShipAPI getFirstShipOnSegment(Vector2f from, Vector2f to, CombatEntityAPI exception) {
		ShipAPI winner = null;
		float record = Float.MAX_VALUE;

		for (ShipAPI s : getShipsOnSegment(from, to)) {
			if (s == exception) {
				continue;
			}

			float dist2 = MathUtils.getDistanceSquared(s, from);

			if (dist2 < record) {
				record = dist2;
				winner = s;
			}
		}

		return winner;
	}

	public static ShipAPI getFirstShipOnSegment(Vector2f from, Vector2f to) {
		return getFirstShipOnSegment(from, to, null);
	}

	public static ShipAPI getShipInLineOfFire(WeaponAPI weapon) {
		Vector2f endPoint = weapon.getLocation();
		endPoint.x += Math.cos(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange();
		endPoint.y += Math.sin(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange();

		return getFirstShipOnSegment(weapon.getLocation(), endPoint, weapon.getShip());
	}

	@Deprecated
	public static float getShieldUpkeep(ShipAPI ship) throws JSONException, IOException {
		if (shieldUpkeeps == null) {
			shieldUpkeeps = new HashMap<>();

			JSONArray j = Global.getSettings().getMergedSpreadsheetDataForMod("id", "data/hulls/ship_data.csv", "starsector-core");
			for (int i = 0; i < j.length(); ++i) {
				JSONObject s = j.getJSONObject(i);
				String id = s.getString("id");

				if (id == null || id.isEmpty()) continue;

				float upkeep = (float) s.getDouble("shield upkeep");
				shieldUpkeeps.put(id, upkeep);
			}
		}

		return shieldUpkeeps.get(ship.getHullSpec().getBaseHullId());
	}

	public static String getSystemID(String hullID) throws JSONException, IOException {
		if (systemIDs == null) {
			systemIDs = new HashMap<>();

			JSONArray j = Global.getSettings().getMergedSpreadsheetDataForMod("id", "data/hulls/ship_data.csv", "starsector-core");
			for (int i = 0; i < j.length(); ++i) {
				JSONObject s = j.getJSONObject(i);
				String id = s.getString("id");

				if (id == null || id.isEmpty()) continue;

				String systemID = s.getString("system id");
				systemIDs.put(id, systemID);
			}
		}

		return systemIDs.get(hullID);
	}

	public static float getArmorPercent(ShipAPI ship) {
		float acc = 0;
		int width = ship.getArmorGrid().getGrid().length;
		int height = ship.getArmorGrid().getGrid()[0].length;

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				acc += ship.getArmorGrid().getArmorFraction(x, y);
			}
		}

		return acc / (width * height);
	}

	public static void setArmorPercentage(ShipAPI ship, float armorPercent) {
		ArmorGridAPI armorGrid = ship.getArmorGrid();

		armorPercent = Math.min(1, Math.max(0, armorPercent));

		for (int x = 0; x < armorGrid.getGrid().length; ++x) {
			for (int y = 0; y < armorGrid.getGrid()[0].length; ++y) {
				armorGrid.setArmorValue(x, y, armorGrid.getMaxArmorInCell() * armorPercent);
			}
		}
	}

	public static List<Vector2f> getCellLocations(ShipAPI ship) {
		int width = ship.getArmorGrid().getGrid().length;
		int height = ship.getArmorGrid().getGrid()[0].length;
		//List cellLocations = new ArrayList(width * height);
		// Not sure if above works the way I think it does.
		List<Vector2f> cellLocations = new ArrayList<>();

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				cellLocations.add(getCellLocation(ship, x, y));
			}
		}

		return cellLocations;
	}

	public static void setLocation(CombatEntityAPI entity, Vector2f location) {
		Vector2f dif = new Vector2f(location);
		Vector2f.sub(location, entity.getLocation(), dif);
		Vector2f.add(entity.getLocation(), dif, entity.getLocation());
	}

	public static Vector2f getCellLocation(ShipAPI ship, float x, float y) {
		x -= ship.getArmorGrid().getGrid().length / 2f;
		y -= ship.getArmorGrid().getGrid()[0].length / 2f;
		float cellSize = ship.getArmorGrid().getCellSize();
		Vector2f cellLoc = new Vector2f();
		float theta = (float) (((ship.getFacing() - 90) / 360f) * (Math.PI * 2));
		cellLoc.x = (float) (x * Math.cos(theta) - y * Math.sin(theta)) * cellSize + ship.getLocation().x;
		cellLoc.y = (float) (x * Math.sin(theta) + y * Math.cos(theta)) * cellSize + ship.getLocation().y;

		return cellLoc;
	}

	public static void log(String str) {
		Global.getLogger(SUN_ICE_IceUtils.class).debug(str);
	}

	public static void print(String str) {
		print(Global.getCombatEngine().getPlayerShip(), str);
	}

	public static void print(ShipAPI at, String str) {
		if (at == null) {
			return;
		}

		Global.getCombatEngine().addFloatingText(at.getLocation(), str, 40, Color.green, at, 1, 5);
	}

	public static void print(Vector2f at, String str) {
		if (at == null) {
			return;
		}

		Global.getCombatEngine().addFloatingText(at, str, 40, Color.green, null, 1, 5);
	}

	public static void destroy(CombatEntityAPI entity) {
		Global.getCombatEngine().applyDamage(entity, entity.getLocation(), entity.getMaxHitpoints() * 10f, DamageType.OTHER, 0, true, true, entity);
	}

	public static float estimateIncomingDamage(ShipAPI ship) {
		return estimateIncomingDamage(ship, DEFAULT_DAMAGE_WINDOW);
	}

	public static float estimateIncomingDamage(ShipAPI ship, float damageWindowSeconds) {
		float accumulator = 0f;

		accumulator += estimateIncomingBeamDamage(ship, damageWindowSeconds);

		for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {

			if (proj.getOwner() == ship.getOwner()) {
				continue; // Ignore friendly projectiles
			}

			Vector2f endPoint = new Vector2f(proj.getVelocity());
			endPoint.scale(damageWindowSeconds);
			Vector2f.add(endPoint, proj.getLocation(), endPoint);

			if ((ship.getShield() != null && ship.getShield().isWithinArc(proj.getLocation())) || !CollisionUtils.getCollides(proj.getLocation(), endPoint, new Vector2f(ship.getLocation()), ship.getCollisionRadius())) {
				continue;
			}

			accumulator += proj.getDamageAmount() + proj.getEmpAmount();// * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(proj, ship) / safeDistance, 2)));
		}

		return accumulator;
	}

	public static float estimateIncomingBeamDamage(ShipAPI ship, float damageWindowSeconds) {
		float accumulator = 0f;

		for (BeamAPI beam : Global.getCombatEngine().getBeams()) {
			if (beam.getDamageTarget() != ship) {
				continue;
			}

			float dps = beam.getWeapon().getDerivedStats().getDamageOver30Sec() / 30f;
			float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond();

			accumulator += (dps + emp);
		}

		return accumulator * damageWindowSeconds;
	}

	public static float estimateIncomingMissileDamage(ShipAPI ship) {
		float accumulator = 0f;
		DamagingProjectileAPI missile;

		for (MissileAPI missileAPI : Global.getCombatEngine().getMissiles()) {
			missile = missileAPI;

			if (missile.getOwner() == ship.getOwner()) {
				continue; // Ignore friendly missiles
			}

			float safeDistance = SAFE_DISTANCE + ship.getCollisionRadius();
			float threat = missile.getDamageAmount() + missile.getEmpAmount();

			if (ship.getShield() != null && ship.getShield().isWithinArc(missile.getLocation())) {
				continue;
			}

			accumulator += threat * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(missile, ship) / safeDistance, 2)));
		}

		return accumulator;
	}

	public static float getHitChance(DamagingProjectileAPI proj, CombatEntityAPI target) {
		float estTimeTilHit = MathUtils.getDistance(target, proj.getLocation()) / Math.max(1, proj.getWeapon().getProjectileSpeed());

		Vector2f estTargetPosChange = new Vector2f(target.getVelocity().x * estTimeTilHit, target.getVelocity().y * estTimeTilHit);

		float estFacingChange = target.getAngularVelocity() * estTimeTilHit;

		Vector2f projVelocity = proj.getVelocity();

		target.setFacing(target.getFacing() + estFacingChange);
		Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());

		projVelocity.scale(estTimeTilHit * 3);
		Vector2f.add(projVelocity, proj.getLocation(), projVelocity);
		Vector2f estHitLoc = CollisionUtils.getCollisionPoint(proj.getLocation(), projVelocity, target);

		target.setFacing(target.getFacing() - estFacingChange);
		Vector2f.add(target.getLocation(), (Vector2f) estTargetPosChange.scale(-1), target.getLocation());

		if (estHitLoc == null) {
			return 0;
		}

		return 1;
	}

	public static float getHitChance(WeaponAPI weapon, CombatEntityAPI target) {
		float estTimeTilHit = MathUtils.getDistance(target, weapon.getLocation()) / Math.max(1, weapon.getProjectileSpeed());

		Vector2f estTargetPosChange = new Vector2f(target.getVelocity().x * estTimeTilHit, target.getVelocity().y * estTimeTilHit);

		float estFacingChange = target.getAngularVelocity() * estTimeTilHit;

		double theta = weapon.getCurrAngle() * (Math.PI / 180);
		Vector2f projVelocity = new Vector2f((float) Math.cos(theta) * weapon.getProjectileSpeed() + weapon.getShip().getVelocity().x, (float) Math.sin(theta) * weapon.getProjectileSpeed() + weapon.getShip().getVelocity().y);

		target.setFacing(target.getFacing() + estFacingChange);
		Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());

		projVelocity.scale(estTimeTilHit * 3);
		Vector2f.add(projVelocity, weapon.getLocation(), projVelocity);
		Vector2f estHitLoc = CollisionUtils.getCollisionPoint(weapon.getLocation(), projVelocity, target);

		target.setFacing(target.getFacing() - estFacingChange);
		Vector2f.add(target.getLocation(), (Vector2f) estTargetPosChange.scale(-1), target.getLocation());

		if (estHitLoc == null) {
			return 0;
		}

		return 1;
	}

	public static float getFPWorthOfSupport(ShipAPI ship, float range) {
		float retVal = 0;

		for (ShipAPI ally : AIUtils.getNearbyAllies(ship, range)) {
			if (ally == ship) {
				continue;
			}
			float colDist = ship.getCollisionRadius() + ally.getCollisionRadius();
			float distance = Math.max(0, MathUtils.getDistance(ship, ally) - colDist);
			float maxRange = Math.max(1, range - colDist);

			retVal += getFPStrength(ally) * (1 - distance / maxRange);
		}

		return retVal;
	}

	public static float getFPWorthOfHostility(ShipAPI ship, float range) {
		float retVal = 0;

		for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
			float colDist = ship.getCollisionRadius() + enemy.getCollisionRadius();
			float distance = Math.max(0, MathUtils.getDistance(ship, enemy) - colDist);
			float maxRange = Math.max(1, range - colDist);

			retVal += getFPStrength(enemy) * (1 - distance / maxRange);
		}

		return retVal;
	}

	public static float getStrengthInArea(Vector2f at, float range) {
		float retVal = 0;

		for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
			retVal += getFPStrength(ship);
		}

		return retVal;
	}

	public static float getStrengthInArea(Vector2f at, float range, int owner) {
		float retVal = 0;

		for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
			if (ship.getOwner() == owner) {
				retVal += getFPStrength(ship);
			}
		}

		return retVal;
	}

	public static float getFPStrength(ShipAPI ship) {
		FleetMemberAPI member = ship.getFleetMember();
		return member == null ? 0f : member.getMemberStrength();
	}

	public static float getFPCost(ShipAPI ship) {
		FleetMemberAPI member = ship.getFleetMember();
		return member == null ? 0f : member.getFleetPointCost();
	}

	public static float getFPAverage(ShipAPI ship) {
		float strength = getFPStrength(ship);
		float cost = getFPCost(ship);
		return strength == 0f || cost == 0f ? 0f : strength + cost;
	}

	public static boolean isInRefit() {
		return Global.getCombatEngine().isInCampaign() || Global.getCombatEngine().getCombatUI() == null;
	}

	public static float getBaseOverloadDuration(ShipAPI ship) {
		return baseOverloadTimes.get(ship.getHullSize());
	}

	public static float estimateOverloadDurationOnHit(ShipAPI ship, float damage, DamageType type) {
		if (ship.getShield() == null) {
			return 0;
		}

		float fluxDamage = damage * type.getShieldMult() * ship.getMutableStats().getShieldAbsorptionMult().getModifiedValue();
		fluxDamage += ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getMaxFlux();

		if (fluxDamage <= 0) {
			return 0;
		}

		return Math.min(15, getBaseOverloadDuration(ship) + fluxDamage / 25);
	}

	public static float getLifeExpectancy(ShipAPI ship) {
		float damage = estimateIncomingDamage(ship);
		return (damage <= 0) ? 3600 : ship.getHitpoints() / damage;
	}

	public static boolean isConsideredPhaseShip(FleetMemberAPI member) {
		return isConsideredPhaseShip(member, true);
	}

	public static boolean isConsideredPhaseShip(FleetMemberAPI member, boolean generally) {
		if (member.isPhaseShip()) return true;
		return generally && member.getVariant().hasHullMod(HullMods.PHASE_FIELD);
	}

	public static boolean isPhysicallyPhaseShip(ShipAPI ship) {
		return ship.getPhaseCloak() != null && ship.getPhaseCloak().getSpecAPI().isPhaseCloak();
	}

	public static void decivilize(MarketAPI market) {
		if (market == null) return;

		if (!market.getPrimaryEntity().isDiscoverable()) {

			market.setAdmin(null);

			for (SectorEntityToken entity : market.getConnectedEntities()) {
				entity.setFaction("neutral");
			}

			market.setPlanetConditionMarketOnly(true);
			market.setFactionId("neutral");
			market.getCommDirectory().clear();

			List<Object> toRemove = new ArrayList<>();
			toRemove.addAll(market.getPeopleCopy());
			for (Object person : toRemove) {
				market.removePerson((PersonAPI) person);
			}
			toRemove.clear();

			market.clearCommodities();
			toRemove.addAll(market.getConditions());
			for (Object mc : toRemove) {
				market.removeSpecificCondition(((MarketConditionAPI) mc).getIdForPluginModifications());
			}
			toRemove.clear();

			toRemove.addAll(market.getIndustries());
			for (Object ind : toRemove) {
				market.removeIndustry(((Industry) ind).getId(), null, false);
			}
			toRemove.clear();

			toRemove.addAll(market.getSubmarketsCopy());
			for (Object sub : toRemove) {
				market.removeSubmarket(((SubmarketAPI) sub).getSpecId());
			}

			market.getMemoryWithoutUpdate().set("$wasCivilized", true);
			market.setSize(1);
			market.getPopulation().setWeight(CoreImmigrationPluginImpl.getWeightForMarketSizeStatic((float) market.getSize()));
			market.getPopulation().normalize();

			SectorEntityToken entity = market.getPrimaryEntity();
			market.getConnectedEntities().clear();
			market.setPrimaryEntity(entity);
			market.setPlayerOwned(false);
			Global.getSector().getEconomy().removeMarket(market);
			Misc.removeRadioChatter(market);
			market.advance(0f);
		}
	}

	public static void setMarketOwner(MarketAPI market, FactionAPI newOwner) {
		FactionAPI oldOwner = market.getFaction();
		market.getPrimaryEntity().setFaction(newOwner.getId());
		market.setFactionId(newOwner.getId());
		market.setPlayerOwned(true);

		if (newOwner == Global.getSector().getPlayerFaction()) {
			market.addSubmarket(Submarkets.LOCAL_RESOURCES);
			if (!market.hasIndustry("commerce")) market.removeSubmarket(Submarkets.SUBMARKET_OPEN);
			market.removeSubmarket(Submarkets.GENERIC_MILITARY);
			market.removeSubmarket(Submarkets.SUBMARKET_BLACK);
		} else {
			market.removeSubmarket(Submarkets.LOCAL_RESOURCES);
			market.addSubmarket(Submarkets.SUBMARKET_OPEN);
			market.addSubmarket(Submarkets.GENERIC_MILITARY);
			market.addSubmarket(Submarkets.SUBMARKET_BLACK);
		}

		for (SubmarketAPI subMarket : market.getSubmarketsCopy()) {
			subMarket.setFaction(newOwner);
			if (subMarket.getPlugin() instanceof BaseSubmarketPlugin) {
				BaseSubmarketPlugin plugin = (BaseSubmarketPlugin) subMarket.getPlugin();
				plugin.setSinceSWUpdate(plugin.getMinSWUpdateInterval() + 1f);
				plugin.setSinceLastCargoUpdate(plugin.getMinSWUpdateInterval() + 1f);
				plugin.updateCargoPrePlayerInteraction();
				plugin.setSinceSWUpdate(0f);
			}
		}

		for (CommDirectoryEntryAPI dir : market.getCommDirectory().getEntriesCopy()) {
			if (dir.getType() != CommDirectoryEntryAPI.EntryType.PERSON) {
				continue;
			}
			PersonAPI person = (PersonAPI) dir.getEntryData();
			person.setFaction(newOwner.getId());
		}

		for (CampaignFleetAPI fleet : market.getContainingLocation().getFleets()) {
			if (fleet.getFaction() == oldOwner) {
				fleet.setFaction(newOwner.getId());
			}
		}

		for (CampaignFleetAPI fleet : Global.getSector().getHyperspace().getFleets()) {
			if (fleet.getFaction() == oldOwner) {
				fleet.setFaction(newOwner.getId());
			}
		}

		market.reapplyConditions();
		market.reapplyIndustries();
	}

	public static String md5(String data) {
		StringBuilder sb = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("md5");
			byte[] md5 = md.digest(data.getBytes(StandardCharsets.UTF_8));

			for (byte b : md5) {
				sb.append(Integer.toHexString(b & 0xff));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}
}