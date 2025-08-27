package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardsComparisonService {

    // Industry performance standards (based on common benchmarks)
    private static final Map<String, Object> INDUSTRY_STANDARDS = new HashMap<>();

    static {
        // Response Time Standards (milliseconds)
        Map<String, Long> responseTimeStds = new HashMap<>();
        responseTimeStds.put("excellent", 200L);
        responseTimeStds.put("good", 500L);
        responseTimeStds.put("acceptable", 1000L);
        responseTimeStds.put("poor", 3000L);
        INDUSTRY_STANDARDS.put("responseTime", responseTimeStds);

        // Token Efficiency Standards (tokens per second)
        Map<String, Double> tokenEfficiencyStds = new HashMap<>();
        tokenEfficiencyStds.put("excellent", 50.0);
        tokenEfficiencyStds.put("good", 30.0);
        tokenEfficiencyStds.put("acceptable", 15.0);
        tokenEfficiencyStds.put("poor", 5.0);
        INDUSTRY_STANDARDS.put("tokenEfficiency", tokenEfficiencyStds);

        // Cost Efficiency Standards (cost per 1K tokens in cents)
        Map<String, Double> costStds = new HashMap<>();
        costStds.put("excellent", 0.03);
        costStds.put("good", 0.06);
        costStds.put("acceptable", 0.10);
        costStds.put("expensive", 0.20);
        INDUSTRY_STANDARDS.put("costEfficiency", costStds);

        // Dataset Quality Standards
        Map<String, Integer> datasetQualityStds = new HashMap<>();
        datasetQualityStds.put("minElements", 5);
        datasetQualityStds.put("recommendedElements", 10);
        datasetQualityStds.put("excellentElements", 20);
        datasetQualityStds.put("maxMcpCalls", 10);
        INDUSTRY_STANDARDS.put("datasetQuality", datasetQualityStds);

        // Success Rate Standards
        Map<String, Double> successRateStds = new HashMap<>();
        successRateStds.put("excellent", 0.98);
        successRateStds.put("good", 0.95);
        successRateStds.put("acceptable", 0.90);
        successRateStds.put("poor", 0.80);
        INDUSTRY_STANDARDS.put("successRate", successRateStds);
    }

    public static Map<String, Object> compareWithStandards(String testId, Long responseTime,
                                                           Integer tokensUsed, Double cost,
                                                           Integer datasetSize, Boolean success) {

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("testId", testId);
        comparison.put("timestamp", java.time.LocalDateTime.now().toString());
        comparison.put("standards", INDUSTRY_STANDARDS);

        // Individual metric comparisons
        if (responseTime != null) {
            comparison.put("responseTimeComparison", compareResponseTime(responseTime));
        }

        if (tokensUsed != null && responseTime != null) {
            double tokensPerSecond = tokensUsed / (responseTime / 1000.0);
            comparison.put("tokenEfficiencyComparison", compareTokenEfficiency(tokensPerSecond, tokensUsed));
        }

        if (cost != null) {
            comparison.put("costEfficiencyComparison", compareCostEfficiency(cost));
        }

        if (datasetSize != null) {
            comparison.put("datasetQualityComparison", compareDatasetQuality(datasetSize));
        }

        // Overall assessment
        Map<String, Object> overallAssessment = generateOverallAssessment(responseTime, tokensUsed, cost, datasetSize, success);
        comparison.put("overallAssessment", overallAssessment);

        // Recommendations
        List<String> recommendations = generateRecommendations(responseTime, tokensUsed, cost, datasetSize, success);
        comparison.put("recommendations", recommendations);

        // Industry benchmark context
        comparison.put("industryContext", generateIndustryContext());

        return comparison;
    }

    private static Map<String, Object> compareResponseTime(Long responseTime) {
        Map<String, Object> result = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Long> standards = (Map<String, Long>) INDUSTRY_STANDARDS.get("responseTime");

        result.put("actualValue", responseTime + "ms");
        result.put("standards", standards);

        String rating;
        String comparison;
        if (responseTime <= standards.get("excellent")) {
            rating = "Excellent";
            comparison = "Exceeds industry standards";
        } else if (responseTime <= standards.get("good")) {
            rating = "Good";
            comparison = "Meets good industry standards";
        } else if (responseTime <= standards.get("acceptable")) {
            rating = "Acceptable";
            comparison = "Within acceptable range";
        } else {
            rating = "Poor";
            comparison = "Below industry standards";
        }

        result.put("rating", rating);
        result.put("comparison", comparison);
        return result;
    }

    private static Map<String, Object> compareTokenEfficiency(double tokensPerSecond, int totalTokens) {
        Map<String, Object> result = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Double> standards = (Map<String, Double>) INDUSTRY_STANDARDS.get("tokenEfficiency");

        result.put("actualValue", String.format("%.2f tokens/sec", tokensPerSecond));
        result.put("totalTokens", totalTokens);
        result.put("standards", standards);

        String rating;
        String comparison;
        if (tokensPerSecond >= standards.get("excellent")) {
            rating = "Excellent";
            comparison = "High processing efficiency";
        } else if (tokensPerSecond >= standards.get("good")) {
            rating = "Good";
            comparison = "Good processing efficiency";
        } else if (tokensPerSecond >= standards.get("acceptable")) {
            rating = "Acceptable";
            comparison = "Adequate processing efficiency";
        } else {
            rating = "Poor";
            comparison = "Low processing efficiency";
        }

        result.put("rating", rating);
        result.put("comparison", comparison);
        return result;
    }

    private static Map<String, Object> compareCostEfficiency(Double cost) {
        Map<String, Object> result = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Double> standards = (Map<String, Double>) INDUSTRY_STANDARDS.get("costEfficiency");

        result.put("actualValue", String.format("$%.4f", cost));
        result.put("standards", standards);

        String rating;
        String comparison;
        if (cost <= standards.get("excellent")) {
            rating = "Excellent";
            comparison = "Very cost efficient";
        } else if (cost <= standards.get("good")) {
            rating = "Good";
            comparison = "Cost efficient";
        } else if (cost <= standards.get("acceptable")) {
            rating = "Acceptable";
            comparison = "Reasonable cost";
        } else {
            rating = "Expensive";
            comparison = "High cost - optimization needed";
        }

        result.put("rating", rating);
        result.put("comparison", comparison);
        return result;
    }

    private static Map<String, Object> compareDatasetQuality(Integer datasetSize) {
        Map<String, Object> result = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Integer> standards = (Map<String, Integer>) INDUSTRY_STANDARDS.get("datasetQuality");

        result.put("actualValue", datasetSize + " elements");
        result.put("standards", standards);

        String rating;
        String comparison;
        if (datasetSize >= standards.get("excellentElements")) {
            rating = "Excellent";
            comparison = "High-quality comprehensive dataset";
        } else if (datasetSize >= standards.get("recommendedElements")) {
            rating = "Good";
            comparison = "Good dataset size";
        } else if (datasetSize >= standards.get("minElements")) {
            rating = "Acceptable";
            comparison = "Meets minimum requirements";
        } else {
            rating = "Poor";
            comparison = "Below minimum dataset standards";
        }

        result.put("rating", rating);
        result.put("comparison", comparison);
        return result;
    }

    private static Map<String, Object> generateOverallAssessment(Long responseTime, Integer tokensUsed,
                                                                 Double cost, Integer datasetSize, Boolean success) {
        Map<String, Object> assessment = new HashMap<>();

        int excellentCount = 0;
        int goodCount = 0;
        int acceptableCount = 0;
        int poorCount = 0;
        int totalMetrics = 0;

        // Count ratings across all metrics
        if (responseTime != null) {
            totalMetrics++;
            @SuppressWarnings("unchecked")
            Map<String, Long> rtStds = (Map<String, Long>) INDUSTRY_STANDARDS.get("responseTime");
            if (responseTime <= rtStds.get("excellent")) excellentCount++;
            else if (responseTime <= rtStds.get("good")) goodCount++;
            else if (responseTime <= rtStds.get("acceptable")) acceptableCount++;
            else poorCount++;
        }

        if (cost != null) {
            totalMetrics++;
            @SuppressWarnings("unchecked")
            Map<String, Double> costStds = (Map<String, Double>) INDUSTRY_STANDARDS.get("costEfficiency");
            if (cost <= costStds.get("excellent")) excellentCount++;
            else if (cost <= costStds.get("good")) goodCount++;
            else if (cost <= costStds.get("acceptable")) acceptableCount++;
            else poorCount++;
        }

        if (datasetSize != null) {
            totalMetrics++;
            @SuppressWarnings("unchecked")
            Map<String, Integer> dsStds = (Map<String, Integer>) INDUSTRY_STANDARDS.get("datasetQuality");
            if (datasetSize >= dsStds.get("excellentElements")) excellentCount++;
            else if (datasetSize >= dsStds.get("recommendedElements")) goodCount++;
            else if (datasetSize >= dsStds.get("minElements")) acceptableCount++;
            else poorCount++;
        }

        assessment.put("totalMetricsEvaluated", totalMetrics);
        assessment.put("excellentMetrics", excellentCount);
        assessment.put("goodMetrics", goodCount);
        assessment.put("acceptableMetrics", acceptableCount);
        assessment.put("poorMetrics", poorCount);
        assessment.put("successfulExecution", success != null ? success : false);

        // Overall grade
        String overallGrade;
        if (excellentCount >= totalMetrics * 0.7) {
            overallGrade = "Excellent";
        } else if ((excellentCount + goodCount) >= totalMetrics * 0.6) {
            overallGrade = "Good";
        } else if ((excellentCount + goodCount + acceptableCount) >= totalMetrics * 0.8) {
            overallGrade = "Acceptable";
        } else {
            overallGrade = "Needs Improvement";
        }

        assessment.put("overallGrade", overallGrade);

        return assessment;
    }

    private static List<String> generateRecommendations(Long responseTime, Integer tokensUsed,
                                                        Double cost, Integer datasetSize, Boolean success) {
        List<String> recommendations = new java.util.ArrayList<>();

        // Response time recommendations
        if (responseTime != null) {
            @SuppressWarnings("unchecked")
            Map<String, Long> rtStds = (Map<String, Long>) INDUSTRY_STANDARDS.get("responseTime");
            if (responseTime > rtStds.get("good")) {
                recommendations.add("Optimize response time - consider caching, query optimization, or infrastructure scaling");
            }
        }

        // Cost recommendations
        if (cost != null) {
            @SuppressWarnings("unchecked")
            Map<String, Double> costStds = (Map<String, Double>) INDUSTRY_STANDARDS.get("costEfficiency");
            if (cost > costStds.get("acceptable")) {
                recommendations.add("High cost detected - review token usage, model selection, and query complexity");
            }
        }

        // Dataset quality recommendations
        if (datasetSize != null) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> dsStds = (Map<String, Integer>) INDUSTRY_STANDARDS.get("datasetQuality");
            if (datasetSize < dsStds.get("recommendedElements")) {
                recommendations.add("Increase dataset size to meet recommended standards - improve content generation logic");
            }
        }

        // Success recommendations
        if (success != null && !success) {
            recommendations.add("Address execution failures - implement better error handling and retry mechanisms");
        }

        // General recommendations
        recommendations.add("Monitor performance trends over time to identify patterns and optimization opportunities");
        recommendations.add("Consider implementing automated performance alerting for proactive monitoring");

        return recommendations;
    }

    private static Map<String, Object> generateIndustryContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("benchmarkSource", "Industry best practices for AI/ML systems");
        context.put("applicableDomains", List.of("Natural Language Processing", "API Performance", "Dataset Generation", "AI Model Inference"));
        context.put("lastUpdated", "2025-01-27");
        context.put("methodology", "Benchmarks derived from industry reports, academic research, and production system performance data");

        Map<String, String> sources = new HashMap<>();
        sources.put("responseTime", "Google PageSpeed Insights, Web Performance Standards");
        sources.put("tokenEfficiency", "OpenAI, Hugging Face Leaderboards");
        sources.put("costEfficiency", "AI Model Pricing Analysis, Cost Optimization Studies");
        sources.put("datasetQuality", "ML Dataset Standards, Data Science Best Practices");

        context.put("benchmarkSources", sources);
        context.put("note", "Standards are guidelines and may vary based on specific use cases and requirements");

        return context;
    }

    public static Map<String, Object> getIndustryStandards() {
        return new HashMap<>(INDUSTRY_STANDARDS);
    }
}