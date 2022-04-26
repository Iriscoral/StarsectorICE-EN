package data.scripts.tools;

import com.fs.starfarer.api.combat.CombatEngineAPI;

public class SUN_ICE_IntervalTracker {
	private float timeOfNextElapse, min, max;
	private boolean includePausedTime;

	private void init(float min, float max, boolean includePausedTime) {
		this.min = Math.min(min, max);
		this.max = Math.max(min, max);
		this.includePausedTime = includePausedTime;
		this.timeOfNextElapse = -1f;
	}

	private float incrementInterval(float time) {
		return timeOfNextElapse = time + ((min == max) ? min : min + (max - min) * (float) Math.random());
	}

	public SUN_ICE_IntervalTracker() {
		init(1, 1, false);
	}

	public SUN_ICE_IntervalTracker(float intervalDuration) {
		init(intervalDuration, intervalDuration, false);
	}

	public SUN_ICE_IntervalTracker(float minIntervalDuration, float maxIntervalDuration) {
		init(minIntervalDuration, maxIntervalDuration, false);
	}

	public SUN_ICE_IntervalTracker(float minIntervalDuration, float maxIntervalDuration, boolean includePausedTime) {
		init(minIntervalDuration, maxIntervalDuration, includePausedTime);
	}

	private void reset(CombatEngineAPI engine) {
		incrementInterval(engine.getTotalElapsedTime(includePausedTime));
	}

	public boolean intervalIsFixed() {
		return min == max;
	}

	public float getAverageInterval() {
		return (min + max) / 2;
	}

	public float getMinimumInterval() {
		return min;
	}

	public float getMaximumInterval() {
		return max;
	}

	public void setInterval(float intervalDuration) {
		min = max = intervalDuration;
	}

	public void setInterval(float minIntervalDuration, float maxIntervalDuration) {
		min = Math.min(minIntervalDuration, maxIntervalDuration);
		max = Math.max(minIntervalDuration, maxIntervalDuration);
	}

	public boolean intervalElapsed(CombatEngineAPI engine) {
		if (timeOfNextElapse < 0f) {
			reset(engine);
		}

		float time = engine.getTotalElapsedTime(includePausedTime);

		if (timeOfNextElapse <= time) {
			incrementInterval(timeOfNextElapse);
			if (timeOfNextElapse <= time) {
				incrementInterval(time);
			}

			return true;
		}

		return false;
	}
}