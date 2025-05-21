package com.example.ffmpegmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import no.lau.mcp.ffmpeg.FFmpegFake;
import no.lau.mcp.ffmpeg.FFmpegWrapper;
import no.lau.mcp.ffmpeg.FileHasher;
import no.lau.mcp.file.FileManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to showcase the functionalities of FFmpegMcpServerAdvanced
 * from the perspective of an LLM client.
 */
public class FFmpegMcpShowcaseTest {

    private ObjectMapper objectMapper;
    private PipedOutputStream clientToServer;
    private ByteArrayOutputStream serverOutput;
    private Thread serverThread;

    // Use @TempDir for temporary source and output folders managed by JUnit
    @TempDir
    Path testSourcesDir;
    @TempDir
    Path testOutputsDir;

    private Path sampleVideoFile;
    private String sampleVideoFileHash;


    @BeforeEach
    public void setup() throws IOException, InterruptedException {
        objectMapper = new ObjectMapper();

        // Setup pipes for communication
        clientToServer = new PipedOutputStream();
        PipedInputStream serverInput = new PipedInputStream(clientToServer);
        serverOutput = new ByteArrayOutputStream();

        // Create a dummy sample video file in the temporary test_sources directory
        sampleVideoFile = testSourcesDir.resolve("sample_video.mp4");
        Files.writeString(sampleVideoFile, "dummy video content");
        sampleVideoFileHash = FileHasher.getMd5Hash(sampleVideoFile);

        // Start server in a separate thread using the factory
        // The factory will use the testSourcesDir and testOutputsDir paths
        serverThread = new Thread(() -> {
            try {
                // Pass the TempDir paths to the factory for FileManagerImpl
                new FFmpegMcpServerAdvancedTest.FFmpegMcpServerFactory()
                        .createServerWithCustomPaths(serverInput, serverOutput, testSourcesDir.toString(), testOutputsDir.toString());
                // Keep the server thread alive
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interruption status
                // Expected when shutting down test
            } catch (IOException e) {
                // Log or handle IOException during server setup if necessary
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server time to initialize
        Thread.sleep(500); // Adjust if needed, though polling in sendRequest is better
    }

    @AfterEach
    public void tearDown() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
        // Temporary directories and files created with @TempDir and Files.write will be cleaned up by JUnit
    }

    /**
     * Helper method to simulate sending a JSON-RPC request to the server and wait for its response.
     */
    private String sendRequestAndWaitForResponse(String method, Object params, String id) throws IOException, InterruptedException {
        McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", method, id, params);
        String jsonRequest = objectMapper.writeValueAsString(request);

        serverOutput.reset(); // Clear previous response

        clientToServer.write((jsonRequest + "\n").getBytes());
        clientToServer.flush();

        long startTime = System.currentTimeMillis();
        long timeoutMillis = 5000; // 5 seconds

        while (serverOutput.size() == 0 && System.currentTimeMillis() - startTime < timeoutMillis) {
            Thread.sleep(50); // Poll
        }

        if (serverOutput.size() == 0) {
            throw new IOException("Timeout waiting for server response (no data). Request: " + jsonRequest);
        }

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            byte[] bytes = serverOutput.toByteArray();
            if (bytes.length > 0 && bytes[bytes.length - 1] == '\n') {
                break; // Full response received
            }
            Thread.sleep(50); // Poll for newline
        }

        byte[] finalBytes = serverOutput.toByteArray();
        if (finalBytes.length == 0) { // Should have been caught by the first loop
            throw new IOException("Server response is empty. Request: " + jsonRequest);
        }
        if (finalBytes[finalBytes.length - 1] != '\n') {
            System.err.println("Warning: Response might be incomplete or not newline-terminated. Request: " + jsonRequest + " Response: " + new String(finalBytes));
        }
        return new String(finalBytes).trim();
    }

    @Test
    @Disabled //Not functional through MCP Server interface
    public void showcaseFFmpegMcpServerFeatures() throws IOException, InterruptedException {
        // 1. Initialize connection
        McpSchema.InitializeRequest initParams = new McpSchema.InitializeRequest("2024-11-05",
                new McpSchema.ClientCapabilities(null, null, null),
                new McpSchema.Implementation("showcase-test-client", "1.0.0"));
        String initResponse = sendRequestAndWaitForResponse("initialize", initParams, "init-1");
        System.out.println("Initialize Response:\n" + initResponse);
        assertThat(initResponse).contains("\"protocolVersion\":\"2024-11-05\"");
        assertThat(initResponse).contains("\"name\":\"ffmpeg-mcp-server\"");

        // 2. List available tools
        String listToolsResponse = sendRequestAndWaitForResponse("tools/list", null, "list-tools-1");
        System.out.println("\nList Tools Response:\n" + listToolsResponse);
        assertThat(listToolsResponse).contains("\"name\":\"ffmpeg\"");
        assertThat(listToolsResponse).contains("\"name\":\"video_info\"");
        assertThat(listToolsResponse).contains("\"name\":\"list_registered_videos\"");
        assertThat(listToolsResponse).contains("\"name\":\"addTargetVideo\"");

        // 3. List registered source videos
        // The "list_registered_videos" tool in FFmpegMcpServerAdvanced doesn't actually use arguments from its schema.
        // It lists files from the source directory.
        String listRegisteredResponse = sendRequestAndWaitForResponse("tools/call",
                Map.of("name", "list_registered_videos", "arguments", Map.of()), "list-registered-1"); // Empty args
        System.out.println("\nList Registered Videos Response:\n" + listRegisteredResponse);
        assertThat(listRegisteredResponse).contains("\"is_error\":false");
        assertThat(listRegisteredResponse).contains("Video ID: " + sampleVideoFileHash);

        // 4. Add a target video
        String targetVideoName = "myShowcaseOutput";
        Map<String, Object> addTargetParams = Map.of("name", "addTargetVideo", "arguments",
                Map.of("targetName", targetVideoName, "extension", ".mkv"));
        String addTargetResponse = sendRequestAndWaitForResponse("tools/call", addTargetParams, "add-target-1");
        System.out.println("\nAdd Target Video Response:\n" + addTargetResponse);
        assertThat(addTargetResponse).contains("\"is_error\":false");
        assertThat(addTargetResponse).contains("Target video '" + targetVideoName + "' registered with path:");
        assertThat(addTargetResponse).contains(testOutputsDir.toString()); // Check if path is in test_outputs
        assertThat(addTargetResponse).endsWith(".mkv\""); // Check for correct extension in the path string

        // 5. Execute an FFmpeg command
        // Using the hash of the source video and the named target placeholder.
        // Note: FFmpegMcpServerAdvanced currently doesn't replace target placeholders like {{myShowcaseOutput}}.
        // The command sent to FFmpegFake will contain the placeholder literally.
        String ffmpegCommand = String.format("ffmpeg -i {{%s}} -c:v copy -an {{%s}}", sampleVideoFileHash, targetVideoName);
        Map<String, Object> ffmpegParams = Map.of("name", "ffmpeg", "arguments", Map.of("command", ffmpegCommand));
        String ffmpegResponse = sendRequestAndWaitForResponse("tools/call", ffmpegParams, "ffmpeg-1");
        System.out.println("\nFFmpeg Command Response:\n" + ffmpegResponse);
        assertThat(ffmpegResponse).contains("\"is_error\":false"); // FFmpegFake returns success
        assertThat(ffmpegResponse).contains("Response from fake"); // FFmpegFake's output

        // 6. Get video info for the source video
        Map<String, Object> videoInfoParams = Map.of("name", "video_info", "arguments", Map.of("videoref", sampleVideoFileHash));
        String videoInfoResponse = sendRequestAndWaitForResponse("tools/call", videoInfoParams, "video-info-1");
        System.out.println("\nVideo Info Response:\n" + videoInfoResponse);
        assertThat(videoInfoResponse).contains("\"is_error\":false"); // FFmpegFake returns success for info
        // FFmpegWrapper.informationFromVideo calls executor.execute("-i /path/to/video"), so FFmpegFake returns "Response from fake"
        assertThat(videoInfoResponse).contains("Video Information for " + sampleVideoFileHash);
        assertThat(videoInfoResponse).contains("Response from fake");

        System.out.println("\nShowcase completed successfully!");
    }

    @Test
    public void testCreatingTempFile() throws IOException {
        FFmpegWrapper ffmpeg = new FFmpegWrapper(new FileManagerImpl("/tmp/tempvids/src", "/tmp/tempvids/trgt")
        , new FFmpegFake());
        ffmpeg.fileManager().createNewFileWithAutoGeneratedNameInSecondFolder("testingfile");
    }
}
