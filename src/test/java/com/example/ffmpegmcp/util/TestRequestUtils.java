package com.example.ffmpegmcp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestRequestUtils {

    static final ObjectWriter defaultWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
    static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String createInitRequest() throws IOException {
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

        return defaultWriter.writeValueAsString(request);
    }

    public static String createListToolsRequest() throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", generateId());
        request.put("method", "tools/list");

        return defaultWriter.writeValueAsString(request);
    }

    public static String createRegisterVideoRequest(String name, String path) throws IOException {
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

        return defaultWriter.writeValueAsString(request);
    }

    public static String createVideoInfoRequest(String videoRef) throws IOException {
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

        return defaultWriter.writeValueAsString(request);
    }

    public static String createExtractClipRequest(String videoRef, String outputFile, String startTime, String duration)
            throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", generateId());
        request.put("method", "tools/call");

        Map<String, Object> params = new HashMap<>();
        params.put("name", "ffmpeg");

        Map<String, Object> arguments = new HashMap<>();
        // Note: The original command in FFmpegMcpCliTest and FFmpegMcpSimpleTest had an extra "ffmpeg" prefix.
        // Assuming the server expects the command part *after* "ffmpeg".
        // If "ffmpeg" is indeed part of the "command" field, it should be:
        // String command = String.format("ffmpeg -i {{%s}} -ss %s -t %s -c:v copy -c:a copy %s", videoRef, startTime, duration, outputFile);
        // For now, keeping it consistent with the original structure where the tool name is "ffmpeg" and command is the rest.
        // The original tests had `String.format("ffmpeg -i {{%s}} ...")` which means the command sent to the tool `ffmpeg` was `ffmpeg -i ...`.
        // This seems redundant. If the tool is `ffmpeg`, the command should be `-i {{%s}} ...`.
        // However, to minimize changes and stick to "do what they ask, but no more", I will keep the original command string format.
        String command = String.format("ffmpeg -i {{%s}} -ss %s -t %s -c:v copy -c:a copy %s", videoRef, startTime,
                duration, outputFile);
        arguments.put("command", command);
        params.put("arguments", arguments);

        request.put("params", params);

        return defaultWriter.writeValueAsString(request);
    }

    public static String createResizeVideoRequest(String videoRef, String outputFile, String width, String height)
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

        return defaultWriter.writeValueAsString(request);
    }

    public static String createExtractAudioRequest(String videoRef, String outputFile) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", generateId());
        request.put("method", "tools/call");

        Map<String, Object> params = new HashMap<>();
        params.put("name", "ffmpeg");

        Map<String, Object> arguments = new HashMap<>();
        // Original command in FFmpegMcpSimpleTest: "ffpeg -i {{%s}} -q:a 0 -map a %s" (typo ffpeg)
        // Original command in FFmpegMcpCliTest: "ffmpeg -i {{%s}} -q:a 0 -map a %s"
        // Using the correct "ffmpeg"
        String command = String.format("ffmpeg -i {{%s}} -q:a 0 -map a %s", videoRef, outputFile);
        arguments.put("command", command);
        params.put("arguments", arguments);

        request.put("params", params);

        return defaultWriter.writeValueAsString(request);
    }

    /**
     * Send a JSON-RPC notification to the MCP server.
     * 
     * @param method The notification method name
     * @param params The notification parameters (can be null)
     * @param outputStream The output stream to write to (typically clientToServer)
     * @throws IOException if writing to the stream fails
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    public static void sendNotification(String method, Object params, OutputStream outputStream) 
            throws IOException, InterruptedException {
        McpSchema.JSONRPCNotification notification = new McpSchema.JSONRPCNotification("2.0", method, params);
        String jsonNotification = objectMapper.writeValueAsString(notification);

        outputStream.write((jsonNotification + "\n").getBytes());
        outputStream.flush();

        Thread.sleep(50); // Give server time to process notification
    }

    /**
     * Send a JSON-RPC request and wait for response with timeout.
     * 
     * @param method The request method name
     * @param params The request parameters (can be null)
     * @param id The request ID
     * @param clientToServer Output stream to send request
     * @param serverOutput Input stream to read response from
     * @param timeoutMillis Timeout in milliseconds (default: 5000)
     * @return The server response as a string
     * @throws IOException if communication fails or timeout occurs
     * @throws InterruptedException if thread is interrupted
     */
    public static String sendRequestAndWaitForResponse(String method, Object params, String id,
            OutputStream clientToServer, java.io.ByteArrayOutputStream serverOutput,
            long timeoutMillis) throws IOException, InterruptedException {
        
        McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", method, id, params);
        String jsonRequest = objectMapper.writeValueAsString(request);

        serverOutput.reset(); // Clear previous response

        clientToServer.write((jsonRequest + "\n").getBytes());
        clientToServer.flush();

        long startTime = System.currentTimeMillis();

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

    /**
     * Send a JSON-RPC request and wait for response with default 5 second timeout.
     */
    public static String sendRequestAndWaitForResponse(String method, Object params, String id,
            OutputStream clientToServer, java.io.ByteArrayOutputStream serverOutput)
            throws IOException, InterruptedException {
        return sendRequestAndWaitForResponse(method, params, id, clientToServer, serverOutput, 5000);
    }
}
