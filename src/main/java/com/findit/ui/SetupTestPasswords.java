package com.findit.ui;

import com.findit.database.DBConnection;
import com.findit.database.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SetupTestPasswords {

    public static void main(String[] args) throws SQLException {

        String sql = """
                INSERT INTO users (name, email, password_hash, role)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection()) {

            // Primary Admin
            createUser(con, sql,
                    "Admin",
                    "projects.admin.id@gmail.com",
                    requiredEnv("PRIMARY_ADMIN_PASSWORD"),
                    "ADMIN");

            // Secondary / Test Admin
            createUser(con, sql,
                    "Admin",
                    "admin@findit.com",
                    requiredEnv("TEST_ADMIN_PASSWORD"),
                    "ADMIN");

            // Regular user
            createUser(con, sql,
                    "Test User",
                    "testuser@example.com",
                    requiredEnv("TEST_USER_PASSWORD"),
                    "USER");

            // Second regular user
            createUser(con, sql,
                    "Test User 2",
                    "testuser2@example.com",
                    requiredEnv("TEST_USER2_PASSWORD"),
                    "USER");

            System.out.println("\nAll accounts created successfully.");
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

    private static void createUser(
            Connection con,
            String sql,
            String name,
            String email,
            String password,
            String role
    ) throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, PasswordUtil.hashPassword(password));
            ps.setString(4, role);

            ps.executeUpdate();

            System.out.println("Created: " + email + " [" + role + "]");
        }
    }
}