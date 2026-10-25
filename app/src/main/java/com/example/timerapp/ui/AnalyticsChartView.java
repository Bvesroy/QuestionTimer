package com.example.timerapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight, zero-dependency AMOLED Dark Custom Chart View.
 * Renders:
 * 1. Line Graphs (average time per question / pace trend)
 * 2. Bar Charts (daily practice duration)
 * 3. Moving Average Graphs (7-session & 30-session rolling moving average pace lines)
 * 4. Productivity Heatmap (monthly contribution activity grid with intensity levels)
 */
public class AnalyticsChartView extends View {

    public enum ChartType {
        LINE,
        BAR,
        MOVING_AVERAGE,
        HEATMAP
    }

    public static class ChartDataPoint {
        public final String label;
        public final float value; // Seconds for pace, minutes for duration
        public final String tooltipText;

        public ChartDataPoint(String label, float value, String tooltipText) {
            this.label = label;
            this.value = value;
            this.tooltipText = tooltipText;
        }
    }

    public static class HeatmapCell {
        public final int dayOfMonth;
        public final String dateLabel;
        public final float minutes;
        public final int sessionCount;

        public HeatmapCell(int dayOfMonth, String dateLabel, float minutes, int sessionCount) {
            this.dayOfMonth = dayOfMonth;
            this.dateLabel = dateLabel;
            this.minutes = minutes;
            this.sessionCount = sessionCount;
        }
    }

    private ChartType chartType = ChartType.LINE;
    private final List<ChartDataPoint> dataPoints = new ArrayList<>();
    private final List<HeatmapCell> heatmapCells = new ArrayList<>();
    private float targetValue = -1f;

    // Drawing tools
    private Paint gridPaint;
    private Paint targetPaint;
    private Paint linePaint;
    private Paint ma7Paint;
    private Paint ma30Paint;
    private Paint fillPaint;
    private Paint pointOuterPaint;
    private Paint pointInnerPaint;
    private Paint barPaint;
    private Paint barHighlightPaint;
    private Paint textPaint;
    private Paint valueTextPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;
    private Paint heatmapCellPaint;

    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final Path ma7Path = new Path();
    private final Path ma30Path = new Path();
    private final Rect textBounds = new Rect();

    // Selection / interaction
    private int selectedIndex = -1;
    private int selectedHeatmapIndex = -1;

    public AnalyticsChartView(Context context) {
        super(context);
        init();
    }

    public AnalyticsChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalyticsChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#222224"));
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4f * density, 4f * density}, 0));

        targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setColor(Color.parseColor("#FF9F0A")); // Amber
        targetPaint.setStrokeWidth(1.5f * density);
        targetPaint.setStyle(Paint.Style.STROKE);
        targetPaint.setPathEffect(new DashPathEffect(new float[]{6f * density, 4f * density}, 0));

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#30D158")); // Running Green
        linePaint.setStrokeWidth(3f * density);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        ma7Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ma7Paint.setColor(Color.parseColor("#30D158")); // 7-Session MA Green
        ma7Paint.setStrokeWidth(3f * density);
        ma7Paint.setStyle(Paint.Style.STROKE);
        ma7Paint.setStrokeCap(Paint.Cap.ROUND);

        ma30Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ma30Paint.setColor(Color.parseColor("#0A84FF")); // 30-Session MA Blue
        ma30Paint.setStrokeWidth(2.5f * density);
        ma30Paint.setStyle(Paint.Style.STROKE);
        ma30Paint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointOuterPaint.setColor(Color.parseColor("#4D30D158"));
        pointOuterPaint.setStyle(Paint.Style.FILL);

        pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointInnerPaint.setColor(Color.parseColor("#FFFFFF"));
        pointInnerPaint.setStyle(Paint.Style.FILL);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#0A84FF"));
        barPaint.setStyle(Paint.Style.FILL);

        barHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barHighlightPaint.setColor(Color.parseColor("#30D158"));
        barHighlightPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8E8E93"));
        textPaint.setTextSize(10f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);

        valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setColor(Color.parseColor("#FFFFFF"));
        valueTextPaint.setTextSize(9f * density);
        valueTextPaint.setTextAlign(Paint.Align.CENTER);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(Color.parseColor("#2C2C2E"));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.parseColor("#FFFFFF"));
        tooltipTextPaint.setTextSize(11f * density);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        heatmapCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatmapCellPaint.setStyle(Paint.Style.FILL);
    }

    public void setChartType(ChartType type) {
        this.chartType = type;
        this.selectedIndex = -1;
        this.selectedHeatmapIndex = -1;
        invalidate();
    }

    public ChartType getChartType() {
        return chartType;
    }

    public void setData(List<ChartDataPoint> points, float target) {
        this.dataPoints.clear();
        if (points != null) {
            this.dataPoints.addAll(points);
        }
        this.targetValue = target;
        this.selectedIndex = -1;
        invalidate();
    }

    public void setHeatmapData(List<HeatmapCell> cells) {
        this.heatmapCells.clear();
        if (cells != null) {
            this.heatmapCells.addAll(cells);
        }
        this.selectedHeatmapIndex = -1;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (chartType == ChartType.HEATMAP) {
            return handleHeatmapTouch(event);
        }

        if (dataPoints.isEmpty()) return super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            int closest = findClosestPointIndex(x);
            if (closest != selectedIndex) {
                selectedIndex = closest;
                invalidate();
            }
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean handleHeatmapTouch(MotionEvent event) {
        if (heatmapCells.isEmpty()) return super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            int cols = 7;
            float padX = dp(16);
            float padY = dp(24);
            float availW = getWidth() - padX * 2;
            float cellSize = Math.min(dp(32), (availW - dp(6) * (cols - 1)) / cols);
            float gap = dp(6);

            int col = (int) ((x - padX) / (cellSize + gap));
            int row = (int) ((y - padY) / (cellSize + gap));

            if (col >= 0 && col < cols && row >= 0) {
                int index = row * cols + col;
                if (index < heatmapCells.size() && index != selectedHeatmapIndex) {
                    selectedHeatmapIndex = index;
                    invalidate();
                }
            }
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int findClosestPointIndex(float touchX) {
        if (dataPoints.isEmpty()) return -1;
        float paddingLeft = getPaddingLeft() + dp(36);
        float paddingRight = getPaddingRight() + dp(16);
        float chartWidth = getWidth() - paddingLeft - paddingRight;

        int count = dataPoints.size();
        if (count == 1) return 0;

        float step = chartWidth / (count - 1);
        if (chartType == ChartType.BAR) {
            step = chartWidth / count;
            int idx = (int) ((touchX - paddingLeft) / step);
            return Math.max(0, Math.min(count - 1, idx));
        }

        int closestIdx = 0;
        float minDiff = Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float px = paddingLeft + i * step;
            float diff = Math.abs(px - touchX);
            if (diff < minDiff) {
                minDiff = diff;
                closestIdx = i;
            }
        }
        return closestIdx;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        if (chartType == ChartType.HEATMAP) {
            drawHeatmap(canvas, width, height);
            return;
        }

        if (dataPoints.isEmpty()) {
            drawEmptyState(canvas, width, height);
            return;
        }

        if (chartType == ChartType.LINE) {
            drawLineChart(canvas, width, height);
        } else if (chartType == ChartType.MOVING_AVERAGE) {
            drawMovingAverageChart(canvas, width, height);
        } else {
            drawBarChart(canvas, width, height);
        }
    }

    private void drawEmptyState(Canvas canvas, int width, int height) {
        textPaint.setColor(Color.parseColor("#636366"));
        textPaint.setTextSize(dp(13));
        canvas.drawText("NO DATA RECORDED YET", width / 2f, height / 2f, textPaint);
        textPaint.setTextSize(dp(10));
        canvas.drawText("Complete practice questions to see pace trends", width / 2f, height / 2f + dp(20), textPaint);
    }

    private void drawLineChart(Canvas canvas, int width, int height) {
        float paddingLeft = getPaddingLeft() + dp(38);
        float paddingRight = getPaddingRight() + dp(18);
        float paddingTop = getPaddingTop() + dp(28);
        float paddingBottom = getPaddingBottom() + dp(28);

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;
        if (chartWidth <= 0 || chartHeight <= 0) return;

        float maxVal = 0f;
        for (ChartDataPoint p : dataPoints) {
            if (p.value > maxVal) maxVal = p.value;
        }
        if (targetValue > maxVal) maxVal = targetValue;
        if (maxVal <= 0f) maxVal = 60f;
        maxVal = (float) Math.ceil(maxVal * 1.25f);

        // Horizontal grid
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            float fraction = (float) i / gridSteps;
            float y = paddingTop + chartHeight * (1f - fraction);
            float val = maxVal * fraction;

            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(Color.parseColor("#636366"));
            canvas.drawText(formatTimeLabel(val), paddingLeft - dp(6), y + dp(3), textPaint);
        }

        // Target Line
        if (targetValue > 0) {
            float targetY = paddingTop + chartHeight * (1f - (targetValue / maxVal));
            if (targetY >= paddingTop && targetY <= (paddingTop + chartHeight)) {
                canvas.drawLine(paddingLeft, targetY, width - paddingRight, targetY, targetPaint);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                textPaint.setColor(Color.parseColor("#FF9F0A"));
                canvas.drawText("TARGET " + formatTimeLabel(targetValue), width - paddingRight, targetY - dp(4), textPaint);
            }
        }

        int count = dataPoints.size();
        float stepX = (count > 1) ? (chartWidth / (count - 1)) : 0;

        linePath.reset();
        fillPath.reset();

        float[] pointXs = new float[count];
        float[] pointYs = new float[count];

        for (int i = 0; i < count; i++) {
            float x = (count > 1) ? (paddingLeft + i * stepX) : (paddingLeft + chartWidth / 2f);
            float y = paddingTop + chartHeight * (1f - (dataPoints.get(i).value / maxVal));
            pointXs[i] = x;
            pointYs[i] = y;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, paddingTop + chartHeight);
                fillPath.lineTo(x, y);
            } else {
                float prevX = pointXs[i - 1];
                float prevY = pointYs[i - 1];
                float midX = (prevX + x) / 2f;
                linePath.cubicTo(midX, prevY, midX, y, x, y);
                fillPath.cubicTo(midX, prevY, midX, y, x, y);
            }
        }

        if (count > 0) {
            fillPath.lineTo(pointXs[count - 1], paddingTop + chartHeight);
            fillPath.close();

            fillPaint.setShader(new LinearGradient(
                    0, paddingTop, 0, paddingTop + chartHeight,
                    Color.parseColor("#3330D158"), Color.parseColor("#00000000"),
                    Shader.TileMode.CLAMP
            ));
            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(linePath, linePaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < count; i++) {
            float px = pointXs[i];
            float py = pointYs[i];

            canvas.drawCircle(px, py, dp(5), pointOuterPaint);
            canvas.drawCircle(px, py, dp(2.5f), pointInnerPaint);

            boolean showLabel = (count <= 7) || (i % Math.max(1, count / 6) == 0) || (i == count - 1);
            if (showLabel) {
                textPaint.setColor(Color.parseColor("#8E8E93"));
                canvas.drawText(dataPoints.get(i).label, px, height - getPaddingBottom() - dp(6), textPaint);
            }
        }

        if (selectedIndex >= 0 && selectedIndex < count) {
            drawTooltip(canvas, pointXs[selectedIndex], pointYs[selectedIndex], dataPoints.get(selectedIndex));
        }
    }

    private void drawMovingAverageChart(Canvas canvas, int width, int height) {
        float paddingLeft = getPaddingLeft() + dp(38);
        float paddingRight = getPaddingRight() + dp(18);
        float paddingTop = getPaddingTop() + dp(34);
        float paddingBottom = getPaddingBottom() + dp(28);

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;
        if (chartWidth <= 0 || chartHeight <= 0) return;

        // Draw Legend at top
        textPaint.setTextAlign(Paint.Align.LEFT);
        ma7Paint.setStrokeWidth(dp(2.5f));
        canvas.drawLine(paddingLeft, dp(14), paddingLeft + dp(18), dp(14), ma7Paint);
        textPaint.setColor(Color.parseColor("#30D158"));
        canvas.drawText("7-Session MA", paddingLeft + dp(24), dp(18), textPaint);

        canvas.drawLine(paddingLeft + dp(110), dp(14), paddingLeft + dp(128), dp(14), ma30Paint);
        textPaint.setColor(Color.parseColor("#0A84FF"));
        canvas.drawText("30-Session MA", paddingLeft + dp(134), dp(18), textPaint);

        float maxVal = 0f;
        for (ChartDataPoint p : dataPoints) {
            if (p.value > maxVal) maxVal = p.value;
        }
        if (maxVal <= 0f) maxVal = 60f;
        maxVal = (float) Math.ceil(maxVal * 1.25f);

        // Grid
        for (int i = 0; i <= 4; i++) {
            float fraction = (float) i / 4;
            float y = paddingTop + chartHeight * (1f - fraction);
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(Color.parseColor("#636366"));
            canvas.drawText(formatTimeLabel(maxVal * fraction), paddingLeft - dp(6), y + dp(3), textPaint);
        }

        int count = dataPoints.size();
        float stepX = (count > 1) ? (chartWidth / (count - 1)) : 0;

        ma7Path.reset();
        ma30Path.reset();

        float[] ma7Vals = new float[count];
        float[] ma30Vals = new float[count];

        for (int i = 0; i < count; i++) {
            // Compute 7-session rolling average
            float sum7 = 0;
            int count7 = 0;
            for (int k = Math.max(0, i - 6); k <= i; k++) {
                sum7 += dataPoints.get(k).value;
                count7++;
            }
            ma7Vals[i] = sum7 / count7;

            // Compute 30-session rolling average
            float sum30 = 0;
            int count30 = 0;
            for (int k = Math.max(0, i - 29); k <= i; k++) {
                sum30 += dataPoints.get(k).value;
                count30++;
            }
            ma30Vals[i] = sum30 / count30;
        }

        for (int i = 0; i < count; i++) {
            float x = (count > 1) ? (paddingLeft + i * stepX) : (paddingLeft + chartWidth / 2f);
            float y7 = paddingTop + chartHeight * (1f - (ma7Vals[i] / maxVal));
            float y30 = paddingTop + chartHeight * (1f - (ma30Vals[i] / maxVal));

            if (i == 0) {
                ma7Path.moveTo(x, y7);
                ma30Path.moveTo(x, y30);
            } else {
                ma7Path.lineTo(x, y7);
                ma30Path.lineTo(x, y30);
            }
        }

        canvas.drawPath(ma30Path, ma30Paint);
        canvas.drawPath(ma7Path, ma7Paint);

        // Selected point readout
        if (selectedIndex >= 0 && selectedIndex < count) {
            float x = (count > 1) ? (paddingLeft + selectedIndex * stepX) : (paddingLeft + chartWidth / 2f);
            float y7 = paddingTop + chartHeight * (1f - (ma7Vals[selectedIndex] / maxVal));
            canvas.drawCircle(x, y7, dp(5), pointOuterPaint);
            canvas.drawCircle(x, y7, dp(2.5f), pointInnerPaint);

            String text = String.format(Locale.ROOT, "%s: 7-MA %.1fs • 30-MA %.1fs",
                    dataPoints.get(selectedIndex).label, ma7Vals[selectedIndex], ma30Vals[selectedIndex]);
            ChartDataPoint dp = new ChartDataPoint(dataPoints.get(selectedIndex).label, ma7Vals[selectedIndex], text);
            drawTooltip(canvas, x, y7, dp);
        }
    }

    private void drawBarChart(Canvas canvas, int width, int height) {
        float paddingLeft = getPaddingLeft() + dp(38);
        float paddingRight = getPaddingRight() + dp(18);
        float paddingTop = getPaddingTop() + dp(28);
        float paddingBottom = getPaddingBottom() + dp(28);

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;
        if (chartWidth <= 0 || chartHeight <= 0) return;

        float maxVal = 0f;
        for (ChartDataPoint p : dataPoints) {
            if (p.value > maxVal) maxVal = p.value;
        }
        if (targetValue > maxVal) maxVal = targetValue;
        if (maxVal <= 0f) maxVal = 30f;
        maxVal = (float) Math.ceil(maxVal * 1.25f);

        for (int i = 0; i <= 4; i++) {
            float fraction = (float) i / 4;
            float y = paddingTop + chartHeight * (1f - fraction);
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(Color.parseColor("#636366"));
            canvas.drawText(String.format(Locale.ROOT, "%dm", (int) (maxVal * fraction)), paddingLeft - dp(6), y + dp(3), textPaint);
        }

        if (targetValue > 0) {
            float targetY = paddingTop + chartHeight * (1f - (targetValue / maxVal));
            if (targetY >= paddingTop && targetY <= (paddingTop + chartHeight)) {
                canvas.drawLine(paddingLeft, targetY, width - paddingRight, targetY, targetPaint);
                textPaint.setTextAlign(Paint.Align.RIGHT);
                textPaint.setColor(Color.parseColor("#FF9F0A"));
                canvas.drawText("TARGET " + (int) targetValue + "m", width - paddingRight, targetY - dp(4), textPaint);
            }
        }

        int count = dataPoints.size();
        float slotWidth = chartWidth / count;
        float barWidth = Math.min(dp(28), slotWidth * 0.65f);

        for (int i = 0; i < count; i++) {
            ChartDataPoint dp = dataPoints.get(i);
            float centerX = paddingLeft + i * slotWidth + (slotWidth / 2f);
            float barHeight = (dp.value / maxVal) * chartHeight;
            float topY = paddingTop + (chartHeight - barHeight);
            float bottomY = paddingTop + chartHeight;

            RectF barRect = new RectF(centerX - barWidth / 2f, topY, centerX + barWidth / 2f, bottomY);
            Paint paintToUse = (i == selectedIndex) ? barHighlightPaint : barPaint;
            canvas.drawRoundRect(barRect, dp(4), dp(4), paintToUse);

            if (barHeight > dp(14)) {
                valueTextPaint.setColor(Color.parseColor("#FFFFFF"));
                canvas.drawText(String.format(Locale.ROOT, "%dm", (int) dp.value), centerX, topY - dp(4), valueTextPaint);
            }

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.parseColor("#8E8E93"));
            canvas.drawText(dp.label, centerX, height - getPaddingBottom() - dp(6), textPaint);
        }

        if (selectedIndex >= 0 && selectedIndex < count) {
            float centerX = paddingLeft + selectedIndex * slotWidth + (slotWidth / 2f);
            float barHeight = (dataPoints.get(selectedIndex).value / maxVal) * chartHeight;
            float topY = paddingTop + (chartHeight - barHeight);
            drawTooltip(canvas, centerX, topY, dataPoints.get(selectedIndex));
        }
    }

    private void drawHeatmap(Canvas canvas, int width, int height) {
        if (heatmapCells.isEmpty()) {
            drawEmptyState(canvas, width, height);
            return;
        }

        int cols = 7;
        float padX = dp(16);
        float padY = dp(24);
        float availW = width - padX * 2;
        float cellSize = Math.min(dp(32), (availW - dp(6) * (cols - 1)) / cols);
        float gap = dp(6);

        // Day of week labels (M T W T F S S)
        String[] dayNames = {"M", "T", "W", "T", "F", "S", "S"};
        textPaint.setColor(Color.parseColor("#636366"));
        textPaint.setTextSize(dp(10));
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < cols; i++) {
            float cx = padX + i * (cellSize + gap) + cellSize / 2f;
            canvas.drawText(dayNames[i], cx, padY - dp(6), textPaint);
        }

        for (int i = 0; i < heatmapCells.size(); i++) {
            HeatmapCell cell = heatmapCells.get(i);
            int col = i % cols;
            int row = i / cols;

            float x = padX + col * (cellSize + gap);
            float y = padY + row * (cellSize + gap);

            RectF rect = new RectF(x, y, x + cellSize, y + cellSize);

            // Determine intensity color
            int color;
            if (cell.minutes <= 0) {
                color = Color.parseColor("#1C1C1E"); // 0m
            } else if (cell.minutes < 30) {
                color = Color.parseColor("#3330D158"); // 1-29m
            } else if (cell.minutes < 60) {
                color = Color.parseColor("#8030D158"); // 30-59m
            } else {
                color = Color.parseColor("#FF30D158"); // 60m+
            }

            heatmapCellPaint.setColor(color);
            canvas.drawRoundRect(rect, dp(4), dp(4), heatmapCellPaint);

            // Draw day number inside tile
            valueTextPaint.setColor(cell.minutes >= 60 ? Color.parseColor("#000000") : Color.parseColor("#8E8E93"));
            canvas.drawText(String.valueOf(cell.dayOfMonth), rect.centerX(), rect.centerY() + dp(3), valueTextPaint);
        }

        // Draw Heatmap Tooltip
        if (selectedHeatmapIndex >= 0 && selectedHeatmapIndex < heatmapCells.size()) {
            HeatmapCell cell = heatmapCells.get(selectedHeatmapIndex);
            int col = selectedHeatmapIndex % cols;
            int row = selectedHeatmapIndex / cols;
            float x = padX + col * (cellSize + gap) + cellSize / 2f;
            float y = padY + row * (cellSize + gap);

            String tip = String.format(Locale.ROOT, "%s: %dm (%d sessions)",
                    cell.dateLabel, Math.round(cell.minutes), cell.sessionCount);
            drawTooltip(canvas, x, y, new ChartDataPoint(cell.dateLabel, cell.minutes, tip));
        }
    }

    private void drawTooltip(Canvas canvas, float anchorX, float anchorY, ChartDataPoint dp) {
        String text = (dp.tooltipText != null && !dp.tooltipText.isEmpty())
                ? dp.tooltipText
                : String.format(Locale.ROOT, "%s: %.1f", dp.label, dp.value);

        tooltipTextPaint.getTextBounds(text, 0, text.length(), textBounds);

        float paddingH = dp(12);
        float paddingV = dp(6);
        float tooltipWidth = textBounds.width() + paddingH * 2;
        float tooltipHeight = textBounds.height() + paddingV * 2;

        float tooltipX = anchorX - tooltipWidth / 2f;
        float tooltipY = anchorY - tooltipHeight - dp(10);

        if (tooltipX < dp(8)) tooltipX = dp(8);
        if (tooltipX + tooltipWidth > getWidth() - dp(8)) {
            tooltipX = getWidth() - dp(8) - tooltipWidth;
        }
        if (tooltipY < dp(8)) {
            tooltipY = anchorY + dp(12);
        }

        RectF rect = new RectF(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight);
        canvas.drawRoundRect(rect, dp(6), dp(6), tooltipBgPaint);
        canvas.drawText(text, rect.centerX(), rect.centerY() + textBounds.height() / 2f - dp(1), tooltipTextPaint);
    }

    private String formatTimeLabel(float seconds) {
        if (seconds >= 60) {
            int mins = (int) (seconds / 60);
            int remSec = (int) (seconds % 60);
            return (remSec == 0) ? mins + "m" : String.format(Locale.ROOT, "%dm%ds", mins, remSec);
        }
        return (int) seconds + "s";
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
