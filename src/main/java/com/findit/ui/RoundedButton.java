package com.findit.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern SaaS styled button with rounded corners, subtle hover transitions, and multiple variants.
 */
public class RoundedButton extends JButton {

    public enum Style {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS,
        SIDEBAR
    }

    private Style style;
    private int cornerRadius = 10;
    private boolean isActive = false;
    private Icon leadingIcon = null;

    public void setStyle(Style style) {
        this.style = style;
        repaint();
    }

    public Style getStyle() {
        return style;
    }

    public RoundedButton(String text) {
        this(text, Style.PRIMARY);
    }

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        initButton();
    }

    public RoundedButton(String text, Icon icon, Style style) {
        super(text, icon);
        this.style = style;
        this.leadingIcon = icon;
        initButton();
        if (icon != null) {
            setIcon(icon);
            setIconTextGap(8);
        }
    }

    public void setLeadingIcon(Icon icon) {
        this.leadingIcon = icon;
        setIcon(icon);
        repaint();
    }

    public Icon getLeadingIcon() {
        return leadingIcon;
    }

    private void initButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (style == Style.SIDEBAR) {
            setFont(AppTheme.font(Font.BOLD, 13f));
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setPreferredSize(new Dimension(210, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        } else {
            setFont(AppTheme.font(Font.BOLD, 13f));
            setVerticalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(4, 14, 4, 14));
            setPreferredSize(new Dimension(140, 38));
        }
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setActive(boolean active) {
        this.isActive = active;
        repaint();
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        AppTheme.applyQualityRendering(g2);
        ButtonModel model = getModel();
        int w = getWidth();
        int h = getHeight();

        Color bg;
        Color fg;
        Color border = null;

        switch (style) {
            case PRIMARY -> {
                if (!isEnabled()) {
                    bg = AppTheme.isLight() ? new Color(0xF1, 0xF5, 0xF9) : new Color(0x1E, 0x24, 0x38);
                    fg = AppTheme.isLight() ? new Color(0x94, 0xA3, 0xB8) : new Color(0x47, 0x55, 0x69);
                    border = AppTheme.BORDER;
                } else if (model.isPressed()) {
                    bg = new Color(0x43, 0x38, 0xCA); // Indigo pressed #4338CA
                    fg = Color.WHITE;
                } else if (model.isRollover()) {
                    bg = AppTheme.PRIMARY_HOVER;       // Indigo hover
                    fg = Color.WHITE;
                } else {
                    bg = AppTheme.PRIMARY_INDIGO;      // Radiant indigo
                    fg = Color.WHITE;
                }
            }
            case SECONDARY -> {
                if (AppTheme.isLight()) {
                    if (!isEnabled()) {
                        bg = new Color(0xF1, 0xF5, 0xF9);
                        fg = new Color(0x94, 0xA3, 0xB8);
                        border = AppTheme.BORDER;
                    } else if (model.isPressed()) {
                        bg = new Color(0xE2, 0xE8, 0xF0);
                        fg = AppTheme.TEXT_PRIMARY;
                        border = new Color(0x94, 0xA3, 0xB8);
                    } else if (model.isRollover()) {
                        bg = new Color(0xF1, 0xF5, 0xF9);
                        fg = AppTheme.TEXT_PRIMARY;
                        border = new Color(0x94, 0xA3, 0xB8);
                    } else {
                        bg = AppTheme.BG_CARD;            // Crisp white
                        fg = AppTheme.TEXT_PRIMARY;        // Midnight slate #0F172A
                        border = AppTheme.FIELD_BORDER;    // Slate 300
                    }
                } else {
                    if (!isEnabled()) {
                        bg = new Color(0x16, 0x20, 0x32);
                        fg = new Color(0x47, 0x55, 0x69);
                        border = AppTheme.BORDER;
                    } else if (model.isPressed()) {
                        bg = new Color(0x0F, 0x17, 0x2A);
                        fg = AppTheme.TEXT_PRIMARY;
                        border = new Color(0x47, 0x55, 0x69);
                    } else if (model.isRollover()) {
                        bg = new Color(0x27, 0x35, 0x4A); // Elevated slate hover
                        fg = AppTheme.TEXT_PRIMARY;
                        border = new Color(0x47, 0x55, 0x69);
                    } else {
                        bg = AppTheme.FIELD_BG;            // Slate dark
                        fg = AppTheme.TEXT_PRIMARY;
                        border = AppTheme.BORDER;
                    }
                }
            }
            case DANGER -> {
                if (!isEnabled()) {
                    bg = AppTheme.isLight() ? new Color(0xFF, 0xE4, 0xE6) : new Color(0x2A, 0x15, 0x20);
                    fg = AppTheme.isLight() ? new Color(0xFB, 0x71, 0x85) : new Color(0x64, 0x74, 0x8B);
                } else if (model.isPressed()) {
                    bg = new Color(0x9F, 0x12, 0x39);
                    fg = Color.WHITE;
                } else if (model.isRollover()) {
                    bg = new Color(0xBE, 0x12, 0x3C);
                    fg = Color.WHITE;
                } else {
                    bg = AppTheme.DANGER;              // Radiant rose
                    fg = Color.WHITE;
                }
            }
            case SUCCESS -> {
                if (!isEnabled()) {
                    bg = AppTheme.isLight() ? new Color(0xDC, 0xFC, 0xE7) : new Color(0x11, 0x26, 0x1E);
                    fg = AppTheme.isLight() ? new Color(0x86, 0xEF, 0xAC) : new Color(0x64, 0x74, 0x8B);
                } else if (model.isPressed()) {
                    bg = new Color(0x06, 0x5F, 0x46);
                    fg = Color.WHITE;
                } else if (model.isRollover()) {
                    bg = new Color(0x05, 0x96, 0x69);
                    fg = Color.WHITE;
                } else {
                    bg = AppTheme.SUCCESS;             // Emerald
                    fg = Color.WHITE;
                }
            }
            case SIDEBAR -> {
                if (isActive) {
                    bg = AppTheme.BG_SIDEBAR_ACTIVE;   // Radiant indigo #4F46E5
                    fg = Color.WHITE;
                } else if (model.isRollover()) {
                    bg = AppTheme.BG_SIDEBAR_HOVER;    // #F1F5F9 in light, #1E293B in dark
                    fg = AppTheme.isLight() ? AppTheme.TEXT_PRIMARY : Color.WHITE;
                } else {
                    bg = new Color(0, 0, 0, 0);        // Transparent
                    fg = AppTheme.isLight() ? AppTheme.TEXT_SECONDARY : AppTheme.TEXT_ON_DARK_MUTED;
                }
            }
            default -> {
                bg = AppTheme.PRIMARY_INDIGO;
                fg = Color.WHITE;
            }
        }

        // Draw background
        if (bg.getAlpha() > 0) {
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        }

        // Draw border
        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, cornerRadius, cornerRadius));
        }

        // Active indicator on left for sidebar items
        if (style == Style.SIDEBAR && isActive) {
            g2.setColor(AppTheme.ACCENT_CYAN);
            g2.fillRoundRect(2, 6, 4, h - 12, 2, 2);
        }

        setForeground(fg);
        g2.dispose();

        super.paintComponent(g);
    }
}

