package com.findit.model;

import java.time.LocalDate;

public class LostReport extends Report {

    public LostReport() {
        super();
    }

    public LostReport(int reportId, int userId, Item item,
                      String location, LocalDate reportDate,
                      String status) {
        super(reportId, userId, item, location, reportDate, status);
    }

    @Override
    public String getReportType() {
        return "LOST";
    }
}
