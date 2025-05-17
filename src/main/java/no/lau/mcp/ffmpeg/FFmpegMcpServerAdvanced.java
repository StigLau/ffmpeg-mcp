package no.lau.mcp.ffmpeg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import no.lau.mcp.file.FileManager;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Advanced MCP Server implementation that wraps FFmpeg functionality with multiple tools.
 * This server provides various video processing capabilities through the MCP protocol.
 */
public class FFmpegMcpServerAdvanced {

	private final McpSyncServer server;
	FFmpegWrapper ffmpeg;
	FileManager fileManager;
	//Logger log = LoggerFactory.getLogger(FFmpegMcpServerAdvanced.class);
	// Track video references for better user experience


	/**
	 * Creates a new FFmpeg MCP server with the default stdio transport.
	 */
	public FFmpegMcpServerAdvanced() throws IOException {
		this(new StdioServerTransportProvider(new ObjectMapper()));
		fileManager = new FileManager("/tmp/vids/sources", "/tmp/vids/outputs");
		//Todo move this initial listing into FileManager Object and keep functions as static
		Map<String, Path> videoReferences = fileManager.listFilesWithGeneratedKeys();
		ffmpeg = new FFmpegWrapper(videoReferences);

	}

	/**
	 * Creates a new FFmpeg MCP server with a custom transport provider.
	 * @param transportProvider The transport provider to use for MCP communication
	 */
	public FFmpegMcpServerAdvanced(StdioServerTransportProvider transportProvider) {
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

		// Create the server with multiple FFmpeg-related tools
		this.server = McpServer.sync(transportProvider)
			.serverInfo("ffmpeg-mcp-server", "1.0.0")
			.requestTimeout(Duration.ofMinutes(5)) // Longer timeout for video processing
			.instructions("""
					This server provides FFmpeg video processing capabilities. Available tools:

					1. ffmpeg - Execute FFmpeg commands on video files
					2. video_info - Get information about a video file
					3. list_registered_videos - Register a video file with a friendly name for easy reference

					Use {{videoref}} as a placeholder in FFmpeg commands to reference registered videos. Other video references can be created like {{videoref_snippet1}}, which will be created.
					""")
			.tool(new Tool("ffmpeg", "Execute FFmpeg commands to process video and audio files", ffmpegSchemaJson),
					this::handleFFmpegCommand)
			.tool(new Tool("video_info", "Get information about a video file", videoInfoSchemaJson),
					this::handleVideoInfo)
			.tool(new Tool("list_registered_videos", "List videos in storage which are registered", registerVideoSchemaJson),
					this::listRegisteredVideos)
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
			Path resolvedVideoPath = fileManager.listFilesWithGeneratedKeys().get(videoRef);
			if(resolvedVideoPath == null) {
				textContent = "Video reference not found: " + videoRef;
				System.err.println("Could not find videoRef " + videoRef);
			} else {
				String ffmpegInfoCommand = "-i " + resolvedVideoPath;
				String result = FFmpegWrapper.performFFMPEG(ffmpegInfoCommand);
				textContent = "Video Information for " + videoRef + ":\n" + result;
				isError = false;
			}
		}
		catch (Exception e) {
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
			Set<String> vidIds = fileManager.listFilesWithGeneratedKeys().keySet();

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
	 * Start the server.
	 */
	public void start() {
		System.err.println("FFmpeg MCP Server (Advanced) started...");
		//System.err.println("Available tools: ffmpeg, video_info, register_video");
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