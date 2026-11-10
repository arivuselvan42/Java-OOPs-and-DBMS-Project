package com.findit.ui;

import com.findit.dao.ReportDAO;
import com.findit.database.DBConnection;
import com.findit.model.Report;
import com.findit.model.User;
import com.findit.service.ImageStorageService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Premium SaaS dialog for editing an existing Lost or Found report in FindIt.
 * Styled with a dark & colorful aesthetic and supports image updates and inspection.
 */
public class EditReportFrame extends JFrame {

    private final Report report;
    private final User currentUser;

    private final JTextField itemNameField = new JTextField();
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JTextField locationField = new JTextField();
    private final JTextField dateField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(3, 20);

    // Image Controls
    private final JLabel imageStatusLabel = new JLabel("No image attached");
    private final RoundedButton viewImageBtn = new RoundedButton("View", RoundedButton.Style.SECONDARY);
    private final RoundedButton changeImageBtn = new RoundedButton("Change", RoundedButton.Style.SECONDARY);
    private final JButton removeImageBtn = new JButton("Remove");
    private File newSelectedImage = null;
    private boolean imageRemoved = false;

    private final RoundedButton saveBtn = new RoundedButton("Save Changes", RoundedButton.Style.PRIMARY);
    private final RoundedButton cancelBtn = new RoundedButton("Cancel", RoundedButton.Style.SECONDARY);

    private final Map<String, Integer> categoryIds = new LinkedHashMap<>();
    private boolean busy = false;

    public EditReportFrame(Report report, User user) {
        this.report = report;
        this.currentUser = user;

        setTitle("FINDIT - Edit Report #" + report.getReportId());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(580, 720);
        setMinimumSize(new Dimension(520, 660));
        setLocationRelativeTo(null);
        setResizable(false);

        // Prepopulate values
        if (report.getItem() != null) {
            itemNameField.setText(report.getItem().getItemName());
            descriptionArea.setText(report.getItem().getDescription());
        }
        locationField.setText(report.getLocation());
        dateField.setText(report.getReportDate() != null ? report.getReportDate().toString() : "");

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Card Container
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(AppTheme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);

        JLabel title = new JLabel("Edit Report Details");
        title.setFont(AppTheme.font(Font.BOLD, 19f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Report #" + report.getReportId() + " (" + report.getReportType() + ") - Status: " + report.getStatus());
        subtitle.setFont(AppTheme.font(Font.PLAIN, 12.5f));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);

        header.add(title);
        header.add(subtitle);

        // Form Fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        int row = 0;

        // Two-column row for Item Name and Category
        JPanel nameAndCat = new JPanel(new GridLayout(1, 2, 12, 0));
        nameAndCat.setOpaque(false);
        nameAndCat.add(createFieldGroup("Item Name", UiKit.textBox(itemNameField)));
        nameAndCat.add(createFieldGroup("Category", categoryBox));

        gbc.gridy = row++;
        form.add(nameAndCat, gbc);

        // Location & Date
        JPanel locAndDate = new JPanel(new GridLayout(1, 2, 12, 0));
        locAndDate.setOpaque(false);
        locAndDate.add(createFieldGroup("Location", UiKit.textBox(locationField)));
        locAndDate.add(createFieldGroup("Report Date", UiKit.textBox(dateField)));

        gbc.gridy = row++;
        form.add(locAndDate, gbc);

        // Description
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(AppTheme.font(Font.PLAIN, 13f));
        descriptionArea.setBackground(AppTheme.FIELD_BG);
        descriptionArea.setForeground(AppTheme.TEXT_PRIMARY);
        descriptionArea.setCaretColor(AppTheme.ACCENT_CYAN);
        descriptionArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        descScroll.setPreferredSize(new Dimension(0, 80));

        gbc.gridy = row++;
        form.add(createFieldGroup("Description", descScroll), gbc);

        // Image Section
        JPanel imageSection = buildImageSection();
        gbc.gridy = row++;
        form.add(createFieldGroup("Attached Photo", imageSection), gbc);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        saveBtn.setPreferredSize(new Dimension(140, 38));

        buttonRow.add(cancelBtn);
        buttonRow.add(saveBtn);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(buttonRow, BorderLayout.SOUTH);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);

        // Style Combo with modern dark SaaS styling
        UiKit.styleComboBox(categoryBox);

        // Actions
        saveBtn.addActionListener(e -> saveChanges());
        cancelBtn.addActionListener(e -> dispose());
        viewImageBtn.addActionListener(e -> onViewImage());
        changeImageBtn.addActionListener(e -> onChangeImage());
        removeImageBtn.addActionListener(e -> onRemoveImage());

        // Load Categories
        loadCategoriesAsync();
    }

    private JPanel buildImageSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        imageStatusLabel.setFont(AppTheme.font(Font.PLAIN, 12f));
        imageStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        String currentImg = report.getItem() != null ? report.getItem().getImagePath() : null;
        if (currentImg != null && !currentImg.isBlank()) {
            imageStatusLabel.setText("Photo attached: " + currentImg);
            imageStatusLabel.setForeground(AppTheme.ACCENT_CYAN);
            viewImageBtn.setVisible(true);
            removeImageBtn.setVisible(true);
        } else {
            viewImageBtn.setVisible(false);
            removeImageBtn.setVisible(false);
        }

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        viewImageBtn.setPreferredSize(new Dimension(75, 36));
        changeImageBtn.setPreferredSize(new Dimension(85, 36));

        removeImageBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        removeImageBtn.setForeground(AppTheme.DANGER);
        removeImageBtn.setContentAreaFilled(false);
        removeImageBtn.setBorderPainted(false);
        removeImageBtn.setFocusPainted(false);
        removeImageBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        controls.add(removeImageBtn);
        controls.add(viewImageBtn);
        controls.add(changeImageBtn);

        panel.add(imageStatusLabel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.EAST);
        return panel;
    }

    private void onViewImage() {
        String path = report.getItem() != null ? report.getItem().getImagePath() : null;
        if (newSelectedImage != null) {
            path = newSelectedImage.getAbsolutePath();
        }
        if (path != null) {
            String name = report.getItem() != null ? report.getItem().getItemName() : "Item Image";
            new ImageViewerDialog(this, name, path).setVisible(true);
        }
    }

    private void onChangeImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Replacement Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.webp)", "jpg", "jpeg", "png", "webp"));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (f.length() > ImageStorageService.MAX_FILE_SIZE_BYTES) {
                DialogUtils.showWarning(this, "File Too Large", "Selected file exceeds the 5 MB limit.");
                return;
            }
            this.newSelectedImage = f;
            this.imageRemoved = false;
            imageStatusLabel.setText("New image selected: " + f.getName());
            imageStatusLabel.setForeground(AppTheme.ACCENT_CYAN);
            viewImageBtn.setVisible(true);
            removeImageBtn.setVisible(true);
        }
    }

    private void onRemoveImage() {
        this.newSelectedImage = null;
        this.imageRemoved = true;
        imageStatusLabel.setText("Image removed");
        imageStatusLabel.setForeground(AppTheme.DANGER);
        viewImageBtn.setVisible(false);
        removeImageBtn.setVisible(false);
    }

    private JPanel createFieldGroup(String label, JComponent comp) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.font(Font.BOLD, 12f));
        lbl.setForeground(AppTheme.TEXT_PRIMARY);

        group.add(lbl, BorderLayout.NORTH);
        group.add(comp, BorderLayout.CENTER);
        return group;
    }

    private void loadCategoriesAsync() {
        new SwingWorker<Map<String, Integer>, Void>() {
            @Override
            protected Map<String, Integer> doInBackground() throws Exception {
                Map<String, Integer> map = new LinkedHashMap<>();
                String sql = "SELECT category_id, category_name FROM categories ORDER BY category_name";
                try (Connection con = DBConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.put(rs.getString("category_name"), rs.getInt("category_id"));
                    }
                }
                return map;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Integer> map = get();
                    categoryIds.clear();
                    categoryBox.removeAllItems();

                    int currentCatId = report.getItem() != null ? report.getItem().getCategoryId() : -1;
                    String selectedCat = null;

                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        categoryIds.put(entry.getKey(), entry.getValue());
                        categoryBox.addItem(entry.getKey());
                        if (entry.getValue() == currentCatId) {
                            selectedCat = entry.getKey();
                        }
                    }

                    if (selectedCat != null) {
                        categoryBox.setSelectedItem(selectedCat);
                    }
                } catch (Exception e) {
                    DialogUtils.showError(EditReportFrame.this, "Error", "Could not load categories: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        saveBtn.setEnabled(!busy);
        cancelBtn.setEnabled(!busy);
        changeImageBtn.setEnabled(!busy);
        saveBtn.setText(busy ? "Saving..." : "Save Changes");
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void saveChanges() {
        if (busy) return;

        String itemName = itemNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String location = locationField.getText().trim();
        String dateText = dateField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        if (itemName.isEmpty() || description.isEmpty() || location.isEmpty() || dateText.isEmpty() || category == null) {
            DialogUtils.showWarning(this, "Missing Information", "Please fill in all required fields.");
            return;
        }

        LocalDate reportDate;
        try {
            reportDate = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            DialogUtils.showWarning(this, "Invalid Date", "Please enter the report date in YYYY-MM-DD format.");
            return;
        }

        int categoryId = categoryIds.getOrDefault(category, report.getItem() != null ? report.getItem().getCategoryId() : 1);

        if (report.getItem() != null) {
            report.getItem().setItemName(itemName);
            report.getItem().setCategoryId(categoryId);
            report.getItem().setCategoryName(category);
            report.getItem().setDescription(description);
        }
        report.setLocation(location);
        report.setReportDate(reportDate);

        setBusy(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (imageRemoved) {
                    if (report.getItem() != null) {
                        report.getItem().setImagePath(null);
                    }
                } else if (newSelectedImage != null) {
                    String relative = ImageStorageService.saveImage(newSelectedImage);
                    if (report.getItem() != null) {
                        report.getItem().setImagePath(relative);
                    }
                }

                boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
                return new ReportDAO().updateReport(report, currentUser.getUserId(), isAdmin);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        DialogUtils.showSuccess(EditReportFrame.this, "Updated", "Report details updated successfully.");
                        dispose();
                    } else {
                        DialogUtils.showError(EditReportFrame.this, "Failed", "Could not update the report.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(EditReportFrame.this, "Update Error", "Failed to update report: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
