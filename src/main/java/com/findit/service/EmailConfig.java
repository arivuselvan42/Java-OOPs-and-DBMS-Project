package com.findit.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Manages Gmail SMTP configuration securely.
 * Reads credentials strictly from environment variables without hardcoding.
 */
public class EmailConfig {

    public static final String DEFAULT_HOST = "smtp.gmail.com";
    public static final int DEFAULT_PORT = 587;

    private static String host = DEFAULT_HOST;
    private static int port = DEFAULT_PORT;
    private static String username = "";
    private static String password = "";
    private static boolean auth = true;
    private static boolean starttls = true;

    static {
        loadConfig();
    }

    public static synchronized void loadConfig() {
        // 1. Primary: Read from process environment variables
        String envUser = System.getenv("FINDIT_EMAIL");
        String envPass = System.getenv("FINDIT_EMAIL_PASSWORD");

        // Alternate environment variable names for compatibility
        if (envUser == null || envUser.isBlank()) {
            envUser = System.getenv("FINDIT_SMTP_USER");
        }
        if (envPass == null || envPass.isBlank()) {
            envPass = System.getenv("FINDIT_SMTP_PASS");
        }

        // 2. Windows Fallback: If process did not inherit newly set User env vars
        if ((envUser == null || envUser.isBlank() || envPass == null || envPass.isBlank())
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                if (envUser == null || envUser.isBlank()) {
                    envUser = readWindowsUserEnv("FINDIT_EMAIL");
                }
                if (envPass == null || envPass.isBlank()) {
                    envPass = readWindowsUserEnv("FINDIT_EMAIL_PASSWORD");
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Optional local file fallback: email.properties
        File propFile = new File("email.properties");
        if ((envUser == null || envUser.isBlank() || envPass == null || envPass.isBlank()) && propFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(propFile)) {
                props.load(in);
                if (envUser == null || envUser.isBlank()) {
                    envUser = props.getProperty("smtp.user", "");
                }
                if (envPass == null || envPass.isBlank()) {
                    envPass = props.getProperty("smtp.password", "");
                }
            } catch (Exception ignored) {
            }
        }

        username = envUser != null ? envUser.trim() : "";
        // Clean Google App Password formatting (Google app passwords are 16 letters, often displayed with spaces)
        password = envPass != null ? envPass.replaceAll("\\s+", "").trim() : "";
    }

    private static String readWindowsUserEnv(String varName) {
        try {
            Process process = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", varName).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("REG_SZ")) {
                        String[] parts = line.trim().split("REG_SZ\\s+");
                        if (parts.length >= 2) {
                            return parts[1].trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static boolean isAuth() {
        return auth;
    }

    public static boolean isStarttls() {
        return starttls;
    }

    @Override
    public String toString() {
        // Strict security: Never expose password or sensitive information
        return "EmailConfig{host='" + host + "', port=" + port + ", configured=" + isConfigured() + "}";
    }
}
