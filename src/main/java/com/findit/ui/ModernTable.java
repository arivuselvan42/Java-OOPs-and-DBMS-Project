package com.findit.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Premium SaaS Table Component with a high-contrast, clean aesthetic.
 * Features subtle alternating rows, distinct selection highlights,
 * clear typography, and an elegant header designed for maximum legibility.
 */
public class ModernTable extends JTable {

    // Lighter SaaS Palette for optimal item readability
    public static final Color TABLE_BG           = new Color(0xFF, 0xFF, 0xFF); // Crisp pure white #FFFFFF
    public static final Color TABLE_ROW_ALT      = new Color(0xF8, 0xFA, 0xFC); // Soft mist slate #F8FAFC
    public static final Color TABLE_GRID         = new Color(0xF1, 0xF5, 0xF9); // Subtle row divider #F1F5F9
    public static final Color TABLE_HEADER_BG    = new Color(0xF1, 0xF5, 0xF9); // Elevated header #F1F5F9
    public static final Color TABLE_HEADER_FG    = new Color(0x33, 0x41, 0x55); // Bold slate-700 #334155
    public static final Color TABLE_HEADER_BORDER= new Color(0xCB, 0xD5, 0xE1); // Border #CBD5E1

    public static final Color ROW_TEXT_PRIMARY   = new Color(0x0F, 0x17, 0x2A); // Midnight black-slate #0F172A
    public static final Color ROW_TEXT_SECONDARY = new Color(0x33, 0x41, 0x55); // Slate #334155
    public static final Color ROW_TEXT_MUTED     = new Color(0x64, 0x74, 0x8B); // Cool slate #64748B

    public static final Color SELECTION_BG       = new Color(0xE0, 0xE7, 0xFF); // Vibrant soft indigo #E0E7FF
    public static final Color SELECTION_FG       = new Color(0x1E, 0x1B, 0x4B); // Deep indigo #1E1B4B
    public static final Color SELECTION_BAR      = new Color(0x4F, 0x46, 0xE5); // Left accent bar #4F46E5

    private String emptyMessage = "No items to display";
    private String emptySubMessage = "Records will appear here when added";

    public ModernTable(TableModel model) {
        super(model);
        initStyle();
    }

    private void initStyle() {
        setRowHeight(44);
        setShowHorizontalLines(true);
        setShowVerticalLines(false);
        setGridColor(TABLE_GRID);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setBackground(TABLE_BG);
        setFont(AppTheme.font(Font.PLAIN, 13f));
        setForeground(ROW_TEXT_PRIMARY);
        setSelectionBackground(SELECTION_BG);
        setSelectionForeground(SELECTION_FG);

        // Header Customization: Clean, sharp, elevated
        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        header.setFont(AppTheme.font(Font.BOLD, 12f));
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TABLE_HEADER_FG);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                lbl.setOpaque(true);
                lbl.setBackground(TABLE_HEADER_BG);
                lbl.setForeground(TABLE_HEADER_FG);
                lbl.setFont(AppTheme.font(Font.BOLD, 12f));
                int hPad = (column == 0 || column == 1) ? 4 : 12;
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, TABLE_HEADER_BORDER),
                        BorderFactory.createEmptyBorder(2, hPad, 2, hPad)
                ));
                lbl.setVerticalAlignment(SwingConstants.CENTER);
                if (column == 0 || column == 1) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return lbl;
            }
        });

        // Default Cell Renderer: alternating rows with bold Item Name and crystal-clear text
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Padding inside cell: vertical margin prevents descender clipping
                int hPad = (column == 0 || column == 1) ? 4 : 12;
                lbl.setBorder(BorderFactory.createEmptyBorder(2, hPad, 2, hPad));
                lbl.setVerticalAlignment(SwingConstants.CENTER);

                if (isSelected) {
                    lbl.setBackground(SELECTION_BG);
                    lbl.setForeground(SELECTION_FG);
                    if (column == 3) {
                        lbl.setFont(AppTheme.font(Font.BOLD, 13f));
                    } else {
                        lbl.setFont(AppTheme.font(Font.PLAIN, 13f));
                    }
                    if (column == 0 || column == 1) {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                } else {
                    lbl.setBackground(row % 2 == 0 ? TABLE_BG : TABLE_ROW_ALT);

                    if (column == 0 || column == 1) {
                        // S.No. & ID columns: clean muted gray, centered
                        lbl.setFont(AppTheme.font(Font.PLAIN, 12.5f));
                        lbl.setForeground(ROW_TEXT_MUTED);
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    } else if (column == 3) {
                        // Item Name: prominent bold midnight text
                        lbl.setFont(AppTheme.font(Font.BOLD, 13.5f));
                        lbl.setForeground(ROW_TEXT_PRIMARY);
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else if (column == 4 || column == 8) {
                        // Category & Reporter: solid dark slate
                        lbl.setFont(AppTheme.font(Font.PLAIN, 13f));
                        lbl.setForeground(ROW_TEXT_SECONDARY);
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else if (column == 6) {
                        // Report Date: crisp clear text
                        lbl.setFont(AppTheme.font(Font.PLAIN, 13f));
                        lbl.setForeground(ROW_TEXT_SECONDARY);
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        // Location & others
                        lbl.setFont(AppTheme.font(Font.PLAIN, 13f));
                        lbl.setForeground(ROW_TEXT_SECONDARY);
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                }

                return lbl;
            }
        });

        // Clicking empty area of the table clears selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int clickedRow = rowAtPoint(e.getPoint());
                if (clickedRow == -1) {
                    clearSelection();
                }
            }
        });

        // Pressing Escape clears table selection
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearTableSelection");
        getActionMap().put("clearTableSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelection();
            }
        });
    }

    /**
     * Enables toggle deselection: clicking an already selected row unselects it.
     */
    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (rowIndex == getSelectedRow() && !extend) {
            clearSelection();
        } else {
            super.changeSelection(rowIndex, columnIndex, toggle, extend);
        }
    }

    public void setEmptyMessage(String main, String sub) {
        this.emptyMessage = main;
        this.emptySubMessage = sub;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getRowCount() == 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            AppTheme.applyQualityRendering(g2);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(TABLE_BG);
            g2.fillRect(0, 0, w, h);

            int centerY = h / 2 - 20;

            // Brand Logo Placeholder
            int iconSize = 42;
            int iconX = (w - iconSize) / 2;
            int iconY = centerY - 44;
            UiKit.drawFindItLogo(g2, iconX, iconY, iconSize);

            // Empty message
            g2.setFont(AppTheme.font(Font.BOLD, 15f));
            g2.setColor(ROW_TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();
            int msgX = (w - fm.stringWidth(emptyMessage)) / 2;
            g2.drawString(emptyMessage, msgX, centerY + 22);

            // Sub message
            g2.setFont(AppTheme.font(Font.PLAIN, 13f));
            g2.setColor(ROW_TEXT_MUTED);
            FontMetrics fmSub = g2.getFontMetrics();
            int subX = (w - fmSub.stringWidth(emptySubMessage)) / 2;
            g2.drawString(emptySubMessage, subX, centerY + 44);

            g2.dispose();
        }
    }
}
