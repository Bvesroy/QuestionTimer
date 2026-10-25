package com.example.timerapp.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.timerapp.R;
import com.example.timerapp.model.TimerSession;
import com.example.timerapp.wallpaper.JeeWallpaperGenerator;

import java.io.File;
import java.util.List;

/**
 * AMOLED Modal Dialog for Mode B Wallpaper Preview, Selection and Export.
 */
public class WallpaperPreviewDialog extends Dialog {

    private final List<TimerSession> sessions;
    private final long examTargetMillis;
    private final int streak;
    private Bitmap currentBitmap;

    private ImageView ivWallpaperPreview;
    private Button btnApplyBoth;
    private Button btnApplyHome;
    private Button btnApplyLock;
    private Button btnRegenerate;
    private Button btnExportImage;
    private Button btnClose;

    public WallpaperPreviewDialog(Context context, List<TimerSession> sessions, long examTargetMillis, int streak) {
        super(context, R.style.Theme_FullScreenTimer_Dialog);
        this.sessions = sessions;
        this.examTargetMillis = (examTargetMillis > 0) ? examTargetMillis : JeeWallpaperGenerator.DEFAULT_JEE_2027_MILLIS;
        this.streak = streak;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_wallpaper_preview);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ivWallpaperPreview = findViewById(R.id.ivWallpaperPreview);
        btnApplyBoth = findViewById(R.id.btnApplyBoth);
        btnApplyHome = findViewById(R.id.btnApplyHome);
        btnApplyLock = findViewById(R.id.btnApplyLock);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnExportImage = findViewById(R.id.btnExportImage);
        btnClose = findViewById(R.id.btnCloseWallpaperDialog);

        generateAndDisplayBitmap();

        btnApplyBoth.setOnClickListener(v -> applyWallpaperTarget("BOTH"));
        btnApplyHome.setOnClickListener(v -> applyWallpaperTarget("HOME"));
        btnApplyLock.setOnClickListener(v -> applyWallpaperTarget("LOCK"));

        btnRegenerate.setOnClickListener(v -> generateAndDisplayBitmap());
        btnExportImage.setOnClickListener(v -> exportImage());
        btnClose.setOnClickListener(v -> dismiss());
    }

    private void generateAndDisplayBitmap() {
        JeeWallpaperGenerator.WallpaperMetrics metrics =
                JeeWallpaperGenerator.WallpaperMetrics.fromSessions(sessions, examTargetMillis, streak);
        currentBitmap = JeeWallpaperGenerator.generateWallpaper(getContext(), metrics);
        if (ivWallpaperPreview != null && currentBitmap != null) {
            ivWallpaperPreview.setImageBitmap(currentBitmap);
        }
    }

    private void applyWallpaperTarget(String target) {
        if (currentBitmap == null) return;
        boolean success = JeeWallpaperGenerator.applyWallpaper(getContext(), currentBitmap, target);
        if (success) {
            Toast.makeText(getContext(), "✓ Wallpaper applied to " + target + " screen", Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            Toast.makeText(getContext(), "Could not set wallpaper (check permissions)", Toast.LENGTH_LONG).show();
        }
    }

    private void exportImage() {
        if (currentBitmap == null) return;
        try {
            File file = JeeWallpaperGenerator.exportWallpaperImage(getContext(), currentBitmap);
            Toast.makeText(getContext(), "Saved to " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to export: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Avoid leaking bitmap memory
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            // Keep reference released
            currentBitmap = null;
        }
    }
}
