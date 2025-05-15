package com.example.ffmpegmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for the advanced FFmpeg MCP server. These tests verify the additional
 * functionality of the advanced server.
 */
public class FFmpegMcpServerAdvancedTest {

	private ObjectMapper objectMapper;

	private PipedOutputStream clientToServer;

	private PipedInputStream serverToClient;

	private ByteArrayOutputStream serverOutput;

	@BeforeEach
	public void setup() throws IOException {
		objectMapper = new ObjectMapper();

		// Setup pipes for communication
		clientToServer = new PipedOutputStream();
		PipedInputStream serverInput = new PipedInputStream(clientToServer);

		PipedOutputStream serverToClientOutput = new PipedOutputStream();
		serverToClient = new PipedInputStream(serverToClientOutput);

		serverOutput = new ByteArrayOutputStream();
		OutputStream testOutput = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				serverOutput.write(b);
				serverToClientOutput.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				serverOutput.write(b, off, len);
				serverToClientOutput.write(b, off, len);
			}
		};

		// Create mock FFmpegWrapper to avoid real FFmpeg calls
		// mockFFmpegWrapper();

		// Start server in a separate thread
		Thread serverThread = new Thread(() -> {
			try {
				// Need to create server with custom I/O streams for testing
				new FFmpegMcpServerFactory().createServer(serverInput, testOutput);

				// This thread will keep running as the server processes messages
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				// Expected when shutting down test
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();

		// Give the server time to initialize
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Mock the FFmpegWrapper to avoid real FFmpeg calls.
	 */
	private void mockFFmpegWrapper() {
		try (var mocked = mockStatic(FFmpegWrapper.class)) {
			mocked.when(() -> FFmpegWrapper.performFFMPEG(anyString())).thenAnswer(inv -> {
				String command = inv.getArgument(0);
				if (command.contains("error_trigger")) {
					throw new IOException("Simulated FFmpeg error");
				}
				return "Mock output for: " + command;
			});
		}
	}

	/**
	 * Helper method to simulate sending a JSON-RPC request to the server.
	 */
	private void sendRequest(String method, Object params, String id) throws IOException {
		McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", method, id, params);

		String json = objectMapper.writeValueAsString(request);
		clientToServer.write((json + "\n").getBytes());
		clientToServer.flush();

		// Give server time to process
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Test
	public void testListTools() throws IOException {
		// First initialize
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));

		sendRequest("initialize", initRequest, "1");

		// Then list tools
		sendRequest("tools/list", null, "2");

		// Verify the response contains all three tools
		String response = serverOutput.toString();
		assertThat(response).contains("ffmpeg");
		assertThat(response).contains("video_info");
		assertThat(response).contains("register_video");
	}

	@Test
	public void testRegisterVideoAndUse() throws IOException {
		// First initialize
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));

		sendRequest("initialize", initRequest, "1");

		// Register a video
		var registerParams = Map.of("name", "register_video", "arguments",
				Map.of("name", "myvideo", "path", "/tmp/vids/wZ5.mp4"));

		sendRequest("tools/call", registerParams, "2");

		// Then use the registered video in an ffmpeg command
		var ffmpegParams = Map.of("name", "ffmpeg", "arguments",
				Map.of("command", "convert {{myvideo}} to output.mp4"));

		sendRequest("tools/call", ffmpegParams, "3");

		// Verify the response shows the registered path was used
		String response = serverOutput.toString();
		assertThat(response).contains("Successfully registered video");
		assertThat(response).contains("/path/to/video.mp4");
	}

	@Test
	public void testVideoInfo() throws IOException {
		// First initialize
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));

		sendRequest("initialize", initRequest, "1");

		// Use the video_info tool
		var infoParams = Map.of("name", "video_info", "arguments", Map.of("videoref", "/path/to/video.mp4"));

		sendRequest("tools/call", infoParams, "2");

		// Verify the response
		String response = serverOutput.toString();
		assertThat(response).contains("Video Information");
		assertThat(response).contains("/path/to/video.mp4");
	}

	/**
	 * Factory to help create servers for testing.
	 */
	private static class FFmpegMcpServerFactory {

		public FFmpegMcpServerAdvanced createServer(InputStream input, OutputStream output) {
			ObjectMapper mapper = new ObjectMapper();
			io.modelcontextprotocol.server.transport.StdioServerTransportProvider provider = new io.modelcontextprotocol.server.transport.StdioServerTransportProvider(
					mapper, input, output);
			return new FFmpegMcpServerAdvanced(provider);
		}

	}

}