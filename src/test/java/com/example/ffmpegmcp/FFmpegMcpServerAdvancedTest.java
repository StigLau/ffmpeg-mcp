package com.example.ffmpegmcp;

import com.example.ffmpegmcp.util.TestRequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import no.lau.mcp.ffmpeg.FFmpegExecutor;
import no.lau.mcp.ffmpeg.FFmpegFake;
import no.lau.mcp.ffmpeg.FFmpegMcpServerAdvanced;
import no.lau.mcp.ffmpeg.FFmpegWrapper;
import no.lau.mcp.file.FileManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused test suite for FFmpeg MCP server with only essential and high-value tests.
 */
public class FFmpegMcpServerAdvancedTest {

	private ObjectMapper objectMapper;
	private PipedOutputStream clientToServer;
	private ByteArrayOutputStream serverOutput;

	@BeforeEach
	public void setup() throws IOException, InterruptedException {
		objectMapper = new ObjectMapper();
		
		// Setup pipes for communication
		clientToServer = new PipedOutputStream();
		PipedInputStream serverInput = new PipedInputStream(clientToServer);
		
		serverOutput = new ByteArrayOutputStream();
		OutputStream testOutput = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				serverOutput.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				serverOutput.write(b, off, len);
			}
		};

		// Start server in a separate thread
		Thread serverThread = new Thread(() -> {
			try {
				new FFmpegMcpServerFactory().createServer(serverInput, testOutput);
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				// Expected when shutting down test
			} catch (IOException e) {
	            throw new RuntimeException(e);
	        }
	    });
		serverThread.setDaemon(true);
		serverThread.start();

		Thread.sleep(500);
	}

	private void sendNotification(String method, Object params) throws IOException, InterruptedException {
		TestRequestUtils.sendNotification(method, params, clientToServer);
	}

	private String sendRequestAndWaitForResponse(String method, Object params, String id) throws IOException, InterruptedException {
		return TestRequestUtils.sendRequestAndWaitForResponse(method, params, id, clientToServer, serverOutput, 500);
	}

	@Test
	public void testListTools() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));

		sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		String response = sendRequestAndWaitForResponse("tools/list", null, "2");

		assertThat(response).contains("ffmpeg");
		assertThat(response).contains("video_info");
		assertThat(response).contains("list_registered_videos");
		assertThat(response).contains("addTargetVideo");
	}

	@Test
	public void testAddTargetVideoAndUseInFFmpeg() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));
		sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		var addTargetParams = Map.of("name", "addTargetVideo", "arguments",
				Map.of("targetName", "myOutput", "extension", ".mov"));
		String addTargetResponse = sendRequestAndWaitForResponse("tools/call", addTargetParams, "2");

		assertThat(addTargetResponse).contains("Target video 'myOutput' registered");

		var ffmpegParams = Map.of("name", "ffmpeg", "arguments",
				Map.of("command", "ffmpeg -i some_input.mp4 -c:v copy {{myOutput}}"));
		String ffmpegResponse = sendRequestAndWaitForResponse("tools/call", ffmpegParams, "4");

		assertThat(ffmpegResponse).contains("\"isError\":true");
		assertThat(ffmpegResponse).contains("Command contains potential direct filename ('some_input.mp4')");
	}

	@Test
	public void testVideoInfo() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));

		sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		var infoParams = Map.of("name", "video_info", "arguments", Map.of("videoref", "/path/to/video.mp4"));
		String response = sendRequestAndWaitForResponse("tools/call", infoParams, "2");

		assertThat(response).contains("Video reference not found: /path/to/video.mp4");
		assertThat(response).contains("\"isError\":true");
	}

	@Test
	public void testFFmpegCommandWithDirectPathFails() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));
		sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		var ffmpegParams = Map.of("name", "ffmpeg", "arguments",
				Map.of("command", "ffmpeg -i /some/direct/input.mp4 -o {{output_target}}"));
		String ffmpegResponse = sendRequestAndWaitForResponse("tools/call", ffmpegParams, "2");

		assertThat(ffmpegResponse).contains("\"isError\":true");
		assertThat(ffmpegResponse).contains("Command contains direct path separator");
	}

	@Test
	public void testFFmpegCommandWithDirectPathInOptionFails() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));
		sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		var ffmpegParams = Map.of("name", "ffmpeg", "arguments",
				Map.of("command", "ffmpeg -i {{input_source}} -vf \"drawtext=fontfile=/usr/share/fonts/DejaVuSans.ttf\" {{output_target}}"));
		String ffmpegResponse = sendRequestAndWaitForResponse("tools/call", ffmpegParams, "2");

		assertThat(ffmpegResponse).contains("\"isError\":true");
		assertThat(ffmpegResponse).contains("Command contains direct path separator");
	}

	@Test
	public void testFFmpegCommandWithPathTraversalFails() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));
		String initResponse = sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		var ffmpegParams = Map.of("name", "ffmpeg", "arguments",
				Map.of("command", "ffmpeg -i {{input_source}} -o ../../etc/passwd"));
		String ffmpegResponse = sendRequestAndWaitForResponse("tools/call", ffmpegParams, "2");

		assertThat(ffmpegResponse).contains("\"isError\":true");
		assertThat(ffmpegResponse).contains("Command contains path traversal attempt ('..')");
	}

	@Test
	public void testFFmpegCommandWithDirectFilenameExtFails() throws IOException, InterruptedException {
		McpSchema.InitializeRequest initRequest = new McpSchema.InitializeRequest("2024-11-05",
				new McpSchema.ClientCapabilities(null, null, null),
				new McpSchema.Implementation("test-client", "1.0.0"));
		sendRequestAndWaitForResponse("initialize", initRequest, "1");
		sendNotification("notifications/initialized", null);

		var ffmpegParams = Map.of("name", "ffmpeg", "arguments",
				Map.of("command", "ffmpeg -i {{input_source}} -o output.mp4"));
		String ffmpegResponse = sendRequestAndWaitForResponse("tools/call", ffmpegParams, "2");

		assertThat(ffmpegResponse).contains("\"isError\":true");
		assertThat(ffmpegResponse).contains("Command contains potential direct filename ('output.mp4')");
	}

	/**
	 * Factory to help create servers for testing.
	 */
	public static class FFmpegMcpServerFactory {

		public FFmpegMcpServerAdvanced createServer(InputStream input, OutputStream output) throws IOException {
			return createServerWithCustomPaths(input, output, "/tmp/vids/test_sources", "/tmp/vids/test_outputs");
		}

		public FFmpegMcpServerAdvanced createServerWithCustomPaths(InputStream input, OutputStream output, String sourcePath, String outputPath) throws IOException {
			ObjectMapper mapper = new ObjectMapper();
			io.modelcontextprotocol.server.transport.StdioServerTransportProvider provider = new io.modelcontextprotocol.server.transport.StdioServerTransportProvider(
					mapper, input, output);

			Files.createDirectories(Paths.get(sourcePath));
			Files.createDirectories(Paths.get(outputPath));

			FileManagerImpl fileManager = new FileManagerImpl(sourcePath, outputPath);
			FFmpegExecutor mockExecutor = new FFmpegFake();
			FFmpegWrapper ffmpegWrapper = new FFmpegWrapper(fileManager, mockExecutor);

			FFmpegMcpServerAdvanced server = new FFmpegMcpServerAdvanced(provider, ffmpegWrapper);
			server.start();
			
			return server;
		}
	}
}