package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseAnalysisService {

    public static void analyzeResponseData(String responseData, TestResult testResult) {
        // Check for email sending confirmation
        analyzeEmailNotification(responseData, testResult);

        // Count dataset elements
        analyzeDatasetSize(responseData, testResult);

        // Count MCP calls
        analyzeMcpCalls(responseData, testResult);

        // Extract URLs
        analyzeUrlsFetched(responseData, testResult);

        // Estimate tokens and cost
        estimateTokensAndCost(responseData, testResult);

        // Generate performance evaluation
        generatePerformanceEvaluation(testResult);
    }

    private static void analyzeEmailNotification(String responseData, TestResult testResult) {
        boolean emailSent = responseData.toLowerCase().contains("email") &&
                (responseData.toLowerCase().contains("sent") ||
                        responseData.toLowerCase().contains("envoyé") ||
                        responseData.toLowerCase().contains("notification"));

        if (!emailSent) {
            if (testResult.recommendations == null) {
                testResult.recommendations = "";
            }
            testResult.recommendations += "WARNING: Email notification may not have been sent. Ensure send-email tool is working properly.\n";
        }
    }

    private static void analyzeDatasetSize(String responseData, TestResult testResult) {
        // Count dataset elements using multiple patterns
        Pattern jsonPattern = Pattern.compile("\"Input\":\\s*\"");
        Matcher matcher = jsonPattern.matcher(responseData);
        int datasetCount = 0;
        while (matcher.find()) {
            datasetCount++;
        }

        // Alternative pattern for dataset counting
        if (datasetCount == 0) {
            Pattern alternativePattern = Pattern.compile("\\{[^}]*\"Input\"[^}]*\\}");
            Matcher altMatcher = alternativePattern.matcher(responseData);
            while (altMatcher.find()) {
                datasetCount++;
            }
        }

        testResult.datasetSize = datasetCount;
    }

    private static void analyzeMcpCalls(String responseData, TestResult testResult) {
        String[] mcpPatterns = {"get_markdown", "get_raw_text", "get_rendered_html", "send-email"};
        int mcpCallsCount = 0;

        for (String pattern : mcpPatterns) {
            mcpCallsCount += responseData.split(Pattern.quote(pattern), -1).length - 1;
        }

        testResult.mcpCallsCount = mcpCallsCount;
    }

    private static void analyzeUrlsFetched(String responseData, TestResult testResult) {
        Pattern urlPattern = Pattern.compile("https://www\\.discoveryintech\\.com[^\\s\"]*");
        Matcher urlMatcher = urlPattern.matcher(responseData);
        Set<String> uniqueUrls = new HashSet<>();

        while (urlMatcher.find()) {
            String url = urlMatcher.group().replaceAll("[\"\\s,;]$", ""); // Clean trailing characters
            uniqueUrls.add(url);
        }

        testResult.urlsFetched = new ArrayList<>(uniqueUrls);
    }

    private static void estimateTokensAndCost(String responseData, TestResult testResult) {
        // Estimate tokens (rough estimation: ~4 chars per token)
        testResult.tokensUsed = (int) Math.ceil(responseData.length() / 4.0);

        // Calculate cost (Groq pricing for llama-3.1-8b-instant)
        double inputCostPer1K = 0.05; // cents
        double outputCostPer1K = 0.08; // cents
        double inputTokens = testResult.tokensUsed * 0.7; // 70% input
        double outputTokens = testResult.tokensUsed * 0.3; // 30% output

        testResult.cost = (inputTokens / 1000 * inputCostPer1K) + (outputTokens / 1000 * outputCostPer1K);
    }

    private static void generatePerformanceEvaluation(TestResult testResult) {
        StringBuilder evaluation = new StringBuilder();
        StringBuilder recommendations = new StringBuilder();

        // Response time evaluation
        if (testResult.responseTime <= 1000) {
            evaluation.append("Response Time: Excellent (").append(testResult.responseTime).append("ms)\n");
        } else if (testResult.responseTime <= 3000) {
            evaluation.append("Response Time: Good (").append(testResult.responseTime).append("ms)\n");
        } else if (testResult.responseTime <= 5000) {
            evaluation.append("Response Time: Acceptable (").append(testResult.responseTime).append("ms)\n");
            recommendations.append("- Consider optimizing query processing for better response times\n");
        } else {
            evaluation.append("Response Time: Poor (").append(testResult.responseTime).append("ms)\n");
            recommendations.append("- Response time is too high. Review system performance and optimize\n");
        }

        // Dataset quality evaluation
        if (testResult.datasetSize != null) {
            if (testResult.datasetSize >= 10) {
                evaluation.append("Dataset Size: Excellent (").append(testResult.datasetSize).append(" elements)\n");
            } else if (testResult.datasetSize >= 5) {
                evaluation.append("Dataset Size: Good (").append(testResult.datasetSize).append(" elements)\n");
            } else if (testResult.datasetSize >= 3) {
                evaluation.append("Dataset Size: Acceptable (").append(testResult.datasetSize).append(" elements)\n");
                recommendations.append("- Increase dataset size to at least 5 elements for better quality\n");
            } else {
                evaluation.append("Dataset Size: Poor (").append(testResult.datasetSize).append(" elements)\n");
                recommendations.append("- Dataset size is below minimum requirements. Ensure at least 5 elements\n");
            }
        }

        // MCP calls evaluation
        if (testResult.mcpCallsCount != null) {
            if (testResult.mcpCallsCount >= 3) {
                evaluation.append("MCP Tool Usage: Good (").append(testResult.mcpCallsCount).append(" calls)\n");
            } else if (testResult.mcpCallsCount >= 1) {
                evaluation.append("MCP Tool Usage: Basic (").append(testResult.mcpCallsCount).append(" calls)\n");
                recommendations.append("- Increase MCP tool usage for better data extraction\n");
            } else {
                evaluation.append("MCP Tool Usage: Poor (").append(testResult.mcpCallsCount).append(" calls)\n");
                recommendations.append("- Insufficient MCP tool usage. Ensure proper tool utilization for data fetching\n");
            }
        }

        // Cost evaluation
        if (testResult.cost != null) {
            if (testResult.cost <= 0.05) {
                evaluation.append("Cost Efficiency: Excellent ($").append(String.format("%.4f", testResult.cost)).append(")\n");
            } else if (testResult.cost <= 0.10) {
                evaluation.append("Cost Efficiency: Good ($").append(String.format("%.4f", testResult.cost)).append(")\n");
            } else if (testResult.cost <= 0.20) {
                evaluation.append("Cost Efficiency: Acceptable ($").append(String.format("%.4f", testResult.cost)).append(")\n");
                recommendations.append("- Monitor costs - consider optimizing token usage\n");
            } else {
                evaluation.append("Cost Efficiency: High ($").append(String.format("%.4f", testResult.cost)).append(")\n");
                recommendations.append("- High cost detected - review and optimize query complexity\n");
            }
        }

        // URLs fetched evaluation
        if (testResult.urlsFetched != null && !testResult.urlsFetched.isEmpty()) {
            evaluation.append("Data Sources: Good (").append(testResult.urlsFetched.size()).append(" URLs fetched)\n");
        } else {
            evaluation.append("Data Sources: Poor (no URLs detected)\n");
            recommendations.append("- Ensure proper web data fetching using MCP tools\n");
        }

        testResult.performanceEvaluation = evaluation.toString();
        testResult.recommendations = recommendations.toString();
    }

    public static String compareWithStandards(TestResult testResult, Map<String, Object> standards) {
        StringBuilder comparison = new StringBuilder();
        comparison.append("PERFORMANCE COMPARISON WITH INDUSTRY STANDARDS:\n");
        comparison.append("=".repeat(50)).append("\n");

        // Compare response time with standards
        @SuppressWarnings("unchecked")
        Map<String, Long> responseTimeStds = (Map<String, Long>) standards.get("responseTime");
        if (responseTimeStds != null) {
            comparison.append("Response Time Analysis:\n");
            comparison.append("- Your result: ").append(testResult.responseTime).append("ms\n");
            comparison.append("- Industry excellent: <").append(responseTimeStds.get("excellent")).append("ms\n");
            comparison.append("- Industry good: <").append(responseTimeStds.get("good")).append("ms\n");
            comparison.append("- Industry acceptable: <").append(responseTimeStds.get("acceptable")).append("ms\n");

            if (testResult.responseTime <= responseTimeStds.get("excellent")) {
                comparison.append("- Assessment: Exceeds industry standards ✓\n");
            } else if (testResult.responseTime <= responseTimeStds.get("good")) {
                comparison.append("- Assessment: Meets good industry standards ✓\n");
            } else if (testResult.responseTime <= responseTimeStds.get("acceptable")) {
                comparison.append("- Assessment: Within acceptable range ⚠\n");
            } else {
                comparison.append("- Assessment: Below industry standards ✗\n");
            }
            comparison.append("\n");
        }

        // Compare dataset quality
        @SuppressWarnings("unchecked")
        Map<String, Integer> qualityStds = (Map<String, Integer>) standards.get("datasetQuality");
        if (qualityStds != null && testResult.datasetSize != null) {
            comparison.append("Dataset Quality Analysis:\n");
            comparison.append("- Your result: ").append(testResult.datasetSize).append(" elements\n");
            comparison.append("- Industry minimum: ").append(qualityStds.get("minElements")).append(" elements\n");
            comparison.append("- Industry recommended: ").append(qualityStds.get("recommendedElements")).append(" elements\n");
            comparison.append("- Industry excellent: ").append(qualityStds.get("excellentElements")).append(" elements\n");

            if (testResult.datasetSize >= qualityStds.get("excellentElements")) {
                comparison.append("- Assessment: Exceeds industry standards ✓\n");
            } else if (testResult.datasetSize >= qualityStds.get("recommendedElements")) {
                comparison.append("- Assessment: Meets recommended standards ✓\n");
            } else if (testResult.datasetSize >= qualityStds.get("minElements")) {
                comparison.append("- Assessment: Meets minimum requirements ⚠\n");
            } else {
                comparison.append("- Assessment: Below minimum standards ✗\n");
            }
            comparison.append("\n");
        }

        // Compare cost efficiency
        @SuppressWarnings("unchecked")
        Map<String, Double> costStds = (Map<String, Double>) standards.get("costEfficiency");
        if (costStds != null && testResult.cost != null) {
            comparison.append("Cost Efficiency Analysis:\n");
            comparison.append("- Your result: $").append(String.format("%.4f", testResult.cost)).append("\n");
            comparison.append("- Industry excellent: <$").append(String.format("%.4f", costStds.get("excellent"))).append("\n");
            comparison.append("- Industry good: <$").append(String.format("%.4f", costStds.get("good"))).append("\n");
            comparison.append("- Industry acceptable: <$").append(String.format("%.4f", costStds.get("acceptable"))).append("\n");

            if (testResult.cost <= costStds.get("excellent")) {
                comparison.append("- Assessment: Excellent cost efficiency ✓\n");
            } else if (testResult.cost <= costStds.get("good")) {
                comparison.append("- Assessment: Good cost efficiency ✓\n");
            } else if (testResult.cost <= costStds.get("acceptable")) {
                comparison.append("- Assessment: Acceptable cost efficiency ⚠\n");
            } else {
                comparison.append("- Assessment: High cost - needs optimization ✗\n");
            }
        }

        return comparison.toString();
    }
}