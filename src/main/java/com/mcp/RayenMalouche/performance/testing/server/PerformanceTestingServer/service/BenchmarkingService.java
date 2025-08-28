package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service;

import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.model.TestResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BenchmarkingService {

    private static final Map<String, String> PREDEFINED_SCENARIOS = new HashMap<>();

    static {
        PREDEFINED_SCENARIOS.put("sage", "Créez un dataset complet sur toutes les solutions Sage de Discovery Intech. IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
        PREDEFINED_SCENARIOS.put("qad", "Générez un dataset détaillé sur les solutions QAD proposées par Discovery Intech. IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
        PREDEFINED_SCENARIOS.put("microsoft", "Produisez un dataset sur les solutions Microsoft Dynamics 365 de Discovery Intech. IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
        PREDEFINED_SCENARIOS.put("sap", "Créez un dataset sur les solutions SAP proposées par Discovery Intech. IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
        PREDEFINED_SCENARIOS.put("sectors", "Générez un dataset sur tous les secteurs d'activité couverts par Discovery Intech. IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
        PREDEFINED_SCENARIOS.put("services", "Créez un dataset sur tous les services proposés par Discovery Intech. IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
        PREDEFINED_SCENARIOS.put("company", "Produisez un dataset sur l'entreprise Discovery Intech (équipe, partenaires, références). IMPERATIVE: You must send an email notification to rayenmalouche@gmail.com after completing the dataset.");
    }

    public static Map<String, Object> benchmarkScenarios(List<String> scenarios, String targetUrl) {
        List<String> scenariosToRun;
        if (scenarios.contains("all")) {
            scenariosToRun = new ArrayList<>(PREDEFINED_SCENARIOS.keySet());
        } else {
            scenariosToRun = scenarios.stream()
                    .filter(PREDEFINED_SCENARIOS::containsKey)
                    .collect(Collectors.toList());
        }

        String benchmarkId = "benchmark_" + System.currentTimeMillis();
        List<Map<String, Object>> results = new ArrayList<>();

        for (String scenario : scenariosToRun) {
            String query = PREDEFINED_SCENARIOS.get(scenario);
            String testId = benchmarkId + "_" + scenario;

            try {
                TestResult testResult = PerformanceTestService.runPerformanceTest(query, targetUrl, testId);
                Map<String, Object> scenarioResult = createScenarioResult(scenario, testResult);
                results.add(scenarioResult);
            } catch (Exception e) {
                Map<String, Object> scenarioResult = new HashMap<>();
                scenarioResult.put("scenario", scenario);
                scenarioResult.put("testId", testId);
                scenarioResult.put("success", false);
                scenarioResult.put("error", e.getMessage());
                results.add(scenarioResult);
            }
        }

        return createBenchmarkSummary(benchmarkId, results);
    }

    private static Map<String, Object> createScenarioResult(String scenario, TestResult testResult) {
        Map<String, Object> scenarioResult = new HashMap<>();
        scenarioResult.put("scenario", scenario);
        scenarioResult.put("testId", testResult.testId);
        scenarioResult.put("success", testResult.success);
        scenarioResult.put("responseTime", testResult.responseTime);
        scenarioResult.put("tokensUsed", testResult.tokensUsed);
        scenarioResult.put("cost", testResult.cost);
        scenarioResult.put("datasetSize", testResult.datasetSize);
        scenarioResult.put("mcpCallsCount", testResult.mcpCallsCount);
        scenarioResult.put("urlsFetched", testResult.urlsFetched);
        scenarioResult.put("performanceEvaluation", testResult.performanceEvaluation);
        scenarioResult.put("recommendations", testResult.recommendations);

        if (!testResult.success) {
            scenarioResult.put("error", testResult.errorMessage);
        }

        return scenarioResult;
    }

    private static Map<String, Object> createBenchmarkSummary(String benchmarkId, List<Map<String, Object>> results) {
        List<Map<String, Object>> successfulResults = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("success")))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        if (!successfulResults.isEmpty()) {
            summary.put("averageResponseTime", successfulResults.stream()
                    .mapToDouble(r -> ((Number) r.get("responseTime")).doubleValue())
                    .average().orElse(0.0));

            summary.put("totalTokens", successfulResults.stream()
                    .mapToInt(r -> r.get("tokensUsed") != null ? ((Number) r.get("tokensUsed")).intValue() : 0)
                    .sum());

            summary.put("totalCost", successfulResults.stream()
                    .mapToDouble(r -> r.get("cost") != null ? ((Number) r.get("cost")).doubleValue() : 0.0)
                    .sum());

            summary.put("averageDatasetSize", successfulResults.stream()
                    .mapToDouble(r -> r.get("datasetSize") != null ? ((Number) r.get("datasetSize")).doubleValue() : 0)
                    .average().orElse(0.0));

            // Generate overall recommendations
            List<String> overallRecommendations = generateOverallRecommendations(successfulResults);
            summary.put("recommendations", overallRecommendations);
        }

        Map<String, Object> benchmarkResult = new HashMap<>();
        benchmarkResult.put("benchmarkId", benchmarkId);
        benchmarkResult.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        benchmarkResult.put("totalScenarios", results.size());
        benchmarkResult.put("successfulScenarios", successfulResults.size());
        benchmarkResult.put("failedScenarios", results.size() - successfulResults.size());
        benchmarkResult.put("results", results);
        benchmarkResult.put("summary", summary);

        return benchmarkResult;
    }

    private static List<String> generateOverallRecommendations(List<Map<String, Object>> successfulResults) {
        List<String> recommendations = new ArrayList<>();

        // Analyze average response time
        double avgResponseTime = successfulResults.stream()
                .mapToDouble(r -> ((Number) r.get("responseTime")).doubleValue())
                .average().orElse(0.0);

        if (avgResponseTime > 3000) {
            recommendations.add("Overall response time is high - consider system optimization");
        }

        // Analyze average dataset size
        double avgDatasetSize = successfulResults.stream()
                .mapToDouble(r -> r.get("datasetSize") != null ? ((Number) r.get("datasetSize")).doubleValue() : 0)
                .average().orElse(0.0);

        if (avgDatasetSize < 5) {
            recommendations.add("Average dataset size is below recommended minimum - improve content generation");
        }

        // Analyze cost efficiency
        double totalCost = successfulResults.stream()
                .mapToDouble(r -> r.get("cost") != null ? ((Number) r.get("cost")).doubleValue() : 0.0)
                .sum();

        if (totalCost > 0.5) {
            recommendations.add("Total benchmark cost is high - consider cost optimization strategies");
        }

        // Check for email notifications
        long emailIssues = successfulResults.stream()
                .mapToLong(r -> {
                    String recs = (String) r.get("recommendations");
                    return recs != null && recs.contains("email") ? 1 : 0;
                })
                .sum();

        if (emailIssues > 0) {
            recommendations.add("Email notification issues detected in " + emailIssues + " scenarios - verify SMTP configuration");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("All scenarios performed within acceptable parameters");
        }

        return recommendations;
    }

    public static Map<String, String> getAvailableScenarios() {
        return new HashMap<>(PREDEFINED_SCENARIOS);
    }
}