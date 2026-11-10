package com.findit.dao;

import com.findit.database.DBConnection;
import com.findit.database.PasswordUtil;
import com.findit.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public boolean registerUser(User user) throws SQLException {
        String sql = """
                INSERT INTO users (name, email, password_hash, role)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());

            return ps.executeUpdate() == 1;
        }
    }

    public User getUserByEmail(String email) throws SQLException {
        String sql = """
                SELECT user_id, name, email, password_hash, role, created_at, profile_image
                FROM users
                WHERE email = ?
                """;

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    String created = ts != null ? ts.toLocalDateTime().toLocalDate().toString() : "N/A";
                    String profileImg = null;
                    try {
                        profileImg = rs.getString("profile_image");
                    } catch (SQLException ignored) {
                    }

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            created,
                            profileImg);
                }
            }
        }

        return null;
    }

    public User getUserById(int userId) throws SQLException {
        String sql = """
                SELECT user_id, name, email, password_hash, role, created_at, profile_image
                FROM users
                WHERE user_id = ?
                """;

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    String created = ts != null ? ts.toLocalDateTime().toLocalDate().toString() : "N/A";
                    String profileImg = null;
                    try {
                        profileImg = rs.getString("profile_image");
                    } catch (SQLException ignored) {
                    }

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            created,
                            profileImg);
                }
            }
        }

        return null;
    }

    public boolean registerSecureUser(User user, String password) throws SQLException {
        String sql = """
                INSERT INTO users (name, email, password_hash, role)
                VALUES (?, ?, ?, ?)
                """;

        String hash = PasswordUtil.hashPassword(password);

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, hash);
            ps.setString(4, user.getRole());

            return ps.executeUpdate() == 1;
        }
    }

    public User authenticate(String email, String password) throws SQLException {
        User user = getUserByEmail(email);

        if (user == null) {
            return null;
        }

        boolean valid = PasswordUtil.verifyPassword(password, user.getPasswordHash());

        return valid ? user : null;
    }

    /**
     * Updates user's display name.
     */
    public boolean updateUserName(int userId, String newName) throws SQLException {
        String sql = "UPDATE users SET name = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Authenticated password change requiring verification of current password.
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) throws SQLException {
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }

        // Verify current password against stored BCrypt hash
        if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
            return false;
        }

        // Hash new password and update
        String newHash = PasswordUtil.hashPassword(newPassword);
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Updates user's avatar / profile picture file path.
     */
    public boolean updateProfileImage(int userId, String imagePath) throws SQLException {
        String sql = "UPDATE users SET profile_image = ? WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imagePath != null && !imagePath.isBlank() ? imagePath.trim() : null);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }
}
