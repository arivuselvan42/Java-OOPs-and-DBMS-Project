package com.findit.model;

import java.time.LocalDate;

public class FoundReport extends Report {

    public FoundReport() {
        super();
    }

    public FoundReport(int reportId, int userId, Item item,
                       String location, LocalDate reportDate,
                       String status) {
        super(reportId, userId, item, location, reportDate, status);
    }

    @Override
    public String getReportType() {
        return "FOUND";
    }
}
