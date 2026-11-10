package com.findit.model;

import java.time.LocalDateTime;

public class PasswordResetToken {
    private int tokenId;
    private String email;
    private String otpHash;
    private int attemptsLeft;
    private boolean isUsed;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(int tokenId, String email, String otpHash,
                              int attemptsLeft, boolean isUsed,
                              LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.email = email;
        this.otpHash = otpHash;
        this.attemptsLeft = attemptsLeft;
        this.isUsed = isUsed;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public int getTokenId() {
        return tokenId;
    }

    public void setTokenId(int tokenId) {
        this.tokenId = tokenId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    public void setAttemptsLeft(int attemptsLeft) {
        this.attemptsLeft = attemptsLeft;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}

