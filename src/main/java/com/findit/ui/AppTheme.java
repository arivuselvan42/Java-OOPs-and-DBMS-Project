package com.findit.ui;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Premium, Colorful, Modern SaaS Design System for FindIt.
 * Supports dynamic switching between:
 * - Modern Light Theme: Clean, airy Slate-50 canvas with pure white elevated cards,
 *   crisp typography, vibrant pastel badges, and radiant indigo accents.
 * - Midnight Dark Theme: Deep Navy surfaces with electric indigo, cyan, emerald, and amber accents.
 */
public final class AppTheme {

    public enum ThemeMode {
        LIGHT,
        DARK
    }

    private static final String THEME_FILE = "theme.properties";
    private static ThemeMode currentMode = ThemeMode.LIGHT;
    private static final List<Runnable> themeChangeListeners = new ArrayList<>();

    // ---------- Surface Colors (Dynamic) ----------
    public static Color BG_MAIN;
    public static Color BG_CARD;
    public static Color BG_CARD_HOVER;
    public static Color BG_SIDEBAR;
    public static Color BG_SIDEBAR_HOVER;
    public static Color BG_SIDEBAR_ACTIVE;
    public static Color FIELD_BG;
    public static Color FIELD_BORDER;

    // ---------- High-Contrast Typography ----------
    public static Color TEXT_PRIMARY;
    public static Color TEXT_SECONDARY;
    public static Color TEXT_MUTED;
    public static Color TEXT_ON_DARK;
    public static Color TEXT_ON_DARK_MUTED;

    // ---------- Borders & Dividers ----------
    public static Color BORDER;
    public static Color BORDER_LIGHT;
    public static Color BORDER_FOCUS;

    // ---------- Primary & Interactive Colours ----------
    public static Color PRIMARY_INDIGO;
    public static Color PRIMARY_HOVER;
    public static Color PRIMARY_NAVY;
    public static Color PRIMARY_VIOLET;
    public static Color SECONDARY_BLUE;
    public static Color SECONDARY_HOVER;
    public static Color ACCENT_CYAN;

    // Emerald Green & Teal (Success / Open)
    public static Color SUCCESS;
    public static Color SUCCESS_BG;
    public static Color SUCCESS_TEXT;

    // Amber & Orange (Warning / Claims Pending)
    public static Color WARNING;
    public static Color WARNING_BG;
    public static Color WARNING_TEXT;

    // Coral Rose & Crimson (Danger / Lost Items)
    public static Color DANGER;
    public static Color DANGER_BG;
    public static Color DANGER_TEXT;

    // Soft Purple & Violet (Claimed Items)
    public static Color PURPLE;
    public static Color PURPLE_BG;
    public static Color PURPLE_TEXT;

    // Electric Teal / Sky (Found Items)
    public static Color INFO;
    public static Color INFO_BG;
    public static Color INFO_TEXT;

    // Returned (Neutral Slate)
    public static Color RETURNED_BG;
    public static Color RETURNED_TEXT;

    static {
        currentMode = loadThemePreference();
        applyPalette(currentMode);
    }

    private AppTheme() {}

    private static ThemeMode loadThemePreference() {
        File file = new File(THEME_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);
                String val = props.getProperty("theme", "LIGHT");
                return "DARK".equalsIgnoreCase(val) ? ThemeMode.DARK : ThemeMode.LIGHT;
            } catch (Exception ignored) {}
        }
        return ThemeMode.LIGHT; // Default to Modern Light theme
    }

    private static void saveThemePreference(ThemeMode mode) {
        try (FileOutputStream out = new FileOutputStream(THEME_FILE)) {
            Properties props = new Properties();
            props.setProperty("theme", mode.name());
            props.store(out, "FindIt Theme Configuration");
        } catch (Exception ignored) {}
    }

    public static synchronized void setThemeMode(ThemeMode mode) {
        if (mode == null) mode = ThemeMode.LIGHT;
        currentMode = mode;
        applyPalette(mode);
        saveThemePreference(mode);
        notifyThemeChange();
    }

    public static synchronized void toggleTheme() {
        setThemeMode(currentMode == ThemeMode.LIGHT ? ThemeMode.DARK : ThemeMode.LIGHT);
    }

    public static boolean isLight() {
        return currentMode == ThemeMode.LIGHT;
    }

    public static boolean isDark() {
        return currentMode == ThemeMode.DARK;
    }

    public static ThemeMode getThemeMode() {
        return currentMode;
    }

    public static void addThemeChangeListener(Runnable listener) {
        if (listener != null && !themeChangeListeners.contains(listener)) {
            themeChangeListeners.add(listener);
        }
    }

    public static void removeThemeChangeListener(Runnable listener) {
        themeChangeListeners.remove(listener);
    }

    public static void notifyThemeChange() {
        for (Runnable listener : new ArrayList<>(themeChangeListeners)) {
            try {
                listener.run();
            } catch (Exception ignored) {}
        }

        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                window.revalidate();
                window.repaint();
            }
        }
    }

    private static void applyPalette(ThemeMode mode) {
        if (mode == ThemeMode.LIGHT) {
            // Modern SaaS Light Palette
            BG_MAIN          = new Color(0xF8, 0xFA, 0xFC); // Slate-50 cool mist canvas
            BG_CARD          = new Color(0xFF, 0xFF, 0xFF); // Crisp pure white #FFFFFF
            BG_CARD_HOVER    = new Color(0xF1, 0xF5, 0xF9); // Slate-100 #F1F5F9
            BG_SIDEBAR       = new Color(0xFF, 0xFF, 0xFF); // Clean white sidebar #FFFFFF
            BG_SIDEBAR_HOVER = new Color(0xF1, 0xF5, 0xF9); // Soft slate hover
            BG_SIDEBAR_ACTIVE= new Color(0x4F, 0x46, 0xE5); // Radiant rich indigo #4F46E5
            FIELD_BG         = new Color(0xFF, 0xFF, 0xFF); // Crisp white input
            FIELD_BORDER     = new Color(0xCB, 0xD5, 0xE1); // Slate-300 border #CBD5E1

            TEXT_PRIMARY     = new Color(0x0F, 0x17, 0x2A); // Midnight Slate-900 #0F172A
            TEXT_SECONDARY   = new Color(0x47, 0x55, 0x69); // Slate-600 #475569
            TEXT_MUTED       = new Color(0x64, 0x74, 0x8B); // Slate-500 #64748B
            TEXT_ON_DARK     = new Color(0xFF, 0xFF, 0xFF);
            TEXT_ON_DARK_MUTED = new Color(0x94, 0xA3, 0xB8);

            BORDER           = new Color(0xE2, 0xE8, 0xF0); // Slate-200 border #E2E8F0
            BORDER_LIGHT     = new Color(0xF1, 0xF5, 0xF9); // Slate-100 divider #F1F5F9
            BORDER_FOCUS     = new Color(0x4F, 0x46, 0xE5); // Indigo focus ring #4F46E5

            PRIMARY_INDIGO   = new Color(0x4F, 0x46, 0xE5); // Rich Indigo #4F46E5
            PRIMARY_HOVER    = new Color(0x43, 0x38, 0xCA); // Deep Indigo #4338CA
            PRIMARY_NAVY     = new Color(0x4F, 0x46, 0xE5);
            PRIMARY_VIOLET   = new Color(0x7C, 0x3A, 0xED);
            SECONDARY_BLUE   = new Color(0x25, 0x63, 0xEB); // Vibrant Blue #2563EB
            SECONDARY_HOVER  = new Color(0x1D, 0x4E, 0xD8);
            ACCENT_CYAN      = new Color(0x02, 0x84, 0xC7); // Sky Cyan-600 #0284C7

            SUCCESS          = new Color(0x05, 0x96, 0x69); // Emerald-600 #059669
            SUCCESS_BG       = new Color(0xEC, 0xFD, 0xF5); // Emerald-50 light tint
            SUCCESS_TEXT     = new Color(0x06, 0x5F, 0x46); // Emerald-800 deep text

            WARNING          = new Color(0xD9, 0x77, 0x06); // Amber-600 #D97706
            WARNING_BG       = new Color(0xFF, 0xFB, 0xEB); // Amber-50 light tint
            WARNING_TEXT     = new Color(0x92, 0x40, 0x0E); // Amber-800 deep text

            DANGER           = new Color(0xE1, 0x1D, 0x48); // Rose-600 #E11D48
            DANGER_BG         = new Color(0xFF, 0xF1, 0xF2); // Rose-50 light tint
            DANGER_TEXT       = new Color(0x9F, 0x12, 0x39); // Rose-800 deep text

            PURPLE           = new Color(0x7C, 0x3A, 0xED); // Purple-600 #7C3AED
            PURPLE_BG        = new Color(0xF5, 0xF3, 0xFF); // Purple-50 light tint
            PURPLE_TEXT      = new Color(0x5B, 0x21, 0xB6); // Purple-800 deep text

            INFO             = new Color(0x02, 0x84, 0xC7); // Sky-600 #0284C7
            INFO_BG          = new Color(0xF0, 0xF9, 0xFF); // Sky-50 light tint
            INFO_TEXT        = new Color(0x07, 0x59, 0x85); // Sky-800 deep text

            RETURNED_BG      = new Color(0xF1, 0xF5, 0xF9); // Soft slate #F1F5F9
            RETURNED_TEXT    = new Color(0x47, 0x55, 0x69); // Slate-600 #475569
        } else {
            // Midnight Dark Palette
            BG_MAIN          = new Color(0x0F, 0x17, 0x2A); // Midnight Navy #0F172A
            BG_CARD          = new Color(0x1E, 0x29, 0x3B); // Elevated Slate #1E293B
            BG_CARD_HOVER    = new Color(0x27, 0x35, 0x49);
            BG_SIDEBAR       = new Color(0x0A, 0x0F, 0x1D); // Deepest Midnight #0A0F1D
            BG_SIDEBAR_HOVER = new Color(0x1E, 0x29, 0x3B);
            BG_SIDEBAR_ACTIVE= new Color(0x4F, 0x46, 0xE5); // Rich Indigo #4F46E5
            FIELD_BG         = new Color(0x0F, 0x17, 0x2A); // Dark input surface
            FIELD_BORDER     = new Color(0x33, 0x41, 0x55); // Slate border #334155

            TEXT_PRIMARY     = new Color(0xFF, 0xFF, 0xFF); // Pure White #FFFFFF
            TEXT_SECONDARY   = new Color(0xCB, 0xD5, 0xE1); // Soft Light Slate #CBD5E1
            TEXT_MUTED       = new Color(0x94, 0xA3, 0xB8); // Cool Slate #94A3B8
            TEXT_ON_DARK     = new Color(0xFF, 0xFF, 0xFF);
            TEXT_ON_DARK_MUTED = new Color(0x94, 0xA3, 0xB8);

            BORDER           = new Color(0x33, 0x41, 0x55); // #334155
            BORDER_LIGHT     = new Color(0x1E, 0x29, 0x3B); // #1E293B
            BORDER_FOCUS     = new Color(0x3B, 0x82, 0xF6); // Electric Blue #3B82F6

            PRIMARY_INDIGO   = new Color(0x63, 0x66, 0xF1); // Rich Indigo #6366F1
            PRIMARY_HOVER    = new Color(0x4F, 0x46, 0xE5); // Deep Indigo #4F46E5
            PRIMARY_NAVY     = new Color(0x63, 0x66, 0xF1);
            PRIMARY_VIOLET   = new Color(0x7C, 0x3A, 0xED);
            SECONDARY_BLUE   = new Color(0x3B, 0x82, 0xF6); // Electric Blue #3B82F6
            SECONDARY_HOVER  = new Color(0x25, 0x63, 0xEB);
            ACCENT_CYAN      = new Color(0x06, 0xB6, 0xD4); // Electric Cyan #06B6D4

            SUCCESS          = new Color(0x10, 0xB9, 0x81); // Emerald #10B981
            SUCCESS_BG       = new Color(0x06, 0x4E, 0x3B); // Dark Emerald #064E3B
            SUCCESS_TEXT     = new Color(0x34, 0xD3, 0x99); // Mint #34D399

            WARNING          = new Color(0xF5, 0x9E, 0x0B); // Amber #F59E0B
            WARNING_BG       = new Color(0x45, 0x1A, 0x03); // Dark Amber #451A03
            WARNING_TEXT     = new Color(0xFB, 0xBF, 0x24); // Warm Amber #FBBF24

            DANGER           = new Color(0xF4, 0x3F, 0x5E); // Coral Rose #F43F5E
            DANGER_BG         = new Color(0x4C, 0x05, 0x19); // Dark Rose #4C0519
            DANGER_TEXT       = new Color(0xFD, 0xA4, 0xAF); // Bright Rose #FDA4AF

            PURPLE           = new Color(0x8B, 0x5C, 0xF6); // Soft Purple #8B5CF6
            PURPLE_BG        = new Color(0x3B, 0x07, 0x64); // Dark Purple #3B0764
            PURPLE_TEXT      = new Color(0xE9, 0xD5, 0xFF); // Lilac #E9D5FF

            INFO             = new Color(0x0E, 0xA5, 0xE9); // Sky Blue #0EA5E9
            INFO_BG          = new Color(0x0C, 0x4A, 0x6E); // Dark Sky #0C4A6E
            INFO_TEXT        = new Color(0x7D, 0xD3, 0xFC); // Soft Cyan #7DD3FC

            RETURNED_BG      = new Color(0x1E, 0x29, 0x3B);
            RETURNED_TEXT    = new Color(0xCB, 0xD5, 0xE1);
        }
    }

    // ---------- Typography ----------
    private static final String FONT_FAMILY = selectSystemFont();

    private static String selectSystemFont() {
        String[] preferred = {"Segoe UI", "Inter", "SF Pro Display", "Helvetica Neue", "Arial"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String target : preferred) {
            for (String family : available) {
                if (family.equalsIgnoreCase(target)) {
                    return family;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, (int) size).deriveFont(size);
    }

    public static Font regular(float size) {
        return font(Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return font(Font.BOLD, size);
    }

    // ---------- Rendering Helpers ----------
    public static void applyQualityRendering(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static void drawRoundedCard(Graphics2D g2, int width, int height, int arc, Color bg, Color border) {
        applyQualityRendering(g2);
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, arc, arc));
        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, arc, arc));
        }
    }
}
