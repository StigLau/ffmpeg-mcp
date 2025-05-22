package com.example.ffmpegmcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.example.ffmpegmcp.util.TestRequestUtils.*;


/**
 * This is a CLI-based test that simulates how an LLM would use the FFmpeg MCP Server. It
 * sends JSON-RPC requests directly to the server as if it were an MCP client.
 *
 * This test can be run manually to simulate the interaction with the server.
 */
public class FFmpegMcpCliTest {

	// Path to source video file
	private static final String SOURCE_VIDEO = "/tmp/vids/wZ5.mp4";

	// Path for output files
	private static final String OUTPUT_DIR = "/tmp/vids/output";

	/**
	 * Manual test to demonstrate how an LLM would use the server. To run this, first
	 * start the server in one terminal:
	 *
	 * java -cp target/classes no.lau.mcp.Main --advanced
	 *
	 * Then run this test in another terminal.
	 */

	public void manualCliTest() throws Exception {
		System.out.println("FFmpeg MCP CLI Test");
		System.out.println("===================");
		System.out.println("This test simulates how an LLM would interact with the FFmpeg MCP Server.");
		System.out.println("Press Enter after each step to continue, or type 'exit' to quit.\n");

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		// Show available commands
		printCommands();
		String command;

		while (!(command = reader.readLine()).equalsIgnoreCase("exit")) {
			if (command.isEmpty()) {
				executeNextStep(reader);
			}
			else if (command.equalsIgnoreCase("help")) {
				printCommands();
			}
			else {
				System.out.println("Unknown command: " + command);
				printCommands();
			}
		}

		System.out.println("Test complete.");
	}

	private void printCommands() {
		System.out.println("\nCommands:");
		System.out.println("  <Enter> - Execute next step");
		System.out.println("  help    - Show this help");
		System.out.println("  exit    - Exit the test");
	}

	private int currentStep = 0;

	private void executeNextStep(BufferedReader reader) throws IOException {
		currentStep++;

		switch (currentStep) {
			case 1:
				System.out.println("\n[Step 1] Initialize connection with server");
				System.out.println("LLM would typically first establish a connection with the MCP server");
				System.out.println("JSON-RPC request:");

				String initRequest = createInitRequest();
				System.out.println(initRequest);

				System.out.println("\nServer would respond with capabilities including available tools.");
				break;

			case 2:
				System.out.println("\n[Step 2] List available tools");
				System.out.println("LLM would request the list of available tools");
				System.out.println("JSON-RPC request:");

				String listToolsRequest = createListToolsRequest();
				System.out.println(listToolsRequest);

				System.out.println("\nServer would respond with a list of tools including:");
				System.out.println("- ffmpeg: Execute FFmpeg commands to process video files");
				System.out.println("- video_info: Get information about a video file");
				System.out.println("- register_video: Register a video with a friendly name");
				break;

			case 3:
				System.out.println("\n[Step 3] Register the source video");
				System.out.println("LLM would register your video file for easy reference");
				System.out.println("JSON-RPC request:");

				String registerRequest = createRegisterVideoRequest("sourceVideo", SOURCE_VIDEO);
				System.out.println(registerRequest);

				System.out.println("\nServer would respond confirming registration.");
				break;

			case 4:
				System.out.println("\n[Step 4] Get information about the video");
				System.out.println("LLM would get information about the video to report to the user");
				System.out.println("JSON-RPC request:");

				String infoRequest = createVideoInfoRequest("sourceVideo");
				System.out.println(infoRequest);

				System.out.println("\nServer would respond with video details like duration, resolution, etc.");
				break;

			case 5:
				System.out.println("\n[Step 5] Extract a 3-second clip from the video");
				System.out.println("LLM would execute an FFmpeg command to extract a short clip");
				System.out.println("JSON-RPC request:");

				String outputFile = OUTPUT_DIR + "/clip_" + System.currentTimeMillis() + ".mp4";
				String extractRequest = createExtractClipRequest("sourceVideo", outputFile, "00:00:10", "00:00:03");
				System.out.println(extractRequest);

				System.out.println("\nServer would execute the FFmpeg command and respond with the result.");
				System.out.println("The output clip would be saved to: " + outputFile);
				break;

			case 6:
				System.out.println("\n[Step 6] Convert video to lower resolution");
				System.out.println("LLM would execute an FFmpeg command to resize the video");
				System.out.println("JSON-RPC request:");

				String resizedFile = OUTPUT_DIR + "/resized_" + System.currentTimeMillis() + ".mp4";
				String resizeRequest = createResizeVideoRequest("sourceVideo", resizedFile, "480", "360");
				System.out.println(resizeRequest);

				System.out.println("\nServer would execute the FFmpeg command and respond with the result.");
				System.out.println("The resized video would be saved to: " + resizedFile);
				break;

			case 7:
				System.out.println("\n[Step 7] Extract audio from the video");
				System.out.println("LLM would execute an FFmpeg command to extract audio");
				System.out.println("JSON-RPC request:");

				String audioFile = OUTPUT_DIR + "/audio_" + System.currentTimeMillis() + ".mp3";
				String audioRequest = createExtractAudioRequest("sourceVideo", audioFile);
				System.out.println(audioRequest);

				System.out.println("\nServer would execute the FFmpeg command and respond with the result.");
				System.out.println("The audio would be saved to: " + audioFile);
				break;

			default:
				System.out.println("\nTest complete! You've seen all the steps.");
				System.out.println("In a real interaction, the LLM would continue to respond to user requests");
				System.out.println("and execute additional FFmpeg commands as needed.");
				currentStep--; // Stay at last step
				break;
		}
	}

	/**
	 * Entry point for running the test directly.
	 */
	public static void main(String[] args) throws Exception {
		new FFmpegMcpCliTest().manualCliTest();
	}

}
