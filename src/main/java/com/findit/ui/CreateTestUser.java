package com.findit.ui;

import com.findit.database.DBConnection;
import com.findit.database.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CreateTestUser {

    public static void main(String[] args) {

        String name = "Test User 2";
        String email = "testuser2@example.com";
        String password = requiredEnv("TEST_USER2_PASSWORD");

        String checkSql = "SELECT user_id FROM users WHERE email = ?";

        String insertSql = """
                INSERT INTO users (name, email, password_hash, role)
                VALUES (?, ?, ?, 'USER')
                """;

        try (Connection con = DBConnection.getConnection()) {

            // Check whether user already exists
            try (PreparedStatement check = con.prepareStatement(checkSql)) {

                check.setString(1, email);

                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("This user already exists.");
                        return;
                    }
                }
            }

            // Hash password using BCrypt
            String hashedPassword =
                    PasswordUtil.hashPassword(password);

            // Insert new user
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, hashedPassword);

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    System.out.println(
                            "Second user created successfully!"
                    );
                    System.out.println("Email: " + email);
                    System.out.println("Role: USER");
                    System.out.println("Password stored securely.");
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Could not create user: " + e.getMessage()
            );
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String requiredEnv(String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing environment variable: " + name
            );
        }

        return value;
    }
}