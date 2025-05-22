package com.example.ffmpegmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import no.lau.mcp.ffmpeg.FFmpegExecutor;
import no.lau.mcp.ffmpeg.FFmpegFake;
import no.lau.mcp.ffmpeg.FFmpegMcpServerAdvanced;
import no.lau.mcp.ffmpeg.FFmpegWrapper;
import no.lau.mcp.file.FileManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that actually starts an FFmpeg MCP server and sends requests to it.
 */
@Disabled // Complex integration test - recommend using WorkingMcpClientTest pattern instead  
public class FFmpegServerIntegrationTest {

	private PipedOutputStream clientToServer;

	private PipedInputStream clientFromServer;

	private PipedOutputStream serverToClient;

	private ByteArrayOutputStream serverOutput;

	private ExecutorService serverThread;

	private ObjectMapper objectMapper;

	private FFmpegMcpServerAdvanced server;


	@BeforeEach
	public void setup() throws IOException, InterruptedException {
		objectMapper = new ObjectMapper();

		// Setup pipes for bidirectional communication
		clientToServer = new PipedOutputStream();
		PipedInputStream serverInput = new PipedInputStream(clientToServer);

		serverToClient = new PipedOutputStream();
		clientFromServer = new PipedInputStream(serverToClient);

		serverOutput = new ByteArrayOutputStream();
		OutputStream testOutput = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				serverOutput.write(b);
				serverToClient.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				serverOutput.write(b, off, len);
				serverToClient.write(b, off, len);
			}
		};

		FFmpegExecutor executorMock = new FFmpegFake();
		FileManager fileManagerMock = new FileManagerFake(Map.of());

		// Start the server in a separate thread
		serverThread = Executors.newSingleThreadExecutor();
		serverThread.submit(() -> {
			try {
				// Create a custom transport provider for the test
				StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(new ObjectMapper(),
						serverInput, testOutput);

				// Create and start the server
				server = new FFmpegMcpServerAdvanced(transportProvider, new FFmpegWrapper(fileManagerMock, executorMock));
				server.start();

				// Block this thread to keep the server running
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				// Expected when test is finished
			}
			return null;
		});

		// Give the server time to initialize
		Thread.sleep(500);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (serverThread != null) {
			serverThread.shutdownNow();
		}
		if (server != null) {
			server.shutdown();
		}
		if (clientToServer != null)
			clientToServer.close();
		if (clientFromServer != null)
			clientFromServer.close();
		if (serverToClient != null)
			serverToClient.close();
	}

	/**
	 * Test the complete flow of using the FFmpeg MCP server.
	 */
	@Test
	public void testFullFFmpegServerFlow() throws Exception {
		// Step 1: Initialize the server
		String initializeResponse = sendRequestAndWaitForResponse(createInitRequest());
		assertThat(initializeResponse).contains("\"result\"").contains("ffmpeg-mcp-server");

		// Step 2: List available tools
		String toolsResponse = sendRequestAndWaitForResponse(createListToolsRequest());
		assertThat(toolsResponse).contains("\"result\"")
			.contains("ffmpeg")
			.contains("video_info")
			.contains("register_video");

		// Step 3: Register a video
		String registerResponse = sendRequestAndWaitForResponse(
				createRegisterVideoRequest("testVideo", "/tmp/video.mp4"));
		assertThat(registerResponse).contains("\"result\"").contains("Successfully registered");

		// Step 4: Call FFmpeg to extract a clip
		String ffmpegResponse = sendRequestAndWaitForResponse(
				createExtractClipRequest("testVideo", "/tmp/output.mp4", "00:00:10", "00:00:03"));
		assertThat(ffmpegResponse).contains("\"result\"").contains("Mock FFmpeg output");
	}

	/**
	 * Send a request and wait for the response.
	 */
	private String sendRequestAndWaitForResponse(String request) throws Exception {
		// Create a latch to wait for the response
		CountDownLatch latch = new CountDownLatch(1);

		// Start a thread to read the response
		StringBuilder responseBuilder = new StringBuilder();
		Thread responseThread = new Thread(() -> {
			try {
				byte[] buffer = new byte[4096];
				int bytesRead;

				// Wait for the response
				while ((bytesRead = clientFromServer.read(buffer)) != -1) {
					String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
					responseBuilder.append(response);

					// If we have a complete response, release the latch
					if (response.contains("\"result\"") || response.contains("\"error\"")) {
						latch.countDown();
						break;
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});
		responseThread.start();

		// Send the request
		clientToServer.write((request + "\n").getBytes(StandardCharsets.UTF_8));
		clientToServer.flush();

		// Wait for the response with a timeout
		boolean received = latch.await(5, TimeUnit.SECONDS);
		if (!received) {
			throw new AssertionError("Timed out waiting for response");
		}

		return responseBuilder.toString();
	}

	// Methods to create JSON-RPC requests

	private String createInitRequest() throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "initialize");

		Map<String, Object> params = new HashMap<>();
		params.put("protocolVersion", "2024-11-05");

		Map<String, Object> clientInfo = new HashMap<>();
		clientInfo.put("name", "test-client");
		clientInfo.put("version", "1.0.0");
		params.put("clientInfo", clientInfo);

		Map<String, Object> caps = new HashMap<>();
		params.put("capabilities", caps);

		request.put("params", params);

		return objectMapper.writeValueAsString(request);
	}

	private String createListToolsRequest() throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/list");

		return objectMapper.writeValueAsString(request);
	}

	private String createRegisterVideoRequest(String name, String path) throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/call");

		Map<String, Object> params = new HashMap<>();
		params.put("name", "register_video");

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("name", name);
		arguments.put("path", path);
		params.put("arguments", arguments);

		request.put("params", params);

		return objectMapper.writeValueAsString(request);
	}

	private String createExtractClipRequest(String videoRef, String outputFile, String startTime, String duration)
			throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/call");

		Map<String, Object> params = new HashMap<>();
		params.put("name", "ffmpeg");

		Map<String, Object> arguments = new HashMap<>();
		String command = String.format("-i {{%s}} -ss %s -t %s -c:v copy -c:a copy %s", videoRef, startTime, duration,
				outputFile);
		arguments.put("command", command);
		params.put("arguments", arguments);

		request.put("params", params);

		return objectMapper.writeValueAsString(request);
	}

	private String generateId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

}