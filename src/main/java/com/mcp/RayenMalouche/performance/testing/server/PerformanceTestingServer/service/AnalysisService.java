package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.util.*;
import java.util.stream.Collectors;

public class AnalysisService {

    public static Map<String, Object> analyzeTestResults(List<String> testIds, boolean generateReport, boolean compareWithStandards) {
        Collection<TestResult> resultsToAnalyze;

        Map<String, TestResult> allResults = PerformanceTestService.getAllTestResults();

        if (testIds != null && !testIds.isEmpty()) {
            resultsToAnalyze = allResults.values().stream()
                    .filter(r -> testIds.contains(r.testId))
                    .collect(Collectors.toList());
        } else {
            resultsToAnalyze = allResults.values();
        }

        if (resultsToAnalyze.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No test results found to analyze.");
            return response;
        }

        List<TestResult> successfulTests = resultsToAnalyze.stream()
                .filter(r -> r.success)
                .collect(Collectors.toList());

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalTests", resultsToAnalyze.size());
        analysis.put("successfulTests", successfulTests.size());
        analysis.put("failedTests", resultsToAnalyze.size() - successfulTests.size());
        analysis.put("successRate", (double) successfulTests.size() / resultsToAnalyze.size() * 100);

        if (!successfulTests.isEmpty()) {
            // Performance metrics
            analysis.put("averageResponseTime", calculateAverage(
                    successfulTests.stream().mapToDouble(r -> r.responseTime).boxed().collect(Collectors.toList())
            ));
            analysis.put("totalTokensUsed", successfulTests.stream().mapToInt(r -> r.tokensUsed != null ? r.tokensUsed : 0).sum());
            analysis.put("totalCost", successfulTests.stream().mapToDouble(r -> r.cost != null ? r.cost : 0.0).sum());
            analysis.put("averageDatasetSize", calculateAverage(
                    successfulTests.stream().mapToDouble(r -> r.datasetSize != null ? r.datasetSize : 0).boxed().collect(Collectors.toList())
            ));
            analysis.put("averageMcpCalls", calculateAverage(
                    successfulTests.stream().mapToDouble(r -> r.mcpCallsCount != null ? r.mcpCallsCount : 0).boxed().collect(Collectors.toList())
            ));

            // URL analysis
            Set<String> uniqueUrls = successfulTests.stream()
                    .flatMap(r -> r.urlsFetched.stream())
                    .collect(Collectors.toSet());
            analysis.put("uniqueUrlsFetched", uniqueUrls);

            // Performance distribution
            List<Long> responseTimes = successfulTests.stream().map(r -> r.responseTime).collect(Collectors.toList());
            Map<String, Object> performanceMetrics = new HashMap<>();
            performanceMetrics.put("fastest", Collections.min(responseTimes));
            performanceMetrics.put("slowest", Collections.max(responseTimes));
            performanceMetrics.put("median", calculateMedian(responseTimes));
            performanceMetrics.put("p95", calculatePercentile(responseTimes, 95));
            analysis.put("performanceMetrics", performanceMetrics);

            // Quality assessment
            Map<String, Object> qualityMetrics = analyzeQuality(successfulTests);
            analysis.put("qualityAssessment", qualityMetrics);

            // Email notification analysis
            Map<String, Object> emailAnalysis = analyzeEmailNotifications(successfulTests);
            analysis.put("emailNotificationAnalysis", emailAnalysis);
        }

        // Standards comparison if requested
        if (compareWithStandards) {
            Map<String, Object> standardsComparison = generateStandardsComparison(successfulTests);
            analysis.put("standardsComparison", standardsComparison);
        }

        // Generate recommendations
        List<String> recommendations = generateAnalysisRecommendations(analysis, successfulTests);
        analysis.put("recommendations", recommendations);

        if (generateReport) {
            try {
                String reportPath = generateDetailedReport(analysis, new ArrayList<>(resultsToAnalyze));
                analysis.put("reportGenerated", true);
                analysis.put("reportPath", reportPath);
            } catch (Exception e) {