package com.findit.ui;

import com.findit.dao.UserDAO;
import com.findit.model.User;
import com.findit.service.ImageStorageService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/**
 * Premium SaaS User Profile & Account Settings Dialog for FindIt.
 * Features a modern dark & colorful aesthetic, cleanly structured metadata cards,
 * profile picture uploading, authenticated password change, and forgot password access.
 */
public class UserProfileDialog extends JDialog {

    private final User user;
    private final UserDAO userDAO = new UserDAO();
    private final Runnable onProfileUpdated;

    private final JTextField nameField = new JTextField();
    private final JPasswordField currentPasswordField = new JPasswordField();
    private final JPasswordField newPasswordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();

    private final JLabel profileStatusLabel = UiKit.label(" ", Font.PLAIN, 12, AppTheme.DANGER, SwingConstants.LEFT);
    private final JLabel passwordStatusLabel = UiKit.label(" ", Font.PLAIN, 12, AppTheme.DANGER, SwingConstants.LEFT);

    private final RoundedButton saveNameBtn = new RoundedButton("Save Name", RoundedButton.Style.SECONDARY);
    private final RoundedButton changePasswordBtn = new RoundedButton("Save New Password", RoundedButton.Style.PRIMARY);
    private final RoundedButton uploadPhotoBtn = new RoundedButton("Change Photo", RoundedButton.Style.SECONDARY);
    private final RoundedButton removePhotoBtn = new RoundedButton("Remove Photo", RoundedButton.Style.SECONDARY);

    private JComponent avatar;
    private boolean busy = false;

    public UserProfileDialog(Frame parent, User user, Runnable onProfileUpdated) {
        super(parent, "FINDIT - Account Settings", true);
        this.user = user;
        this.onProfileUpdated = onProfileUpdated;

        setSize(660, 800);
        setMinimumSize(new Dimension(600, 600));
        setLocationRelativeTo(parent);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Elevated card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 1. Header: Avatar + User Name + Role Badge + Photo Actions
        JPanel userHeader = new JPanel(new BorderLayout(16, 0));
        userHeader.setOpaque(false);
        userHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        userHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        // Circular Avatar Component (Supports uploaded photo or stylish gradient fallback)
        avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int size = 62;
                int x = Math.max(1, (getWidth() - size) / 2);
                int y = Math.max(1, (getHeight() - size) / 2);

                // Try rendering profile picture from disk if available
                if (user.getProfileImage() != null && !user.getProfileImage().isBlank()) {
                    File imgFile = new File(user.getProfileImage());
                    if (imgFile.exists()) {
                        try {
                            BufferedImage img = ImageIO.read(imgFile);
                            if (img != null) {
                                Shape circle = new Ellipse2D.Float(x, y, size - 2, size - 2);
                                g2.setClip(circle);
                                g2.drawImage(img, x, y, size - 2, size - 2, null);
                                g2.setClip(null);

                                g2.setColor(AppTheme.ACCENT_CYAN);
                                g2.setStroke(new BasicStroke(2.2f));
                                g2.draw(new Ellipse2D.Float(x, y, size - 2, size - 2));
                                g2.dispose();
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                // Fallback: Gradient avatar with initial
                g2.setPaint(new GradientPaint(x, y, AppTheme.PRIMARY_INDIGO, x + size, y + size, AppTheme.ACCENT_CYAN));
                g2.fill(new Ellipse2D.Float(x, y, size - 2, size - 2));

                String initial = (user.getName() != null && !user.getName().isEmpty())
                        ? user.getName().substring(0, 1).toUpperCase() : "U";
                g2.setFont(AppTheme.font(Font.BOLD, 22f));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initial)) / 2;
                int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initial, tx, ty);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(62, 62);
            }
        };

        JPanel headerInfo = new JPanel();
        headerInfo.setLayout(new BoxLayout(headerInfo, BoxLayout.Y_AXIS));
        headerInfo.setOpaque(false);
        headerInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel nameAndRole = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nameAndRole.setOpaque(false);
        nameAndRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameAndRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel nameTitle = new JLabel(user.getName());
        nameTitle.setFont(AppTheme.font(Font.BOLD, 19f));
        nameTitle.setForeground(AppTheme.TEXT_PRIMARY);
        nameAndRole.add(nameTitle);

        // Role badge
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        JLabel roleBadge = new JLabel(" " + (isAdmin ? "ADMINISTRATOR" : "USER") + " ");
        roleBadge.setFont(AppTheme.font(Font.BOLD, 10.5f));
        roleBadge.setOpaque(true);
        if (isAdmin) {
            roleBadge.setBackground(AppTheme.PURPLE_BG);
            roleBadge.setForeground(AppTheme.PURPLE_TEXT);
            roleBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.PURPLE, 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        } else {
            roleBadge.setBackground(AppTheme.INFO_BG);
            roleBadge.setForeground(AppTheme.INFO_TEXT);
            roleBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.INFO, 1, true),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
        }
        nameAndRole.add(Box.createHorizontalStrut(10));
        nameAndRole.add(roleBadge);

        JLabel subTitle = new JLabel("Account Overview & Security Settings");
        subTitle.setFont(AppTheme.font(Font.PLAIN, 12f));
        subTitle.setForeground(AppTheme.TEXT_SECONDARY);
        subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        subTitle.setBorder(new EmptyBorder(3, 0, 8, 0));

        // Photo Action Buttons
        JPanel photoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        photoRow.setOpaque(false);
        photoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        uploadPhotoBtn.setPreferredSize(new Dimension(125, 32));
        uploadPhotoBtn.setFont(AppTheme.font(Font.BOLD, 12f));

        removePhotoBtn.setPreferredSize(new Dimension(130, 32));
        removePhotoBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        boolean hasCustomPhoto = user.getProfileImage() != null && !user.getProfileImage().isBlank();
        removePhotoBtn.setVisible(hasCustomPhoto);

        photoRow.add(uploadPhotoBtn);
        if (hasCustomPhoto) {
            photoRow.add(Box.createHorizontalStrut(8));
        }
        photoRow.add(removePhotoBtn);

        headerInfo.add(nameAndRole);
        headerInfo.add(subTitle);
        headerInfo.add(photoRow);

        userHeader.add(avatar, BorderLayout.WEST);
        userHeader.add(headerInfo, BorderLayout.CENTER);
        card.add(userHeader);
        card.add(UiKit.vgap(12));

        // 2. Structured Account Details Tile Box
        JPanel metaBox = new JPanel(new GridLayout(1, 2, 20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                g2.setColor(AppTheme.FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.FIELD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        metaBox.setOpaque(false);
        metaBox.setBorder(new EmptyBorder(8, 14, 8, 14));
        metaBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        // Email tile
        metaBox.add(createMetaItem("Registered Email", user.getEmail() != null ? user.getEmail() : "N/A"));

        // Member since tile
        String rawCreated = user.getCreatedAt() != null ? user.getCreatedAt().toString() : "";
        String memberDate = !rawCreated.isEmpty() ? (rawCreated.length() >= 10 ? rawCreated.substring(0, 10) : rawCreated) : "Active";
        metaBox.add(createMetaItem("Member Since", memberDate));

        card.add(metaBox);
        card.add(UiKit.vgap(12));

        // 3. Theme & Appearance Section
        card.add(UiKit.divider("Appearance & Theme"));
        card.add(UiKit.vgap(8));

        JPanel themeRow = new JPanel(new BorderLayout(10, 0));
        themeRow.setOpaque(false);
        themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel themeStatusLbl = new JLabel(AppTheme.isLight() ? "Active: Light Theme" : "Active: Dark Theme");
        themeStatusLbl.setFont(AppTheme.font(Font.BOLD, 12.5f));
        themeStatusLbl.setForeground(AppTheme.TEXT_PRIMARY);

        RoundedButton themeSwitchBtn = new RoundedButton(
                AppTheme.isLight() ? "Switch to Dark" : "Switch to Light",
                AppTheme.isLight() ? UiKit.createMoonIcon(16, null) : UiKit.createSunIcon(16, null),
                RoundedButton.Style.SECONDARY
        );
        themeSwitchBtn.setPreferredSize(new Dimension(145, 36));
        themeSwitchBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        themeSwitchBtn.addActionListener(e -> {
            AppTheme.toggleTheme();
            themeStatusLbl.setText(AppTheme.isLight() ? "Active: Light Theme" : "Active: Dark Theme");
            themeSwitchBtn.setText(AppTheme.isLight() ? "Switch to Dark" : "Switch to Light");
            themeSwitchBtn.setLeadingIcon(AppTheme.isLight() ? UiKit.createMoonIcon(16, null) : UiKit.createSunIcon(16, null));
            root.setBackground(AppTheme.BG_MAIN);
            card.repaint();
            root.repaint();
        });

        themeRow.add(themeStatusLbl, BorderLayout.CENTER);
        themeRow.add(themeSwitchBtn, BorderLayout.EAST);
        card.add(themeRow);
        card.add(UiKit.vgap(12));

        // 4. Profile Details Section
        card.add(UiKit.divider("Profile Details"));
        card.add(UiKit.vgap(8));

        nameField.setText(user.getName());
        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        saveNameBtn.setPreferredSize(new Dimension(115, 44));

        nameRow.add(UiKit.textBox(nameField), BorderLayout.CENTER);
        nameRow.add(saveNameBtn, BorderLayout.EAST);

        JPanel nameGroup = UiKit.labeled("Display Name", nameRow);
        nameGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        card.add(nameGroup);
        card.add(UiKit.vgap(4));

        profileStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(profileStatusLabel);
        card.add(UiKit.vgap(8));

        // 4. Security & Password Section
        card.add(UiKit.divider("Security & Password"));
        card.add(UiKit.vgap(8));

        // Current Password Header with "Forgot password?" Link
        JPanel currPassHeader = new JPanel(new BorderLayout());
        currPassHeader.setOpaque(false);
        JLabel currPassLbl = new JLabel("Current Password");
        currPassLbl.setFont(AppTheme.font(Font.BOLD, 12f));
        currPassLbl.setForeground(AppTheme.TEXT_SECONDARY);

        JButton forgotBtn = new JButton("Forgot password?");
        forgotBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        forgotBtn.setForeground(AppTheme.ACCENT_CYAN);
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setBorderPainted(false);
        forgotBtn.setFocusPainted(false);
        forgotBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotBtn.addActionListener(e -> {
            dispose(); // Close modal so it unblocks the entire application
            ForgotPasswordFrame forgotFrame = new ForgotPasswordFrame();
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                forgotFrame.setEmail(user.getEmail());
            }
            forgotFrame.setVisible(true);
            forgotFrame.toFront();
            forgotFrame.requestFocus();
        });

        currPassHeader.add(currPassLbl, BorderLayout.WEST);
        currPassHeader.add(forgotBtn, BorderLayout.EAST);

        JPanel currPassGroup = new JPanel(new BorderLayout(0, 6));
        currPassGroup.setOpaque(false);
        currPassGroup.add(currPassHeader, BorderLayout.NORTH);
        currPassGroup.add(UiKit.passwordBox(currentPasswordField), BorderLayout.CENTER);
        currPassGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        currPassGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.add(currPassGroup);
        card.add(UiKit.vgap(8));

        JPanel newPassGroup = UiKit.labeled("New Password (minimum 8 characters)", UiKit.passwordBox(newPasswordField));
        newPassGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        newPassGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        card.add(newPassGroup);
        card.add(UiKit.vgap(8));

        JPanel confirmPassGroup = UiKit.labeled("Confirm New Password", UiKit.passwordBox(confirmPasswordField));
        confirmPassGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmPassGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        card.add(confirmPassGroup);
        card.add(UiKit.vgap(6));

        passwordStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordStatusLabel);
        card.add(UiKit.vgap(12));

        // Prominent Save Password Action Button
        changePasswordBtn.setPreferredSize(new Dimension(420, 44));
        changePasswordBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        changePasswordBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(changePasswordBtn);

        // Wrap card in a smooth JScrollPane so all fields and buttons are 100% accessible
        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        root.add(scrollPane, BorderLayout.CENTER);

        // Bottom Close Button
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomBar.setOpaque(false);
        RoundedButton closeBtn = new RoundedButton("Close", RoundedButton.Style.SECONDARY);
        closeBtn.setPreferredSize(new Dimension(100, 38));
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);

        // Wire Actions
        saveNameBtn.addActionListener(e -> onSaveNameClicked());
        changePasswordBtn.addActionListener(e -> onChangePasswordClicked());
        uploadPhotoBtn.addActionListener(e -> onUploadPhotoClicked());
        removePhotoBtn.addActionListener(e -> onRemovePhotoClicked());

        UiKit.onChange(nameField, () -> profileStatusLabel.setText(" "));
        UiKit.onChange(currentPasswordField, () -> passwordStatusLabel.setText(" "));
        UiKit.onChange(newPasswordField, () -> passwordStatusLabel.setText(" "));
        UiKit.onChange(confirmPasswordField, () -> passwordStatusLabel.setText(" "));
    }

    private JPanel createMetaItem(String title, String value) {
        JPanel item = new JPanel(new GridLayout(2, 1, 0, 1));
        item.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.font(Font.PLAIN, 11f));
        titleLbl.setForeground(AppTheme.TEXT_MUTED);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(AppTheme.font(Font.BOLD, 12.5f));
        valueLbl.setForeground(AppTheme.TEXT_PRIMARY);

        item.add(titleLbl);
        item.add(valueLbl);
        return item;
    }

    private void onUploadPhotoClicked() {
        if (busy) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Picture");
        chooser.setFileFilter(new FileNameExtensionFilter("Images (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                String savedRelativePath = ImageStorageService.saveImage(selectedFile);
                userDAO.updateProfileImage(user.getUserId(), savedRelativePath);
                user.setProfileImage(savedRelativePath);

                removePhotoBtn.setVisible(true);
                avatar.repaint();

                if (onProfileUpdated != null) {
                    onProfileUpdated.run();
                }

                DialogUtils.showSuccess(this, "Photo Updated", "Your profile picture has been updated successfully.");
            } catch (Exception ex) {
                DialogUtils.showError(this, "Photo Upload Failed", ex.getMessage());
            }
        }
    }

    private void onRemovePhotoClicked() {
        if (busy) return;

        boolean confirm = DialogUtils.showConfirm(
                this,
                "Remove Photo",
                "Are you sure you want to remove your profile picture?"
        );

        if (confirm) {
            try {
                userDAO.updateProfileImage(user.getUserId(), null);
                user.setProfileImage(null);

                removePhotoBtn.setVisible(false);
                avatar.repaint();

                if (onProfileUpdated != null) {
                    onProfileUpdated.run();
                }

                DialogUtils.showSuccess(this, "Photo Removed", "Your profile picture has been reset to default.");
            } catch (Exception ex) {
                DialogUtils.showError(this, "Error", "Could not remove photo: " + ex.getMessage());
            }
        }
    }

    private void onSaveNameClicked() {
        if (busy) return;
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            profileStatusLabel.setForeground(AppTheme.DANGER);
            profileStatusLabel.setText("Name cannot be empty.");
            return;
        }

        busy = true;
        saveNameBtn.setEnabled(false);
        saveNameBtn.setText("Saving...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return userDAO.updateUserName(user.getUserId(), newName);
            }

            @Override
            protected void done() {
                busy = false;
                saveNameBtn.setEnabled(true);
                saveNameBtn.setText("Save Name");
                try {
                    boolean ok = get();
                    if (ok) {
                        user.setName(newName);
                        profileStatusLabel.setForeground(AppTheme.SUCCESS);
                        profileStatusLabel.setText("Profile name updated successfully.");
                        avatar.repaint();
                        if (onProfileUpdated != null) {
                            onProfileUpdated.run();
                        }
                    } else {
                        profileStatusLabel.setForeground(AppTheme.DANGER);
                        profileStatusLabel.setText("Failed to update name.");
                    }
                } catch (Exception ex) {
                    profileStatusLabel.setForeground(AppTheme.DANGER);
                    profileStatusLabel.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onChangePasswordClicked() {
        if (busy) return;

        char[] currChars = currentPasswordField.getPassword();
        char[] newChars = newPasswordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();

        if (currChars.length == 0 || newChars.length == 0 || confirmChars.length == 0) {
            passwordStatusLabel.setForeground(AppTheme.DANGER);
            passwordStatusLabel.setText("Please fill in all password fields.");
            Arrays.fill(currChars, '\0');
            Arrays.fill(newChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        if (newChars.length < 8) {
            passwordStatusLabel.setForeground(AppTheme.DANGER);
            passwordStatusLabel.setText("New password must be at least 8 characters long.");
            Arrays.fill(currChars, '\0');
            Arrays.fill(newChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        if (!Arrays.equals(newChars, confirmChars)) {
            passwordStatusLabel.setForeground(AppTheme.DANGER);
            passwordStatusLabel.setText("New passwords do not match.");
            Arrays.fill(currChars, '\0');
            Arrays.fill(newChars, '\0');
            Arrays.fill(confirmChars, '\0');
            return;
        }

        String currentPass = new String(currChars);
        String newPass = new String(newChars);
        Arrays.fill(currChars, '\0');
        Arrays.fill(newChars, '\0');
        Arrays.fill(confirmChars, '\0');

        busy = true;
        changePasswordBtn.setEnabled(false);
        changePasswordBtn.setText("Verifying & Updating...");
        passwordStatusLabel.setForeground(AppTheme.TEXT_MUTED);
        passwordStatusLabel.setText("Checking current password...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return userDAO.changePassword(user.getUserId(), currentPass, newPass);
            }

            @Override
            protected void done() {
                busy = false;
                changePasswordBtn.setEnabled(true);
                changePasswordBtn.setText("Save New Password");
                try {
                    boolean ok = get();
                    if (ok) {
                        passwordStatusLabel.setForeground(AppTheme.SUCCESS);
                        passwordStatusLabel.setText("Password updated successfully.");
                        currentPasswordField.setText("");
                        newPasswordField.setText("");
                        confirmPasswordField.setText("");
                        DialogUtils.showSuccess(UserProfileDialog.this, "Password Updated", "Your password has been changed successfully.");
                    } else {
                        passwordStatusLabel.setForeground(AppTheme.DANGER);
                        passwordStatusLabel.setText("Current password is incorrect.");
                    }
                } catch (Exception ex) {
                    passwordStatusLabel.setForeground(AppTheme.DANGER);
                    passwordStatusLabel.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
