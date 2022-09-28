package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

public class SUN_ICE_TurbulenceValveVisual extends BaseCombatLayeredRenderingPlugin {

	private final ShipAPI anchor;
	private static final Color EFFECT_NORMAL = new Color(155, 255, 215);
	private static final Color EFFECT_DISTANT = new Color(60, 205, 185);

	private SpriteAPI auraTex;
	private SDVParams current;
	private SDVParams next;
	private final float minRange = 50f;
	private final float maxRange;

	private int segments;
	private float textElapsed = 0f;

	private float effectLevel = 0f;
	private boolean valid = true;

	public void updateEffectLevel(float effectLevel) {
		this.effectLevel = effectLevel;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public SUN_ICE_TurbulenceValveVisual(ShipAPI anchor) {
		this.anchor = anchor;
		this.maxRange = anchor.getCollisionRadius() + 200f;
	}

	@Override
	public float getRenderRadius() {
		return anchor.getCollisionRadius() * 2f + 500f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES_LOWER);
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		textElapsed += amount;
	}

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);

		auraTex = Global.getSettings().getSprite("fx", "sun_ice_turbulence");

		float circle = (float)(Math.PI * 2f * maxRange);
		float lengthPerSeg = 40f;
		segments = Math.round(circle / lengthPerSeg);

		float rangeDuration = maxRange - minRange;
		current = new SDVParams(anchor, rangeDuration);
		next = new SDVParams(anchor, rangeDuration);
	}

	@Override
	public boolean isExpired() {
		return !valid;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		float x = entity.getLocation().x;
		float y = entity.getLocation().y;

		float alphaMult = viewport.getAlphaMult() * effectLevel;
		if (layer == CombatEngineLayers.ABOVE_PARTICLES_LOWER) {
			renderAura(anchor, x, y, minRange, maxRange, segments, alphaMult);
		}
	}

	public void renderAura(ShipAPI anchor, float x, float y, float minRange, float maxRange, int segments, float alphaMult) {

		float startRad = (float)Math.toRadians(0f);
		float endRad = (float)Math.toRadians(360f);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0f);
		GL11.glRotatef(anchor.getFacing(), 0f, 0f, 1f);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		auraTex.bindTexture();

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glColor4ub((byte)EFFECT_NORMAL.getRed(), (byte)EFFECT_NORMAL.getGreen(), (byte)EFFECT_NORMAL.getBlue(), (byte)(int)(EFFECT_NORMAL.getAlpha() * alphaMult));
		float rangeDuration = maxRange - minRange;

		float circle = (float)(Math.PI * 2f * maxRange);
		float texPerSegment = maxRange / circle;
		float totalTex = Math.max(1f, Math.round(texPerSegment * segments));
		texPerSegment = totalTex / segments;

		float shortBeforeBase = -1f * minRange * 0.5f / rangeDuration;
		float base = 0f;
		float oneAfterBase = 1f;

		GL11.glBegin(GL11.GL_QUAD_STRIP);
		float leftEdgeOfShowingTex = 0f;
		for (int i = 0; i < segments; i++) {

			float theta = anglePerSegment * i;
			if (i == 0) {
				current.advance(theta, effectLevel);
			} else {
				current.transfer(next);
			}

			int iNext = i + 1;
			if (iNext == segments) iNext = 0;
			float thetaNext = anglePerSegment * iNext;
			next.advance(thetaNext, effectLevel);

			float colorFactor = effectLevel;
			Color effectColor = Misc.interpolateColor(EFFECT_NORMAL, EFFECT_DISTANT, colorFactor);

			GL11.glColor4ub((byte)EFFECT_NORMAL.getRed(), (byte)EFFECT_NORMAL.getGreen(), (byte)EFFECT_NORMAL.getBlue(), (byte)0);
			GL11.glTexCoord2f(leftEdgeOfShowingTex, shortBeforeBase - textElapsed);
			GL11.glVertex2f(current.xMin, current.yMin);
			GL11.glTexCoord2f(leftEdgeOfShowingTex + texPerSegment, shortBeforeBase - textElapsed);
			GL11.glVertex2f(next.xMin, next.yMin);

			GL11.glColor4ub((byte)effectColor.getRed(), (byte)effectColor.getGreen(), (byte)effectColor.getBlue(), (byte)(int)(effectColor.getAlpha() * alphaMult));
			GL11.glTexCoord2f(leftEdgeOfShowingTex, base - textElapsed);
			GL11.glVertex2f(current.xMid, current.yMid);
			GL11.glTexCoord2f(leftEdgeOfShowingTex + texPerSegment, base - textElapsed);
			GL11.glVertex2f(next.xMid, next.yMid);

			GL11.glColor4ub((byte)EFFECT_DISTANT.getRed(), (byte)EFFECT_DISTANT.getGreen(), (byte)EFFECT_DISTANT.getBlue(), (byte)0);
			GL11.glTexCoord2f(leftEdgeOfShowingTex, oneAfterBase - textElapsed);
			GL11.glVertex2f(current.xMax, current.yMax);
			GL11.glTexCoord2f(leftEdgeOfShowingTex + texPerSegment, oneAfterBase - textElapsed);
			GL11.glVertex2f(next.xMax, next.yMax);

			leftEdgeOfShowingTex += texPerSegment;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}

	public static class SDVParams {

		ShipAPI anchor;
		float rangeDuration;

		float outRange;
		float innerRange;

		float xMin;
		float yMin;
		float xMid;
		float yMid;
		float xMax;
		float yMax;

		public SDVParams(ShipAPI anchor, float rangeDuration) {
			this.anchor = anchor;
			this.rangeDuration = rangeDuration;
		}

		public void advance(float theta, float effectLevel) {
			float cos = (float)FastTrig.cos(theta);
			float sin = (float)FastTrig.sin(theta);

			outRange = getOutRange(anchor, theta) * effectLevel;
			innerRange = Math.min(outRange * 0.5f, rangeDuration);

			float factorMult = 1f;

			xMin = cos * (outRange - innerRange);
			yMin = sin * (outRange - innerRange);
			xMid = cos * outRange * factorMult;
			yMid = sin * outRange * factorMult;
			xMax = cos * (outRange + rangeDuration) * factorMult;
			yMax = sin * (outRange + rangeDuration) * factorMult;
		}

		public void transfer(SDVParams data) {
			this.outRange = data.outRange;
			this.innerRange = data.innerRange;

			this.xMin = data.xMin;
			this.yMin = data.yMin;
			this.xMid = data.xMid;
			this.yMid = data.yMid;
			this.xMax = data.xMax;
			this.yMax = data.yMax;
		}

		private float getOutRange(ShipAPI anchor, float theta) {
			if (theta == 0f) theta += 0.5f;

			Vector2f endPoint = MathUtils.getPoint(anchor.getLocation(), anchor.getCollisionRadius(), anchor.getFacing() + (float)Math.toDegrees(theta));
			Vector2f collisionPoint = CollisionUtils.getCollisionPoint(endPoint, anchor.getLocation(), anchor);
			if (collisionPoint == null) collisionPoint = endPoint;

			return MathUtils.getDistance(anchor.getLocation(), collisionPoint) * 0.25f + anchor.getCollisionRadius() * 0.25f + 100f;
		}
	}
}