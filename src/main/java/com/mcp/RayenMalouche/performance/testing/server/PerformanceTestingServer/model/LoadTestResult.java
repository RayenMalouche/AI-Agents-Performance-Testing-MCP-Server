package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model;

public class LoadTestResult {
    public String testId;
    public String startTime;
    public String endTime;
    public int totalTests;
    public int successfulTests;
    public int failedTests;
    public double averageResponseTime;
    public int totalTokens;
    public double totalCost;
    public int concurrentUsers;
    public int requestsPerUser;
    public String performanceEvaluation;
    public String recommendations;
}