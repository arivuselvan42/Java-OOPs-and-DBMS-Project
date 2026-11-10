package com.findit.ui;

import com.findit.dao.NotificationDAO;
import com.findit.model.Notification;
import com.findit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Premium Notification Center modal showing real-time updates for
 * claim submissions, approvals, returns, and smart matches.
 */
public class NotificationCenterDialog extends JDialog {

    private final User currentUser;
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final JPanel listContainer = new JPanel();
    private final Runnable onDismissCallback;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

    public NotificationCenterDialog(Window parent, User user, Runnable onDismissCallback) {
        super(parent, "FINDIT - Notification Centre", ModalityType.APPLICATION_MODAL);
        this.currentUser = user;
        this.onDismissCallback = onDismissCallback;

        setSize(520, 560);
        setMinimumSize(new Dimension(460, 420));
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Notification Centre");
        title.setFont(AppTheme.font(Font.BOLD, 18f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Updates on claims, returns, and smart matches");
        subtitle.setFont(AppTheme.font(Font.PLAIN, 12f));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        RoundedButton markAllBtn = new RoundedButton("Mark all read", RoundedButton.Style.SECONDARY);
        markAllBtn.setPreferredSize(new Dimension(110, 32));
        markAllBtn.setFont(AppTheme.font(Font.BOLD, 11f));
        markAllBtn.addActionListener(e -> {
            notificationDAO.markAllAsRead(currentUser.getUserId());
            loadNotifications();
        });

        header.add(titleBox, BorderLayout.WEST);
        header.add(markAllBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Scrollable list
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        scrollPane.getViewport().setBackground(AppTheme.BG_MAIN);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        root.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.Style.PRIMARY);
        closeBtn.setPreferredSize(new Dimension(90, 36));
        closeBtn.addActionListener(e -> {
            dispose();
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        });
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        loadNotifications();
    }

    private void loadNotifications() {
        listContainer.removeAll();
        List<Notification> list = notificationDAO.getNotificationsForUser(currentUser.getUserId());

        if (list.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setOpaque(false);
            empty.setBorder(new EmptyBorder(60, 20, 60, 20));

            JLabel icon = new JLabel("🔔", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel txt = new JLabel("No notifications yet", SwingConstants.CENTER);
            txt.setFont(AppTheme.font(Font.BOLD, 15f));
            txt.setForeground(AppTheme.TEXT_PRIMARY);
            txt.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel sub = new JLabel("You'll receive updates when someone claims your item or finds a match.", SwingConstants.CENTER);
            sub.setFont(AppTheme.font(Font.PLAIN, 12f));
            sub.setForeground(AppTheme.TEXT_MUTED);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(icon);
            empty.add(Box.createVerticalStrut(10));
            empty.add(txt);
            empty.add(Box.createVerticalStrut(4));
            empty.add(sub);
            listContainer.add(empty);
        } else {
            for (Notification n : list) {
                listContainer.add(buildNotificationCard(n));
                listContainer.add(Box.createVerticalStrut(8));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel buildNotificationCard(Notification n) {
        JPanel card = new JPanel(new BorderLayout(10, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();

                Color unreadBg = AppTheme.isLight() ? new Color(0xEE, 0xF2, 0xFF) : new Color(0x1E, 0x22, 0x38);
                g2.setColor(n.isRead() ? AppTheme.BG_CARD : unreadBg);
                g2.fillRoundRect(0, 0, w, h, 10, 10);

                g2.setColor(n.isRead() ? AppTheme.BORDER : AppTheme.PRIMARY_INDIGO);
                g2.setStroke(new BasicStroke(n.isRead() ? 1f : 1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        // Top line: Title + Time
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLbl = new JLabel(n.getTitle());
        titleLbl.setFont(AppTheme.font(Font.BOLD, 13.5f));
        titleLbl.setForeground(n.isRead() ? AppTheme.TEXT_PRIMARY : AppTheme.PRIMARY_INDIGO);

        String timeStr = n.getCreatedAt() != null ? n.getCreatedAt().format(TIME_FMT) : "";
        JLabel timeLbl = new JLabel(timeStr);
        timeLbl.setFont(AppTheme.font(Font.PLAIN, 11f));
        timeLbl.setForeground(AppTheme.TEXT_MUTED);

        top.add(titleLbl, BorderLayout.WEST);
        top.add(timeLbl, BorderLayout.EAST);

        // Body: Message
        JLabel msgLbl = new JLabel("<html>" + n.getMessage() + "</html>");
        msgLbl.setFont(AppTheme.font(Font.PLAIN, 12f));
        msgLbl.setForeground(AppTheme.TEXT_SECONDARY);

        card.add(top, BorderLayout.NORTH);
        card.add(msgLbl, BorderLayout.CENTER);

        // Click marks as read
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!n.isRead()) {
                    notificationDAO.markAsRead(n.getNotificationId());
                    n.setRead(true);
                    card.repaint();
                }
            }
        });

        return card;
    }
}

