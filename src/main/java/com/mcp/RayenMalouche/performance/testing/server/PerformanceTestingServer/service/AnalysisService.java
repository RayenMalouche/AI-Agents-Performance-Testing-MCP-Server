package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                analysis.put("reportError", "Failed to generate report: " + e.getMessage());
            }
        }

        return analysis;
    }

    private static Map<String, Object> analyzeQuality(List<TestResult> successfulTests) {
        Map<String, Object> qualityMetrics = new HashMap<>();

        // Dataset quality analysis
        double avgDatasetSize = successfulTests.stream()
                .mapToInt(r -> r.datasetSize != null ? r.datasetSize : 0)
                .average()
                .orElse(0.0);
        qualityMetrics.put("averageDatasetSize", avgDatasetSize);

        // MCP utilization analysis
        double avgMcpCalls = successfulTests.stream()
                .mapToInt(r -> r.mcpCallsCount != null ? r.mcpCallsCount : 0)
                .average()
                .orElse(0.0);
        qualityMetrics.put("averageMcpCalls", avgMcpCalls);

        return qualityMetrics;
    }

    private static Map<String, Object> analyzeEmailNotifications(List<TestResult> successfulTests) {
        Map<String, Object> emailAnalysis = new HashMap<>();

        // Count tests with email notification issues
        long emailIssues = successfulTests.stream()
                .filter(r -> r.recommendations != null && r.recommendations.contains("email"))
                .count();

        emailAnalysis.put("totalTestsWithEmailIssues", emailIssues);
        emailAnalysis.put("emailSuccessRate",
                (double) (successfulTests.size() - emailIssues) / successfulTests.size() * 100);

        return emailAnalysis;
    }

    private static Map<String, Object> generateStandardsComparison(List<TestResult> successfulTests) {
        Map<String, Object> comparison = new HashMap<>();

        // Get industry standards
        Map<String, Object> standards = StandardsComparisonService.getIndustryStandards();
        comparison.put("industryStandards", standards);

        // Compare response times
        double avgResponseTime = successfulTests.stream()
                .mapToLong(r -> r.responseTime)
                .average()
                .orElse(0.0);

        @SuppressWarnings("unchecked")
        Map<String, Long> responseTimeStds = (Map<String, Long>) standards.get("responseTime");
        String responseTimeRating = getResponseTimeRating(avgResponseTime, responseTimeStds);
        comparison.put("responseTimeRating", responseTimeRating);

        return comparison;
    }

    private static String getResponseTimeRating(double avgResponseTime, Map<String, Long> standards) {
        if (avgResponseTime <= standards.get("excellent")) {
            return "Excellent";
        } else if (avgResponseTime <= standards.get("good")) {
            return "Good";
        } else if (avgResponseTime <= standards.get("acceptable")) {
            return "Acceptable";
        } else {
            return "Poor";
        }
    }

    private static List<String> generateAnalysisRecommendations(Map<String, Object> analysis, List<TestResult> successfulTests) {
        List<String> recommendations = new ArrayList<>();

        // Response time recommendations
        double avgResponseTime = (Double) analysis.get("averageResponseTime");
        if (avgResponseTime > 3000) {
            recommendations.add("Optimize response time - consider caching, query optimization, or infrastructure scaling");
        }

        // Dataset quality recommendations
        double avgDatasetSize = (Double) analysis.get("averageDatasetSize");
        if (avgDatasetSize < 5) {
            recommendations.add("Increase dataset size to meet recommended standards - improve content generation logic");
        }

        // Cost recommendations
        double totalCost = (Double) analysis.get("totalCost");
        if (totalCost > 5.0) {
            recommendations.add("High total cost detected - consider implementing cost controls");
        }

        // Email notification recommendations
        @SuppressWarnings("unchecked")
        Map<String, Object> emailAnalysis = (Map<String, Object>) analysis.get("emailNotificationAnalysis");
        long emailIssues = (Long) emailAnalysis.get("totalTestsWithEmailIssues");
        if (emailIssues > 0) {
            recommendations.add("Email notification issues detected - verify SMTP configuration and email sending logic");
        }

        return recommendations;
    }

    private static String generateDetailedReport(Map<String, Object> analysis, List<TestResult> results) throws Exception {
        String reportPath = "performance_report_" + System.currentTimeMillis() + ".html";
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("analysis", analysis);
        reportData.put("results", results);

        String htmlContent = generateHTMLReport(reportData);
        Files.write(Paths.get(reportPath), htmlContent.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return reportPath;
    }

    private static String generateHTMLReport(Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Performance Test Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .summary { background: #f5f5f5; padding: 20px; border-radius: 8px; margin-bottom: 30px; }
                    .metric { display: inline-block; margin: 10px 20px; }
                    .metric-value { font-size: 24px; font-weight: bold; color: #2196F3; }
                    .metric-label { font-size: 14px; color: #666; }
                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #f2f2f2; }
                    .success { color: #4CAF50; }
                    .failure { color: #f44336; }
                </style>
            </head>
            <body>
                <h1>Performance Test Report</h1>
                <div class="summary">
                    <h2>Summary</h2>
            """);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) data.get("analysis");
        if (analysis != null) {
            html.append(String.format("""
                <div class="metric">
                    <div class="metric-value">%s</div>
                    <div class="metric-label">Total Tests</div>
                </div>
                <div class="metric">
                    <div class="metric-value">%s</div>
                    <div class="metric-label">Successful Tests</div>
                </div>
                <div class="metric">
                    <div class="metric-value">%.2f%%</div>
                    <div class="metric-label">Success Rate</div>
                </div>
                """,
                    analysis.get("totalTests"),
                    analysis.get("successfulTests"),
                    analysis.get("successRate")
            ));
        }

        html.append("""
                </div>
                <p>Generated on: """).append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("""
                </p>
            </body>
            </html>""");

        return html.toString();
    }

    private static double calculateAverage(List<Double> numbers) {
        if (numbers.isEmpty()) return 0.0;
        return numbers.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double calculateMedian(List<Long> numbers) {
        if (numbers.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private static double calculatePercentile(List<Long> numbers, double percentile) {
        if (numbers.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size());
        return sorted.get(index - 1);
    }
}