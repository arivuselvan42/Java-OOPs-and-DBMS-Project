package com.findit.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared look-and-feel helpers and modern components for FindIt.
 * Harmonized with AppTheme for a dark and colorful SaaS design system.
 */
final class UiKit {

    private UiKit() { }

    // ---------- Palette (Dark & Colorful) ----------
    public static final Color BG_TOP        = new Color(15, 23, 42);     // #0F172A
    public static final Color BG_BOTTOM     = new Color(11, 15, 25);     // #0B0F19
    public static final Color NAVY          = AppTheme.PRIMARY_INDIGO;   // #6366F1
    public static final Color BLUE          = AppTheme.PRIMARY_INDIGO;   // #6366F1
    public static final Color BLUE_HOVER    = AppTheme.PRIMARY_HOVER;    // #4F46E5
    public static final Color BLUE_PRESSED  = new Color(67, 56, 202);
    public static final Color BLUE_LIGHT    = AppTheme.ACCENT_CYAN;      // #06B6D4
    public static final Color TEXT          = AppTheme.TEXT_PRIMARY;     // #FFFFFF
    public static final Color MUTED         = AppTheme.TEXT_SECONDARY;   // #CBD5E1
    public static final Color BORDER        = AppTheme.BORDER;           // #334155
    public static final Color FIELD_BG      = AppTheme.FIELD_BG;         // #0F172A
    public static final Color ERROR         = AppTheme.DANGER;           // #F43F5E
    public static final Color SUCCESS       = AppTheme.SUCCESS;          // #10B981

    // ---------- Fonts ----------
    public static Font font(int style, float size) {
        return AppTheme.font(style, size);
    }

    public static Font font(int style, int size) {
        return AppTheme.font(style, (float) size);
    }

    public static JLabel label(String text, int style, int size, Color color, int align) {
        JLabel label = new JLabel(text, align);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    public static void smooth(Graphics2D g2) {
        AppTheme.applyQualityRendering(g2);
    }

    public static void onChange(JTextComponent component, Runnable action) {
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { action.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    // ---------- Modern ComboBox Styling ----------

    /**
     * Styles any JComboBox with a premium dark SaaS appearance:
     * - Dark background with clear white text (fixes white-on-white bug)
     * - Anti-aliased custom chevron dropdown arrow
     * - Sleek rounded border with focus glow
     * - Beautiful popup list with active row highlights
     */
    public static <T> void styleComboBox(JComboBox<T> combo) {
        combo.setOpaque(false);
        combo.setFont(AppTheme.font(Font.PLAIN, 13f));
        combo.setBackground(AppTheme.FIELD_BG);
        combo.setForeground(AppTheme.TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 42));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        combo.setMinimumSize(new Dimension(100, 42));

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        smooth(g2);
                        int w = getWidth();
                        int h = getHeight();

                        // Chevron down arrow
                        g2.setColor(getModel().isRollover() ? AppTheme.ACCENT_CYAN : AppTheme.TEXT_MUTED);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int cx = w / 2;
                        int cy = h / 2;
                        Path2D chevron = new Path2D.Float();
                        chevron.moveTo(cx - 5, cy - 2);
                        chevron.lineTo(cx, cy + 3);
                        chevron.lineTo(cx + 5, cy - 2);
                        g2.draw(chevron);
                        g2.dispose();
                    }
                };
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                button.setBorder(new EmptyBorder(0, 0, 0, 8));
                return button;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                Graphics2D g2 = (Graphics2D) g.create();
                smooth(g2);
                g2.setColor(AppTheme.FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10));
                g2.dispose();
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) comboBox.getRenderer();
                Component c = renderer.getListCellRendererComponent(
                        listBox, comboBox.getSelectedItem(), -1, false, false);
                c.setFont(comboBox.getFont());
                c.setForeground(AppTheme.TEXT_PRIMARY);
                c.setBackground(AppTheme.FIELD_BG);

                boolean shouldValidate = false;
                if (c instanceof JPanel) {
                    shouldValidate = true;
                }
                int x = bounds.x + 8;
                int y = bounds.y;
                int w = bounds.width - 8;
                int h = bounds.height;
                currentValuePane.paintComponent(g, c, comboBox, x, y, w, h, shouldValidate);
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane scroller = super.createScroller();
                        scroller.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
                        scroller.getViewport().setBackground(AppTheme.BG_CARD);
                        return scroller;
                    }

                    @Override
                    public void show() {
                        super.show();
                        if (list != null) {
                            list.setBackground(AppTheme.BG_CARD);
                            list.setSelectionBackground(AppTheme.PRIMARY_INDIGO);
                            list.setSelectionForeground(Color.WHITE);
                        }
                    }
                };
                popup.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
                return popup;
            }
        });

        // Custom Renderer that guarantees pure white text and vibrant selection
        combo.setRenderer(new ListCellRenderer<T>() {
            private final JLabel label = new JLabel();

            {
                label.setOpaque(true);
                label.setFont(AppTheme.font(Font.PLAIN, 13f));
                label.setBorder(new EmptyBorder(8, 12, 8, 12));
            }

            @Override
            public Component getListCellRendererComponent(
                    JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
                label.setText(value != null ? value.toString() : "");

                if (isSelected && index >= 0) {
                    label.setBackground(AppTheme.PRIMARY_INDIGO);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(index < 0 ? AppTheme.FIELD_BG : AppTheme.BG_CARD);
                    label.setForeground(AppTheme.TEXT_PRIMARY);
                }
                return label;
            }
        });

        // Outer styling with focus border
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.FIELD_BORDER, 1, true),
                new EmptyBorder(2, 6, 2, 2)
        ));
    }

    // ---------- Building blocks ----------

    /** Window background: dynamic vertical gradient adapting to active theme. */
    public static final class GradientPanel extends JPanel {
        public GradientPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color top = AppTheme.isLight() ? new Color(0xF8, 0xFA, 0xFC) : BG_TOP;
            Color bottom = AppTheme.isLight() ? new Color(0xEE, 0xF2, 0xF6) : BG_BOTTOM;
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Elevated card panel with ambient drop shadow and subtle border. */
    public static final class CardPanel extends JPanel {
        private static final int SHADOW = 8;
        private static final int ARC = 18;

        public CardPanel(int verticalPadding, int horizontalPadding) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(
                    SHADOW + verticalPadding, SHADOW + horizontalPadding,
                    SHADOW + verticalPadding, SHADOW + horizontalPadding));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();

            // Diffused ambient drop shadow
            if (AppTheme.isLight()) {
                for (int i = 0; i < SHADOW; i++) {
                    g2.setColor(new Color(15, 23, 42, 6));
                    g2.fillRoundRect(i, i + 2, w - 2 * i, h - 2 * i - 2, ARC + (SHADOW - i), ARC + (SHADOW - i));
                }
            } else {
                for (int i = 0; i < SHADOW; i++) {
                    g2.setColor(new Color(0, 0, 0, 15));
                    g2.fillRoundRect(i, i + 2, w - 2 * i, h - 2 * i - 2, ARC + (SHADOW - i), ARC + (SHADOW - i));
                }
            }

            // Card background
            g2.setColor(AppTheme.BG_CARD);
            g2.fillRoundRect(SHADOW, SHADOW, w - 2 * SHADOW, h - 2 * SHADOW, ARC, ARC);
            g2.setColor(AppTheme.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(SHADOW, SHADOW, w - 2 * SHADOW - 1, h - 2 * SHADOW - 1, ARC, ARC);
            g2.dispose();
        }
    }

    /** Vertical stack where every child stretches to the full column width. */
    public static final class Column extends JPanel {
        private final int width;
        private int row = 0;

        public Column(int width) {
            super(new GridBagLayout());
            this.width = width;
            setOpaque(false);
        }

        public void addRow(JComponent component, int topGap) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(topGap, 0, 0, 0);
            add(component, gbc);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, width), d.height);
        }
    }

    /**
     * Paints the official FindIt Royal Blue Rounded App Icon with Bold White Magnifier
     * matching the attached brand design.
     */
    public static void drawFindItLogo(Graphics2D g2, int x, int y, int size) {
        smooth(g2);

        // 1. Vibrant Royal Blue Rounded Background
        int arc = Math.max(6, (int) (size * 0.28f));
        g2.setColor(new Color(0x3B, 0x82, 0xF6)); // #3B82F6 Royal Blue
        g2.fill(new RoundRectangle2D.Float(x, y, size, size, arc, arc));

        // 2. Bold White Magnifying Glass
        float strokeWidth = Math.max(2.2f, size * 0.115f);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Circle lens centered slightly upper-left
        float cx = x + size * 0.43f;
        float cy = y + size * 0.43f;
        float r = size * 0.19f;
        g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f));

        // Diagonal handle at 45 degrees pointing to bottom-right
        float cos45 = 0.7071f;
        float sin45 = 0.7071f;
        float startX = cx + r * cos45;
        float startY = cy + r * sin45;
        float handleLength = size * 0.21f;
        float endX = startX + handleLength * cos45;
        float endY = startY + handleLength * sin45;
        g2.draw(new Line2D.Float(startX, startY, endX, endY));
    }

    /** Modern glowing Royal Blue app badge with bold white magnifier. */
    public static final class LogoBadge extends JComponent {
        private static final int SIZE = 56;

        public LogoBadge() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int x = (getWidth() - SIZE) / 2;
            int y = Math.max(0, (getHeight() - SIZE) / 2);
            drawFindItLogo(g2, x, y, SIZE);
            g2.dispose();
        }
    }

    /** Text input drawn as an elevated dark box with glowing focus outline. */
    public static final class InputBox extends JPanel {
        private final JTextField field;
        private boolean focused;

        public InputBox(JTextField field, JComponent trailing) {
            super(new BorderLayout(8, 0));
            this.field = field;
            setOpaque(false);
            setBorder(new EmptyBorder(0, 14, 0, trailing == null ? 14 : 8));
            setPreferredSize(new Dimension(200, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setMinimumSize(new Dimension(100, 44));

            if (field instanceof JPasswordField) {
                field.setUI(new javax.swing.plaf.basic.BasicPasswordFieldUI());
            } else {
                field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
            }
            field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            field.setOpaque(false);
            field.setBackground(new Color(0, 0, 0, 0));
            field.setFont(AppTheme.font(Font.PLAIN, 13.5f));
            field.setForeground(AppTheme.TEXT_PRIMARY);
            field.setCaretColor(AppTheme.ACCENT_CYAN);
            add(field, BorderLayout.CENTER);

            if (trailing != null) {
                add(trailing, BorderLayout.EAST);
            }

            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true;  repaint(); }
                @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
            });
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (field != null) {
                if (field instanceof JPasswordField) {
                    field.setUI(new javax.swing.plaf.basic.BasicPasswordFieldUI());
                } else {
                    field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
                }
                field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                field.setOpaque(false);
                field.setBackground(new Color(0, 0, 0, 0));
                field.setForeground(AppTheme.TEXT_PRIMARY);
                field.setCaretColor(AppTheme.ACCENT_CYAN);
            }
        }

        @Override
        protected void paintChildren(Graphics g) {
            if (field != null) {
                if (!(field.getBorder() instanceof EmptyBorder)) {
                    field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                }
                if (field.isOpaque()) {
                    field.setOpaque(false);
                }
                field.setBackground(new Color(0, 0, 0, 0));
                field.setForeground(AppTheme.TEXT_PRIMARY);
            }
            super.paintChildren(g);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(AppTheme.FIELD_BG);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));

            g2.setColor(focused ? AppTheme.BORDER_FOCUS : AppTheme.FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 10, 10));
            g2.dispose();
        }
    }

    /** Plain text field in a rounded box. */
    public static InputBox textBox(JTextField field) {
        return new InputBox(field, null);
    }

    /** Password field in a rounded box with a Show / Hide toggle. */
    public static InputBox passwordBox(JPasswordField field) {
        char defaultEcho = field.getEchoChar();

        JButton toggle = new JButton("Show");
        toggle.setFont(AppTheme.font(Font.BOLD, 12f));
        toggle.setForeground(AppTheme.ACCENT_CYAN);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setFocusable(false);
        toggle.setBorder(new EmptyBorder(0, 4, 0, 4));
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            boolean currentlyHidden = field.getEchoChar() != 0;
            field.setEchoChar(currentlyHidden ? (char) 0 : defaultEcho);
            toggle.setText(currentlyHidden ? "Hide" : "Show");
        });

        return new InputBox(field, toggle);
    }

    /** Bold caption above an input. */
    public static JPanel labeled(String caption, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(label(caption, Font.BOLD, 12, AppTheme.TEXT_SECONDARY, SwingConstants.LEFT), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        int prefH = input.getPreferredSize().height > 0 ? input.getPreferredSize().height : 44;
        panel.setPreferredSize(new Dimension(input.getPreferredSize().width, prefH + 26));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefH + 26));
        return panel;
    }

    /** --- text --- separator. */
    public static JPanel divider(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 1;
        panel.add(line(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 12, 0, 12);
        panel.add(label(text, Font.PLAIN, 12, MUTED, SwingConstants.CENTER), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(line(), gbc);

        return panel;
    }

    public static Component vgap(int h) {
        Box.Filler filler = (Box.Filler) Box.createVerticalStrut(h);
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    private static JComponent line() {
        JSeparator separator = new JSeparator();
        separator.setForeground(AppTheme.BORDER);
        return separator;
    }

    /**
     * Modern SaaS Search Bar Component:
     * - Vector Magnifying Glass Icon
     * - Ghost placeholder text
     * - Vector clear "x" button
     * - Glowing electric-blue focus outline
     */
    public static final class SearchBox extends JPanel {
        private final JTextField field;
        private final JButton clearBtn;
        private boolean focused = false;
        private final String placeholder;

        public SearchBox(JTextField field, String placeholder) {
            super(new BorderLayout(10, 0));
            this.field = field;
            this.placeholder = placeholder != null ? placeholder : "Search...";

            setOpaque(false);
            setPreferredSize(new Dimension(320, 42));
            setMaximumSize(new Dimension(460, 42));
            setBorder(new EmptyBorder(0, 10, 0, 8));

            // Left: Official FindIt Royal Blue Magnifier Badge Icon
            JComponent searchIcon = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    int iconSize = 26;
                    int y = (getHeight() - iconSize) / 2;
                    drawFindItLogo(g2, 0, y, iconSize);
                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(28, 28);
                }
            };

            // Center: Text Field with Ghost Placeholder
            field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
            field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            field.setOpaque(false);
            field.setBackground(new Color(0, 0, 0, 0));
            field.setFont(AppTheme.font(Font.PLAIN, 12.5f));
            field.setForeground(AppTheme.TEXT_PRIMARY);
            field.setCaretColor(AppTheme.ACCENT_CYAN);

            JPanel centerPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (field.getText().isEmpty()) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        smooth(g2);
                        g2.setFont(AppTheme.font(Font.PLAIN, 12f));
                        g2.setColor(AppTheme.isLight() ? new Color(0x64, 0x74, 0x8B) : new Color(0x94, 0xA3, 0xB8));
                        FontMetrics fm = g2.getFontMetrics();
                        int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                        g2.drawString(placeholder, 2, y);
                        g2.dispose();
                    }
                }
            };
            centerPanel.setOpaque(false);
            centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            centerPanel.add(field, BorderLayout.CENTER);

            // Right: Clear Button (Aesthetic circular dismiss pill)
            clearBtn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    smooth(g2);
                    ButtonModel model = getModel();
                    int w = getWidth();
                    int h = getHeight();

                    // Perfectly centered 18x18 circular badge pill
                    int badgeSize = 18;
                    int bx = (w - badgeSize) / 2;
                    int by = (h - badgeSize) / 2;

                    // Capsule pill background
                    if (AppTheme.isLight()) {
                        if (model.isPressed()) {
                            g2.setColor(new Color(0xCB, 0xD5, 0xE1));
                        } else if (model.isRollover()) {
                            g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                        } else {
                            g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                        }
                    } else {
                        if (model.isPressed()) {
                            g2.setColor(new Color(0x47, 0x55, 0x69));
                        } else if (model.isRollover()) {
                            g2.setColor(new Color(0x33, 0x41, 0x55));
                        } else {
                            g2.setColor(new Color(0x1E, 0x29, 0x3B, 210));
                        }
                    }
                    g2.fillOval(bx, by, badgeSize, badgeSize);

                    // Crisp ring
                    if (AppTheme.isLight()) {
                        g2.setColor(model.isRollover() ? AppTheme.PRIMARY_INDIGO : new Color(0xCB, 0xD5, 0xE1));
                    } else {
                        g2.setColor(model.isRollover() ? AppTheme.ACCENT_CYAN : new Color(255, 255, 255, 35));
                    }
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawOval(bx, by, badgeSize - 1, badgeSize - 1);

                    // Centered crisp 'X'
                    int arm = 4;
                    int cx = bx + badgeSize / 2;
                    int cy = by + badgeSize / 2;

                    if (AppTheme.isLight()) {
                        g2.setColor(model.isRollover() ? AppTheme.TEXT_PRIMARY : new Color(0x47, 0x55, 0x69));
                    } else {
                        g2.setColor(model.isRollover() ? Color.WHITE : new Color(0x94, 0xA3, 0xB8));
                    }
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
                    g2.drawLine(cx - arm, cy - arm, cx + arm, cy + arm);
                    g2.drawLine(cx + arm, cy - arm, cx - arm, cy + arm);
                    g2.dispose();
                }
            };
            clearBtn.setPreferredSize(new Dimension(24, 24));
            clearBtn.setOpaque(false);
            clearBtn.setContentAreaFilled(false);
            clearBtn.setBorderPainted(false);
            clearBtn.setFocusPainted(false);
            clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clearBtn.setVisible(false);

            clearBtn.addActionListener(e -> {
                field.setText("");
                clearBtn.setVisible(false);
                repaint();
            });

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });

            onChange(field, () -> {
                clearBtn.setVisible(!field.getText().isEmpty());
                repaint();
            });

            add(searchIcon, BorderLayout.WEST);
            add(centerPanel, BorderLayout.CENTER);
            add(clearBtn, BorderLayout.EAST);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (field != null) {
                field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
                field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                field.setOpaque(false);
                field.setBackground(new Color(0, 0, 0, 0));
                field.setFont(AppTheme.font(Font.PLAIN, 12.5f));
                field.setForeground(AppTheme.TEXT_PRIMARY);
                field.setCaretColor(AppTheme.ACCENT_CYAN);
            }
        }

        @Override
        protected void paintChildren(Graphics g) {
            if (field != null) {
                if (!(field.getBorder() instanceof EmptyBorder)) {
                    field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                }
                if (field.isOpaque()) {
                    field.setOpaque(false);
                }
                field.setBackground(new Color(0, 0, 0, 0));
                field.setForeground(AppTheme.TEXT_PRIMARY);
            }
            super.paintChildren(g);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(AppTheme.FIELD_BG);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));

            g2.setColor(focused ? AppTheme.BORDER_FOCUS : AppTheme.FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 10, 10));
            g2.dispose();
        }
    }

    /** Rounded button helper maintained for full compatibility with existing code. */
    public static final class RoundedButton extends JButton {
        private final boolean primary;

        public RoundedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(AppTheme.font(Font.BOLD, 13.5f));
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            ButtonModel model = getModel();
            int w = getWidth();
            int h = getHeight();

            if (primary) {
                g2.setColor(model.isPressed() ? BLUE_PRESSED : model.isRollover() ? BLUE_HOVER : BLUE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            } else {
                g2.setColor(model.isPressed() ? new Color(30, 41, 59)
                        : model.isRollover() ? new Color(30, 41, 59) : AppTheme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 10, 10));
            }
            g2.dispose();

            super.paintComponent(g);
        }
    }

    // =========================================================================
    // Pure Java 2D Vector Icons (100% Platform Independent, Never Missing Glyphs)
    // =========================================================================

    @FunctionalInterface
    public interface IconPainter {
        void paint(Graphics2D g2, int width, int height, Component c);
    }

    public static class VectorIcon implements Icon {
        private final int width;
        private final int height;
        private final IconPainter painter;

        public VectorIcon(int width, int height, IconPainter painter) {
            this.width = width;
            this.height = height;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            AppTheme.applyQualityRendering(g2);
            g2.translate(x, y);
            painter.paint(g2, width, height, c);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }

    public static Icon createGridIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            int gap = 2;
            int s = (size - gap) / 2;
            g2.fillRoundRect(0, 0, s, s, 2, 2);
            g2.fillRoundRect(s + gap, 0, s, s, 2, 2);
            g2.fillRoundRect(0, s + gap, s, s, 2, 2);
            g2.fillRoundRect(s + gap, s + gap, s, s, 2, 2);
        });
    }

    public static Icon createSearchIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            double scale = size / 20.0;
            g2.scale(scale, scale);

            Color col = color != null ? color : (c != null ? c.getForeground() : Color.WHITE);

            // Golden-ratio Lens Center (8.5, 8.5) and Radius 5.8
            float cx = 8.5f;
            float cy = 8.5f;
            float r = 5.8f;

            // Subtle Electric Cyan Glass Tint Fill
            g2.setColor(new Color(0x38, 0xBD, 0xF8, 40));
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // Crisp Outer Lens Ring
            g2.setColor(col);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // Inner Specular Glint Reflection Arc (Top-Left of Lens)
            float gr = r - 1.6f;
            g2.setColor(new Color(0x38, 0xBD, 0xF8, 220)); // Radiant cyan highlight
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            g2.draw(new Arc2D.Float(cx - gr, cy - gr, gr * 2, gr * 2, 105, 75, Arc2D.OPEN));

            // Ergonomic 45-degree Angled Handle with Rounded Tip
            g2.setColor(col);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            g2.draw(new Line2D.Float(12.8f, 12.8f, 18.2f, 18.2f));

            // Subtle Cyan Inner Core on Handle Grip
            g2.setColor(new Color(0x38, 0xBD, 0xF8, 180));
            g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            g2.draw(new Line2D.Float(13.8f, 13.8f, 17.2f, 17.2f));
        });
    }

    public static Icon createBoxIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(1, 3, size - 3, size - 5, 3, 3);
            g2.drawLine(1, 7, size - 2, 7);
            g2.drawRoundRect(size / 2 - 2, 6, 4, 3, 1, 1);
        });
    }

    public static Icon createUserIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int headD = size / 2 - 1;
            int hx = (size - headD) / 2;
            g2.drawOval(hx, 1, headD, headD);
            g2.drawArc(2, headD + 2, size - 4, size - headD - 2, 0, 180);
        });
    }

    public static Icon createShieldIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(2, 2);
            p.lineTo(size - 3, 2);
            p.lineTo(size - 3, size / 2);
            p.quadTo(size - 3, size - 2, size / 2, size - 1);
            p.quadTo(2, size - 2, 2, size / 2);
            p.closePath();
            g2.draw(p);
            g2.drawLine(5, 8, 8, 11);
            g2.drawLine(8, 11, size - 6, 5);
        });
    }

    public static Icon createBellIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            double scale = size / 20.0;
            g2.scale(scale, scale);

            Color col = color != null ? color : (c != null ? c.getForeground() : Color.WHITE);

            // 1. Top Suspension Loop / Crown Ring
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D ring = new Path2D.Float();
            ring.moveTo(8.5f, 4.5f);
            ring.curveTo(8.5f, 1.0f, 11.5f, 1.0f, 11.5f, 4.5f);
            g2.draw(ring);

            // 2. Bell Body with Flared Skirt Lip
            Path2D bell = new Path2D.Float();
            bell.moveTo(7.2f, 4.8f);
            bell.quadTo(10f, 4.0f, 12.8f, 4.8f);
            bell.curveTo(13.6f, 8.2f, 14.8f, 11.2f, 17.5f, 13.8f);
            bell.quadTo(18.0f, 14.5f, 16.8f, 14.8f);
            bell.curveTo(13.5f, 15.5f, 6.5f, 15.5f, 3.2f, 14.8f);
            bell.quadTo(2.0f, 14.5f, 2.5f, 13.8f);
            bell.curveTo(5.2f, 11.2f, 6.4f, 8.2f, 7.2f, 4.8f);
            bell.closePath();

            // Subtle Cyan/White Translucent Glass Accent Fill
            g2.setColor(new Color(0x38, 0xBD, 0xF8, 45));
            g2.fill(bell);

            // Bell Body Stroke
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(bell);

            // 3. Suspended Bell Clapper
            Path2D clapper = new Path2D.Float();
            clapper.moveTo(8.0f, 15.2f);
            clapper.curveTo(8.0f, 18.5f, 12.0f, 18.5f, 12.0f, 15.2f);
            clapper.closePath();
            g2.setColor(new Color(0x38, 0xBD, 0xF8)); // Accent Cyan Clapper
            g2.fill(clapper);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(clapper);

            // 4. Elegant Left-Shoulder Light Glint
            Path2D glint = new Path2D.Float();
            glint.moveTo(7.8f, 6.5f);
            glint.curveTo(7.2f, 9.0f, 6.4f, 11.0f, 4.8f, 12.8f);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            g2.draw(glint);
        });
    }

    public static Icon createCloseIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            Color col = color != null ? color : (c != null ? c.getForeground() : (AppTheme.isLight() ? AppTheme.TEXT_PRIMARY : Color.WHITE));
            int badgeSize = size;
            int bx = 0;
            int by = 0;

            // Capsule pill background
            if (AppTheme.isLight()) {
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
            } else {
                g2.setColor(new Color(0x1E, 0x29, 0x3B, 210));
            }
            g2.fillOval(bx, by, badgeSize, badgeSize);

            // Ring
            if (AppTheme.isLight()) {
                g2.setColor(new Color(0xCB, 0xD5, 0xE1));
            } else {
                g2.setColor(new Color(255, 255, 255, 35));
            }
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(bx, by, badgeSize - 1, badgeSize - 1);

            // Centered 'X' cross
            int arm = badgeSize / 4;
            int cx = bx + badgeSize / 2;
            int cy = by + badgeSize / 2;

            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            g2.drawLine(cx - arm, cy - arm, cx + arm, cy + arm);
            g2.drawLine(cx + arm, cy - arm, cx - arm, cy + arm);
        });
    }

    public static Icon createSunIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            Color col = color != null ? color : (c != null ? c.getForeground() : (AppTheme.isLight() ? AppTheme.TEXT_PRIMARY : Color.WHITE));
            double scale = size / 20.0;
            g2.scale(scale, scale);

            // Center sun disc
            float cx = 10f;
            float cy = 10f;
            float r = 4.0f;

            g2.setColor(new Color(0xF5, 0x9E, 0x0B, 60)); // Amber glow
            g2.fill(new Ellipse2D.Float(cx - r - 1f, cy - r - 1f, (r + 1f) * 2, (r + 1f) * 2));

            g2.setColor(new Color(0xF5, 0x9E, 0x0B)); // Warm amber sun disc
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // 8 Rays
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
            g2.setColor(new Color(0xF5, 0x9E, 0x0B));
            float inner = 6.2f;
            float outer = 8.5f;

            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                float x1 = (float) (cx + inner * Math.cos(angle));
                float y1 = (float) (cy + inner * Math.sin(angle));
                float x2 = (float) (cx + outer * Math.cos(angle));
                float y2 = (float) (cy + outer * Math.sin(angle));
                g2.draw(new Line2D.Float(x1, y1, x2, y2));
            }
        });
    }

    public static Icon createMoonIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            Color col = color != null ? color : (c != null ? c.getForeground() : (AppTheme.isLight() ? AppTheme.TEXT_PRIMARY : Color.WHITE));
            double scale = size / 20.0;
            g2.scale(scale, scale);

            Path2D moon = new Path2D.Float();
            moon.moveTo(14.5f, 4.0f);
            moon.curveTo(8.5f, 5.0f, 6.0f, 10.5f, 7.5f, 15.5f);
            moon.curveTo(11.5f, 18.0f, 16.5f, 16.0f, 17.5f, 12.5f);
            moon.curveTo(13.0f, 13.0f, 9.5f, 9.5f, 14.5f, 4.0f);
            moon.closePath();

            // Subtle indigo/cyan glow fill
            g2.setColor(new Color(0x63, 0x66, 0xF1, 50));
            g2.fill(moon);

            g2.setColor(col);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(moon);
        });
    }

    public static Icon createLeftChevronIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = size / 2;
            int cy = size / 2;
            Path2D p = new Path2D.Float();
            p.moveTo(cx + 2, cy - 4);
            p.lineTo(cx - 3, cy);
            p.lineTo(cx + 2, cy + 4);
            g2.draw(p);
        });
    }

    public static Icon createRightChevronIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = size / 2;
            int cy = size / 2;
            Path2D p = new Path2D.Float();
            p.moveTo(cx - 2, cy - 4);
            p.lineTo(cx + 3, cy);
            p.lineTo(cx - 2, cy + 4);
            g2.draw(p);
        });
    }

    /** Chain/link icon used for Smart Matches navigation. */
    public static Icon createLinkIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            g2.setColor(color != null ? color : c.getForeground());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Draw two interlocked circles (chain links)
            int r = size / 4;
            int cx = size / 2;
            int cy = size / 2;
            // Left arc
            g2.drawArc(cx - r - r / 2, cy - r, 2 * r, 2 * r, 60, 240);
            // Right arc
            g2.drawArc(cx - r / 2, cy - r, 2 * r, 2 * r, 240, 240);
            // Connecting lines
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx - r / 2, cy - r / 2 + 1, cx + r / 2, cy - r / 2 + 1);
            g2.drawLine(cx - r / 2, cy + r / 2 - 1, cx + r / 2, cy + r / 2 - 1);
        });
    }

    /**
     * Vector-drawn smooth red heart icon.
     */
    public static Icon createHeartIcon(int size, Color color) {
        return new VectorIcon(size, size, (g2, w, h, c) -> {
            Color fill = color != null ? color : new Color(0xEF, 0x44, 0x44);
            g2.setColor(fill);
            float scale = size / 24.0f;
            Path2D heart = new Path2D.Float();
            heart.moveTo(12 * scale, 21.35f * scale);
            heart.curveTo(11.45f * scale, 20.85f * scale, 6.75f * scale, 16.58f * scale, 3.8f * scale, 12.85f * scale);
            heart.curveTo(1.5f * scale, 9.95f * scale, 1.5f * scale, 6.5f * scale, 3.8f * scale, 4.2f * scale);
            heart.curveTo(6.1f * scale, 1.9f * scale, 9.7f * scale, 1.9f * scale, 12 * scale, 4.6f * scale);
            heart.curveTo(14.3f * scale, 1.9f * scale, 17.9f * scale, 1.9f * scale, 20.2f * scale, 4.2f * scale);
            heart.curveTo(22.5f * scale, 6.5f * scale, 22.5f * scale, 9.95f * scale, 20.2f * scale, 12.85f * scale);
            heart.curveTo(17.25f * scale, 16.58f * scale, 12.55f * scale, 20.85f * scale, 12 * scale, 21.35f * scale);
            heart.closePath();
            g2.fill(heart);
        });
    }
}
