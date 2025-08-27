package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TestResult {
    public String testId;
    public String timestamp;
    public String query;
    public long responseTime;
    public Integer tokensUsed;
    public Double cost;
    public boolean success;
    public String errorMessage;
    public Integer datasetSize;
    public Integer mcpCallsCount;
    public List<String> urlsFetched = new ArrayList<>();
    public String performanceEvaluation;
    public String recommendations;

    public TestResult(String testId, String query) {
        this.testId = testId;
        this.query = query;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}