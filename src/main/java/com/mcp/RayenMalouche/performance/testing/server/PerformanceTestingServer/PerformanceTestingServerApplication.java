package com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.RayenMalouche.performance.testing.server.PerformanceTestingServer.service.*;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceTestingServerApplication {

	public static void main(String[] args) throws Exception {
		boolean useStdio = args.length > 0 && "--stdio".equals(args[0]);
		boolean useStreamableHttp = args.length > 0 && "--streamable-http".equals(args[0]);

		if (useStdio) {
			System.err.println("Starting Performance Test MCP server with STDIO transport...");
			startStdioServer();
		} else {
			System.out.println("Starting Performance Test MCP server with HTTP/SSE transport...");
			startHttpServer(useStreamableHttp);
		}
	}

	private static void startStdioServer() {
		try {
			System.err.println("Initializing STDIO Performance Test MCP server...");

			StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(new ObjectMapper());

			McpSyncServer syncServer = McpServer.sync(transportProvider)
					.serverInfo("performance-test-server", "1.0.0")
					.capabilities(McpSchema.ServerCapabilities.builder()
							.tools(true)
							.logging()
							.build())
					.tools(
							ToolsService.createPerformanceTestTool(),
							ToolsService.createLoadTestTool(),
							ToolsService.createAnalyzeResultsTool(),
							ToolsService.createMonitorCostsTool(),
							ToolsService.createBenchmarkScenariosTool(),
							ToolsService.createExportResultsTool(),
							ToolsService.createCompareWithStandardsTool()
					)
					.build();

			System.err.println("Performance Test MCP server started. Awaiting requests...");

		} catch (Exception e) {
			System.err.println("Fatal error in STDIO server: " + e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	private static void startHttpServer(boolean streamableHttp) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		HttpServletSseServerTransportProvider transportProvider;
		if (streamableHttp) {
			transportProvider = new HttpServletSseServerTransportProvider(objectMapper, "/message", "/sse");
		} else {
			transportProvider = new HttpServletSseServerTransportProvider(objectMapper, "/", "/sse");
		}

		McpSyncServer syncServer = McpServer.sync(transportProvider)
				.serverInfo("performance-test-server", "1.0.0")
				.capabilities(McpSchema.ServerCapabilities.builder()
						.tools(true)
						.logging()
						.build())
				.tools(
						ToolsService.createPerformanceTestTool(),
						ToolsService.createLoadTestTool(),
						ToolsService.createAnalyzeResultsTool(),
						ToolsService.createMonitorCostsTool(),
						ToolsService.createBenchmarkScenariosTool(),
						ToolsService.createExportResultsTool(),
						ToolsService.createCompareWithStandardsTool()
				)
				.build();

		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("performance-test-server");

		Server server = new Server(threadPool);
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(45451);
		server.addConnector(connector);

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		context.addServlet(new ServletHolder(transportProvider), "/*");
		context.addServlet(new ServletHolder(new HealthServlet()), "/api/health");

		server.setHandler(context);
		server.start();

		System.err.println("=================================");
		System.err.println("Performance Test MCP Server started on port 45451");
		if (streamableHttp) {
			System.err.println("Mode: Streamable HTTP (for MCP Inspector)");
			System.err.println("MCP endpoint: http://localhost:45451/message");
		} else {
			System.err.println("Mode: Standard HTTP/SSE");
			System.err.println("MCP endpoint: http://localhost:45451/");
		}
		System.err.println("SSE endpoint: http://localhost:45451/sse");
		System.err.println("Health check: http://localhost:45451/api/health");
		System.err.println("=================================");
		server.join();
	}

	// Health check servlet
	public static class HealthServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			sendHealthResponse(resp);
		}

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			sendHealthResponse(resp);
		}

		private void sendHealthResponse(HttpServletResponse resp) throws IOException {
			resp.setContentType("application/json");
			resp.setHeader("Access-Control-Allow-Origin", "*");

			Map<String, Object> healthStatus = new HashMap<>();
			healthStatus.put("status", "healthy");
			healthStatus.put("server", "Performance Test MCP Server");
			healthStatus.put("version", "1.0.0");
			healthStatus.put("features", List.of("performance_testing", "load_testing", "benchmarking", "standards_comparison"));
			healthStatus.put("timestamp", java.time.Instant.now().toString());

			String jsonResponse = new ObjectMapper().writeValueAsString(healthStatus);
			resp.getWriter().write(jsonResponse);
		}
	}
}