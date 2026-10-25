package com.example.timerapp.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages runtime visual styles, custom color themes, and accent highlights.
 *
 * Supports the 8 Minimalist Theme Library:
 * 1. Minimal Light: Clean #FAFAFA, dark charcoal text #111827, soft borders #E5E7EB
 * 2. Midnight Minimal: Deep navy #0B0F19, sky blue #38BDF8 accent
 * 3. Graphite Focus: Zinc dark #18181B, muted platinum #A1A1AA contrast
 * 4. Ocean Teal: Deep ocean #041E24, fresh mint #14B8A6 accent
 * 5. Soft Warm Neutral: Sepia cream #F5F0EB, warm espresso #3E2723 text
 * 6. AMOLED Black: Pitch black #000000, emerald green #10B981 accent
 * 7. Clean Blue: Slate dark #0F172A, electric blue #3B82F6 accent
 * 8. Low-Contrast Focus: Soft dim #1F2428, muted #C9D1D9 text
 */
public final class ThemeManager {

    public static final String PREFS_NAME = "question_timer_prefs";
    public static final String KEY_THEME = "app_theme_mode";
    public static final String KEY_ACCENT = "app_accent_color";

    // 8 Minimalist Theme Modes
    public static final String THEME_MINIMAL_LIGHT = "MINIMAL_LIGHT";
    public static final String THEME_MIDNIGHT_MINIMAL = "MIDNIGHT_MINIMAL";
    public static final String THEME_GRAPHITE_FOCUS = "GRAPHITE_FOCUS";
    public static final String THEME_OCEAN_TEAL = "OCEAN_TEAL";
    public static final String THEME_SOFT_WARM_NEUTRAL = "SOFT_WARM_NEUTRAL";
    public static final String THEME_AMOLED_BLACK = "AMOLED_BLACK";
    public static final String THEME_CLEAN_BLUE = "CLEAN_BLUE";
    public static final String THEME_LOW_CONTRAST_FOCUS = "LOW_CONTRAST_FOCUS";

    // Legacy Aliases for Backward Compatibility
    public static final String THEME_AMOLED = THEME_AMOLED_BLACK;
    public static final String THEME_SLATE = THEME_GRAPHITE_FOCUS;
    public static final String THEME_LIGHT = THEME_MINIMAL_LIGHT;
    public static final String THEME_SOLARIZED = THEME_OCEAN_TEAL;

    // Accent Color Modes
    public static final String ACCENT_GREEN = "GREEN";
    public static final String ACCENT_BLUE = "BLUE";
    public static final String ACCENT_ORANGE = "ORANGE";
    public static final String ACCENT_PURPLE = "PURPLE";
    public static final String ACCENT_SKY = "SKY";
    public static final String ACCENT_MINT = "MINT";
    public static final String ACCENT_PLATINUM = "PLATINUM";

    private ThemeManager() {}

    public static List<String> getAllThemes() {
        return Arrays.asList(
                THEME_AMOLED_BLACK,
                THEME_MIDNIGHT_MINIMAL,
                THEME_GRAPHITE_FOCUS,
                THEME_OCEAN_TEAL,
                THEME_CLEAN_BLUE,
                THEME_LOW_CONTRAST_FOCUS,
                THEME_MINIMAL_LIGHT,
                THEME_SOFT_WARM_NEUTRAL
        );
    }

    public static String getThemeDisplayName(String theme) {
        if (THEME_MINIMAL_LIGHT.equals(theme)) return "Minimal Light";
        if (THEME_MIDNIGHT_MINIMAL.equals(theme)) return "Midnight Minimal";
        if (THEME_GRAPHITE_FOCUS.equals(theme)) return "Graphite Focus";
        if (THEME_OCEAN_TEAL.equals(theme)) return "Ocean Teal";
        if (THEME_SOFT_WARM_NEUTRAL.equals(theme)) return "Soft Warm Neutral";
        if (THEME_AMOLED_BLACK.equals(theme) || "AMOLED".equals(theme)) return "AMOLED Black";
        if (THEME_CLEAN_BLUE.equals(theme)) return "Clean Blue";
        if (THEME_LOW_CONTRAST_FOCUS.equals(theme)) return "Low-Contrast Focus";
        return "AMOLED Black";
    }

    public static String getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_AMOLED_BLACK);
    }

    public static void setTheme(Context context, String theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    public static String getAccent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCENT, ACCENT_GREEN);
    }

    public static void setAccent(Context context, String accent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCENT, accent).apply();
    }

    public static boolean isLightTheme(Context context) {
        String theme = getTheme(context);
        return THEME_MINIMAL_LIGHT.equals(theme) || THEME_SOFT_WARM_NEUTRAL.equals(theme);
    }

    public static int getBackgroundColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#FAFAFA");
            case THEME_MIDNIGHT_MINIMAL:
                return Color.parseColor("#0B0F19");
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#18181B");
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#041E24");
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#F5F0EB");
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#0F172A");
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#1F2428");
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#000000");
        }
    }

    public static int getSurfaceColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#FFFFFF");
            case THEME_MIDNIGHT_MINIMAL:
                return Color.parseColor("#111827");
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#27272A");
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#082F38");
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#EFEBE9");
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#1E293B");
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#24292E");
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#121212");
        }
    }

    public static int getCardColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#F3F4F6");
            case THEME_MIDNIGHT_MINIMAL:
                return Color.parseColor("#1E293B");
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#3F3F46");
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#0F404B");
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#E6DFD8");
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#334155");
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#2F363D");
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#1C1C1E");
        }
    }

    public static int getBorderColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#E5E7EB");
            case THEME_MIDNIGHT_MINIMAL:
                return Color.parseColor("#334155");
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#52525B");
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#155E75");
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#D7CCC8");
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#475569");
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#444D56");
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#2C2C2E");
        }
    }

    public static int getTextPrimaryColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#111827");
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#3E2723");
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#C9D1D9");
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#F0FDFA");
            case THEME_MIDNIGHT_MINIMAL:
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#F8FAFC");
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#FAFAFA");
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#FFFFFF");
        }
    }

    public static int getTextSecondaryColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#6B7280");
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#795548");
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#8B949E");
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#99F6E4");
            case THEME_MIDNIGHT_MINIMAL:
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#94A3B8");
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#A1A1AA");
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#9E9E9E");
        }
    }

    public static int getDividerColor(Context context) {
        return getBorderColor(context);
    }

    public static int getAccentColor(Context context) {
        String theme = getTheme(context);

        // Native theme defaults matching prompt specification
        switch (theme) {
            case THEME_MIDNIGHT_MINIMAL:
                return Color.parseColor("#38BDF8"); // sky blue
            case THEME_GRAPHITE_FOCUS:
                return Color.parseColor("#A1A1AA"); // muted platinum
            case THEME_OCEAN_TEAL:
                return Color.parseColor("#14B8A6"); // fresh mint
            case THEME_SOFT_WARM_NEUTRAL:
                return Color.parseColor("#8D6E63"); // warm sepia accent
            case THEME_CLEAN_BLUE:
                return Color.parseColor("#3B82F6"); // electric blue
            case THEME_LOW_CONTRAST_FOCUS:
                return Color.parseColor("#58A6FF"); // soft blue
            case THEME_MINIMAL_LIGHT:
                return Color.parseColor("#2563EB"); // royal blue
            case THEME_AMOLED_BLACK:
            default:
                return Color.parseColor("#10B981"); // emerald green
        }
    }

    public static int getAccentBgColor(Context context) {
        int accent = getAccentColor(context);
        return Color.argb(35, Color.red(accent), Color.green(accent), Color.blue(accent));
    }
}
