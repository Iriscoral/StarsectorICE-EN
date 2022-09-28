package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.plugins.SUN_ICE_RenderPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class SUN_ICE_FoFInverterStats extends BaseShipSystemScript {
	private static final String DATA_KEY = "SUN_ICE_RenderPlugin";
	public static final float RANGE_FACTOR = 1600f;
	private static final float MAX_MISSILE_HP = 600f;
	private final Color GLOW_COLOR = new Color(255, 191, 0);
	private final Color PHASE_COLOR = new Color(255, 255, 0, 40);
	private float timer = 0f;

	public static float getRange(ShipAPI ship) {
		if (ship == null) {
			return RANGE_FACTOR;
		}
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null || Global.getCombatEngine().isPaused()) {
			return;
		}

		SUN_ICE_RenderPlugin.LocalRenderData localData = (SUN_ICE_RenderPlugin.LocalRenderData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
		if (localData == null) {
			return;
		}

		Map<ShipAPI, SUN_ICE_RenderPlugin.FoFData> theData = localData.fofRenderData;
		if (!theData.containsKey(ship)) {
			theData.put(ship, new SUN_ICE_RenderPlugin.FoFData(ship));
		}
		SUN_ICE_RenderPlugin.FoFData data = theData.get(ship);

		if (ship.isPhased()) {
			ship.setJitterUnder(ship, PHASE_COLOR, effectLevel, 10, 0f, 18f);
		}

		timer += Global.getCombatEngine().getElapsedInLastFrame();
		data.setAlpha(effectLevel);
		data.multSize(timer);

		List<MissileAPI> missiles = AIUtils.getNearbyEnemyMissiles(ship, getRange(ship));
		if (missiles.isEmpty()) {
			return;
		}

		MissileAPI missile = missiles.get((int) Math.floor(Math.pow(Math.random(), 3) * missiles.size()));
		float ecmChance = 0.9f * effectLevel; // originally 0.92
		ecmChance *= 1f - Math.min(MAX_MISSILE_HP, missile.getHitpoints()) / (MAX_MISSILE_HP * 1.5f);
		ecmChance *= 1f - 0.3f * MathUtils.getDistance(ship, missile) / getRange(ship);
		ecmChance *= missile.getSource().getVariant().getHullMods().contains("eccm") ? 0.75f : 1f;

		if (Math.random() < ecmChance * Global.getCombatEngine().getElapsedInLastFrame()) {
			missile.setSource(ship);
			missile.setOwner(ship.getOwner());

			if (missile.getCollisionClass() == CollisionClass.MISSILE_FF) {
				missile.setCollisionClass(CollisionClass.MISSILE_NO_FF);
			} else {
				missile.setFlightTime(0.1f); // not 0
			}

			Global.getCombatEngine().addSmoothParticle(missile.getLocation(), missile.getVelocity(), missile.getHitpoints(), 0.8f, 0.8f, GLOW_COLOR);
			Global.getSoundPlayer().playSound("sun_ice_system_fofinverter", 1, Math.min(missile.getHitpoints() * 0.01f, 1f), missile.getLocation(), missile.getVelocity());
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		timer = 0f;
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}

		SUN_ICE_RenderPlugin.LocalRenderData localData = (SUN_ICE_RenderPlugin.LocalRenderData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
		if (localData == null) {
			return;
		}

		Map<ShipAPI, SUN_ICE_RenderPlugin.FoFData> theData = localData.fofRenderData;
		if (theData.containsKey(ship)) {
			theData.get(ship).setAlpha(0f);
		}
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}