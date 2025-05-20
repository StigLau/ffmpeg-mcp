package com.example.ffmpegmcp;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static com.example.ffmpegmcp.util.TestRequestUtils.*;


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

	/**
	 * Entry point for running the test directly.
	 */
	public static void main(String[] args) throws Exception {
		new FFmpegMcpSimpleTest().testFfmpegMcpUsage();
	}

}
