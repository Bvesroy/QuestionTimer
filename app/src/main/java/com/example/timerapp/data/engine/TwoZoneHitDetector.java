package com.example.timerapp.data.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance Two-Zone Screen Hit Detection Engine for Full-Screen Timers.
 * Partitions touch interactions into:
 * 1. ZONE_LEFT: Primary action (e.g. Pause / Next / Correct)
 * 2. ZONE_RIGHT: Secondary action (e.g. Skip / Negative / Break)
 * 3. ZONE_EXCLUDED: Areas reserved for interactive controls (top bars, bottom toolbars)
 * 4. ZONE_DEADZONE: Center dividing margin preventing ambiguous or accidental boundary taps
 * 5. ZONE_OUT_OF_BOUNDS: Touches outside screen coordinates
 */
public class TwoZoneHitDetector implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    public enum TouchZone {
        ZONE_LEFT,
        ZONE_RIGHT,
        ZONE_UPPER,
        ZONE_LOWER,
        ZONE_EXCLUDED,
        ZONE_DEADZONE,
        ZONE_OUT_OF_BOUNDS
    }

    public static class ExclusionRect implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        public ExclusionRect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public boolean contains(float x, float y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }

    private final int screenWidth;
    private final int screenHeight;
    private float splitRatio = 0.5f;
    private float deadZoneWidthPx = 0f;
    private Orientation orientation = Orientation.HORIZONTAL;
    private final List<ExclusionRect> exclusionZones = new ArrayList<>();

    public TwoZoneHitDetector(int screenWidth, int screenHeight) {
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
    }

    public TwoZoneHitDetector(int screenWidth, int screenHeight, float splitRatio, float deadZoneWidthPx) {
        this(screenWidth, screenHeight);
        setSplitRatio(splitRatio);
        setDeadZoneWidthPx(deadZoneWidthPx);
    }

    public void setOrientation(Orientation orientation) {
        if (orientation != null) {
            this.orientation = orientation;
        }
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setSplitRatio(float splitRatio) {
        if (splitRatio > 0.05f && splitRatio < 0.95f) {
            this.splitRatio = splitRatio;
        }
    }

    public float getSplitRatio() {
        return splitRatio;
    }

    public void setDeadZoneWidthPx(float widthPx) {
        this.deadZoneWidthPx = Math.max(0f, widthPx);
    }

    public float getDeadZoneWidthPx() {
        return deadZoneWidthPx;
    }

    public void addExclusionZone(int left, int top, int right, int bottom) {
        exclusionZones.add(new ExclusionRect(left, top, right, bottom));
    }

    public void clearExclusionZones() {
        exclusionZones.clear();
    }

    public List<ExclusionRect> getExclusionZones() {
        return new ArrayList<>(exclusionZones);
    }

    /**
     * Convenience method specifically for vertical two-zone division (Upper=Wrong, Lower=Correct).
     */
    public TouchZone detectVerticalZone(float x, float y) {
        Orientation prev = this.orientation;
        try {
            this.orientation = Orientation.VERTICAL;
            return detectZone(x, y);
        } finally {
            this.orientation = prev;
        }
    }

    /**
     * Evaluates screen coordinates (x, y) and classifies into appropriate TouchZone.
     */
    public TouchZone detectZone(float x, float y) {
        // 1. Boundary / out of bounds check
        if (x < 0f || x > screenWidth || y < 0f || y > screenHeight) {
            return TouchZone.ZONE_OUT_OF_BOUNDS;
        }

        // 2. Interactive control exclusion zones check
        for (ExclusionRect rect : exclusionZones) {
            if (rect.contains(x, y)) {
                return TouchZone.ZONE_EXCLUDED;
            }
        }

        // 3. Vertical two-zone evaluation (Upper vs Lower)
        if (orientation == Orientation.VERTICAL) {
            float splitY = screenHeight * splitRatio;
            if (deadZoneWidthPx > 0f) {
                float halfDead = deadZoneWidthPx / 2.0f;
                if (y >= (splitY - halfDead) && y <= (splitY + halfDead)) {
                    return TouchZone.ZONE_DEADZONE;
                }
            }
            return (y < splitY) ? TouchZone.ZONE_UPPER : TouchZone.ZONE_LOWER;
        }

        // 4. Horizontal two-zone evaluation (Left vs Right)
        float splitX = screenWidth * splitRatio;
        if (deadZoneWidthPx > 0f) {
            float halfDead = deadZoneWidthPx / 2.0f;
            if (x >= (splitX - halfDead) && x <= (splitX + halfDead)) {
                return TouchZone.ZONE_DEADZONE;
            }
        }

        return (x < splitX) ? TouchZone.ZONE_LEFT : TouchZone.ZONE_RIGHT;
    }
}
