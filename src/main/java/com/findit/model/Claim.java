package com.findit.model;

import java.time.LocalDateTime;

public class Claim {
    private int claimId;
    private int reportId;
    private int claimantId;
    private String claimantName;
    private String itemName;
    private String claimDetails;
    private String claimStatus;
    private LocalDateTime claimDate;

    public Claim() {
    }

    public Claim(int claimId, int reportId, int claimantId,
                 String claimDetails, String claimStatus,
                 LocalDateTime claimDate) {
        this.claimId = claimId;
        this.reportId = reportId;
        this.claimantId = claimantId;
        this.claimDetails = claimDetails;
        this.claimStatus = claimStatus;
        this.claimDate = claimDate;
    }

    public Claim(int claimId, int reportId, int claimantId,
                 String claimantName, String itemName,
                 String claimDetails, String claimStatus,
                 LocalDateTime claimDate) {
        this.claimId = claimId;
        this.reportId = reportId;
        this.claimantId = claimantId;
        this.claimantName = claimantName;
        this.itemName = itemName;
        this.claimDetails = claimDetails;
        this.claimStatus = claimStatus;
        this.claimDate = claimDate;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getClaimantId() {
        return claimantId;
    }

    public void setClaimantId(int claimantId) {
        this.claimantId = claimantId;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getClaimDetails() {
        return claimDetails;
    }

    public void setClaimDetails(String claimDetails) {
        this.claimDetails = claimDetails;
    }

    public String getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(String claimStatus) {
        this.claimStatus = claimStatus;
    }

    public LocalDateTime getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDateTime claimDate) {
        this.claimDate = claimDate;
    }
}
