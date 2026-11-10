package com.findit.dao;

import com.findit.database.DBConnection;
import com.findit.model.Item;
import com.findit.model.LostReport;
import com.findit.model.FoundReport;
import com.findit.model.Report;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public boolean createReport(Report report) throws SQLException {
        String itemSql = """
                INSERT INTO items (item_name, category_id, description, image_path)
                VALUES (?, ?, ?, ?)
                """;

        String reportSql = """
                INSERT INTO reports
                (user_id, item_id, report_type, location, report_date, status)
                VALUES (?, ?, ?, ?, ?, 'OPEN')
                """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int itemId;

                try (PreparedStatement ps = con.prepareStatement(
                        itemSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, report.getItem().getItemName());
                    ps.setInt(2, report.getItem().getCategoryId());
                    ps.setString(3, report.getItem().getDescription());
                    ps.setString(4, report.getItem().getImagePath());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Item ID was not generated.");
                        }
                        itemId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(reportSql)) {
                    ps.setInt(1, report.getUserId());
                    ps.setInt(2, itemId);
                    ps.setString(3, report.getReportType());
                    ps.setString(4, report.getLocation());
                    ps.setDate(5, Date.valueOf(report.getReportDate()));
                    ps.executeUpdate();
                }

                con.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<Report> getAllReports() throws SQLException {
        List<Report> reports = new ArrayList<>();

        String sql = """
                SELECT r.report_id, r.user_id, u.name AS reporter_name, u.email AS reporter_email,
                       u.profile_image AS reporter_profile_image, r.report_type,
                       r.location, r.report_date, r.status,
                       i.item_id, i.item_name, i.category_id, c.category_name,
                       i.description, i.image_path
                FROM reports r
                JOIN items i ON r.item_id = i.item_id
                LEFT JOIN categories c ON i.category_id = c.category_id
                LEFT JOIN users u ON r.user_id = u.user_id
                ORDER BY r.created_at DESC
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        }

        return reports;
    }

    public Report getReportById(int reportId) throws SQLException {
        String sql = """
                SELECT r.report_id, r.user_id, u.name AS reporter_name, u.email AS reporter_email,
                       u.profile_image AS reporter_profile_image, r.report_type,
                       r.location, r.report_date, r.status,
                       i.item_id, i.item_name, i.category_id, c.category_name,
                       i.description, i.image_path
                FROM reports r
                JOIN items i ON r.item_id = i.item_id
                LEFT JOIN categories c ON i.category_id = c.category_id
                LEFT JOIN users u ON r.user_id = u.user_id
                WHERE r.report_id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reportId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReport(rs);
                }
            }
        }

        return null;
    }

    public List<Report> searchReports(String keyword) throws SQLException {
        List<Report> reports = new ArrayList<>();

        String sql = """
                SELECT r.report_id, r.user_id, u.name AS reporter_name, u.email AS reporter_email,
                       u.profile_image AS reporter_profile_image, r.report_type,
                       r.location, r.report_date, r.status,
                       i.item_id, i.item_name, i.category_id, c.category_name,
                       i.description, i.image_path
                FROM reports r
                JOIN items i ON r.item_id = i.item_id
                LEFT JOIN categories c ON i.category_id = c.category_id
                LEFT JOIN users u ON r.user_id = u.user_id
                WHERE i.item_name LIKE ?
                   OR i.description LIKE ?
                   OR r.location LIKE ?
                   OR c.category_name LIKE ?
                ORDER BY r.created_at DESC
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
        }

        return reports;
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Item item = new Item(
                rs.getInt("item_id"),
                rs.getString("item_name"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getString("description"),
                rs.getString("image_path")
        );

        Date sqlDate = rs.getDate("report_date");
        String type = rs.getString("report_type");
        Report report;

        if ("LOST".equalsIgnoreCase(type)) {
            report = new LostReport();
        } else {
            report = new FoundReport();
        }

        report.setReportId(rs.getInt("report_id"));
        report.setUserId(rs.getInt("user_id"));
        report.setReporterName(rs.getString("reporter_name"));
        try {
            report.setReporterEmail(rs.getString("reporter_email"));
        } catch (SQLException ignored) {}
        try {
            report.setReporterProfileImage(rs.getString("reporter_profile_image"));
        } catch (SQLException ignored) {}
        report.setItem(item);
        report.setLocation(rs.getString("location"));
        report.setReportDate(sqlDate != null ? sqlDate.toLocalDate() : null);
        report.setStatus(rs.getString("status"));

        return report;
    }

    public boolean updateReportStatus(int reportId, String status)
            throws SQLException {

        String sql = """
                UPDATE reports
                SET status = ?
                WHERE report_id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reportId);

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteReport(int reportId, int requestingUserId,
                                boolean isAdmin) throws SQLException {

        String findSql = """
            SELECT item_id, user_id
            FROM reports
            WHERE report_id = ?
            FOR UPDATE
            """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int itemId;
                int ownerId;

                try (PreparedStatement ps = con.prepareStatement(findSql)) {
                    ps.setInt(1, reportId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }

                        itemId = rs.getInt("item_id");
                        ownerId = rs.getInt("user_id");
                    }
                }

                if (!isAdmin && ownerId != requestingUserId) {
                    con.rollback();
                    throw new SQLException(
                            "You are not authorized to delete this report."
                    );
                }

                String deleteClaims =
                        "DELETE FROM claims WHERE report_id = ?";
                try (PreparedStatement ps =
                             con.prepareStatement(deleteClaims)) {
                    ps.setInt(1, reportId);
                    ps.executeUpdate();
                }

                String deleteHistory =
                        "DELETE FROM status_history WHERE report_id = ?";
                try (PreparedStatement ps =
                             con.prepareStatement(deleteHistory)) {
                    ps.setInt(1, reportId);
                    ps.executeUpdate();
                }

                String deleteReport =
                        "DELETE FROM reports WHERE report_id = ?";
                try (PreparedStatement ps =
                             con.prepareStatement(deleteReport)) {
                    ps.setInt(1, reportId);

                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }

                String deleteUnusedItem = """
                    DELETE FROM items
                    WHERE item_id = ?
                    AND NOT EXISTS (
                        SELECT 1 FROM reports WHERE item_id = ?
                    )
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(deleteUnusedItem)) {
                    ps.setInt(1, itemId);
                    ps.setInt(2, itemId);
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

    public boolean updateReport(Report report, int requestingUserId,
                                boolean isAdmin) throws SQLException {

        String findSql = """
        SELECT item_id, user_id
        FROM reports
        WHERE report_id = ?
        FOR UPDATE
        """;

        String updateItemSql = """
        UPDATE items
        SET item_name = ?, category_id = ?, description = ?, image_path = ?
        WHERE item_id = ?
        """;

        String updateReportSql = """
        UPDATE reports
        SET location = ?, report_date = ?
        WHERE report_id = ?
        """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int itemId;
                int ownerId;

                try (PreparedStatement ps = con.prepareStatement(findSql)) {
                    ps.setInt(1, report.getReportId());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }

                        itemId = rs.getInt("item_id");
                        ownerId = rs.getInt("user_id");
                    }
                }

                if (!isAdmin && ownerId != requestingUserId) {
                    throw new SQLException(
                            "You are not authorized to edit this report."
                    );
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateItemSql)) {
                    ps.setString(1, report.getItem().getItemName());
                    ps.setInt(2, report.getItem().getCategoryId());
                    ps.setString(3, report.getItem().getDescription());
                    ps.setString(4, report.getItem().getImagePath());
                    ps.setInt(5, itemId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateReportSql)) {
                    ps.setString(1, report.getLocation());
                    ps.setDate(2,
                            java.sql.Date.valueOf(report.getReportDate()));
                    ps.setInt(3, report.getReportId());
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

    public boolean markReportAsReturned(
            int reportId, int requestingUserId, boolean isAdmin)
            throws SQLException {

        String findSql = """
        SELECT user_id, status
        FROM reports
        WHERE report_id = ?
        FOR UPDATE
        """;

        String updateSql = """
        UPDATE reports
        SET status = 'RETURNED'
        WHERE report_id = ?
        """;

        String historySql = """
        INSERT INTO status_history
            (report_id, changed_by, old_status, new_status)
        VALUES (?, ?, ?, 'RETURNED')
        """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int ownerId;
                String oldStatus;

                try (PreparedStatement ps =
                             con.prepareStatement(findSql)) {
                    ps.setInt(1, reportId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }

                        ownerId = rs.getInt("user_id");
                        oldStatus = rs.getString("status");
                    }
                }

                if (!isAdmin && ownerId != requestingUserId) {
                    throw new SQLException(
                            "You are not authorized to return this report."
                    );
                }

                if (!"CLAIMED".equals(oldStatus)) {
                    throw new SQLException(
                            "Only CLAIMED reports can be marked as RETURNED."
                    );
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {
                    ps.setInt(1, reportId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps =
                             con.prepareStatement(historySql)) {
                    ps.setInt(1, reportId);
                    ps.setInt(2, requestingUserId);
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
}
