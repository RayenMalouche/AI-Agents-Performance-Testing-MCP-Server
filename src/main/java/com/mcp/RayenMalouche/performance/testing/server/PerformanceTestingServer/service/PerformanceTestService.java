package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;
import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.LoadTestResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PerformanceTestService {

    private static final Map<String, TestResult> testResults = new ConcurrentHashMap<>();
    private static final Map<String, LoadTestResult> loadTestResults = new ConcurrentHashMap<>();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public static TestResult runPerformanceTest(String query, String targetUrl, String testId) {
        TestResult testResult = new TestResult(testId, query);
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", query +
                    "\n\nIMPERATIVE: After creating the dataset, you MUST send an email notification using the send-email tool with these exact parameters:" +
                    "\n- to: rayenmalouche@gmail.com" +
                    "\n- subject: Dataset for [topic] created successfully" +
                    "\n- body: Dataset has been generated with [number] elements on [topic]. Generation completed at [current timestamp].");
            requestBody.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl + "/discovery-ai/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long endTime = System.currentTimeMillis();
            testResult.responseTime = endTime - startTime;
            testResult.success = response.statusCode() >= 200 && response.statusCode() < 300;

            if (testResult.success) {
                ResponseAnalysisService.analyzeResponseData(response.body(), testResult);
            } else {
                testResult.errorMessage = "HTTP " + response.statusCode() + ": " + response.body();
            }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            testResult.responseTime = endTime - startTime;
            testResult.success = false;
            testResult.errorMessage = e.getMessage();
        }

        testResults.put(testId, testResult);
        return testResult;
    }

    public static LoadTestResult runLoadTest(List<String> queries, int concurrentUsers, int requestsPerUser, String targetUrl) {
        String loadTestId = "load_test_" + System.currentTimeMillis();
        Instant startTime = Instant.now();

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<CompletableFuture<TestResult>> futures = new ArrayList<>();

        // Create test tasks
        for (int user = 0; user < concurrentUsers; user++) {
            for (int req = 0; req < requestsPerUser; req++) {
                String query = queries.get(req % queries.size());
                String testId = String.format("%s_user%d_req%d", loadTestId, user, req);

                CompletableFuture<TestResult> future = CompletableFuture.supplyAsync(() ->
                        runPerformanceTest(query, targetUrl, testId), executor);
                futures.add(future);
            }
        }

        // Wait for all tests to complete
        List<TestResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        executor.shutdown();
        Instant endTime = Instant.now();

        // Process results
        List<TestResult> successfulTests = results.stream()
                .filter(r -> r.success)
                .collect(Collectors.toList());

        LoadTestResult loadTestResult = new LoadTestResult();
        loadTestResult.testId = loadTestId;
        loadTestResult.startTime = startTime.toString();
        loadTestResult.endTime = endTime.toString();
        loadTestResult.totalTests = results.size();
        loadTestResult.successfulTests = successfulTests.size();
        loadTestResult.failedTests = results.size() - successfulTests.size();
        loadTestResult.concurrentUsers = concurrentUsers;
        loadTestResult.requestsPerUser = requestsPerUser;

        if (!successfulTests.isEmpty()) {
            loadTestResult.averageResponseTime = successfulTests.stream()
                    .mapToLong(r -> r.responseTime)
                    .average()
                    .orElse(0.0);

            loadTestResult.totalTokens = successfulTests.stream()
                    .mapToInt(r -> r.tokensUsed != null ? r.tokensUsed : 0)
                    .sum();

            loadTestResult.totalCost = successfulTests.stream()
                    .mapToDouble(r -> r.cost != null ? r.cost : 0.0)
                    .sum();
        }

        loadTestResults.put(loadTestId, loadTestResult);
        return loadTestResult;
    }

    public static Map<String, TestResult> getAllTestResults() {
        return new HashMap<>(testResults);
    }

    public static Map<String, LoadTestResult> getAllLoadTestResults() {
        return new HashMap<>(loadTestResults);
    }

    public static void clearAllResults() {
        testResults.clear();
        loadTestResults.clear();
    }
}