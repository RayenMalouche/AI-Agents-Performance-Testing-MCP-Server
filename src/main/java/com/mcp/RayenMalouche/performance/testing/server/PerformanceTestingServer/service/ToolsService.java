package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolsService {

    public static McpServerFeatures.SyncToolSpecification createPerformanceTestTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "run_performance_test",
                        "Run a single performance test against the Discovery Intech MCP client",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "query": {
                              "type": "string",
                              "description": "The query to test"
                            },
                            "targetUrl": {
                              "type": "string",
                              "description": "Target Discovery client URL (default: http://localhost:8072)"
                            },
                            "testId": {
                              "type": "string",
                              "description": "Unique test identifier"
                            }
                          },
                          "required": ["query"]
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        String query = (String) params.get("query");
                        String targetUrl = (String) params.getOrDefault("targetUrl", "http://localhost:8072");
                        String testId = (String) params.getOrDefault("testId", "test_" + System.currentTimeMillis());

                        System.err.printf("Executing performance test: testId=%s, query=%s%n", testId, query);

                        var result = PerformanceTestService.runPerformanceTest(query, targetUrl, testId);

                        Map<String, Object> response = new HashMap<>();
                        response.put("testId", result.testId);
                        response.put("success", result.success);
                        response.put("responseTime", result.responseTime);
                        response.put("tokensUsed", result.tokensUsed);
                        response.put("cost", result.cost);
                        response.put("datasetSize", result.datasetSize);
                        response.put("mcpCallsCount", result.mcpCallsCount);
                        response.put("urlsFetched", result.urlsFetched);
                        response.put("performanceEvaluation", result.performanceEvaluation);
                        response.put("recommendations", result.recommendations);
                        if (!result.success) {
                            response.put("error", result.errorMessage);
                        }

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(new ObjectMapper().writeValueAsString(response))),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in run_performance_test tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    public static McpServerFeatures.SyncToolSpecification createLoadTestTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "run_load_test",
                        "Run a load test with multiple concurrent requests",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "queries": {
                              "type": "array",
                              "items": {"type": "string"},
                              "description": "Array of queries to test"
                            },
                            "concurrentUsers": {
                              "type": "number",
                              "description": "Number of concurrent users",
                              "default": 5
                            },
                            "requestsPerUser": {
                              "type": "number",
                              "description": "Number of requests per user",
                              "default": 2
                            },
                            "targetUrl": {
                              "type": "string",
                              "description": "Target Discovery client URL"
                            }
                          },
                          "required": ["queries"]
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> queries = (List<String>) params.get("queries");
                        int concurrentUsers = ((Number) params.getOrDefault("concurrentUsers", 5)).intValue();
                        int requestsPerUser = ((Number) params.getOrDefault("requestsPerUser", 2)).intValue();
                        String targetUrl = (String) params.getOrDefault("targetUrl", "http://localhost:8072");

                        System.err.printf("Executing load test: users=%d, requests=%d%n", concurrentUsers, requestsPerUser);

                        var result = PerformanceTestService.runLoadTest(queries, concurrentUsers, requestsPerUser, targetUrl);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(new ObjectMapper().writeValueAsString(result))),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in run_load_test tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    public static McpServerFeatures.SyncToolSpecification createAnalyzeResultsTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "analyze_test_results",
                        "Analyze and summarize test results with performance evaluation",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "testIds": {
                              "type": "array",
                              "items": {"type": "string"},
                              "description": "Specific test IDs to analyze (optional)"
                            },
                            "generateReport": {
                              "type": "boolean",
                              "description": "Generate a detailed report file",
                              "default": false
                            },
                            "compareWithStandards": {
                              "type": "boolean",
                              "description": "Compare results with industry standards",
                              "default": false
                            }
                          }
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> testIds = (List<String>) params.get("testIds");
                        boolean generateReport = Boolean.TRUE.equals(params.get("generateReport"));
                        boolean compareWithStandards = Boolean.TRUE.equals(params.get("compareWithStandards"));

                        Map<String, Object> analysis = AnalysisService.analyzeTestResults(testIds, generateReport, compareWithStandards);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(new ObjectMapper().writeValueAsString(analysis))),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in analyze_test_results tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    public static McpServerFeatures.SyncToolSpecification createMonitorCostsTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "monitor_costs",
                        "Monitor and track API costs with recommendations",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "timeframe": {
                              "type": "string",
                              "description": "Timeframe for cost analysis (hour, day, week)",
                              "default": "day"
                            },
                            "modelName": {
                              "type": "string",
                              "description": "AI model name for cost calculation",
                              "default": "llama-3.1-8b-instant"
                            }
                          }
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        String timeframe = (String) params.getOrDefault("timeframe", "day");
                        String modelName = (String) params.getOrDefault("modelName", "llama-3.1-8b-instant");

                        Map<String, Object> costAnalysis = CostMonitoringService.monitorCosts(timeframe, modelName);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(new ObjectMapper().writeValueAsString(costAnalysis))),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in monitor_costs tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    public static McpServerFeatures.SyncToolSpecification createBenchmarkScenariosTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "benchmark_scenarios",
                        "Run predefined benchmark scenarios for Discovery Intech with performance evaluation",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "scenarios": {
                              "type": "array",
                              "items": {
                                "type": "string",
                                "enum": ["sage", "qad", "microsoft", "sap", "sectors", "services", "company", "all"]
                              },
                              "description": "Scenarios to benchmark",
                              "default": ["all"]
                            },
                            "targetUrl": {
                              "type": "string",
                              "description": "Target Discovery client URL"
                            }
                          }
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> scenarios = (List<String>) params.getOrDefault("scenarios", List.of("all"));
                        String targetUrl = (String) params.getOrDefault("targetUrl", "http://localhost:8072");

                        Map<String, Object> benchmarkResult = BenchmarkingService.benchmarkScenarios(scenarios, targetUrl);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(new ObjectMapper().writeValueAsString(benchmarkResult))),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in benchmark_scenarios tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    public static McpServerFeatures.SyncToolSpecification createExportResultsTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "export_results",
                        "Export test results to various formats with comprehensive reporting",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "format": {
                              "type": "string",
                              "enum": ["json", "csv", "html"],
                              "description": "Export format",
                              "default": "json"
                            },
                            "filepath": {
                              "type": "string",
                              "description": "Output file path"
                            },
                            "includeDetails": {
                              "type": "boolean",
                              "description": "Include detailed test data",
                              "default": true
                            }
                          },
                          "required": ["filepath"]
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        String format = (String) params.getOrDefault("format", "json");
                        String filepath = (String) params.get("filepath");
                        boolean includeDetails = !Boolean.FALSE.equals(params.get("includeDetails"));

                        String result = ExportService.exportResults(format, filepath, includeDetails);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(result)),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in export_results tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    public static McpServerFeatures.SyncToolSpecification createCompareWithStandardsTool() {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "compare_with_standards",
                        "Compare test results with industry performance standards and provide recommendations",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "testId": {
                              "type": "string",
                              "description": "Test ID to compare with standards"
                            },
                            "responseTime": {
                              "type": "number",
                              "description": "Response time in milliseconds"
                            },
                            "tokensUsed": {
                              "type": "number",
                              "description": "Number of tokens used"
                            },
                            "cost": {
                              "type": "number",
                              "description": "Cost in dollars"
                            },
                            "datasetSize": {
                              "type": "number",
                              "description": "Number of dataset elements generated"
                            },
                            "success": {
                              "type": "boolean",
                              "description": "Whether the test was successful",
                              "default": true
                            }
                          }
                        }
                        """
                ),
                (exchange, params) -> {
                    try {
                        String testId = (String) params.get("testId");
                        Long responseTime = params.get("responseTime") != null ?
                                ((Number) params.get("responseTime")).longValue() : null;
                        Integer tokensUsed = params.get("tokensUsed") != null ?
                                ((Number) params.get("tokensUsed")).intValue() : null;
                        Double cost = params.get("cost") != null ?
                                ((Number) params.get("cost")).doubleValue() : null;
                        Integer datasetSize = params.get("datasetSize") != null ?
                                ((Number) params.get("datasetSize")).intValue() : null;
                        Boolean success = (Boolean) params.getOrDefault("success", true);

                        Map<String, Object> comparison = StandardsComparisonService.compareWithStandards(
                                testId, responseTime, tokensUsed, cost, datasetSize, success);

                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(new ObjectMapper().writeValueAsString(comparison))),
                                false
                        );
                    } catch (Exception e) {
                        System.err.println("ERROR in compare_with_standards tool: " + e.getMessage());
                        e.printStackTrace(System.err);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        String.format("{\"status\": \"error\", \"message\": \"%s\"}",
                                                escapeJsonString(e.getMessage()))
                                )),
                                true
                        );
                    }
                }
        );
    }

    private static String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}