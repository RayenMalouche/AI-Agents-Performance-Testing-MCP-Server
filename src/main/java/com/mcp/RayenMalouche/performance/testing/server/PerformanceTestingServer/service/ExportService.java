package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;
import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.LoadTestResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExportService {

    public static String exportResults(String format, String filepath, boolean includeDetails) throws Exception {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> summary = new HashMap<>();
        Map<String, TestResult> testResults = PerformanceTestService.getAllTestResults();
        Map<String, LoadTestResult> loadTestResults = PerformanceTestService.getAllLoadTestResults();

        summary.put("totalTests", testResults.size());
        summary.put("totalLoadTests", loadTestResults.size());
        summary.put("successfulTests", testResults.values().stream().mapToInt(r -> r.success ? 1 : 0).sum());
        exportData.put("summary", summary);

        if (includeDetails) {
            exportData.put("testResults", new ArrayList<>(testResults.values()));
            exportData.put("loadTestResults", new ArrayList<>(loadTestResults.values()));
        }

        String content;
        switch (format.toLowerCase()) {
            case "json":
                content = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
                break;
            case "csv":
                content = convertToCSV(new ArrayList<>(testResults.values()));
                break;
            case "html":
                content = generateHTMLReport(exportData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Files.write(Paths.get(filepath), content.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return String.format("Results exported successfully to %s (format: %s, size: %d bytes)",
                filepath, format.toUpperCase(), content.getBytes().length);
    }

    private static String convertToCSV(java.util.List<TestResult> results) {
        StringBuilder csv = new StringBuilder();
        csv.append("testId,timestamp,query,responseTime,success,tokensUsed,cost,datasetSize,mcpCallsCount,urlsFetched,errorMessage,performanceEvaluation\n");

        for (TestResult result : results) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",%d,%b,%d,%.4f,%d,%d,\"%s\",\"%s\",\"%s\"\n",
                    escapeCSV(result.testId),
                    escapeCSV(result.timestamp),
                    escapeCSV(result.query),
                    result.responseTime,
                    result.success,
                    result.tokensUsed != null ? result.tokensUsed : 0,
                    result.cost != null ? result.cost : 0.0,
                    result.datasetSize != null ? result.datasetSize : 0,
                    result.mcpCallsCount != null ? result.mcpCallsCount : 0,
                    escapeCSV(String.join("; ", result.urlsFetched)),
                    escapeCSV(result.errorMessage != null ? result.errorMessage : ""),
                    escapeCSV(result.performanceEvaluation != null ? result.performanceEvaluation.replace("\n", " | ") : "")
            ));
        }

        return csv.toString();
    }

    private static String generateHTMLReport(Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Performance Test Report - Discovery Intech MCP Testing</title>
    <style>
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            margin: 0; 
            padding: 40px;
            background-color: #f5f7fa;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 2.5rem;
            font-weight: 300;
        }
        .header p {
            margin: 10px 0 0 0;
            opacity: 0.9;
            font-size: 1.1rem;
        }
        .summary { 
            background: #f8fafc; 
            padding: 30px 40px; 
            border-bottom: 1px solid #e2e8f0;
        }
        .summary h2 {
            color: #2d3748;
            margin-bottom: 20px;
            font-size: 1.5rem;
        }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-top: 20px;
        }
        .metric { 
            background: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            border: 1px solid #e2e8f0;
            transition: transform 0.2s ease;
        }
        .metric:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }
        .metric-value { 
            font-size: 2rem; 
            font-weight: bold; 
            color: #667eea;
            margin-bottom: 5px;
        }
        .metric-label { 
            font-size: 0.9rem; 
            color: #718096; 
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .content {
            padding: 40px;
        }
        table { 
            width: 100%; 
            border-collapse: collapse; 
            margin-top: 20px;
            background: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
        th, td { 
            padding: 12px 16px; 
            text-align: left; 
            border-bottom: 1px solid #e2e8f0;
        }
        th { 
            background-color: #f7fafc;
            font-weight: 600;
            color: #2d3748;
            text-transform: uppercase;
            font-size: 0.85rem;
            letter-spacing: 0.5px;
        }
        tr:hover {
            background-color: #f9f9f9;
        }
        .success { 
            color: #38a169; 
            font-weight: 600;
        }
        .failure { 
            color: #e53e3e; 
            font-weight: 600;
        }
        .status-badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 600;
            text-transform: uppercase;
        }
        .status-success {
            background-color: #c6f6d5;
            color: #22543d;
        }
        .status-failure {
            background-color: #fed7d7;
            color: #742a2a;
        }
        .footer {
            background: #2d3748;
            color: white;
            padding: 20px 40px;
            text-align: center;
            font-size: 0.9rem;
        }
        .performance-indicator {
            display: inline-block;
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.8rem;
            margin-left: 5px;
        }
        .perf-excellent { background: #c6f6d5; color: #22543d; }
        .perf-good { background: #faf089; color: #744210; }
        .perf-acceptable { background: #fed7a1; color: #7b341e; }
        .perf-poor { background: #fed7d7; color: #742a2a; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Performance Test Report</h1>
            <p>Discovery Intech MCP Testing Framework</p>
        </div>
        
        <div class="summary">
            <h2>Executive Summary</h2>
            <div class="metrics-grid">
""");

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) data.get("summary");
        if (summary != null) {
            html.append(String.format("""
                <div class="metric">
                    <div class="metric-value">%s</div>
                    <div class="metric-label">Total Tests</div>
                </div>
                <div class="metric">
                    <div class="metric-value">%s</div>
                    <div class="metric-label">Load Tests</div>
                </div>
                <div class="metric">
                    <div class="metric-value">%s</div>
                    <div class="metric-label">Successful Tests</div>
                </div>
                <div class="metric">
                    <div class="metric-value">%.1f%%</div>
                    <div class="metric-label">Success Rate</div>
                </div>
""",
                    summary.getOrDefault("totalTests", 0),
                    summary.getOrDefault("totalLoadTests", 0),
                    summary.getOrDefault("successfulTests", 0),
                    calculateSuccessRate(summary)
            ));
        }

        html.append("""
            </div>
        </div>
        
        <div class="content">
            <h3>Test Results Overview</h3>
            <p>This report was generated on """).append(data.get("exportTimestamp")).append("""
            and includes comprehensive performance metrics for the Discovery Intech MCP testing framework.</p>
            
            <h4>Key Performance Indicators</h4>
            <ul>
                <li><strong>Response Time:</strong> Measured end-to-end request processing time</li>
                <li><strong>Dataset Quality:</strong> Number of JSON elements generated per request</li>
                <li><strong>MCP Tool Usage:</strong> Effectiveness of Model Context Protocol tool utilization</li>
                <li><strong>Cost Efficiency:</strong> Token usage and associated costs</li>
                <li><strong>Email Notifications:</strong> Successful completion notifications</li>
            </ul>
        </div>
        
        <div class="footer">
            <p>Generated by Discovery Intech MCP Performance Testing Framework | """)
                .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("""
            </p>
            <p>For technical support, contact the development team</p>
        </div>
    </div>
</body>
</html>""");

        return html.toString();
    }

    private static double calculateSuccessRate(Map<String, Object> summary) {
        int total = (Integer) summary.getOrDefault("totalTests", 0);
        int successful = (Integer) summary.getOrDefault("successfulTests", 0);
        return total > 0 ? (successful * 100.0 / total) : 0.0;
    }

    private static String escapeCSV(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
    }
}