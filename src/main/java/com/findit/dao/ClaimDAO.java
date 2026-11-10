package com.findit.dao;

import com.findit.database.DBConnection;
import com.findit.model.Claim;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClaimDAO {

    public boolean submitClaim(Claim claim) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String lockSql = """
                    SELECT status, user_id, report_type
                    FROM reports
                    WHERE report_id = ?
                    FOR UPDATE
                    """;

                String oldStatus;

                try (PreparedStatement ps = con.prepareStatement(lockSql)) {
                    ps.setInt(1, claim.getReportId());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Report not found.");
                        }
                        String status = rs.getString("status");
                        int reportOwnerId = rs.getInt("user_id");
                        String reportType = rs.getString("report_type");

                        if (!"FOUND".equals(reportType)) {
                            throw new SQLException(
                                    "Claims can only be submitted for FOUND reports.");
                        }

                        if (reportOwnerId == claim.getClaimantId()) {
                            throw new SQLException(
                                    "You cannot claim your own found report.");
                        }

                        if (!"OPEN".equals(status)) {
                            throw new SQLException(
                                    "This report is no longer available for claiming.");
                        }

                        oldStatus = rs.getString("status");
                    }
                }

                if (!"OPEN".equals(oldStatus)) {
                    throw new SQLException(
                            "This report is not available for claims."
                    );
                }

                String insertSql = """
                    INSERT INTO claims
                    (report_id, claimant_id, claim_details, claim_status)
                    VALUES (?, ?, ?, 'PENDING')
                    """;

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, claim.getReportId());
                    ps.setInt(2, claim.getClaimantId());
                    ps.setString(3, claim.getClaimDetails());
                    ps.executeUpdate();
                }

                String updateSql = """
                    UPDATE reports
                    SET status = 'CLAIM_PENDING'
                    WHERE report_id = ?
                    """;

                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, claim.getReportId());
                    ps.executeUpdate();
                }

                String historySql = """
                    INSERT INTO status_history
                    (report_id, changed_by, old_status, new_status)
                    VALUES (?, ?, ?, 'CLAIM_PENDING')
                    """;

                try (PreparedStatement ps = con.prepareStatement(historySql)) {
                    ps.setInt(1, claim.getReportId());
                    ps.setInt(2, claim.getClaimantId());
                    ps.setString(3, oldStatus);
                    ps.executeUpdate();
                }

                con.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Claim> getClaimsForReport(int reportId)
            throws SQLException {

        List<Claim> claims = new ArrayList<>();

        String sql = """
                SELECT c.claim_id, c.report_id, c.claimant_id,
                       u.name AS claimant_name, i.item_name,
                       c.claim_details, c.claim_status, c.claim_date
                FROM claims c
                LEFT JOIN users u ON c.claimant_id = u.user_id
                LEFT JOIN reports r ON c.report_id = r.report_id
                LEFT JOIN items i ON r.item_id = i.item_id
                WHERE c.report_id = ?
                ORDER BY c.claim_date DESC
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reportId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("claim_date");

                    Claim claim = new Claim(
                            rs.getInt("claim_id"),
                            rs.getInt("report_id"),
                            rs.getInt("claimant_id"),
                            rs.getString("claimant_name"),
                            rs.getString("item_name"),
                            rs.getString("claim_details"),
                            rs.getString("claim_status"),
                            timestamp != null ? timestamp.toLocalDateTime() : null
                    );

                    claims.add(claim);
                }
            }
        }

        return claims;
    }

    public boolean reviewClaim(int claimId, int adminId,
                               String decision) throws SQLException {
        String roleSql = "SELECT role FROM users WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(roleSql)) {

            ps.setInt(1, adminId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()
                        || !"ADMIN".equalsIgnoreCase(rs.getString("role"))) {
                    throw new SQLException(
                            "Unauthorized: Only administrators can review claims."
                    );
                }
            }
        }
        if (!"APPROVED".equals(decision)
                && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException(
                    "Decision must be APPROVED or REJECTED.");
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int reportId;
                String currentClaimStatus;

                String lockClaimSql = """
                        SELECT report_id, claim_status
                        FROM claims
                        WHERE claim_id = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(lockClaimSql)) {
                    ps.setInt(1, claimId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Claim not found.");
                        }

                        reportId = rs.getInt("report_id");
                        currentClaimStatus = rs.getString("claim_status");
                    }
                }

                if (!"PENDING".equals(currentClaimStatus)) {
                    throw new SQLException(
                            "This claim has already been reviewed.");
                }

                String oldReportStatus;

                String lockReportSql = """
                    SELECT status, user_id, report_type
                    FROM reports
                    WHERE report_id = ?
                    FOR UPDATE
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(lockReportSql)) {
                    ps.setInt(1, reportId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Report not found.");
                        }

                        oldReportStatus = rs.getString("status");
                    }
                }

                if ("APPROVED".equals(decision)
                        && !"OPEN".equals(oldReportStatus)
                        && !"CLAIM_PENDING".equals(oldReportStatus)) {
                    throw new SQLException(
                            "This report is no longer available.");
                }

                String updateClaimSql = """
                        UPDATE claims
                        SET claim_status = ?
                        WHERE claim_id = ?
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(updateClaimSql)) {
                    ps.setString(1, decision);
                    ps.setInt(2, claimId);
                    ps.executeUpdate();
                }

                String newReportStatus = oldReportStatus;

                if ("APPROVED".equals(decision)) {
                    newReportStatus = "CLAIMED";

                    String rejectOthersSql = """
                            UPDATE claims
                            SET claim_status = 'REJECTED'
                            WHERE report_id = ?
                              AND claim_id <> ?
                              AND claim_status = 'PENDING'
                            """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(rejectOthersSql)) {
                        ps.setInt(1, reportId);
                        ps.setInt(2, claimId);
                        ps.executeUpdate();
                    }
                } else if ("CLAIM_PENDING".equals(oldReportStatus)) {
                    // Check if other pending claims exist for this report before reverting to OPEN
                    String countOtherPendingSql = """
                            SELECT COUNT(*) FROM claims
                            WHERE report_id = ? AND claim_id <> ? AND claim_status = 'PENDING'
                            """;
                    int otherPendingCount = 0;
                    try (PreparedStatement ps = con.prepareStatement(countOtherPendingSql)) {
                        ps.setInt(1, reportId);
                        ps.setInt(2, claimId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                otherPendingCount = rs.getInt(1);
                            }
                        }
                    }

                    if (otherPendingCount == 0) {
                        newReportStatus = "OPEN";
                    } else {
                        newReportStatus = "CLAIM_PENDING";
                    }
                }

                if (!newReportStatus.equals(oldReportStatus)) {
                    String updateReportSql = """
                            UPDATE reports
                            SET status = ?
                            WHERE report_id = ?
                            """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(updateReportSql)) {
                        ps.setString(1, newReportStatus);
                        ps.setInt(2, reportId);
                        ps.executeUpdate();
                    }

                    String historySql = """
                            INSERT INTO status_history
                            (report_id, changed_by, old_status, new_status)
                            VALUES (?, ?, ?, ?)
                            """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(historySql)) {
                        ps.setInt(1, reportId);
                        ps.setInt(2, adminId);
                        ps.setString(3, oldReportStatus);
                        ps.setString(4, newReportStatus);
                        ps.executeUpdate();
                    }
                }

                con.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<Claim> getAllClaims() throws SQLException {
        List<Claim> claims = new ArrayList<>();

        String sql = """
            SELECT c.claim_id, c.report_id, c.claimant_id,
                   u.name AS claimant_name, i.item_name,
                   c.claim_details, c.claim_status, c.claim_date
            FROM claims c
            LEFT JOIN users u ON c.claimant_id = u.user_id
            LEFT JOIN reports r ON c.report_id = r.report_id
            LEFT JOIN items i ON r.item_id = i.item_id
            ORDER BY c.claim_date DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("claim_date");

                claims.add(new Claim(
                        rs.getInt("claim_id"),
                        rs.getInt("report_id"),
                        rs.getInt("claimant_id"),
                        rs.getString("claimant_name"),
                        rs.getString("item_name"),
                        rs.getString("claim_details"),
                        rs.getString("claim_status"),
                        timestamp != null ? timestamp.toLocalDateTime() : null
                ));
            }
        }

        return claims;
    }
}
