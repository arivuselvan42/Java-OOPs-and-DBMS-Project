package com.findit.model;

import java.time.LocalDate;

public abstract class Report {
    private int reportId;
    private int userId;
    private String reporterName;
    private Item item;
    private String location;
    private LocalDate reportDate;
    private String status;
    private String reporterEmail;
    private String reporterProfileImage;

    public Report() {
    }

    public Report(int reportId, int userId, Item item,
                  String location, LocalDate reportDate,
                  String status) {
        this.reportId = reportId;
        this.userId = userId;
        this.item = item;
        this.location = location;
        this.reportDate = reportDate;
        this.status = status;
    }

    public Report(int reportId, int userId, String reporterName, Item item,
                  String location, LocalDate reportDate,
                  String status) {
        this.reportId = reportId;
        this.userId = userId;
        this.reporterName = reporterName;
        this.item = item;
        this.location = location;
        this.reportDate = reportDate;
        this.status = status;
    }

    public abstract String getReportType();

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
    }

    public String getReporterProfileImage() {
        return reporterProfileImage;
    }

    public void setReporterProfileImage(String reporterProfileImage) {
        this.reporterProfileImage = reporterProfileImage;
    }
}
