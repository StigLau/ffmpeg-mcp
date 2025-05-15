package com.example.ffmpegmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import no.lau.mcp.ffmpeg.FFmpegMcpServer;
import no.lau.mcp.ffmpeg.FFmpegWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for FFmpegMcpServer. These tests verify the functionality of the MCP server
 * by simulating client requests.
 */
public class FFmpegMcpServerTest {

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
	public void testInitialize() throws IOException {
		// Send initialize request
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(Map.of(), null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));

		sendRequest("initialize", initRequest, "1");

		// Check that the response contains the expected server info
		String response = serverOutput.toString();
		assertThat(response).contains("ffmpeg-mcp-server");
		assertThat(response).contains("1.0.0");
	}

	@Test
	public void testFFmpegToolCall() throws IOException {
		// First initialize
		testInitialize();

		// Then list tools
		sendRequest("tools/list", null, "2");

		// Then call the ffmpeg tool
		var params = Map.of("name", "ffmpeg", "arguments", Map.of("command", "convert {{videoref}} to output.mp4"));

		sendRequest("tools/call", params, "3");

		// Verify the response
		String response = serverOutput.toString();
		assertThat(response).contains("convert");
		assertThat(response).contains("processed_video.mp4");
	}

	/**
	 * Factory to help create servers for testing.
	 */
	private static class FFmpegMcpServerFactory {

		public FFmpegMcpServer createServer(InputStream input, OutputStream output) {
			ObjectMapper mapper = new ObjectMapper();
			io.modelcontextprotocol.server.transport.StdioServerTransportProvider provider = new io.modelcontextprotocol.server.transport.StdioServerTransportProvider(
					mapper, input, output);
			return new FFmpegMcpServer(provider);
		}

	}

}