package com.findit.dao;

import com.findit.database.DBConnection;
import com.findit.database.PasswordUtil;

import java.security.SecureRandom;
import java.sql.*;

/**
 * Data access and security enforcement for password reset tokens and password updates.
 */
public class PasswordResetDAO {

    private final SecureRandom secureRandom = new SecureRandom();

    public enum RequestStatus {
        SUCCESS,
        EMAIL_NOT_FOUND,
        COOLDOWN_ACTIVE,
        ERROR
    }

    public static class RequestResult {
        private final RequestStatus status;
        private final int cooldownSecondsRemaining;
        private final String plainOtp;

        public RequestResult(RequestStatus status, int cooldownSecondsRemaining, String plainOtp) {
            this.status = status;
            this.cooldownSecondsRemaining = cooldownSecondsRemaining;
            this.plainOtp = plainOtp;
        }

        public RequestStatus getStatus() {
            return status;
        }

        public int getCooldownSecondsRemaining() {
            return cooldownSecondsRemaining;
        }

        public String getPlainOtp() {
            return plainOtp;
        }
    }

    public enum VerifyStatus {
        SUCCESS,
        INVALID_OTP,
        EXPIRED,
        ATTEMPTS_EXCEEDED,
        NO_ACTIVE_TOKEN
    }

    public static class VerifyResult {
        private final VerifyStatus status;
        private final int attemptsRemaining;

        public VerifyResult(VerifyStatus status, int attemptsRemaining) {
            this.status = status;
            this.attemptsRemaining = attemptsRemaining;
        }

        public VerifyStatus getStatus() {
            return status;
        }

        public int getAttemptsRemaining() {
            return attemptsRemaining;
        }
    }

    /**
     * Issues a cryptographically secure 6-digit OTP for the specified email.
     * Enforces user existence check, 60-second cooldown rate limit, and invalidates previous tokens.
     */
    public RequestResult requestOtp(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return new RequestResult(RequestStatus.EMAIL_NOT_FOUND, 0, null);
        }

        String checkUserSql = "SELECT user_id FROM users WHERE email = ?";
        String checkCooldownSql = """
                SELECT TIMESTAMPDIFF(SECOND, created_at, NOW()) AS age_sec
                FROM password_reset_tokens
                WHERE email = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;
        String invalidateOldSql = """
                UPDATE password_reset_tokens
                SET is_used = TRUE
                WHERE email = ? AND is_used = FALSE
                """;
        String insertTokenSql = """
                INSERT INTO password_reset_tokens
                (email, otp_hash, attempts_left, is_used, expires_at)
                VALUES (?, ?, 3, FALSE, DATE_ADD(NOW(), INTERVAL 5 MINUTE))
                """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // 1. Check if user exists
                try (PreparedStatement ps = con.prepareStatement(checkUserSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new RequestResult(RequestStatus.EMAIL_NOT_FOUND, 0, null);
                        }
                    }
                }

                // 2. Check 60-second cooldown rate limit
                try (PreparedStatement ps = con.prepareStatement(checkCooldownSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int age = rs.getInt("age_sec");
                            if (age < 60) {
                                con.rollback();
                                return new RequestResult(RequestStatus.COOLDOWN_ACTIVE, 60 - age, null);
                            }
                        }
                    }
                }

                // 3. Invalidate previous active tokens
                try (PreparedStatement ps = con.prepareStatement(invalidateOldSql)) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }

                // 4. Generate 6-digit OTP
                int otpNum = 100000 + secureRandom.nextInt(900000);
                String plainOtp = String.valueOf(otpNum);
                String otpHash = PasswordUtil.hashSha256(plainOtp);

                // 5. Store hashed OTP
                try (PreparedStatement ps = con.prepareStatement(insertTokenSql)) {
                    ps.setString(1, email);
                    ps.setString(2, otpHash);
                    ps.executeUpdate();
                }

                con.commit();
                return new RequestResult(RequestStatus.SUCCESS, 0, plainOtp);

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Validates the OTP against the active token in the database.
     * Decrements attempts on failure and invalidates on expiration or exceeding max attempts.
     */
    public VerifyResult verifyOtp(String email, String plainOtp) throws SQLException {
        if (email == null || plainOtp == null || plainOtp.isBlank()) {
            return new VerifyResult(VerifyStatus.INVALID_OTP, 0);
        }

        String inputHash = PasswordUtil.hashSha256(plainOtp.trim());
        String lockTokenSql = """
                SELECT token_id, otp_hash, attempts_left, is_used,
                       (NOW() <= expires_at) AS is_fresh
                FROM password_reset_tokens
                WHERE email = ? AND is_used = FALSE
                ORDER BY created_at DESC
                LIMIT 1
                FOR UPDATE
                """;
        String decrementAttemptsSql = """
                UPDATE password_reset_tokens
                SET attempts_left = ?, is_used = ?
                WHERE token_id = ?
                """;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int tokenId;
                String storedHash;
                int attemptsLeft;
                boolean isFresh;

                try (PreparedStatement ps = con.prepareStatement(lockTokenSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return new VerifyResult(VerifyStatus.NO_ACTIVE_TOKEN, 0);
                        }
                        tokenId = rs.getInt("token_id");
                        storedHash = rs.getString("otp_hash");
                        attemptsLeft = rs.getInt("attempts_left");
                        isFresh = rs.getBoolean("is_fresh");
                    }
                }

                // Expiration check
                if (!isFresh) {
                    try (PreparedStatement ps = con.prepareStatement(decrementAttemptsSql)) {
                        ps.setInt(1, 0);
                        ps.setBoolean(2, true); // Mark used/expired
                        ps.setInt(3, tokenId);
                        ps.executeUpdate();
                    }
                    con.commit();
                    return new VerifyResult(VerifyStatus.EXPIRED, 0);
                }

                // Attempts check
                if (attemptsLeft <= 0) {
                    try (PreparedStatement ps = con.prepareStatement(decrementAttemptsSql)) {
                        ps.setInt(1, 0);
                        ps.setBoolean(2, true);
                        ps.setInt(3, tokenId);
                        ps.executeUpdate();
                    }
                    con.commit();
                    return new VerifyResult(VerifyStatus.ATTEMPTS_EXCEEDED, 0);
                }

                // Hash comparison
                if (storedHash.equalsIgnoreCase(inputHash)) {
                    con.commit();
                    return new VerifyResult(VerifyStatus.SUCCESS, attemptsLeft);
                } else {
                    int remaining = attemptsLeft - 1;
                    boolean shouldInvalidate = (remaining <= 0);

                    try (PreparedStatement ps = con.prepareStatement(decrementAttemptsSql)) {
                        ps.setInt(1, Math.max(0, remaining));
                        ps.setBoolean(2, shouldInvalidate);
                        ps.setInt(3, tokenId);
                        ps.executeUpdate();
                    }
                    con.commit();

                    if (shouldInvalidate) {
                        return new VerifyResult(VerifyStatus.ATTEMPTS_EXCEEDED, 0);
                    } else {
                        return new VerifyResult(VerifyStatus.INVALID_OTP, remaining);
                    }
                }

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Resets the user's password using a verified OTP.
     * Performs atomic update of password_hash and marks tokens as consumed.
     */
    public boolean resetPassword(String email, String plainOtp, String newPassword) throws SQLException {
        if (email == null || plainOtp == null || newPassword == null || newPassword.length() < 8) {
            return false;
        }

        // Verify OTP is still valid first
        VerifyResult verify = verifyOtp(email, plainOtp);
        if (verify.getStatus() != VerifyStatus.SUCCESS) {
            return false;
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        String updateUserSql = "UPDATE users SET password_hash = ? WHERE email = ?";
        String invalidateTokensSql = "UPDATE password_reset_tokens SET is_used = TRUE WHERE email = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int updatedRows;
                try (PreparedStatement ps = con.prepareStatement(updateUserSql)) {
                    ps.setString(1, newHash);
                    ps.setString(2, email);
                    updatedRows = ps.executeUpdate();
                }

                if (updatedRows != 1) {
                    con.rollback();
                    return false;
                }

                // Invalidate all tokens for this email
                try (PreparedStatement ps = con.prepareStatement(invalidateTokensSql)) {
                    ps.setString(1, email);
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

