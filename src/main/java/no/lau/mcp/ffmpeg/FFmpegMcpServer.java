package no.lau.mcp.ffmpeg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * MCP Server implementation that wraps the FFmpeg functionality. This server exposes
 * FFmpeg capabilities as an MCP tool that can be invoked by LLMs through Claude Desktop.
 */
public class FFmpegMcpServer {

	private final McpSyncServer server;

	/**
	 * Creates a new FFmpeg MCP server with the default stdio transport.
	 */
	public FFmpegMcpServer() {
		this(new StdioServerTransportProvider(new ObjectMapper()));
	}

	/**
	 * Creates a new FFmpeg MCP server with a custom transport provider.
	 * @param transportProvider The transport provider to use for MCP communication
	 */
	public FFmpegMcpServer(StdioServerTransportProvider transportProvider) {
		// Define the JSON Schema for the FFmpeg tool
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

		// Create the server with FFmpeg tool
		this.server = McpServer.sync(transportProvider)
			.serverInfo("ffmpeg-mcp-server", "1.0.0")
			.requestTimeout(Duration.ofMinutes(5)) // Longer timeout for video processing
			.instructions(
					"This server provides FFmpeg video processing capabilities. Use the ffmpeg tool to execute FFmpeg commands on video files.")
			.tool(new Tool("ffmpeg", "Execute FFmpeg commands to process video and audio files", ffmpegSchemaJson),
					this::handleFFmpegCommand)
			.build();
	}

	/**
	 * Handle FFmpeg command execution.
	 * @param exchange The server exchange for communicating with the client
	 * @param args The tool arguments containing the FFmpeg command
	 * @return The result of executing the FFmpeg command
	 */
	private CallToolResult handleFFmpegCommand(McpSyncServerExchange exchange, Map<String, Object> args) {
		String command = (String) args.get("command");

		try {
			// Log the incoming command
			//System.err.println("Executing FFmpeg command: " + command);

			// Execute the command through our wrapper
			String result = FFmpegWrapper.performFFMPEG(command);

			// Build a successful result
			return CallToolResult.builder().addTextContent(result).isError(false).build();
		}
		catch (IllegalArgumentException e) {
			// Client error (invalid command)
			//System.err.println("Invalid FFmpeg command: " + e.getMessage());
			return CallToolResult.builder().addTextContent("Error: " + e.getMessage()).isError(true).build();
		}
		catch (IOException e) {
			// FFmpeg execution error
			//System.err.println("FFmpeg execution error: " + e.getMessage());
			return CallToolResult.builder()
				.addTextContent("FFmpeg execution failed: " + e.getMessage())
				.isError(true)
				.build();
		}
		catch (Exception e) {
			// Unexpected error
			//System.err.println("Unexpected error: " + e.getMessage());
			//e.printStackTrace();
			return CallToolResult.builder().addTextContent("Unexpected error: " + e.getMessage()).isError(true).build();
		}
	}

	/**
	 * Start the server.
	 */
	public void start() {
		//System.err.println("FFmpeg MCP Server started...");
		// The server is already started when created, nothing else to do
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
	public static void main(String[] args) {
		FFmpegMcpServer server = new FFmpegMcpServer();
		server.start();

		// Add a shutdown hook to close the server gracefully
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

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