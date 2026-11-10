package com.findit.ui;

import com.findit.service.ImageStorageService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Premium dark-themed modal dialog for inspecting item images in full resolution.
 */
public class ImageViewerDialog extends JDialog {

    public ImageViewerDialog(Window parent, String itemName, String imagePath) {
        super(parent, "FINDIT - Image Viewer: " + itemName, ModalityType.APPLICATION_MODAL);

        setSize(720, 600);
        setMinimumSize(new Dimension(500, 420));
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel(itemName != null ? itemName : "Item Image");
        title.setFont(AppTheme.font(Font.BOLD, 17f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(imagePath != null ? imagePath : "No path available");
        subtitle.setFont(AppTheme.font(Font.PLAIN, 12f));
        subtitle.setForeground(AppTheme.TEXT_MUTED);

        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Image Viewport Container
        JPanel canvas = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        canvas.setOpaque(false);
        canvas.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ImageIcon icon = ImageStorageService.loadScaledIcon(imagePath, 660, 440);
        if (icon != null) {
            imageLabel.setIcon(icon);
        } else {
            imageLabel.setText("Image file could not be found or loaded from disk.");
            imageLabel.setFont(AppTheme.font(Font.ITALIC, 14f));
            imageLabel.setForeground(AppTheme.TEXT_MUTED);
        }

        canvas.add(imageLabel);
        root.add(canvas, BorderLayout.CENTER);

        // Bottom Controls
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.Style.SECONDARY);
        closeBtn.setPreferredSize(new Dimension(100, 38));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }
}

