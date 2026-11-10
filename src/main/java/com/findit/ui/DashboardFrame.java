package com.findit.ui;

import com.findit.dao.ClaimDAO;
import com.findit.dao.NotificationDAO;
import com.findit.dao.ReportDAO;
import com.findit.model.Claim;
import com.findit.model.Report;
import com.findit.model.User;
import com.findit.service.ImageStorageService;
import com.findit.service.SmartMatchingService;
import com.findit.service.SmartMatchingService.MatchResult;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Premium SaaS Dashboard for FindIt — Smart Lost & Found Management System.
 * Refined with high-contrast UI, interactive reporter contact popovers,
 * in-table image thumbnails with full-size expansion, collapsible sidebar,
 * clickable KPI filter cards, client-side pagination, and notification center.
 */
public class DashboardFrame extends JFrame {

    private final User currentUser;
    private final ReportDAO reportDAO = new ReportDAO();
    private final ClaimDAO claimDAO = new ClaimDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // Table & Pagination
    private final DefaultTableModel tableModel;
    private final ModernTable reportsTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private List<Report> allFetchedReports = new ArrayList<>();
    private final List<Report> currentFilteredReports = new ArrayList<>();
    private Map<Integer, Integer> chronologicalRankMap = new HashMap<>();

    private int currentPage = 1;
    private int pageSize = 10;
    private final JLabel paginationLabel = new JLabel("Page 1 of 1 (0 reports)");
    private final RoundedButton prevPageBtn = new RoundedButton("Prev", UiKit.createLeftChevronIcon(14, null), RoundedButton.Style.SECONDARY);
    private final RoundedButton nextPageBtn = new RoundedButton("Next", UiKit.createRightChevronIcon(14, null), RoundedButton.Style.SECONDARY);
    private final JComboBox<String> pageSizeCombo = new JComboBox<>(new String[]{"10 per page", "25 per page", "50 per page"});
    private JPanel paginationBar;
    private JLabel sizeLbl;

    // Navigation / Filtering
    private enum NavFilter {
        ALL,
        LOST,
        FOUND,
        MY_REPORTS
    }

    private NavFilter activeFilter = NavFilter.ALL;
    private String statusFilter = null; // null = all statuses

    // Sidebar navigation buttons & components
    private JPanel rootPanel;
    private JPanel sidebarPanel;
    private boolean isSidebarCollapsed = false;
    private final JButton toggleSidebarBtn = new JButton("\u00AB");
    private JPanel brandPanel;
    private JPanel brandLeft;
    private JComponent logoIcon;
    private JPanel brandTextPanel;
    private JLabel brandName;
    private JLabel brandDesc;
    private JLabel menuHeading;
    private JLabel adminHeading;
    private JPanel userRow;
    private JLabel userRoleLabel;
    private JPanel userInfoTextPanel;
    private JPanel userActionsRow;
    private JPanel sidebarCreditPanel;

    private final RoundedButton btnNavAll = new RoundedButton("Dashboard / All", UiKit.createGridIcon(16, null), RoundedButton.Style.SIDEBAR);
    private final RoundedButton btnNavLost = new RoundedButton("Lost Items", UiKit.createSearchIcon(18, null), RoundedButton.Style.SIDEBAR);
    private final RoundedButton btnNavFound = new RoundedButton("Found Items", UiKit.createBoxIcon(16, null), RoundedButton.Style.SIDEBAR);
    private final RoundedButton btnNavMine = new RoundedButton("My Reports", UiKit.createUserIcon(16, null), RoundedButton.Style.SIDEBAR);
    private RoundedButton btnNavAdminClaims = null;

    // Top Bar
    private final JLabel pageTitleLabel = new JLabel("All Reports");
    private final JLabel pageSubtitleLabel = new JLabel("Overview of all lost and found submissions");
    private final JTextField searchField = new JTextField();
    private final RoundedButton notificationBellBtn = new RoundedButton("", UiKit.createBellIcon(18, null), RoundedButton.Style.SECONDARY);
    private final RoundedButton themeToggleBtn = new RoundedButton(
            AppTheme.isLight() ? "Dark" : "Light",
            AppTheme.isLight() ? UiKit.createMoonIcon(16, null) : UiKit.createSunIcon(16, null),
            RoundedButton.Style.SECONDARY);
    private final RoundedButton refreshButton = new RoundedButton("Refresh", RoundedButton.Style.SECONDARY);
    private final RoundedButton newReportButton = new RoundedButton("+ New Report", RoundedButton.Style.PRIMARY);

    // KPI Summary Stat Labels
    private final JLabel statTotalLabel = new JLabel("0");
    private final JLabel statOpenLabel = new JLabel("0");
    private final JLabel statPendingLabel = new JLabel("0");
    private final JLabel statClaimedLabel = new JLabel("0");
    private final JLabel statReturnedLabel = new JLabel("0");

    private final JLabel statOpenPercentLabel = new JLabel("");
    private final JLabel statPendingPercentLabel = new JLabel("");
    private final JLabel statClaimedPercentLabel = new JLabel("");
    private final JLabel statReturnedPercentLabel = new JLabel("");

    private final List<JPanel> statCardsList = new ArrayList<>();
    private final List<JLabel> statCardTitles = new ArrayList<>();
    private final List<JLabel> statCardPercents = new ArrayList<>();
    private JPanel mainContentPanel;

    // Action Toolbar & Preview Banner
    private final JPanel previewBannerPanel = new JPanel(new BorderLayout(12, 0));
    private final JLabel previewImageLabel = new JLabel();
    private final JLabel previewTitleLabel = new JLabel("Select a report from the table to preview details and take action");
    private final JLabel previewMetaLabel = new JLabel("");
    private final JLabel footerCreditLabel = new JLabel();

    private final RoundedButton actionMatchesBtn = new RoundedButton("Smart Matches", RoundedButton.Style.SECONDARY);
    private final RoundedButton actionPhotoBtn = new RoundedButton("View Photo", RoundedButton.Style.SECONDARY);
    private final RoundedButton actionEditBtn = new RoundedButton("View / Edit", RoundedButton.Style.SECONDARY);
    private final RoundedButton actionDeleteBtn = new RoundedButton("Delete", RoundedButton.Style.SECONDARY);
    private final RoundedButton actionClaimBtn = new RoundedButton("Submit Claim", RoundedButton.Style.PRIMARY);
    private final RoundedButton actionReturnBtn = new RoundedButton("Mark as Returned", RoundedButton.Style.SECONDARY);

    // Sidebar User components for live refresh
    private JLabel sidebarUserName;
    private JComponent sidebarAvatar;

    // Loading overlay
    private boolean isLoading = false;
    private final JLabel loadingIndicator = new JLabel("Loading data...");

    public DashboardFrame(User user) {
        this.currentUser = user;

        setTitle("FINDIT - Smart Lost & Found Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);

        // ---------- Setup Table Model ----------
        String[] columns = {
                "S.No.", "ID", "Type", "Item Name", "Category", "Location", "Report Date", "Status", "Reporter"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 1) return Integer.class;
                return String.class;
            }
        };

        reportsTable = new ModernTable(tableModel);
        reportsTable.setRowHeight(48); // Spacious height for thumbnails and circular avatars

        // Row Sorter
        rowSorter = new TableRowSorter<>(tableModel);
        rowSorter.setComparator(0, Comparator.comparingInt(o -> (Integer) o));
        rowSorter.setComparator(1, Comparator.comparingInt(o -> (Integer) o));
        reportsTable.setRowSorter(rowSorter);

        // Assign Badges
        reportsTable.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer(true)); // Type
        reportsTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer(false)); // Status

        // Column 3: Item Name with attached Photo Thumbnail
        reportsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                lbl.setVerticalAlignment(SwingConstants.CENTER);

                Report report = getReportAtRow(row);
                if (report != null && report.getItem() != null && report.getItem().getImagePath() != null
                        && !report.getItem().getImagePath().isBlank()) {
                    ImageIcon thumb = ImageStorageService.getRoundedThumbnail(report.getItem().getImagePath(), 28, 6);
                    lbl.setIcon(thumb);
                    lbl.setIconTextGap(10);
                } else {
                    lbl.setIcon(null);
                }

                if (isSelected) {
                    lbl.setBackground(ModernTable.SELECTION_BG);
                    lbl.setForeground(ModernTable.SELECTION_FG);
                    lbl.setFont(AppTheme.font(Font.BOLD, 13f));
                } else {
                    lbl.setBackground(row % 2 == 0 ? ModernTable.TABLE_BG : ModernTable.TABLE_ROW_ALT);
                    lbl.setForeground(ModernTable.ROW_TEXT_PRIMARY);
                    lbl.setFont(AppTheme.font(Font.BOLD, 13.5f));
                }
                return lbl;
            }
        });

        // Column 8: Reporter with circular avatar thumbnail & name
        reportsTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                lbl.setVerticalAlignment(SwingConstants.CENTER);

                Report report = getReportAtRow(row);
                if (report != null) {
                    String repName = report.getReporterName() != null ? report.getReporterName() : "User #" + report.getUserId();
                    ImageIcon avatar = ImageStorageService.getCircularAvatarThumbnail(report.getReporterProfileImage(), repName, 26);
                    lbl.setIcon(avatar);
                    lbl.setIconTextGap(8);
                    lbl.setText(repName);
                } else {
                    lbl.setIcon(null);
                }

                if (isSelected) {
                    lbl.setBackground(ModernTable.SELECTION_BG);
                    lbl.setForeground(ModernTable.SELECTION_FG);
                    lbl.setFont(AppTheme.font(Font.PLAIN, 13f));
                } else {
                    lbl.setBackground(row % 2 == 0 ? ModernTable.TABLE_BG : ModernTable.TABLE_ROW_ALT);
                    lbl.setForeground(ModernTable.ROW_TEXT_SECONDARY);
                    lbl.setFont(AppTheme.font(Font.PLAIN, 13f));
                }
                return lbl;
            }
        });

        // Column widths
        reportsTable.getColumnModel().getColumn(0).setPreferredWidth(60); // S.No.
        reportsTable.getColumnModel().getColumn(0).setMaxWidth(72);
        reportsTable.getColumnModel().getColumn(1).setPreferredWidth(52); // ID
        reportsTable.getColumnModel().getColumn(1).setMaxWidth(70);
        reportsTable.getColumnModel().getColumn(2).setPreferredWidth(95); // Type
        reportsTable.getColumnModel().getColumn(2).setMaxWidth(110);
        reportsTable.getColumnModel().getColumn(3).setPreferredWidth(185); // Item Name
        reportsTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Category
        reportsTable.getColumnModel().getColumn(5).setPreferredWidth(130); // Location
        reportsTable.getColumnModel().getColumn(6).setPreferredWidth(125); // Report Date
        reportsTable.getColumnModel().getColumn(6).setMinWidth(115);
        reportsTable.getColumnModel().getColumn(7).setPreferredWidth(130); // Status
        reportsTable.getColumnModel().getColumn(8).setPreferredWidth(145); // Reporter

        // Table selection listener
        reportsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionState();
            }
        });

        // Interactive Click & Context Menu Handlers
        reportsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = reportsTable.rowAtPoint(e.getPoint());
                int viewCol = reportsTable.columnAtPoint(e.getPoint());
                if (viewRow == -1 || viewCol == -1) return;

                int modelCol = reportsTable.convertColumnIndexToModel(viewCol);
                Report report = getReportAtRow(viewRow);
                if (report == null) return;

                // Single click on Column 3 (Item Name): if image exists, expand into full image!
                if (modelCol == 3 && report.getItem() != null && report.getItem().getImagePath() != null
                        && !report.getItem().getImagePath().isBlank()) {
                    new ImageViewerDialog(DashboardFrame.this, report.getItem().getItemName(), report.getItem().getImagePath()).setVisible(true);
                    return;
                }

                // Single click on Column 8 (Reporter): open Reporter Profile & Contact dialog!
                if (modelCol == 8) {
                    new ReporterContactDialog(DashboardFrame.this,
                            report.getReporterName(),
                            report.getReporterEmail(),
                            report.getReporterProfileImage(),
                            "USER").setVisible(true);
                    return;
                }

                // Double click row
                if (e.getClickCount() == 2) {
                    onRowDoubleClicked();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            private void showContextMenu(MouseEvent e) {
                int viewRow = reportsTable.rowAtPoint(e.getPoint());
                if (viewRow != -1) {
                    reportsTable.setRowSelectionInterval(viewRow, viewRow);
                    Report report = getReportAtRow(viewRow);
                    if (report != null) {
                        showTableRowContextMenu(e.getComponent(), e.getX(), e.getY(), report);
                    }
                }
            }
        });

        // Interactive Hand Cursor & Tooltips for image and reporter cells
        reportsTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int viewRow = reportsTable.rowAtPoint(e.getPoint());
                int viewCol = reportsTable.columnAtPoint(e.getPoint());
                if (viewRow != -1 && viewCol != -1) {
                    int modelCol = reportsTable.convertColumnIndexToModel(viewCol);
                    Report report = getReportAtRow(viewRow);
                    if (modelCol == 3 && report != null && report.getItem() != null && report.getItem().getImagePath() != null
                            && !report.getItem().getImagePath().isBlank()) {
                        reportsTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        reportsTable.setToolTipText("Click to view full image for " + report.getItem().getItemName());
                        return;
                    } else if (modelCol == 8 && report != null) {
                        reportsTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        String name = report.getReporterName() != null ? report.getReporterName() : "reporter";
                        reportsTable.setToolTipText("Click to view profile & contact info for " + name);
                        return;
                    }
                }
                reportsTable.setCursor(Cursor.getDefaultCursor());
                reportsTable.setToolTipText(null);
            }
        });

        // ---------- Layout Assembly ----------
        rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBackground(AppTheme.BG_MAIN);

        // 1. Sidebar
        sidebarPanel = buildSidebar();
        rootPanel.add(sidebarPanel, BorderLayout.WEST);

        // 2. Main Content
        JPanel mainContent = buildMainContent();
        rootPanel.add(mainContent, BorderLayout.CENTER);

        setContentPane(rootPanel);

        // Listen for live theme toggles
        AppTheme.addThemeChangeListener(this::updateThemeComponents);

        // Wire Actions
        wireActions();

        // Initial Selection State
        updateSelectionState();

        // Initial Load
        loadReportsAsync();
        refreshNotificationCount();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setBackground(AppTheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER),
                new EmptyBorder(16, 14, 16, 14)
        ));

        // Top Brand Header with Collapse Toggle Button
        brandPanel = new JPanel(new BorderLayout(8, 0));
        brandPanel.setOpaque(false);

        logoIcon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UiKit.drawFindItLogo(g2, 0, 0, 36);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(36, 36);
            }
        };

        brandTextPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        brandTextPanel.setOpaque(false);

        brandName = new JLabel("FINDIT");
        brandName.setFont(AppTheme.font(Font.BOLD, 18f));
        brandName.setForeground(AppTheme.TEXT_PRIMARY);

        brandDesc = new JLabel("Smart Lost & Found");
        brandDesc.setFont(AppTheme.font(Font.BOLD, 11.5f));
        brandDesc.setForeground(AppTheme.TEXT_MUTED);

        brandTextPanel.add(brandName);
        brandTextPanel.add(brandDesc);

        // Toggle Sidebar Button
        toggleSidebarBtn.setFont(AppTheme.font(Font.BOLD, 13f));
        toggleSidebarBtn.setForeground(AppTheme.TEXT_MUTED);
        toggleSidebarBtn.setBackground(AppTheme.BG_CARD);
        toggleSidebarBtn.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        toggleSidebarBtn.setFocusPainted(false);
        toggleSidebarBtn.setPreferredSize(new Dimension(28, 28));
        toggleSidebarBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleSidebarBtn.setToolTipText("Toggle sidebar collapse");
        toggleSidebarBtn.addActionListener(e -> toggleSidebar());

        brandLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandLeft.setOpaque(false);
        brandLeft.add(logoIcon);

        brandPanel.add(brandLeft, BorderLayout.WEST);
        brandPanel.add(brandTextPanel, BorderLayout.CENTER);
        brandPanel.add(toggleSidebarBtn, BorderLayout.EAST);

        // Navigation Menu
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        menuHeading = new JLabel("NAVIGATION");
        menuHeading.setFont(AppTheme.font(Font.BOLD, 11f));
        menuHeading.setForeground(AppTheme.TEXT_MUTED);
        menuHeading.setBorder(new EmptyBorder(0, 10, 8, 0));
        menuPanel.add(menuHeading);

        btnNavAll.setActive(true);
        menuPanel.add(btnNavAll);
        menuPanel.add(Box.createVerticalStrut(4));
        menuPanel.add(btnNavLost);
        menuPanel.add(Box.createVerticalStrut(4));
        menuPanel.add(btnNavFound);
        menuPanel.add(Box.createVerticalStrut(4));
        menuPanel.add(btnNavMine);

        // Admin Only Section
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        if (isAdmin) {
            menuPanel.add(Box.createVerticalStrut(18));
            adminHeading = new JLabel("ADMINISTRATION");
            adminHeading.setFont(AppTheme.font(Font.BOLD, 10.5f));
            adminHeading.setForeground(AppTheme.TEXT_MUTED);
            adminHeading.setBorder(new EmptyBorder(0, 10, 8, 0));
            menuPanel.add(adminHeading);

            btnNavAdminClaims = new RoundedButton("Manage Claims", UiKit.createShieldIcon(16, null), RoundedButton.Style.SIDEBAR);
            menuPanel.add(btnNavAdminClaims);
        }

        // Bottom User Profile Card & Footer Credit
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                new EmptyBorder(12, 2, 8, 2)));

        userRow = new JPanel(new BorderLayout(10, 0));
        userRow.setOpaque(false);
        userRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        sidebarAvatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int size = 36;

                if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().isBlank()) {
                    File imgFile = new File(currentUser.getProfileImage());
                    if (imgFile.exists()) {
                        try {
                            BufferedImage img = ImageIO.read(imgFile);
                            if (img != null) {
                                Shape circle = new Ellipse2D.Float(1, 1, size - 2, size - 2);
                                g2.setClip(circle);
                                g2.drawImage(img, 1, 1, size - 2, size - 2, null);
                                g2.setClip(null);

                                g2.setColor(AppTheme.ACCENT_CYAN);
                                g2.setStroke(new BasicStroke(1.8f));
                                g2.draw(new Ellipse2D.Float(1, 1, size - 2, size - 2));
                                g2.dispose();
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                g2.setPaint(new GradientPaint(0, 0, AppTheme.PRIMARY_INDIGO, size, size, AppTheme.ACCENT_CYAN));
                g2.fill(new Ellipse2D.Float(0, 0, size, size));

                String initial = (!currentUser.getName().isEmpty())
                        ? currentUser.getName().substring(0, 1).toUpperCase()
                        : "U";
                g2.setColor(Color.WHITE);
                g2.setFont(AppTheme.font(Font.BOLD, 14.5f));
                FontMetrics fm = g2.getFontMetrics();
                int x = (size - fm.stringWidth(initial)) / 2;
                int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(36, 36);
            }
        };

        userInfoTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        userInfoTextPanel.setOpaque(false);

        sidebarUserName = new JLabel(currentUser.getName());
        sidebarUserName.setFont(AppTheme.font(Font.BOLD, 14f));
        sidebarUserName.setForeground(AppTheme.TEXT_PRIMARY);

        userRoleLabel = new JLabel(isAdmin ? "ADMINISTRATOR" : "VERIFIED USER");
        userRoleLabel.setFont(AppTheme.font(Font.BOLD, 11f));
        userRoleLabel.setForeground(isAdmin
                ? (AppTheme.isLight() ? new Color(0x02, 0x84, 0xC7) : new Color(0x38, 0xBD, 0xF8))
                : (AppTheme.isLight() ? new Color(0x05, 0x96, 0x69) : new Color(0x34, 0xD3, 0x99)));

        userInfoTextPanel.add(sidebarUserName);
        userInfoTextPanel.add(userRoleLabel);

        userRow.add(sidebarAvatar, BorderLayout.WEST);
        userRow.add(userInfoTextPanel, BorderLayout.CENTER);

        userRow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new UserProfileDialog(DashboardFrame.this, currentUser, DashboardFrame.this::refreshUserProfile)
                        .setVisible(true);
            }
        });

        userActionsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        userActionsRow.setOpaque(false);
        userActionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        RoundedButton settingsBtn = new RoundedButton("Settings", RoundedButton.Style.SECONDARY);
        settingsBtn.setPreferredSize(new Dimension(105, 32));
        settingsBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        settingsBtn.setForeground(Color.WHITE);
        settingsBtn.addActionListener(e -> {
            new UserProfileDialog(DashboardFrame.this, currentUser, DashboardFrame.this::refreshUserProfile)
                    .setVisible(true);
        });

        RoundedButton logoutBtn = new RoundedButton("Sign Out", RoundedButton.Style.SECONDARY);
        logoutBtn.setPreferredSize(new Dimension(105, 32));
        logoutBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> logout());

        userActionsRow.add(settingsBtn);
        userActionsRow.add(logoutBtn);

        // "Designed with [Heart Image]" placed directly below Settings and Sign Out buttons
        footerCreditLabel.setText("Designed with");
        footerCreditLabel.setFont(AppTheme.font(Font.BOLD, 12f));
        footerCreditLabel.setForeground(AppTheme.isLight() ? new Color(0x47, 0x55, 0x69) : new Color(0xCB, 0xD5, 0xE1));
        footerCreditLabel.setIcon(UiKit.createHeartIcon(14, new Color(0xEF, 0x44, 0x44)));
        footerCreditLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        footerCreditLabel.setIconTextGap(6);

        sidebarCreditPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        sidebarCreditPanel.setOpaque(false);
        sidebarCreditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        sidebarCreditPanel.add(footerCreditLabel);

        bottomPanel.add(userRow);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(userActionsRow);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(sidebarCreditPanel);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(menuPanel, BorderLayout.CENTER);
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private void toggleSidebar() {
        isSidebarCollapsed = !isSidebarCollapsed;
        int targetWidth = isSidebarCollapsed ? 68 : 250;
        sidebarPanel.setPreferredSize(new Dimension(targetWidth, 0));
        sidebarPanel.setMinimumSize(new Dimension(targetWidth, 0));
        sidebarPanel.setBorder(new EmptyBorder(16, isSidebarCollapsed ? 8 : 14, 16, isSidebarCollapsed ? 8 : 14));

        menuHeading.setVisible(!isSidebarCollapsed);
        if (adminHeading != null) {
            adminHeading.setVisible(!isSidebarCollapsed);
        }
        userActionsRow.setVisible(!isSidebarCollapsed);
        if (sidebarCreditPanel != null) {
            sidebarCreditPanel.setVisible(!isSidebarCollapsed);
        }
        toggleSidebarBtn.setText(isSidebarCollapsed ? "\u00BB" : "\u00AB");
        toggleSidebarBtn.setToolTipText(isSidebarCollapsed ? "Expand sidebar" : "Collapse sidebar");

        if (isSidebarCollapsed) {
            brandPanel.removeAll();
            brandPanel.add(toggleSidebarBtn, BorderLayout.CENTER);

            userRow.removeAll();
            userRow.add(sidebarAvatar, BorderLayout.CENTER);
        } else {
            brandPanel.removeAll();
            brandPanel.add(brandLeft, BorderLayout.WEST);
            brandPanel.add(brandTextPanel, BorderLayout.CENTER);
            brandPanel.add(toggleSidebarBtn, BorderLayout.EAST);

            userRow.removeAll();
            userRow.add(sidebarAvatar, BorderLayout.WEST);
            userRow.add(userInfoTextPanel, BorderLayout.CENTER);
        }

        Dimension btnSize = isSidebarCollapsed ? new Dimension(46, 40) : new Dimension(220, 42);
        int align = isSidebarCollapsed ? SwingConstants.CENTER : SwingConstants.LEFT;

        configureNavButton(btnNavAll, "Dashboard / All", btnSize, align);
        configureNavButton(btnNavLost, "Lost Items", btnSize, align);
        configureNavButton(btnNavFound, "Found Items", btnSize, align);
        configureNavButton(btnNavMine, "My Reports", btnSize, align);
        if (btnNavAdminClaims != null) {
            configureNavButton(btnNavAdminClaims, "Manage Claims", btnSize, align);
        }

        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private void configureNavButton(RoundedButton btn, String text, Dimension size, int align) {
        btn.setPreferredSize(size);
        btn.setMaximumSize(new Dimension(isSidebarCollapsed ? 46 : Integer.MAX_VALUE, size.height));
        btn.setHorizontalAlignment(align);
        btn.setAlignmentX(isSidebarCollapsed ? Component.CENTER_ALIGNMENT : Component.LEFT_ALIGNMENT);
        if (isSidebarCollapsed) {
            btn.setText("");
            btn.setToolTipText(text);
            btn.setBorder(new EmptyBorder(8, 0, 8, 0));
        } else {
            btn.setText(text);
            btn.setToolTipText(null);
            btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        }
    }

    private void refreshUserProfile() {
        if (sidebarUserName != null) {
            sidebarUserName.setText(currentUser.getName());
        }
        if (sidebarAvatar != null) {
            sidebarAvatar.repaint();
        }
    }

    private void updateThemeComponents() {
        if (rootPanel != null) {
            rootPanel.setBackground(AppTheme.BG_MAIN);
        }
        if (mainContentPanel != null) {
            mainContentPanel.setBackground(AppTheme.BG_MAIN);
        }
        if (sidebarPanel != null) {
            sidebarPanel.setBackground(AppTheme.BG_SIDEBAR);
            sidebarPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER),
                    new EmptyBorder(16, isSidebarCollapsed ? 8 : 14, 16, isSidebarCollapsed ? 8 : 14)
            ));
        }
        if (brandName != null) {
            brandName.setForeground(AppTheme.TEXT_PRIMARY);
        }
        if (brandDesc != null) {
            brandDesc.setForeground(AppTheme.TEXT_MUTED);
        }
        if (menuHeading != null) {
            menuHeading.setForeground(AppTheme.TEXT_MUTED);
        }
        if (adminHeading != null) {
            adminHeading.setForeground(AppTheme.TEXT_MUTED);
        }
        if (sidebarUserName != null) {
            sidebarUserName.setForeground(AppTheme.TEXT_PRIMARY);
        }
        if (userRoleLabel != null) {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
            userRoleLabel.setForeground(isAdmin
                    ? (AppTheme.isLight() ? new Color(0x02, 0x84, 0xC7) : new Color(0x38, 0xBD, 0xF8))
                    : (AppTheme.isLight() ? new Color(0x05, 0x96, 0x69) : new Color(0x34, 0xD3, 0x99)));
        }
        if (toggleSidebarBtn != null) {
            toggleSidebarBtn.setForeground(AppTheme.TEXT_MUTED);
            toggleSidebarBtn.setBackground(AppTheme.BG_CARD);
            toggleSidebarBtn.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        }

        pageTitleLabel.setForeground(AppTheme.isLight() ? new Color(0x0F, 0x17, 0x2A) : Color.WHITE);
        pageSubtitleLabel.setForeground(AppTheme.isLight() ? new Color(0x33, 0x41, 0x55) : new Color(0xE2, 0xE8, 0xF0));
        previewTitleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        previewMetaLabel.setForeground(AppTheme.isLight() ? new Color(0x33, 0x41, 0x55) : new Color(0xE2, 0xE8, 0xF0));
        footerCreditLabel.setForeground(AppTheme.isLight() ? new Color(0x47, 0x55, 0x69) : new Color(0xCB, 0xD5, 0xE1));

        for (JLabel l : statCardTitles) {
            l.setForeground(AppTheme.isLight() ? new Color(0x47, 0x55, 0x69) : new Color(0xCB, 0xD5, 0xE1));
        }
        for (JLabel l : statCardPercents) {
            l.setForeground(AppTheme.isLight() ? new Color(0x64, 0x74, 0x8B) : new Color(0x94, 0xA3, 0xB8));
        }

        if (paginationLabel != null) {
            paginationLabel.setForeground(new Color(0x0F, 0x17, 0x2A));
        }
        if (sizeLbl != null) {
            sizeLbl.setForeground(new Color(0x33, 0x41, 0x55));
        }
        if (pageSizeCombo != null) {
            pageSizeCombo.setBackground(Color.WHITE);
            pageSizeCombo.setForeground(new Color(0x0F, 0x17, 0x2A));
            pageSizeCombo.setBorder(BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true));
        }
        if (paginationBar != null) {
            paginationBar.setBackground(new Color(0xF8, 0xFA, 0xFC));
            paginationBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCB, 0xD5, 0xE1)),
                    new EmptyBorder(8, 16, 8, 16)
            ));
        }

        themeToggleBtn.setText(AppTheme.isLight() ? "Dark" : "Light");
        themeToggleBtn.setLeadingIcon(AppTheme.isLight() ? UiKit.createMoonIcon(16, null) : UiKit.createSunIcon(16, null));
        themeToggleBtn.setToolTipText("Switch to " + (AppTheme.isLight() ? "Midnight Dark Theme" : "Modern Light Theme"));

        for (JPanel c : statCardsList) {
            c.repaint();
        }
        reportsTable.repaint();
        if (rootPanel != null) {
            rootPanel.revalidate();
            rootPanel.repaint();
        }
    }

    private void refreshNotificationCount() {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return notificationDAO.getUnreadCount(currentUser.getUserId());
            }

            @Override
            protected void done() {
                try {
                    int unread = get();
                    notificationBellBtn.setText(unread > 0 ? " " + unread : "");
                    notificationBellBtn.setStyle(unread > 0 ? RoundedButton.Style.PRIMARY : RoundedButton.Style.SECONDARY);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JPanel buildMainContent() {
        mainContentPanel = new JPanel(new BorderLayout(0, 16));
        mainContentPanel.setBackground(AppTheme.BG_MAIN);
        mainContentPanel.setBorder(new EmptyBorder(18, 24, 18, 24));

        // 1. Top Bar
        JPanel topBar = buildTopBar();

        // 2. Statistics KPI Cards Bar
        JPanel statsBar = buildStatsBar();

        // 3. Center Section: Table Card with integrated Pagination + Action Toolbar
        JPanel centerSection = new JPanel(new BorderLayout(0, 12));
        centerSection.setOpaque(false);

        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(ModernTable.TABLE_BG);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.isLight() ? new Color(0xCB, 0xD5, 0xE1) : AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(new EmptyBorder(1, 1, 1, 1));

        JScrollPane scrollPane = new JScrollPane(reportsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ModernTable.TABLE_BG);

        scrollPane.getViewport().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                reportsTable.clearSelection();
            }
        });

        JPanel paginationBar = buildPaginationBar();

        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(paginationBar, BorderLayout.SOUTH);

        // Action Toolbar
        JPanel actionToolbar = buildActionToolbar();

        centerSection.add(tableCard, BorderLayout.CENTER);
        centerSection.add(actionToolbar, BorderLayout.SOUTH);

        // Stack TopBar + StatsBar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topBar);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(statsBar);

        mainContentPanel.add(topContainer, BorderLayout.NORTH);
        mainContentPanel.add(centerSection, BorderLayout.CENTER);

        return mainContentPanel;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 58));

        // WEST: Title and Subtitle Box with ample vertical headroom
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.setPreferredSize(new Dimension(390, 58));

        pageTitleLabel.setFont(AppTheme.font(Font.BOLD, 22f));
        pageTitleLabel.setForeground(AppTheme.isLight() ? new Color(0x0F, 0x17, 0x2A) : Color.WHITE);
        pageTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pageTitleLabel.setVerticalAlignment(SwingConstants.CENTER);
        pageTitleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        loadingIndicator.setFont(AppTheme.font(Font.ITALIC, 12f));
        loadingIndicator.setForeground(AppTheme.ACCENT_CYAN);
        loadingIndicator.setVisible(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        titleRow.add(pageTitleLabel);
        titleRow.add(loadingIndicator);

        pageSubtitleLabel.setFont(AppTheme.font(Font.BOLD, 13f));
        pageSubtitleLabel.setForeground(AppTheme.isLight() ? new Color(0x33, 0x41, 0x55) : new Color(0xE2, 0xE8, 0xF0));
        pageSubtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pageSubtitleLabel.setVerticalAlignment(SwingConstants.CENTER);
        pageSubtitleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        titleBox.add(titleRow);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(pageSubtitleLabel);

        // EAST: Controls Panel containing Search Bar + Action Buttons
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 10));
        controlsPanel.setOpaque(false);

        UiKit.SearchBox searchBox = new UiKit.SearchBox(searchField, "Search reports...");
        searchBox.setPreferredSize(new Dimension(205, 38));

        themeToggleBtn.setPreferredSize(new Dimension(86, 38));
        themeToggleBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        themeToggleBtn.setToolTipText("Switch to " + (AppTheme.isLight() ? "Midnight Dark Theme" : "Modern Light Theme"));
        themeToggleBtn.addActionListener(e -> {
            AppTheme.toggleTheme();
            updateThemeComponents();
        });

        notificationBellBtn.setPreferredSize(new Dimension(40, 38));
        notificationBellBtn.setFont(AppTheme.font(Font.BOLD, 13f));
        notificationBellBtn.addActionListener(e -> {
            new NotificationCenterDialog(this, currentUser, this::refreshNotificationCount).setVisible(true);
        });

        refreshButton.setPreferredSize(new Dimension(82, 38));
        refreshButton.setFont(AppTheme.font(Font.BOLD, 12f));

        newReportButton.setPreferredSize(new Dimension(118, 38));
        newReportButton.setFont(AppTheme.font(Font.BOLD, 12.5f));

        controlsPanel.add(searchBox);
        controlsPanel.add(themeToggleBtn);
        controlsPanel.add(notificationBellBtn);
        controlsPanel.add(refreshButton);
        controlsPanel.add(newReportButton);

        bar.add(titleBox, BorderLayout.WEST);
        bar.add(controlsPanel, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildStatsBar() {
        JPanel bar = new JPanel(new GridLayout(1, 5, 12, 0));
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 72)); // Reduced height slightly to 72px

        statCardsList.clear();
        statCardTitles.clear();
        statCardPercents.clear();

        JPanel cardTotal = createStatCard("Total Reports", statTotalLabel, null, AppTheme.PRIMARY_INDIGO, new Color(0x1E, 0x1B, 0x4B), 0, null);
        JPanel cardOpen = createStatCard("Open Reports", statOpenLabel, statOpenPercentLabel, AppTheme.SUCCESS, AppTheme.SUCCESS_BG, 1, "OPEN");
        JPanel cardPending = createStatCard("Claims Pending", statPendingLabel, statPendingPercentLabel, AppTheme.WARNING, AppTheme.WARNING_BG, 2, "CLAIM_PENDING");
        JPanel cardClaimed = createStatCard("Claimed Items", statClaimedLabel, statClaimedPercentLabel, AppTheme.PURPLE, AppTheme.PURPLE_BG, 3, "CLAIMED");
        JPanel cardReturned = createStatCard("Returned Items", statReturnedLabel, statReturnedPercentLabel, AppTheme.INFO, AppTheme.INFO_BG, 4, "RETURNED");

        statCardsList.add(cardTotal);
        statCardsList.add(cardOpen);
        statCardsList.add(cardPending);
        statCardsList.add(cardClaimed);
        statCardsList.add(cardReturned);

        bar.add(cardTotal);
        bar.add(cardOpen);
        bar.add(cardPending);
        bar.add(cardClaimed);
        bar.add(cardReturned);

        return bar;
    }

    private JPanel createStatCard(String label, JLabel countLabel, JLabel percentLabel, Color accentColor,
                                  Color bgBadge, int iconType, String statusTarget) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();

                boolean isActive = (statusTarget == null && statusFilter == null)
                        || (statusTarget != null && statusTarget.equalsIgnoreCase(statusFilter));

                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                if (isActive) {
                    g2.setColor(accentColor);
                    g2.setStroke(new BasicStroke(1.8f));
                } else {
                    g2.setColor(AppTheme.BORDER);
                    g2.setStroke(new BasicStroke(1f));
                }
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Click to filter table by " + label);

        // Click to filter by status
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (statusTarget == null) {
                    statusFilter = null;
                } else if (statusTarget.equalsIgnoreCase(statusFilter)) {
                    statusFilter = null; // Toggle off
                } else {
                    statusFilter = statusTarget;
                }
                for (JPanel c : statCardsList) {
                    c.repaint();
                }
                filterReports();
            }
        });

        // Text Stack: Title on top, count + percentage below
        JPanel textStack = new JPanel(new GridLayout(2, 1, 0, 2));
        textStack.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.font(Font.BOLD, 11.5f));
        lbl.setForeground(AppTheme.isLight() ? new Color(0x47, 0x55, 0x69) : new Color(0xCB, 0xD5, 0xE1));
        statCardTitles.add(lbl);

        JPanel numbersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        numbersRow.setOpaque(false);

        countLabel.setFont(AppTheme.font(Font.BOLD, 20f));
        countLabel.setForeground(accentColor);
        numbersRow.add(countLabel);

        if (percentLabel != null) {
            percentLabel.setFont(AppTheme.font(Font.PLAIN, 11f));
            percentLabel.setForeground(AppTheme.isLight() ? new Color(0x64, 0x74, 0x8B) : new Color(0x94, 0xA3, 0xB8));
            statCardPercents.add(percentLabel);
            numbersRow.add(percentLabel);
        }

        textStack.add(lbl);
        textStack.add(numbersRow);

        // Icon Pill badge on the right with custom vector icons
        JComponent badge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int size = 32;
                Color bg = AppTheme.isLight()
                        ? (iconType == 0 ? new Color(0xEE, 0xF2, 0xFF)
                        : iconType == 1 ? AppTheme.SUCCESS_BG
                        : iconType == 2 ? AppTheme.WARNING_BG
                        : iconType == 3 ? AppTheme.PURPLE_BG
                        : AppTheme.INFO_BG)
                        : bgBadge;
                g2.setColor(bg);
                g2.fillOval(0, 0, size, size);

                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(1.8f));

                switch (iconType) {
                    case 0 -> { // Stack / Layers (Total)
                        g2.drawRoundRect(9, 7, 14, 4, 2, 2);
                        g2.drawRoundRect(8, 13, 16, 4, 2, 2);
                        g2.drawRoundRect(7, 19, 18, 4, 2, 2);
                    }
                    case 1 -> { // Check Circle (Open)
                        g2.drawOval(6, 6, 20, 20);
                        g2.drawLine(11, 16, 15, 20);
                        g2.drawLine(15, 20, 21, 12);
                    }
                    case 2 -> { // Hourglass (Pending)
                        int[] xPoints = {9, 23, 17, 23, 9, 15};
                        int[] yPoints = {8, 8, 16, 24, 24, 16};
                        g2.drawPolygon(xPoints, yPoints, 6);
                        g2.drawLine(9, 8, 23, 8);
                        g2.drawLine(9, 24, 23, 24);
                    }
                    case 3 -> { // Shield & Key (Claimed)
                        g2.drawRoundRect(9, 7, 14, 17, 6, 6);
                        g2.drawOval(13, 11, 6, 6);
                        g2.drawLine(16, 17, 16, 20);
                    }
                    case 4 -> { // Return Arrow (Returned)
                        g2.drawArc(9, 10, 14, 14, 0, 240);
                        g2.drawLine(9, 14, 6, 18);
                        g2.drawLine(9, 14, 13, 18);
                    }
                }
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }
        };

        card.add(textStack, BorderLayout.CENTER);
        card.add(badge, BorderLayout.EAST);

        return card;
    }

    private JPanel buildPaginationBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(new Color(0xF8, 0xFA, 0xFC));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCB, 0xD5, 0xE1)),
                new EmptyBorder(6, 16, 6, 16)
        ));
        bar.setPreferredSize(new Dimension(0, 46));
        this.paginationBar = bar;

        paginationLabel.setFont(AppTheme.font(Font.BOLD, 12.5f));
        paginationLabel.setForeground(new Color(0x0F, 0x17, 0x2A));
        paginationLabel.setPreferredSize(new Dimension(360, 32));
        paginationLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel navBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        navBox.setOpaque(false);

        prevPageBtn.setPreferredSize(new Dimension(90, 32));
        prevPageBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        prevPageBtn.setIcon(UiKit.createLeftChevronIcon(14, null));
        prevPageBtn.setIconTextGap(6);
        prevPageBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        prevPageBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
            }
        });

        nextPageBtn.setPreferredSize(new Dimension(90, 32));
        nextPageBtn.setFont(AppTheme.font(Font.BOLD, 12f));
        nextPageBtn.setIcon(UiKit.createRightChevronIcon(14, null));
        nextPageBtn.setIconTextGap(6);
        nextPageBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        nextPageBtn.addActionListener(e -> {
            int totalPages = Math.max(1, (int) Math.ceil((double) currentFilteredReports.size() / pageSize));
            if (currentPage < totalPages) {
                currentPage++;
                renderCurrentPage();
            }
        });

        navBox.add(prevPageBtn);
        navBox.add(nextPageBtn);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.setOpaque(false);

        this.sizeLbl = new JLabel("Rows:");
        sizeLbl.setFont(AppTheme.font(Font.BOLD, 12f));
        sizeLbl.setForeground(new Color(0x33, 0x41, 0x55));
        sizeLbl.setVerticalAlignment(SwingConstants.CENTER);

        pageSizeCombo.setPreferredSize(new Dimension(132, 32));
        pageSizeCombo.setFont(AppTheme.font(Font.BOLD, 12f));
        pageSizeCombo.setBackground(Color.WHITE);
        pageSizeCombo.setForeground(new Color(0x0F, 0x17, 0x2A));
        pageSizeCombo.setBorder(BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true));
        pageSizeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
                lbl.setFont(AppTheme.font(Font.PLAIN, 12f));
                if (isSelected) {
                    lbl.setBackground(AppTheme.PRIMARY_INDIGO);
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(0x0F, 0x17, 0x2A));
                }
                return lbl;
            }
        });
        pageSizeCombo.addActionListener(e -> {
            int idx = pageSizeCombo.getSelectedIndex();
            pageSize = (idx == 1) ? 25 : (idx == 2) ? 50 : 10;
            currentPage = 1;
            renderCurrentPage();
        });

        rightBox.add(sizeLbl);
        rightBox.add(pageSizeCombo);

        bar.add(paginationLabel, BorderLayout.WEST);
        bar.add(navBox, BorderLayout.CENTER);
        bar.add(rightBox, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildActionToolbar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 0, 0, 0));

        // 1. Dedicated Selected Report Preview Banner
        previewBannerPanel.setOpaque(false);
        previewBannerPanel.setBorder(new EmptyBorder(0, 4, 8, 4));
        previewBannerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        previewTitleLabel.setFont(AppTheme.font(Font.BOLD, 13.5f));
        previewTitleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        previewMetaLabel.setFont(AppTheme.font(Font.BOLD, 12.5f));
        previewMetaLabel.setForeground(AppTheme.isLight() ? new Color(0x33, 0x41, 0x55) : new Color(0xE2, 0xE8, 0xF0));

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);
        textStack.add(previewTitleLabel);
        textStack.add(Box.createVerticalStrut(2));
        textStack.add(previewMetaLabel);

        previewBannerPanel.add(previewImageLabel, BorderLayout.WEST);
        previewBannerPanel.add(textStack, BorderLayout.CENTER);

        // 2. Action Buttons Row (spacious sizing, no truncations)
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonGroup.setOpaque(false);
        buttonGroup.setAlignmentX(Component.LEFT_ALIGNMENT);

        actionMatchesBtn.setPreferredSize(new Dimension(130, 38));
        actionPhotoBtn.setPreferredSize(new Dimension(125, 38)); // Increased to guarantee "View Photo" never truncates
        actionEditBtn.setPreferredSize(new Dimension(110, 38));
        actionDeleteBtn.setPreferredSize(new Dimension(95, 38));
        actionClaimBtn.setPreferredSize(new Dimension(145, 38));
        actionReturnBtn.setPreferredSize(new Dimension(155, 38));

        buttonGroup.add(actionMatchesBtn);
        buttonGroup.add(actionPhotoBtn);
        buttonGroup.add(actionEditBtn);
        buttonGroup.add(actionDeleteBtn);
        buttonGroup.add(actionClaimBtn);
        buttonGroup.add(actionReturnBtn);

        bar.add(previewBannerPanel);
        bar.add(buttonGroup);

        return bar;
    }

    private void wireActions() {
        btnNavAll.addActionListener(
                e -> switchNavigation(NavFilter.ALL, "All Reports", "Overview of all lost and found submissions"));
        btnNavLost.addActionListener(e -> switchNavigation(NavFilter.LOST, "Lost Items",
                "Items reported lost by owners awaiting discovery"));
        btnNavFound.addActionListener(e -> switchNavigation(NavFilter.FOUND, "Found Items",
                "Items found across campus available for claiming"));
        btnNavMine.addActionListener(
                e -> switchNavigation(NavFilter.MY_REPORTS, "My Reports", "Submissions created and managed by you"));

        if (btnNavAdminClaims != null) {
            btnNavAdminClaims.addActionListener(e -> {
                AdminClaimsFrame frame = new AdminClaimsFrame(currentUser);
                frame.setVisible(true);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        loadReportsAsync();
                        refreshNotificationCount();
                    }
                });
            });
        }

        UiKit.onChange(searchField, this::filterReports);

        refreshButton.addActionListener(e -> {
            loadReportsAsync();
            refreshNotificationCount();
        });

        newReportButton.addActionListener(e -> {
            NewReportFrame form = new NewReportFrame(currentUser);
            form.setVisible(true);
            form.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    loadReportsAsync();
                    refreshNotificationCount();
                }
            });
        });

        actionMatchesBtn.addActionListener(e -> onSmartMatchesSelected());
        actionPhotoBtn.addActionListener(e -> onPhotoSelected());
        actionEditBtn.addActionListener(e -> onEditSelected());
        actionDeleteBtn.addActionListener(e -> onDeleteSelected());
        actionClaimBtn.addActionListener(e -> onClaimSelected());
        actionReturnBtn.addActionListener(e -> onReturnSelected());
    }

    private void switchNavigation(NavFilter filter, String title, String subtitle) {
        this.activeFilter = filter;
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);

        btnNavAll.setActive(filter == NavFilter.ALL);
        btnNavLost.setActive(filter == NavFilter.LOST);
        btnNavFound.setActive(filter == NavFilter.FOUND);
        btnNavMine.setActive(filter == NavFilter.MY_REPORTS);

        filterReports();
    }

    private void setLoading(boolean loading) {
        this.isLoading = loading;
        loadingIndicator.setVisible(loading);
        refreshButton.setEnabled(!loading);
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void loadReportsAsync() {
        if (isLoading) return;
        setLoading(true);

        new SwingWorker<List<Report>, Void>() {
            @Override
            protected List<Report> doInBackground() throws Exception {
                return reportDAO.getAllReports();
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    allFetchedReports = get();
                    updateStatistics();
                    filterReports();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    DialogUtils.showError(
                            DashboardFrame.this,
                            "Database Error",
                            "Unable to retrieve reports from the database. Please check your connection.");
                }
            }
        }.execute();
    }

    private void updateStatistics() {
        int total = allFetchedReports.size();
        int open = 0;
        int pending = 0;
        int claimed = 0;
        int returned = 0;

        for (Report r : allFetchedReports) {
            String status = r.getStatus() != null ? r.getStatus().toUpperCase() : "";
            switch (status) {
                case "OPEN" -> open++;
                case "CLAIM_PENDING" -> pending++;
                case "CLAIMED" -> claimed++;
                case "RETURNED" -> returned++;
            }
        }

        statTotalLabel.setText(String.valueOf(total));
        statOpenLabel.setText(String.valueOf(open));
        statPendingLabel.setText(String.valueOf(pending));
        statClaimedLabel.setText(String.valueOf(claimed));
        statReturnedLabel.setText(String.valueOf(returned));

        if (total > 0) {
            statOpenPercentLabel.setText("(" + Math.round((open * 100.0) / total) + "%)");
            statPendingPercentLabel.setText("(" + Math.round((pending * 100.0) / total) + "%)");
            statClaimedPercentLabel.setText("(" + Math.round((claimed * 100.0) / total) + "%)");
            statReturnedPercentLabel.setText("(" + Math.round((returned * 100.0) / total) + "%)");
        } else {
            statOpenPercentLabel.setText("");
            statPendingPercentLabel.setText("");
            statClaimedPercentLabel.setText("");
            statReturnedPercentLabel.setText("");
        }
    }

    /**
     * Builds a map of reportId → chronological serial number.
     * S.No. 1 = oldest report (earliest date, then lowest ID as tiebreaker).
     * The newest report gets the highest S.No.
     * This map is used so filtered/search results retain original S.No. values.
     */
    private Map<Integer, Integer> buildChronologicalRankMap() {
        Map<Integer, Integer> rankMap = new HashMap<>();
        if (allFetchedReports == null || allFetchedReports.isEmpty()) return rankMap;

        List<Report> sorted = new ArrayList<>(allFetchedReports);
        sorted.sort(Comparator
                .comparing((Report r) -> r.getReportDate() != null ? r.getReportDate() : java.time.LocalDate.MIN)
                .thenComparingInt(Report::getReportId));

        for (int i = 0; i < sorted.size(); i++) {
            rankMap.put(sorted.get(i).getReportId(), i + 1);
        }
        return rankMap;
    }

    private void filterReports() {
        // Rebuild chronological rank map whenever we re-filter (allFetchedReports may have changed)
        chronologicalRankMap = buildChronologicalRankMap();

        String keyword = searchField.getText().trim().toLowerCase();
        currentFilteredReports.clear();

        for (Report r : allFetchedReports) {
            if (activeFilter == NavFilter.LOST && !"LOST".equalsIgnoreCase(r.getReportType())) {
                continue;
            }
            if (activeFilter == NavFilter.FOUND && !"FOUND".equalsIgnoreCase(r.getReportType())) {
                continue;
            }
            if (activeFilter == NavFilter.MY_REPORTS && r.getUserId() != currentUser.getUserId()) {
                continue;
            }

            if (statusFilter != null && !statusFilter.equalsIgnoreCase(r.getStatus())) {
                continue;
            }

            if (!keyword.isEmpty()) {
                String name = r.getItem() != null && r.getItem().getItemName() != null
                        ? r.getItem().getItemName().toLowerCase() : "";
                String desc = r.getItem() != null && r.getItem().getDescription() != null
                        ? r.getItem().getDescription().toLowerCase() : "";
                String loc = r.getLocation() != null ? r.getLocation().toLowerCase() : "";
                String cat = r.getItem() != null && r.getItem().getCategoryName() != null
                        ? r.getItem().getCategoryName().toLowerCase() : "";
                String rep = r.getReporterName() != null ? r.getReporterName().toLowerCase() : "";

                if (!name.contains(keyword) && !desc.contains(keyword) && !loc.contains(keyword)
                        && !cat.contains(keyword) && !rep.contains(keyword)) {
                    continue;
                }
            }

            currentFilteredReports.add(r);
        }

        currentPage = 1;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        tableModel.setRowCount(0);

        int total = currentFilteredReports.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int startIdx = (currentPage - 1) * pageSize;
        int endIdx = Math.min(startIdx + pageSize, total);

        for (int i = startIdx; i < endIdx; i++) {
            Report r = currentFilteredReports.get(i);
            int serialNo = chronologicalRankMap.getOrDefault(r.getReportId(), i + 1);
            String catName = r.getItem() != null && r.getItem().getCategoryName() != null
                    ? r.getItem().getCategoryName() : "General";
            String reporter = r.getReporterName() != null && !r.getReporterName().isEmpty()
                    ? r.getReporterName() : "User #" + r.getUserId();

            tableModel.addRow(new Object[] {
                    serialNo,
                    r.getReportId(),
                    r.getReportType(),
                    r.getItem() != null ? r.getItem().getItemName() : "Unnamed Item",
                    catName,
                    r.getLocation(),
                    r.getReportDate() != null ? r.getReportDate().toString() : "N/A",
                    r.getStatus(),
                    reporter
            });
        }

        if (total == 0) {
            paginationLabel.setText("No matching reports found");
        } else {
            paginationLabel.setText("Showing " + (startIdx + 1) + "-" + endIdx + " of " + total + " reports  \u2022  Page " + currentPage + " of " + totalPages);
        }
        if (paginationBar != null) {
            paginationBar.revalidate();
            paginationBar.repaint();
        }
        prevPageBtn.setEnabled(currentPage > 1);
        nextPageBtn.setEnabled(currentPage < totalPages);

        updateSelectionState();
    }

    public Report getReportAtRow(int viewRow) {
        if (viewRow < 0 || viewRow >= reportsTable.getRowCount()) return null;
        int modelRow = reportsTable.convertRowIndexToModel(viewRow);
        int reportId = (int) tableModel.getValueAt(modelRow, 1);

        for (Report r : allFetchedReports) {
            if (r.getReportId() == reportId) {
                return r;
            }
        }
        return null;
    }

    private Report getSelectedReport() {
        int selectedRow = reportsTable.getSelectedRow();
        if (selectedRow == -1) return null;
        return getReportAtRow(selectedRow);
    }

    private void updateSelectionState() {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        Report report = getSelectedReport();

        if (report == null) {
            previewImageLabel.setIcon(null);
            previewImageLabel.setVisible(false);
            previewTitleLabel.setText("Select a report from the table to preview details and take action");
            previewTitleLabel.setFont(AppTheme.font(Font.BOLD, 13.5f));
            previewTitleLabel.setForeground(AppTheme.TEXT_SECONDARY);
            previewMetaLabel.setText("");

            actionMatchesBtn.setEnabled(false);
            actionPhotoBtn.setVisible(false);
            actionEditBtn.setEnabled(false);
            actionDeleteBtn.setEnabled(false);
            actionReturnBtn.setEnabled(false);

            if (isAdmin) {
                actionClaimBtn.setText("Manage Claims");
                actionClaimBtn.setEnabled(true);
            } else {
                actionClaimBtn.setText("Submit Claim");
                actionClaimBtn.setEnabled(false);
            }
            return;
        }

        // Preview Banner Population
        boolean hasImage = report.getItem() != null && report.getItem().getImagePath() != null
                && !report.getItem().getImagePath().isBlank();
        if (hasImage) {
            previewImageLabel.setIcon(ImageStorageService.getRoundedThumbnail(report.getItem().getImagePath(), 32, 6));
            previewImageLabel.setVisible(true);
        } else {
            previewImageLabel.setIcon(null);
            previewImageLabel.setVisible(false);
        }

        String itemName = report.getItem() != null ? report.getItem().getItemName() : "Item";
        previewTitleLabel.setText("Selected #" + report.getReportId() + ": " + itemName + " (" + report.getReportType()
                + " \u2022 " + report.getStatus() + ")");
        previewTitleLabel.setForeground(AppTheme.isLight() ? new Color(0x02, 0x84, 0xC7) : new Color(0x38, 0xBD, 0xF8));

        String category = report.getItem() != null && report.getItem().getCategoryName() != null
                ? report.getItem().getCategoryName() : "General";
        String loc = report.getLocation() != null ? report.getLocation() : "Unknown location";
        String rep = report.getReporterName() != null ? report.getReporterName() : "User #" + report.getUserId();
        previewMetaLabel.setText("Category: " + category + "  \u2022  Location: " + loc + "  \u2022  Reporter: " + rep);
        previewMetaLabel.setForeground(AppTheme.isLight() ? new Color(0x33, 0x41, 0x55) : new Color(0xE2, 0xE8, 0xF0));

        // Action Buttons Smart State
        actionMatchesBtn.setEnabled(true);
        actionPhotoBtn.setVisible(hasImage);

        boolean isOwner = report.getUserId() == currentUser.getUserId();
        actionEditBtn.setEnabled(isOwner || isAdmin);
        actionDeleteBtn.setEnabled(isOwner || isAdmin);

        String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
        actionReturnBtn.setEnabled("CLAIMED".equals(status) && (isOwner || isAdmin));
        actionReturnBtn.setVisible("CLAIMED".equals(status) || isAdmin);

        if (isAdmin) {
            if ("CLAIM_PENDING".equals(status)) {
                actionClaimBtn.setText("Approve Claim");
                actionClaimBtn.setEnabled(true);
            } else if ("CLAIMED".equals(status)) {
                actionClaimBtn.setText("Review Claim");
                actionClaimBtn.setEnabled(true);
            } else {
                actionClaimBtn.setText("Manage Claims");
                actionClaimBtn.setEnabled(true);
            }
        } else {
            actionClaimBtn.setText("Submit Claim");
            boolean canClaim = "FOUND".equalsIgnoreCase(report.getReportType())
                    && "OPEN".equalsIgnoreCase(status) && !isOwner;
            actionClaimBtn.setEnabled(canClaim);
        }
    }

    private void showTableRowContextMenu(Component invoker, int x, int y, Report report) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        menu.setBackground(AppTheme.BG_CARD);

        JMenuItem itemDetails = new JMenuItem("View / Edit Details");
        itemDetails.setFont(AppTheme.font(Font.BOLD, 12f));
        itemDetails.addActionListener(ev -> onEditSelected());
        menu.add(itemDetails);

        if (report.getItem() != null && report.getItem().getImagePath() != null
                && !report.getItem().getImagePath().isBlank()) {
            JMenuItem itemPhoto = new JMenuItem("View Full Image");
            itemPhoto.setFont(AppTheme.font(Font.PLAIN, 12f));
            itemPhoto.addActionListener(ev -> onPhotoSelected());
            menu.add(itemPhoto);
        }

        JMenuItem itemReporter = new JMenuItem("Contact Reporter (" + (report.getReporterName() != null ? report.getReporterName() : "User") + ")");
        itemReporter.setFont(AppTheme.font(Font.PLAIN, 12f));
        itemReporter.addActionListener(ev -> {
            new ReporterContactDialog(this, report.getReporterName(), report.getReporterEmail(),
                    report.getReporterProfileImage(), "USER").setVisible(true);
        });
        menu.add(itemReporter);

        JMenuItem itemMatches = new JMenuItem("Find Smart Matches");
        itemMatches.setFont(AppTheme.font(Font.PLAIN, 12f));
        itemMatches.addActionListener(ev -> onSmartMatchesSelected());
        menu.add(itemMatches);

        menu.addSeparator();

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = report.getUserId() == currentUser.getUserId();

        if ("FOUND".equalsIgnoreCase(report.getReportType()) && !isOwner && "OPEN".equalsIgnoreCase(report.getStatus())) {
            JMenuItem itemClaim = new JMenuItem("Submit Claim");
            itemClaim.setFont(AppTheme.font(Font.PLAIN, 12f));
            itemClaim.addActionListener(ev -> onClaimSelected());
            menu.add(itemClaim);
        }

        if ("CLAIMED".equalsIgnoreCase(report.getStatus()) && (isOwner || isAdmin)) {
            JMenuItem itemReturn = new JMenuItem("Mark as Returned");
            itemReturn.setFont(AppTheme.font(Font.PLAIN, 12f));
            itemReturn.addActionListener(ev -> onReturnSelected());
            menu.add(itemReturn);
        }

        if (isOwner || isAdmin) {
            JMenuItem itemDelete = new JMenuItem("Delete Report");
            itemDelete.setForeground(new Color(0xEF, 0x44, 0x44));
            itemDelete.setFont(AppTheme.font(Font.PLAIN, 12f));
            itemDelete.addActionListener(ev -> onDeleteSelected());
            menu.add(itemDelete);
        }

        menu.show(invoker, x, y);
    }

    private void onRowDoubleClicked() {
        Report report = getSelectedReport();
        if (report == null) return;

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = report.getUserId() == currentUser.getUserId();

        if (isOwner || isAdmin) {
            onEditSelected();
        } else if ("FOUND".equalsIgnoreCase(report.getReportType()) && "OPEN".equalsIgnoreCase(report.getStatus())) {
            onClaimSelected();
        } else if (report.getItem() != null && report.getItem().getImagePath() != null) {
            onPhotoSelected();
        }
    }

    private void onPhotoSelected() {
        Report report = getSelectedReport();
        if (report == null) {
            DialogUtils.showInfo(this, "Select Report", "Please select a report to view its photo.");
            return;
        }
        String path = report.getItem() != null ? report.getItem().getImagePath() : null;
        if (path == null || path.isBlank()) {
            DialogUtils.showInfo(this, "No Photo", "This report does not have an attached photo.");
            return;
        }
        String itemName = report.getItem() != null ? report.getItem().getItemName() : "Item";
        new ImageViewerDialog(this, itemName, path).setVisible(true);
    }

    private void onSmartMatchesSelected() {
        Report report = getSelectedReport();
        if (report == null) {
            DialogUtils.showInfo(this, "Select Report", "Please select a report to search for smart matches.");
            return;
        }

        List<MatchResult> matches = SmartMatchingService.findMatches(report, allFetchedReports, 30);
        SmartMatchesDialog dialog = new SmartMatchesDialog(this, report, currentUser, matches, this::loadReportsAsync);
        dialog.setVisible(true);
    }

    private void onEditSelected() {
        Report report = getSelectedReport();
        if (report == null) {
            DialogUtils.showInfo(this, "Select Report", "Please select a report to view or edit.");
            return;
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        if (!isAdmin && report.getUserId() != currentUser.getUserId()) {
            DialogUtils.showWarning(this, "Unauthorized",
                    "You do not have permission to edit this report.\nOnly the reporter or an administrator can edit it.");
            return;
        }

        EditReportFrame editFrame = new EditReportFrame(report, currentUser);
        editFrame.setVisible(true);
        editFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                loadReportsAsync();
                refreshNotificationCount();
            }
        });
    }

    private void onDeleteSelected() {
        Report report = getSelectedReport();
        if (report == null) {
            DialogUtils.showInfo(this, "Select Report", "Please select a report to delete.");
            return;
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        if (!isAdmin && report.getUserId() != currentUser.getUserId()) {
            DialogUtils.showWarning(this, "Unauthorized",
                    "You are not authorized to delete this report.\nOnly the original reporter or an administrator can delete it.");
            return;
        }

        String itemName = report.getItem() != null ? report.getItem().getItemName() : "this item";
        boolean confirmed = DialogUtils.showConfirm(
                this,
                "Delete Report",
                "Are you sure you want to permanently delete the report for \"" + itemName
                        + "\"? Associated claims and records will also be removed.");

        if (!confirmed) return;

        setLoading(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return reportDAO.deleteReport(report.getReportId(), currentUser.getUserId(), isAdmin);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        DialogUtils.showSuccess(DashboardFrame.this, "Deleted", "Report was successfully deleted.");
                        loadReportsAsync();
                        refreshNotificationCount();
                    } else {
                        DialogUtils.showError(DashboardFrame.this, "Delete Failed", "The report could not be found.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(DashboardFrame.this, "Error", "Could not delete report: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onClaimSelected() {
        Report report = getSelectedReport();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (isAdmin) {
            if (report == null) {
                openAdminClaimsConsole();
                return;
            }
            String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
            if ("CLAIM_PENDING".equals(status) || "CLAIMED".equals(status)) {
                openAdminClaimReviewForReport(report);
            } else {
                openAdminClaimsConsole();
            }
            return;
        }

        if (report == null) {
            DialogUtils.showInfo(this, "Select Report", "Please select a found item from the table to submit a claim.");
            return;
        }

        if (!"FOUND".equalsIgnoreCase(report.getReportType())) {
            DialogUtils.showWarning(this, "Cannot Claim Lost Report",
                    "Claims can only be filed for FOUND items.\nThis report is currently marked as LOST.");
            return;
        }

        if (report.getUserId() == currentUser.getUserId()) {
            DialogUtils.showWarning(this, "Own Report",
                    "You reported this item as found.\nYou cannot claim your own found submission.");
            return;
        }

        String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
        if ("CLAIM_PENDING".equals(status)) {
            DialogUtils.showInfo(this, "Claim Pending",
                    "A claim for this item has already been submitted and is currently awaiting administrator review.");
            return;
        }

        if ("CLAIMED".equals(status)) {
            DialogUtils.showInfo(this, "Already Claimed",
                    "This item has already been successfully claimed by its verified owner.");
            return;
        }

        if ("RETURNED".equals(status)) {
            DialogUtils.showInfo(this, "Already Returned",
                    "This item has already been marked as returned to its owner.");
            return;
        }

        ClaimSubmissionFrame claimFrame = new ClaimSubmissionFrame(this, report, currentUser);
        claimFrame.setVisible(true);
        if (claimFrame.isClaimSubmitted()) {
            loadReportsAsync();
            refreshNotificationCount();
        }
    }

    private void openAdminClaimsConsole() {
        AdminClaimsFrame frame = new AdminClaimsFrame(currentUser);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                loadReportsAsync();
                refreshNotificationCount();
            }
        });
    }

    private void openAdminClaimReviewForReport(Report report) {
        setLoading(true);
        new SwingWorker<List<Claim>, Void>() {
            @Override
            protected List<Claim> doInBackground() throws Exception {
                return claimDAO.getClaimsForReport(report.getReportId());
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    List<Claim> claims = get();
                    if (claims.isEmpty()) {
                        DialogUtils.showInfo(DashboardFrame.this, "No Claims Found",
                                "No claims have been submitted for this report.");
                        return;
                    }

                    Claim target = null;
                    for (Claim c : claims) {
                        if ("PENDING".equalsIgnoreCase(c.getClaimStatus())) {
                            target = c;
                            break;
                        }
                    }
                    if (target == null) {
                        target = claims.get(0);
                    }

                    showClaimApprovalDialog(target, report);
                } catch (Exception ex) {
                    DialogUtils.showError(DashboardFrame.this, "Error", "Could not load claims: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showClaimApprovalDialog(Claim claim, Report report) {
        JDialog dialog = new JDialog(this, "FINDIT - Review & Decision", true);
        dialog.setSize(560, 480);
        dialog.setMinimumSize(new Dimension(500, 420));
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

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
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel titleLabel = new JLabel("Review Claim #" + claim.getClaimId());
        titleLabel.setFont(AppTheme.font(Font.BOLD, 18f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String itemName = report.getItem() != null ? report.getItem().getItemName() : "Item #" + report.getReportId();
        String claimantName = claim.getClaimantName() != null ? claim.getClaimantName()
                : "User #" + claim.getClaimantId();
        JLabel subLabel = new JLabel(
                "Item: " + itemName + "  •  Claimant: " + claimantName + "  •  Status: " + claim.getClaimStatus());
        subLabel.setFont(AppTheme.font(Font.PLAIN, 12.5f));
        subLabel.setForeground(AppTheme.ACCENT_CYAN);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subLabel);
        card.add(Box.createVerticalStrut(14));

        JLabel stmtHeader = new JLabel("Claimant Verification Statement / Proof of Ownership:");
        stmtHeader.setFont(AppTheme.font(Font.BOLD, 12.5f));
        stmtHeader.setForeground(new Color(0xCB, 0xD5, 0xE1));
        stmtHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(stmtHeader);
        card.add(Box.createVerticalStrut(6));

        JTextArea stmtArea = new JTextArea(claim.getClaimDetails());
        stmtArea.setLineWrap(true);
        stmtArea.setWrapStyleWord(true);
        stmtArea.setEditable(false);
        stmtArea.setFont(AppTheme.font(Font.PLAIN, 13f));
        stmtArea.setBackground(AppTheme.FIELD_BG);
        stmtArea.setForeground(Color.WHITE);
        stmtArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane stmtScroll = new JScrollPane(stmtArea);
        stmtScroll.setBorder(BorderFactory.createLineBorder(AppTheme.FIELD_BORDER, 1));
        stmtScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(stmtScroll);

        root.add(card, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        RoundedButton cancelBtn = new RoundedButton("Cancel", RoundedButton.Style.SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(95, 38));
        cancelBtn.addActionListener(e -> dialog.dispose());
        btnRow.add(cancelBtn);

        boolean isPending = "PENDING".equalsIgnoreCase(claim.getClaimStatus());
        if (isPending) {
            RoundedButton rejectBtn = new RoundedButton("Reject Claim", RoundedButton.Style.DANGER);
            rejectBtn.setPreferredSize(new Dimension(125, 38));
            rejectBtn.addActionListener(e -> {
                boolean confirm = DialogUtils.showConfirm(dialog, "Reject Claim",
                        "Are you sure you want to reject this claim?\nThe report will revert to OPEN status for other users to claim.");
                if (confirm) {
                    processClaimDecision(dialog, claim, report, "REJECTED");
                }
            });

            RoundedButton approveBtn = new RoundedButton("Approve Claim", RoundedButton.Style.PRIMARY);
            approveBtn.setPreferredSize(new Dimension(135, 38));
            approveBtn.addActionListener(e -> {
                boolean confirm = DialogUtils.showConfirm(dialog, "Approve Claim",
                        "Are you sure you want to approve this claim?\nThis transfers the item to " + claimantName
                                + " and sets status to CLAIMED.");
                if (confirm) {
                    processClaimDecision(dialog, claim, report, "APPROVED");
                }
            });

            btnRow.add(rejectBtn);
            btnRow.add(approveBtn);
        }

        root.add(btnRow, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private void processClaimDecision(JDialog dialog, Claim claim, Report report, String decision) {
        setLoading(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return claimDAO.reviewClaim(claim.getClaimId(), currentUser.getUserId(), decision);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    boolean ok = get();
                    if (ok) {
                        dialog.dispose();
                        String actionText = "APPROVED".equals(decision) ? "approved" : "rejected";
                        String itemName = report.getItem() != null ? report.getItem().getItemName() : "Item #" + report.getReportId();

                        // Create notification for claimant
                        try {
                            if ("APPROVED".equals(decision)) {
                                notificationDAO.createNotification(
                                        claim.getClaimantId(),
                                        "Claim Approved!",
                                        "Your claim for '" + itemName + "' has been approved by the administrator.",
                                        report.getReportId()
                                );
                                notificationDAO.createNotification(
                                        report.getUserId(),
                                        "Item Claim Verified",
                                        "A claim for your reported item '" + itemName + "' has been approved.",
                                        report.getReportId()
                                );
                            } else {
                                notificationDAO.createNotification(
                                        claim.getClaimantId(),
                                        "Claim Rejected",
                                        "Your claim for '" + itemName + "' was not approved. The report is open for claims.",
                                        report.getReportId()
                                );
                            }
                        } catch (Exception ignored) {}

                        DialogUtils.showSuccess(DashboardFrame.this, "Decision Recorded",
                                "The claim has been " + actionText + " successfully.");
                        loadReportsAsync();
                        refreshNotificationCount();
                    } else {
                        DialogUtils.showError(dialog, "Review Failed", "Unable to record claim decision.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(dialog, "Error", "Operation failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onReturnSelected() {
        Report report = getSelectedReport();
        if (report == null) {
            DialogUtils.showInfo(this, "Select Report", "Please select an item from the table first.");
            return;
        }

        String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
        if (!"CLAIMED".equals(status)) {
            DialogUtils.showWarning(this, "Invalid Status",
                    "Only items with status 'CLAIMED' can be marked as 'RETURNED'.\n" +
                            "Current status is: " + status);
            return;
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = report.getUserId() == currentUser.getUserId();
        if (!isOwner && !isAdmin) {
            DialogUtils.showWarning(this, "Unauthorized",
                    "Only the finder of this item or an administrator can mark it as returned.");
            return;
        }

        String itemName = (report.getItem() != null ? report.getItem().getItemName() : "Report #" + report.getReportId());
        boolean confirmed = DialogUtils.showConfirm(
                this,
                "Mark as Returned",
                "Mark report #" + report.getReportId() + " ("
                        + itemName + ") as officially returned to its owner?");

        if (!confirmed) return;

        setLoading(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return reportDAO.markReportAsReturned(report.getReportId(), currentUser.getUserId(), isAdmin);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        try {
                            notificationDAO.createNotification(
                                    report.getUserId(),
                                    "Item Returned Successfully",
                                    "Your reported item '" + itemName + "' has been marked as returned.",
                                    report.getReportId()
                            );
                        } catch (Exception ignored) {}

                        DialogUtils.showSuccess(DashboardFrame.this, "Success",
                                "Report marked as returned successfully.");
                        loadReportsAsync();
                        refreshNotificationCount();
                    } else {
                        DialogUtils.showError(DashboardFrame.this, "Failed", "Report could not be updated.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(DashboardFrame.this, "Error", ex.getMessage());
                }
            }
        }.execute();
    }

    private void logout() {
        boolean confirmed = DialogUtils.showConfirm(this, "Sign Out", "Are you sure you want to sign out of FINDIT?");
        if (confirmed) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
