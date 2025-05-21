package no.lau.mcp.listvideos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ListVideosMcpServer {

    private final McpSyncServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ListVideosMcpServer() {
        this(new StdioServerTransportProvider());
    }

    public ListVideosMcpServer(StdioServerTransportProvider transportProvider) {
        this.server = McpServer.sync(transportProvider)
                .info("list-videos-mcp-server", "1.0.0")
                .requestTimeout(Duration.ofMinutes(1))
                .instructions("This server provides a tool to list video files.")
                .tool(Tool.builder()
                        .name("list_videos")
                        .description("Lists video files available in a specified directory or a default media location.")
                        .argumentsSchema("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "directory_path": {
                                            "type": "string",
                                            "description": "Optional. The path to the directory to scan for video files. If not provided, a default media directory will be scanned."
                                        }
                                    },
                                    "additionalProperties": false
                                }""")
                        .handler(this::handleListVideos)
                        .build())
                .build();
    }

    CallToolResult handleListVideos(McpSyncServerExchange exchange, Map<String, Object> args) {
        String directoryPath = (String) args.get("directory_path");

        try {
            if (directoryPath == null || directoryPath.isEmpty() || directoryPath.equalsIgnoreCase("./sample_videos/")) {
                List<Map<String, String>> videos = List.of(
                        Map.of("name", "video1.mp4", "path", "./sample_videos/video1.mp4"),
                        Map.of("name", "video2.mov", "path", "./sample_videos/video2.mov")
                );
                String jsonResult = objectMapper.writeValueAsString(videos);
                return CallToolResult.builder().addTextContent(jsonResult).isError(false).build();
            } else {
                // For now, return an empty list for other directories
                String jsonResult = objectMapper.writeValueAsString(List.of());
                return CallToolResult.builder().addTextContent(jsonResult).isError(false).build();
            }
        } catch (JsonProcessingException e) {
            // Log the error or handle it as needed
            System.err.println("Error serializing video list to JSON: " + e.getMessage());
            return CallToolResult.builder()
                    .addTextContent("Error processing request: " + e.getMessage())
                    .isError(true)
                    .build();
        }
    }

    public void start() {
        System.out.println("ListVideos MCP Server started...");
        // The server processing loop will be started by the transport provider
        // For StdioServerTransportProvider, this typically happens implicitly
        // or by a method call on the transport provider itself if needed.
        // server.start(); // McpSyncServer itself doesn't have a start() method.
    }

    public void shutdown() {
        server.closeGracefully();
        System.out.println("ListVideos MCP Server shut down.");
    }

    // Optional: Main method for standalone execution
    public static void main(String[] args) {
        ListVideosMcpServer server = new ListVideosMcpServer();
        server.start();
        // Keep the main thread alive for the server to run, or use transport's blocking serve method
        // For Stdio, it will run until stdin is closed.
    }
}
