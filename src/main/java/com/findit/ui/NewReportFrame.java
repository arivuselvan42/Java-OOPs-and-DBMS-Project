package com.findit.ui;

import com.findit.dao.ReportDAO;
import com.findit.database.DBConnection;
import com.findit.model.FoundReport;
import com.findit.model.Item;
import com.findit.model.LostReport;
import com.findit.model.Report;
import com.findit.model.User;
import com.findit.service.ImageStorageService;
import com.findit.service.SmartMatchingService;
import com.findit.service.SmartMatchingService.MatchResult;

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
import java.util.List;
import java.util.Map;

/**
 * Premium SaaS dialog for creating a new Lost or Found report in FindIt.
 * Styled with a dark & colorful aesthetic and supports item image uploads.
 */
public class NewReportFrame extends JFrame {

    private final User currentUser;
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"LOST", "FOUND"});
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JTextField itemNameField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JTextField dateField = new JTextField(LocalDate.now().toString());
    private final JTextArea descriptionArea = new JTextArea(3, 20);

    // Image Upload Components
    private final JLabel imagePreviewLabel = new JLabel("No image selected");
    private final RoundedButton browseImageBtn = new RoundedButton("Browse...", RoundedButton.Style.SECONDARY);
    private final JButton removeImageBtn = new JButton("Remove");
    private File selectedImageFile = null;

    private final RoundedButton submitBtn = new RoundedButton("Submit Report", RoundedButton.Style.PRIMARY);
    private final RoundedButton cancelBtn = new RoundedButton("Cancel", RoundedButton.Style.SECONDARY);

    private final Map<String, Integer> categoryIds = new LinkedHashMap<>();
    private boolean busy = false;

    public NewReportFrame(User user) {
        this.currentUser = user;

        setTitle("FINDIT - Create New Report");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(580, 720);
        setMinimumSize(new Dimension(520, 660));
        setLocationRelativeTo(null);
        setResizable(false);

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

        JLabel title = new JLabel("Create a Report");
        title.setFont(AppTheme.font(Font.BOLD, 19f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Submit details to record a lost or found possession");
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

        // Two-column row for Type and Category
        JPanel typeAndCatRow = new JPanel(new GridLayout(1, 2, 12, 0));
        typeAndCatRow.setOpaque(false);
        typeAndCatRow.add(createFieldGroup("Report Type", typeBox));
        typeAndCatRow.add(createFieldGroup("Category", categoryBox));

        gbc.gridy = row++;
        form.add(typeAndCatRow, gbc);

        // Item Name
        gbc.gridy = row++;
        form.add(createFieldGroup("Item Name", UiKit.textBox(itemNameField)), gbc);

        // Location & Date
        JPanel locAndDateRow = new JPanel(new GridLayout(1, 2, 12, 0));
        locAndDateRow.setOpaque(false);
        locAndDateRow.add(createFieldGroup("Location Found / Lost", UiKit.textBox(locationField)));
        locAndDateRow.add(createFieldGroup("Report Date (YYYY-MM-DD)", UiKit.textBox(dateField)));

        gbc.gridy = row++;
        form.add(locAndDateRow, gbc);

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
        form.add(createFieldGroup("Item Description & Identifying Marks", descScroll), gbc);

        // Image Upload Section
        JPanel imageSection = buildImageUploadSection();
        gbc.gridy = row++;
        form.add(createFieldGroup("Item Photo (JPG, PNG, WEBP - max 5 MB)", imageSection), gbc);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        submitBtn.setPreferredSize(new Dimension(140, 38));

        buttonRow.add(cancelBtn);
        buttonRow.add(submitBtn);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(buttonRow, BorderLayout.SOUTH);

        root.add(card, BorderLayout.CENTER);
        setContentPane(root);

        // Style controls with modern dark SaaS combobox styling
        UiKit.styleComboBox(typeBox);
        UiKit.styleComboBox(categoryBox);

        // Actions
        submitBtn.addActionListener(e -> submitReport());
        cancelBtn.addActionListener(e -> dispose());
        browseImageBtn.addActionListener(e -> onBrowseImage());
        removeImageBtn.addActionListener(e -> onRemoveImage());

        // Load Categories off EDT
        loadCategoriesAsync();
    }

    private JPanel buildImageUploadSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        imagePreviewLabel.setFont(AppTheme.font(Font.PLAIN, 12f));
        imagePreviewLabel.setForeground(AppTheme.TEXT_MUTED);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        browseImageBtn.setPreferredSize(new Dimension(95, 36));

        removeImageBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        removeImageBtn.setForeground(AppTheme.DANGER);
        removeImageBtn.setContentAreaFilled(false);
        removeImageBtn.setBorderPainted(false);
        removeImageBtn.setFocusPainted(false);
        removeImageBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeImageBtn.setVisible(false);

        controls.add(removeImageBtn);
        controls.add(browseImageBtn);

        panel.add(imagePreviewLabel, BorderLayout.CENTER);
        panel.add(controls, BorderLayout.EAST);
        return panel;
    }

    private void onBrowseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Item Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.webp)", "jpg", "jpeg", "png", "webp"));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.length() > ImageStorageService.MAX_FILE_SIZE_BYTES) {
                DialogUtils.showWarning(this, "File Too Large", "Selected file exceeds the 5 MB size limit.");
                return;
            }
            this.selectedImageFile = file;
            imagePreviewLabel.setText("Selected: " + file.getName() + " (" + (file.length() / 1024) + " KB)");
            imagePreviewLabel.setForeground(AppTheme.ACCENT_CYAN);
            removeImageBtn.setVisible(true);
        }
    }

    private void onRemoveImage() {
        this.selectedImageFile = null;
        imagePreviewLabel.setText("No image selected");
        imagePreviewLabel.setForeground(AppTheme.TEXT_MUTED);
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
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        categoryIds.put(entry.getKey(), entry.getValue());
                        categoryBox.addItem(entry.getKey());
                    }
                } catch (Exception e) {
                    DialogUtils.showError(NewReportFrame.this, "Error", "Could not load categories: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        submitBtn.setEnabled(!busy);
        cancelBtn.setEnabled(!busy);
        browseImageBtn.setEnabled(!busy);
        submitBtn.setText(busy ? "Saving..." : "Submit Report");
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void submitReport() {
        if (busy) return;

        String itemName = itemNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String location = locationField.getText().trim();
        String dateText = dateField.getText().trim();
        String type = (String) typeBox.getSelectedItem();
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

        int categoryId = categoryIds.getOrDefault(category, 1);
        Item item = new Item();
        item.setItemName(itemName);
        item.setCategoryId(categoryId);
        item.setCategoryName(category);
        item.setDescription(description);

        Report report;
        if ("LOST".equalsIgnoreCase(type)) {
            report = new LostReport();
        } else {
            report = new FoundReport();
        }

        report.setUserId(currentUser.getUserId());
        report.setItem(item);
        report.setLocation(location);
        report.setReportDate(reportDate);
        report.setStatus("OPEN");

        setBusy(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // If user selected an image file, validate and save to uploads/
                if (selectedImageFile != null) {
                    String relativePath = ImageStorageService.saveImage(selectedImageFile);
                    item.setImagePath(relativePath);
                }
                return new ReportDAO().createReport(report);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        DialogUtils.showSuccess(NewReportFrame.this, "Report Created", "Your report has been submitted successfully!");
                        dispose();
                    } else {
                        DialogUtils.showError(NewReportFrame.this, "Failed", "Could not create the report. Please try again.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(NewReportFrame.this, "Database Error", "Failed to save report: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
