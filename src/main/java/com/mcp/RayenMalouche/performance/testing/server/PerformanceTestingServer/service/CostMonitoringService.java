package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CostMonitoringService {

    public static Map<String, Object> monitorCosts(String timeframe, String modelName) {
        long timeframeLimitMs;
        switch (timeframe.toLowerCase()) {
            case "hour":
                timeframeLimitMs = 60 * 60 * 1000L;
                break;
            case "week":
                timeframeLimitMs = 7 * 24 * 60 * 60 * 1000L;
                break;
            default: // day
                timeframeLimitMs = 24 * 60 * 60 * 1000L;
                break;
        }

        Instant cutoffTime = Instant.now().minusMillis(timeframeLimitMs);

        List<TestResult> recentTests = PerformanceTestService.getAllTestResults().values().stream()
                .filter(r -> {
                    try {
                        return LocalDateTime.parse(r.timestamp).atZone(ZoneId.systemDefault())
                                .toInstant().isAfter(cutoffTime);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        Map<String, Object> costAnalysis = new HashMap<>();
        costAnalysis.put("timeframe", timeframe);
        costAnalysis.put("modelName", modelName);
        costAnalysis.put("periodStart", cutoffTime.toString());
        costAnalysis.put("periodEnd", Instant.now().toString());
        costAnalysis.put("totalTests", recentTests.size());

        int totalTokens = recentTests.stream().mapToInt(r -> r.tokensUsed != null ? r.tokensUsed : 0).sum();
        double totalCost = recentTests.stream().mapToDouble(r -> r.cost != null ? r.cost : 0.0).sum();

        costAnalysis.put("totalTokens", totalTokens);
        costAnalysis.put("totalCost", totalCost);

        if (!recentTests.isEmpty()) {
            double averageCostPerTest = totalCost / recentTests.size();
            double averageTokensPerTest = (double) totalTokens / recentTests.size();

            costAnalysis.put("averageCostPerTest", averageCostPerTest);
            costAnalysis.put("averageTokensPerTest", averageTokensPerTest);

            // Project monthly cost based on current usage pattern
            double testsPerHour = (double) recentTests.size() / (timeframeLimitMs / (60.0 * 60 * 1000));
            double projectedMonthlyTests = testsPerHour * 24 * 30;
            double projectedMonthlyCost = projectedMonthlyTests * averageCostPerTest;
            double projectedMonthlyTokens = projectedMonthlyTests * averageTokensPerTest;

            costAnalysis.put("projectedMonthlyCost", projectedMonthlyCost);
            costAnalysis.put("projectedMonthlyTokens", projectedMonthlyTokens);

            // Cost efficiency analysis
            Map<String, Object> efficiency = analyzeCostEfficiency(recentTests, averageCostPerTest);
            costAnalysis.put("efficiency", efficiency);

            // Budget recommendations
            List<String> recommendations = generateCostRecommendations(totalCost, projectedMonthlyCost, averageCostPerTest);
            costAnalysis.put("recommendations", recommendations);

        } else {
            costAnalysis.put("averageCostPerTest", 0.0);
            costAnalysis.put("averageTokensPerTest", 0.0);
            costAnalysis.put("projectedMonthlyCost", 0.0);
            costAnalysis.put("projectedMonthlyTokens", 0.0);
            costAnalysis.put("recommendations", List.of("No recent test data available for cost analysis"));
        }

        return costAnalysis;
    }

    private static Map<String, Object> analyzeCostEfficiency(List<TestResult> tests, double averageCost) {
        Map<String, Object> efficiency = new HashMap<>();

        // Cost per dataset element
        List<TestResult> testsWithDataset = tests.stream()
                .filter(r -> r.datasetSize != null && r.datasetSize > 0)
                .collect(Collectors.toList());

        if (!testsWithDataset.isEmpty()) {
            double avgCostPerElement = testsWithDataset.stream()
                    .mapToDouble(r -> (r.cost != null ? r.cost : 0.0) / r.datasetSize)
                    .average().orElse(0.0);
            efficiency.put("averageCostPerDatasetElement", avgCostPerElement);
        }

        // Token efficiency
        double avgTokensPerSecond = tests.stream()
                .filter(r -> r.responseTime > 0 && r.tokensUsed != null)
                .mapToDouble(r -> r.tokensUsed / (r.responseTime / 1000.0))
                .average().orElse(0.0);
        efficiency.put("averageTokensPerSecond", avgTokensPerSecond);

        // Cost distribution
        Map<String, Integer> costDistribution = new HashMap<>();
        costDistribution.put("low", 0);     // < $0.05
        costDistribution.put("medium", 0);  // $0.05 - $0.10
        costDistribution.put("high", 0);    // > $0.10

        for (TestResult test : tests) {
            double cost = test.cost != null ? test.cost : 0.0;
            if (cost < 0.05) {
                costDistribution.put("low", costDistribution.get("low") + 1);
            } else if (cost <= 0.10) {
                costDistribution.put("medium", costDistribution.get("medium") + 1);
            } else {
                costDistribution.put("high", costDistribution.get("high") + 1);
            }
        }
        efficiency.put("costDistribution", costDistribution);

        return efficiency;
    }

    private static List<String> generateCostRecommendations(double totalCost, double projectedMonthlyCost, double averageCostPerTest) {
        List<String> recommendations = new java.util.ArrayList<>();

        // Total cost thresholds
        if (totalCost > 5.0) {
            recommendations.add("High total cost detected - consider implementing cost controls");
        }

        // Monthly projection thresholds
        if (projectedMonthlyCost > 100.0) {
            recommendations.add("Projected monthly cost exceeds $100 - implement budget monitoring");
        } else if (projectedMonthlyCost > 50.0) {
            recommendations.add("Projected monthly cost approaching $50 - monitor usage patterns");
        }

        // Per-test cost efficiency
        if (averageCostPerTest > 0.15) {
            recommendations.add("High average cost per test - optimize query complexity or reduce token usage");
        }

        // Model optimization suggestions
        recommendations.add("Consider using more cost-effective models for routine testing");
        recommendations.add("Implement caching for repeated queries to reduce API calls");
        recommendations.add("Set up automated cost alerts for budget management");

        if (totalCost < 1.0 && projectedMonthlyCost < 20.0) {
            recommendations.add("Cost levels are within acceptable range - maintain current usage patterns");
        }

        return recommendations;
    }
}