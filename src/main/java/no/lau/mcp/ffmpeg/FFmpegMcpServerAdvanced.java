package no.lau.mcp.ffmpeg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import no.lau.mcp.file.FileManagerImpl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced MCP Server implementation that wraps FFmpeg functionality with multiple tools.
 * This server provides various video processing capabilities through the MCP protocol.
 */
public class FFmpegMcpServerAdvanced {

	private final McpSyncServer server;
	FFmpegWrapper ffmpeg;
	//Logger log = LoggerFactory.getLogger(FFmpegMcpServerAdvanced.class);
	// Track video references for better user experience


	/**
	 * Creates a new FFmpeg MCP server with the default stdio transport.
	 */
	public FFmpegMcpServerAdvanced() {
		//Wiring the app with all relevant configuration
		this(new StdioServerTransportProvider(new ObjectMapper()),
				new FFmpegWrapper(
						new FileManagerImpl("/tmp/vids/sources", "/tmp/vids/outputs")
						, new DefaultFFmpegExecutor("/usr/local/bin/ffmpeg")));
	}

	/**
	 * Creates a new FFmpeg MCP server with a custom transport provider and injectable dependencies for testing.
	 * @param transportProvider The transport provider to use for MCP communication
	 * @param ffmpegWrapperInstance The FFmpegWrapper instance to use. If null, a default one will be created.
	 */
	public FFmpegMcpServerAdvanced(StdioServerTransportProvider transportProvider, FFmpegWrapper ffmpegWrapperInstance) {
		this.ffmpeg = ffmpegWrapperInstance;


		// Define the JSON Schemas for the tools

		// Main FFmpeg command tool schema
		String ffmpegSchemaJson = """
				{
				    "type": "object",
				    "properties": {
				        "command": {
				            "type": "string",
				            "description": "The FFmpeg command to execute. {{videoref}} can be used as a placeholder for video files."
				        }
				    },
				    "required": ["command"],
				    "additionalProperties": false
				}
				""";

		// Video information tool schema
		String videoInfoSchemaJson = """
				{
				    "type": "object",
				    "properties": {
				        "videoref": {
				            "type": "string",
				            "description": "The reference to the video file to get information about."
				        }
				    },
				    "required": ["videoref"],
				    "additionalProperties": false
				}
				""";

		// Register video reference tool schema
		String registerVideoSchemaJson = """
				{
				    "type": "object",
				    "properties": {
				        "name": {
				            "type": "string",
				            "description": "A friendly name to reference the video by."
				        },
				        "path": {
				            "type": "string",
				            "description": "The file path to the video."
				        }
				    },
				    "required": ["name", "path"],
				    "additionalProperties": false
				}
				""";
		
		// Add target video tool schema
		String addTargetVideoSchemaJson = """
				{
				    "type": "object",
				    "properties": {
				        "targetName": {
				            "type": "string",
				            "description": "A friendly name to reference the target video by (e.g., 'output_render'). This name will be used in {{targetName}} placeholders."
				        }
				    },
				    "required": ["targetName"],
				    "additionalProperties": false
				}
				""";

		// Create the server with multiple FFmpeg-related tools
		this.server = McpServer.sync(transportProvider)
			.serverInfo("ffmpeg-mcp-server", "1.0.0")
			.requestTimeout(Duration.ofMinutes(5)) // Longer timeout for video processing
			.instructions("""
					This server provides FFmpeg video processing capabilities. Available tools:

					1. ffmpeg - Execute FFmpeg commands on video files. Use {{source_id}} for source files and {{target_id}} for output files.
					2. video_info - Get information about a source video file.
					3. list_registered_videos - List available source videos.
					4. addTargetVideo - Register a target video name and generate a path for an output file.

					Use {{name}} as a placeholder in FFmpeg commands to reference registered source or target videos.
					Target video placeholders (e.g., {{target_video_1}}) must be registered using 'addTargetVideo' before use in an 'ffmpeg' command.
					""")
			.tool(new Tool("ffmpeg", "Execute FFmpeg commands to process video and audio files", ffmpegSchemaJson),
					this::handleFFmpegCommand)
			.tool(new Tool("video_info", "Get information about a video file", videoInfoSchemaJson),
					this::handleVideoInfo)
			.tool(new Tool("list_registered_videos", "List videos in storage which are registered", registerVideoSchemaJson),
					this::listRegisteredVideos)
			.tool(new Tool("addTargetVideo", "Registers a name and generates a filepath for a target (output) video.", addTargetVideoSchemaJson),
					this::handleAddTargetVideo)
			.build();
	}

	/**
	 * Handle FFmpeg command execution.
	 * @param exchange The server exchange for communicating with the client
	 * @param args The tool arguments containing the FFmpeg command
	 * @return The result of executing the FFmpeg command
	 */
	private CallToolResult handleFFmpegCommand(McpSyncServerExchange exchange, Map<String, Object> args) {
		String cmd = (String) args.get("command");

		try {
			// Validate command structure to prevent direct path injection
			validateCommandStructure(cmd);
			// Replace any video references in the command
			String result = ffmpeg.doffMPEGStuff(cmd);

			// Build a successful result
			return CallToolResult.builder().addTextContent(result).isError(false).build();
		}
		catch (IllegalArgumentException e) {
			// Client error (invalid command)
			System.err.println("Invalid FFmpeg command: " + e.getMessage());
			return CallToolResult.builder().addTextContent("Error: " + e.getMessage()).isError(true).build();
		}
		catch (IOException e) {
			// FFmpeg execution error
			System.err.println("FFmpeg execution error: " + e.getMessage());
			return CallToolResult.builder()
				.addTextContent("FFmpeg execution failed: " + e.getMessage())
				.isError(true)
				.build();
		}
		catch (Exception e) {
			// Unexpected error
			System.err.println("Unexpected error: " + e.getMessage());
			e.printStackTrace();
			return CallToolResult.builder().addTextContent("Unexpected error: " + e.getMessage()).isError(true).build();
		}
	}

	/**
	 * Handle the video_info tool to get information about a video file.
	 * @param exchange The server exchange for communicating with the client
	 * @param args The tool arguments containing the video reference
	 * @return Information about the video file
	 */
	private CallToolResult handleVideoInfo(McpSyncServerExchange exchange, Map<String, Object> args) {
		String videoRef = (String) args.get("videoref");
		String textContent;
		boolean isError = true;
		try {
			String rezz = ffmpeg.informationFromVideo(videoRef);
			textContent = "Video Information for " + videoRef + ":\n" + rezz;
			isError = false;
		} catch (FileNotFoundException e) {
			textContent = "Video reference not found: " + videoRef;
			System.err.println("Could not find videoRef " + videoRef);
		} catch (IOException e) {
			textContent = "Error getting video information from " + videoRef + ": " + e.getMessage();
		}
		return CallToolResult.builder()
				.addTextContent(textContent)
				.isError(isError)
				.build();
	}

	/**
	 * Handle the register_video tool to register a video file with a friendly name.
	 * @param exchange The server exchange for communicating with the client
	 * @param args The tool arguments containing the video name and path
	 * @return Confirmation of video registration
	 */
	private CallToolResult listRegisteredVideos(McpSyncServerExchange exchange, Map<String, Object> args) {
		//log.info("calling list_registered_videos {}", args);
		System.err.println("calling list_registered_videos " + args);
		try {
			Set<String> vidIds = ffmpeg.fileManager().listVideoReferences().keySet();

			CallToolResult.Builder builder =  CallToolResult.builder();
			for (String vidId : vidIds) {
				builder.addTextContent("Video ID: " + vidId);
			}
			return builder.isError(false).build();
		}
		catch (Exception e) {
			return CallToolResult.builder()
				.addTextContent("Error registering video: " + e.getMessage())
				.isError(true)
				.build();
		}
	}

	/**
	 * Validates the FFmpeg command string to ensure no direct file/folder paths are used.
	 * All file references must use the {{id}} placeholder syntax.
	 *
	 * @param command The FFmpeg command string to validate.
	 * @throws IllegalArgumentException if the command contains direct path references.
	 */
	private void validateCommandStructure(String command) throws IllegalArgumentException {
		// Replace all {{placeholder}} instances with a benign, unique marker string
		// that does not contain path characters or typical filename patterns.
		String commandWithoutPlaceholders = command.replaceAll("\\{\\{.*?}}", "MCP_GENERATED_PLACEHOLDER");

		// 1. Check for path traversal attempts
		if (commandWithoutPlaceholders.contains("..")) {
			throw new IllegalArgumentException(
					"Command contains path traversal attempt ('..'). " +
							"All file references must use {{id}} placeholders."
			);
		}

		// 2. Check for explicit path separators
		if (commandWithoutPlaceholders.contains("/") || commandWithoutPlaceholders.contains("\\")) {
			throw new IllegalArgumentException(
					"Command contains direct path separator ('/' or '\\'). " +
							"All file references must use {{id}} placeholders."
			);
		}

		// 3. Check for potential direct filenames with extensions (e.g., output.mp4)
		// This regex looks for words with a dot and 2-4 char extension.
		Pattern filenamePattern = Pattern.compile("\\b([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)\\.([a-zA-Z0-9]{2,4})\\b");
		Matcher matcher = filenamePattern.matcher(commandWithoutPlaceholders);
		while (matcher.find()) {
			String potentialFilename = matcher.group(0); // The full match e.g., "file.mp4" or "archive.tar.gz" (if .gz is 2-4 chars)
			// Allow purely numeric values like "1.0", "2.5" which might be FFmpeg parameters
			try {
				Double.parseDouble(potentialFilename);
				// If successful, it's a number, so continue to next match
			} catch (NumberFormatException e) {
				// It's not a simple number, so treat as a disallowed direct filename
				throw new IllegalArgumentException(
						"Command contains potential direct filename ('" + potentialFilename + "'). " +
								"All file references must use {{id}} placeholders."
				);
			}
		}
	}

	/**
	 * Handle the addTargetVideo tool to register a name for a target (output) video file.
	 * @param exchange The server exchange for communicating with the client
	 * @param args The tool arguments containing the target name and optional extension
	 * @return Confirmation of target video registration
	 */
	private CallToolResult handleAddTargetVideo(McpSyncServerExchange exchange, Map<String, Object> args) {
		String targetName = (String) args.get("targetName");

		if (targetName == null || targetName.trim().isBlank()) {
			return CallToolResult.builder()
					.addTextContent("Error: targetName cannot be empty.")
					.isError(true)
					.build();
		}
		
		try {
			ffmpeg.fileManager().createNewFileWithAutoGeneratedNameInSecondFolder(targetName);
			return CallToolResult.builder()
					.addTextContent("Target video '" + targetName + "' registered")
					.isError(false)
					.build();
		} catch (IOException e) {
			System.err.println("Error creating target video file: " + e.getMessage());
			return CallToolResult.builder()
					.addTextContent("Error creating target video file: " + e.getMessage())
					.isError(true)
					.build();
		} catch (IllegalArgumentException e) {
			System.err.println("Error with target video parameters: " + e.getMessage());
			return CallToolResult.builder()
					.addTextContent("Error with target video parameters: " + e.getMessage())
					.isError(true)
					.build();
		}
	}


	/**
	 * Start the server.
	 */
	public void start() {
		System.err.println("FFmpeg MCP Server (Advanced) started...");
	}

	/**
	 * Shutdown the server gracefully.
	 */
	public void shutdown() {
		System.err.println("Shutting down FFmpeg MCP Server...");
		server.closeGracefully();
	}

	/**
	 * Main entry point for starting the FFmpeg MCP server.
	 * @param args Command line arguments (not used)
	 */
	public static void main(String[] args) throws IOException {
		//System.err.println("Starting FFmpeg MCP Server (Advanced)...");

		// Create the server
		FFmpegMcpServerAdvanced server = new FFmpegMcpServerAdvanced();
		server.start();

		// Add a shutdown hook to close the server gracefully
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//			System.err.println("Shutting down FFmpeg MCP Server...");
			server.shutdown();
		}));

		//System.err.println("FFmpeg MCP Server running. Press Ctrl+C to exit.");

		// Keep the server running until terminated
		try {
			// Wait indefinitely
			Thread.currentThread().join();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("Server interrupted: " + e.getMessage());
		}
	}
}
