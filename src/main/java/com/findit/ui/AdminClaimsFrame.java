package com.findit.ui;

import com.findit.dao.ClaimDAO;
import com.findit.model.Claim;
import com.findit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Modern SaaS administrative claims review console for FindIt.
 */
public class AdminClaimsFrame extends JFrame {

    private final User admin;
    private final ClaimDAO claimDAO = new ClaimDAO();

    private final DefaultTableModel tableModel;
    private final ModernTable claimsTable;
    private final JTextArea detailsArea = new JTextArea();

    private final RoundedButton refreshBtn = new RoundedButton("Refresh", RoundedButton.Style.SECONDARY);
    private final RoundedButton approveBtn = new RoundedButton("Approve Claim", RoundedButton.Style.SUCCESS);
    private final RoundedButton rejectBtn = new RoundedButton("Reject Claim", RoundedButton.Style.DANGER);

    private List<Claim> allClaims = new ArrayList<>();
    private boolean busy = false;

    public AdminClaimsFrame(User admin) {
        this.admin = admin;

        setTitle("FINDIT - Admin Claim Review Console");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1080, 680);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        // Security check
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            DialogUtils.showError(this, "Unauthorized", "Access denied: Only administrators can review claims.");
            dispose();
        }

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(AppTheme.BG_MAIN);
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        // Top Header
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setOpaque(false);

        JPanel titleStack = new JPanel(new GridLayout(2, 1, 0, 4));
        titleStack.setOpaque(false);

        JLabel title = new JLabel("Claim Verification Console");
        title.setFont(AppTheme.font(Font.BOLD, 22f));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Inspect claimant statements, verify proof of ownership, and authorize transfers");
        subtitle.setFont(AppTheme.font(Font.PLAIN, 13.5f));
        subtitle.setForeground(AppTheme.TEXT_SECONDARY);

        titleStack.add(title);
        titleStack.add(subtitle);

        refreshBtn.setPreferredSize(new Dimension(100, 40));
        topBar.add(titleStack, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);

        // Claims Table Setup
        String[] cols = {"Claim ID", "Report ID", "Item Name", "Claimant", "Status", "Date Submitted"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        claimsTable = new ModernTable(tableModel);
        claimsTable.setAutoCreateRowSorter(true);
        claimsTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer(false)); // Status Badge

        claimsTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        claimsTable.getColumnModel().getColumn(0).setMaxWidth(90);
        claimsTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        claimsTable.getColumnModel().getColumn(1).setMaxWidth(90);
        claimsTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        claimsTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        claimsTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        claimsTable.getColumnModel().getColumn(5).setPreferredWidth(140);

        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                AppTheme.applyQualityRendering(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(ModernTable.TABLE_BG);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(new Color(0xCB, 0xD5, 0xE1));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(new EmptyBorder(1, 1, 1, 1));

        JScrollPane tableScroll = new JScrollPane(claimsTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(ModernTable.TABLE_BG);
        tableScroll.getViewport().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                claimsTable.clearSelection();
            }
        });
        tableCard.add(tableScroll, BorderLayout.CENTER);

        // Bottom Detail Inspector Panel
        JPanel inspectorCard = new JPanel(new BorderLayout(0, 10)) {
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
        inspectorCard.setOpaque(false);
        inspectorCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        inspectorCard.setPreferredSize(new Dimension(0, 165));

        JLabel inspectorTitle = new JLabel("Claimant Evidence & Statement");
        inspectorTitle.setFont(AppTheme.font(Font.BOLD, 13f));
        inspectorTitle.setForeground(AppTheme.TEXT_PRIMARY);

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(AppTheme.font(Font.PLAIN, 13f));
        detailsArea.setBackground(AppTheme.FIELD_BG);
        detailsArea.setForeground(AppTheme.TEXT_PRIMARY);
        detailsArea.setCaretColor(AppTheme.ACCENT_CYAN);
        detailsArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));

        // Action Buttons Row
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        approveBtn.setPreferredSize(new Dimension(135, 38));
        rejectBtn.setPreferredSize(new Dimension(125, 38));

        approveBtn.setEnabled(false);
        rejectBtn.setEnabled(false);

        actionRow.add(rejectBtn);
        actionRow.add(approveBtn);

        inspectorCard.add(inspectorTitle, BorderLayout.NORTH);
        inspectorCard.add(detailsScroll, BorderLayout.CENTER);
        inspectorCard.add(actionRow, BorderLayout.SOUTH);

        root.add(topBar, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);
        root.add(inspectorCard, BorderLayout.SOUTH);

        setContentPane(root);

        // Selection listener to show details and enable decisions
        claimsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onClaimSelected();
            }
        });

        // Actions
        refreshBtn.addActionListener(e -> loadClaimsAsync());
        approveBtn.addActionListener(e -> reviewSelected("APPROVED"));
        rejectBtn.addActionListener(e -> reviewSelected("REJECTED"));

        // Load claims
        loadClaimsAsync();
    }

    private void onClaimSelected() {
        int selectedRow = claimsTable.getSelectedRow();
        if (selectedRow == -1) {
            detailsArea.setText("");
            approveBtn.setEnabled(false);
            rejectBtn.setEnabled(false);
            return;
        }

        int modelRow = claimsTable.convertRowIndexToModel(selectedRow);
        int claimId = (int) tableModel.getValueAt(modelRow, 0);

        Claim selected = null;
        for (Claim c : allClaims) {
            if (c.getClaimId() == claimId) {
                selected = c;
                break;
            }
        }

        if (selected != null) {
            detailsArea.setText(selected.getClaimDetails());
            boolean isPending = "PENDING".equalsIgnoreCase(selected.getClaimStatus());
            approveBtn.setEnabled(isPending);
            rejectBtn.setEnabled(isPending);
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        refreshBtn.setEnabled(!busy);
        approveBtn.setEnabled(!busy);
        rejectBtn.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadClaimsAsync() {
        if (busy) return;
        setBusy(true);

        new SwingWorker<List<Claim>, Void>() {
            @Override
            protected List<Claim> doInBackground() throws Exception {
                return claimDAO.getAllClaims();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    allClaims = get();
                    tableModel.setRowCount(0);
                    for (Claim c : allClaims) {
                        String claimant = c.getClaimantName() != null ? c.getClaimantName() : "User #" + c.getClaimantId();
                        String item = c.getItemName() != null ? c.getItemName() : "Report #" + c.getReportId();
                        tableModel.addRow(new Object[]{
                                c.getClaimId(),
                                c.getReportId(),
                                item,
                                claimant,
                                c.getClaimStatus(),
                                c.getClaimDate() != null ? c.getClaimDate().toLocalDate().toString() : "N/A"
                        });
                    }
                    onClaimSelected();
                } catch (Exception ex) {
                    DialogUtils.showError(AdminClaimsFrame.this, "Error", "Could not load claims: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void reviewSelected(String decision) {
        int selectedRow = claimsTable.getSelectedRow();
        if (selectedRow == -1) {
            DialogUtils.showWarning(this, "Select Claim", "Please select a claim from the table first.");
            return;
        }

        int modelRow = claimsTable.convertRowIndexToModel(selectedRow);
        int claimId = (int) tableModel.getValueAt(modelRow, 0);
        String currentStatus = String.valueOf(tableModel.getValueAt(modelRow, 4));

        if (!"PENDING".equalsIgnoreCase(currentStatus)) {
            DialogUtils.showWarning(this, "Already Reviewed", "This claim has already been " + currentStatus.toLowerCase() + ".");
            return;
        }

        boolean isApprove = "APPROVED".equalsIgnoreCase(decision);
        String promptMsg = isApprove
                ? "Are you sure you want to APPROVE claim #" + claimId + "?\n\nThis will mark the item as CLAIMED and automatically reject any other pending claims for this report."
                : "Are you sure you want to REJECT claim #" + claimId + "?";

        boolean confirmed = DialogUtils.showConfirm(this, "Confirm Decision", promptMsg);
        if (!confirmed) return;

        setBusy(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return claimDAO.reviewClaim(claimId, admin.getUserId(), decision);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        DialogUtils.showSuccess(
                                AdminClaimsFrame.this,
                                "Claim " + (isApprove ? "Approved" : "Rejected"),
                                "Claim #" + claimId + " has been successfully " + decision.toLowerCase() + "."
                        );
                        loadClaimsAsync();
                    } else {
                        DialogUtils.showError(AdminClaimsFrame.this, "Failed", "Could not complete claim review.");
                    }
                } catch (Exception ex) {
                    DialogUtils.showError(AdminClaimsFrame.this, "Review Error", ex.getMessage());
                }
            }
        }.execute();
    }
}
