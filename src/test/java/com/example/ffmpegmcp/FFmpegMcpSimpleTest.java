package com.example.ffmpegmcp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * A simplified test that shows the FFmpeg MCP JSON-RPC requests without requiring user
 * input.
 */
public class FFmpegMcpSimpleTest {

	// Path to source video file
	private static final String SOURCE_VIDEO = "/tmp/vids/wZ5.mp4";

	// Path for output files
	private static final String OUTPUT_DIR = "/tmp/vids/output";

	/**
	 * Test to demonstrate the FFmpeg MCP usage without interactive input.
	 */
	@Test
	public void testFfmpegMcpUsage() throws IOException {
		System.out.println("FFmpeg MCP Demo");
		System.out.println("==============");
		System.out.println("Showing example JSON-RPC requests an LLM would send to an FFmpeg MCP Server\n");

		// Step 1: Initialize connection
		System.out.println("[Step 1] Initialize connection with server");
		System.out.println(createInitRequest());
		System.out.println();

		// Step 2: List tools
		System.out.println("[Step 2] List available tools");
		System.out.println(createListToolsRequest());
		System.out.println();

		// Step 3: Register video
		System.out.println("[Step 3] Register the source video");
		System.out.println(createRegisterVideoRequest("sourceVideo", SOURCE_VIDEO));
		System.out.println();

		// Step 4: Get video info
		System.out.println("[Step 4] Get information about the video");
		System.out.println(createVideoInfoRequest("sourceVideo"));
		System.out.println();

		// Step 5: Extract clip
		System.out.println("[Step 5] Extract a 3-second clip from the video");
		String outputFile = OUTPUT_DIR + "/clip_" + System.currentTimeMillis() + ".mp4";
		System.out.println(createExtractClipRequest("sourceVideo", outputFile, "00:00:10", "00:00:03"));
		System.out.println();

		// Step 6: Convert video
		System.out.println("[Step 6] Convert video to lower resolution");
		String resizedFile = OUTPUT_DIR + "/resized_" + System.currentTimeMillis() + ".mp4";
		System.out.println(createResizeVideoRequest("sourceVideo", resizedFile, "480", "360"));
		System.out.println();

		// Step 7: Extract audio
		System.out.println("[Step 7] Extract audio from the video");
		String audioFile = OUTPUT_DIR + "/audio_" + System.currentTimeMillis() + ".mp3";
		System.out.println(createExtractAudioRequest("sourceVideo", audioFile));
		System.out.println();

		System.out.println(
				"Demo complete! These are the JSON-RPC requests an LLM would send to interact with the FFmpeg MCP Server.");
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
		clientInfo.put("name", "llm-client");
		clientInfo.put("version", "1.0.0");
		params.put("clientInfo", clientInfo);

		Map<String, Object> capabilities = new HashMap<>();
		// Empty capabilities
		params.put("capabilities", capabilities);

		request.put("params", params);

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
	}

	private String createListToolsRequest() throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/list");

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
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

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
	}

	private String createVideoInfoRequest(String videoRef) throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/call");

		Map<String, Object> params = new HashMap<>();
		params.put("name", "video_info");

		Map<String, Object> arguments = new HashMap<>();
		arguments.put("videoref", videoRef);
		params.put("arguments", arguments);

		request.put("params", params);

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
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
		String command = String.format("ffmpeg -i {{%s}} -ss %s -t %s -c:v copy -c:a copy %s", videoRef, startTime,
				duration, outputFile);
		arguments.put("command", command);
		params.put("arguments", arguments);

		request.put("params", params);

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
	}

	private String createResizeVideoRequest(String videoRef, String outputFile, String width, String height)
			throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/call");

		Map<String, Object> params = new HashMap<>();
		params.put("name", "ffmpeg");

		Map<String, Object> arguments = new HashMap<>();
		String command = String.format(
				"ffmpeg -i {{%s}} -vf \"scale=%s:%s\" -c:v libx264 -crf 23 -preset medium -c:a aac %s", videoRef, width,
				height, outputFile);
		arguments.put("command", command);
		params.put("arguments", arguments);

		request.put("params", params);

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
	}

	private String createExtractAudioRequest(String videoRef, String outputFile) throws IOException {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", generateId());
		request.put("method", "tools/call");

		Map<String, Object> params = new HashMap<>();
		params.put("name", "ffmpeg");

		Map<String, Object> arguments = new HashMap<>();
		String command = String.format("ffpeg -i {{%s}} -q:a 0 -map a %s", videoRef, outputFile);
		arguments.put("command", command);
		params.put("arguments", arguments);

		request.put("params", params);

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request);
	}

	private String generateId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * Entry point for running the test directly.
	 */
	public static void main(String[] args) throws Exception {
		new FFmpegMcpSimpleTest().testFfmpegMcpUsage();
	}

}