package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.util.ArrayList;
import java.util.HashSet;
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
                recommendations.append("- Increase MCP tool usage for better data extraction