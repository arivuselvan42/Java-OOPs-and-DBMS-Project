package com.findit.model;

public class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private String createdAt;
    private String profileImage;

    public User() {
    }

    public User(int userId, String name, String email,
                String passwordHash, String role) {
        this(userId, name, email, passwordHash, role, null, null);
    }

    public User(int userId, String name, String email,
                String passwordHash, String role, String createdAt) {
        this(userId, name, email, passwordHash, role, createdAt, null);
    }

    public User(int userId, String name, String email,
                String passwordHash, String role, String createdAt, String profileImage) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.profileImage = profileImage;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
