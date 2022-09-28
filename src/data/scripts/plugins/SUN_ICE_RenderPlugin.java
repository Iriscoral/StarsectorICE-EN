package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.shipsystems.SUN_ICE_FoFInverterStats;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SUN_ICE_RenderPlugin extends BaseCombatLayeredRenderingPlugin {
	private static final String DATA_KEY = "SUN_ICE_RenderPlugin";
	private static final Vector2f ZERO = new Vector2f();
	private CombatEngineAPI engine;

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);
		engine = Global.getCombatEngine();
		Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalRenderData());
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI view) {
		if (engine == null) {
			return;
		}

		final LocalRenderData localData = (LocalRenderData) engine.getCustomData().get(DATA_KEY);
		final Map<ShipAPI, FoFData> fofData = localData.fofRenderData;
		float amount = engine.getElapsedInLastFrame();
		if (!fofData.isEmpty()) {
			Iterator<Map.Entry<ShipAPI, FoFData>> iter = fofData.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<ShipAPI, FoFData> entry = iter.next();
				ShipAPI ship = entry.getKey();
				FoFData data = entry.getValue();

				if (ship == null || !ship.isAlive() || !engine.isEntityInPlay(ship)) {
					data.setAlpha(0f);
					iter.remove();
					continue;
				}

				if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER) {
					data.angle += amount * 10f;
					if (data.angle > 360f) {
						data.angle -= 360f;
					}
					data.sprite.renderAtCenter(ship.getLocation().x, ship.getLocation().y);
					data.sprite2.renderAtCenter(ship.getLocation().x, ship.getLocation().y);
					data.sprite3.renderAtCenter(ship.getLocation().x, ship.getLocation().y);
					data.sprite.setAngle(data.angle);
					data.sprite2.setAngle(-data.angle * 2f);
					data.sprite3.setAngle(data.angle * 3f);
				}
			}
		}
	}

	@Override
	public float getRenderRadius() {
		return Float.MAX_VALUE;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.allOf(CombatEngineLayers.class);
	}

	public static final class LocalRenderData {
		public final Map<ShipAPI, FoFData> fofRenderData = new LinkedHashMap<>(500);
	}

	public final static class FoFData {
		final ShipAPI ship;
		final SpriteAPI sprite;
		final SpriteAPI sprite2;
		final SpriteAPI sprite3;
		float angle;

		public FoFData(ShipAPI ship) {
			this.ship = ship;
			sprite = Global.getSettings().getSprite("fx", "sun_ice_fof_edge");
			sprite2 = Global.getSettings().getSprite("fx", "sun_ice_fof_edge");
			sprite3 = Global.getSettings().getSprite("fx", "sun_ice_fof_edge");
			sprite.setSize(SUN_ICE_FoFInverterStats.getRange(ship) * 2f, SUN_ICE_FoFInverterStats.getRange(ship) * 2f);
			sprite2.setSize(SUN_ICE_FoFInverterStats.getRange(ship) * 2f, SUN_ICE_FoFInverterStats.getRange(ship) * 2f);
			sprite3.setSize(0f, 0f);
			sprite.setColor(new Color(255, 255, 150, 10));
			sprite2.setColor(new Color(255, 255, 150, 10));
			sprite3.setColor(new Color(255, 255, 200, 15));
			angle = 0f;
		}

		public void setAlpha(float alpha) {
			sprite.setAlphaMult(alpha);
			sprite2.setAlphaMult(alpha);
			sprite3.setAlphaMult(alpha);
		}

		public void multSize(float effectLevel) {
			effectLevel %= 1f;
			float size = SUN_ICE_FoFInverterStats.getRange(ship) * 2f * effectLevel;
			sprite3.setSize(size, size);
		}
	}
}