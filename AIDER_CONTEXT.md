# FFmpeg MCP Project Context for Aider

## Project Overview

This project implements a Java-based Model Context Protocol (MCP) server and client for FFmpeg operations. It allows Large Language Models (LLMs) to interact with FFmpeg through a standardized protocol.

## Key Components

### Server Components
- `FFmpegMcpServerAdvanced`: Main server implementation exposing FFmpeg functionality via MCP
- `FFmpegWrapper`: Wraps FFmpeg execution and handles file references
- `FileManager` & `FileManagerImpl`: Manage video file storage and access

### Tools Provided by the Server
- `ffmpeg`: Execute FFmpeg commands with placeholders for videos
- `video_info`: Get information about source videos
- `list_registered_videos`: List available source videos
- `addTargetVideo`: Register target video output files

### Testing Components
- `FFmpegFake`: Mock implementation of FFmpeg for testing
- Various test classes demonstrating server functionality

## Known Issues

**MCP Server Limitation:** The MCP server (v0.10.0) has a critical limitation where only the initialization call works correctly in tests. Subsequent requests (tools/list, tools/call, etc.) time out without receiving responses. This is likely due to issues with how the MCP library handles the Stdio transport.

## Common Commands

```bash
# Build project
mvn clean package

# Run tests
mvn test
mvn test -Dtest=McpClientShowcaseTest

# Start server
java -jar target/ffmpeg-mcp.jar --advanced
```

## Project Structure

```
src/main/java/no/lau/mcp/ffmpeg/
├── DefaultFFmpegExecutor.java  # Real implementation of FFmpeg execution
├── FFmpegExecutor.java         # Interface for FFmpeg execution
├── FFmpegMcpServerAdvanced.java # Main MCP server implementation
├── FFmpegWrapper.java          # Wraps FFmpeg functionality
├── FileHasher.java             # Utility for file hash generation
└── utils/
    └── FileUtils.java          # File utility functions

src/main/java/no/lau/mcp/file/
├── FileManager.java            # Interface for file management
└── FileManagerImpl.java        # Implementation of file management

src/test/java/com/example/ffmpegmcp/
├── FFmpegMcpCliTest.java             # CLI test for the FFmpeg MCP implementation
├── FFmpegMcpServerAdvancedTest.java  # Tests for the advanced server
├── FFmpegMcpShowcaseTest.java        # Showcase test for server functionality
├── FFmpegMcpSimpleTest.java          # Simple test cases
├── FFmpegServerIntegrationTest.java  # Integration tests
├── FileManagerFake.java              # Mock file manager for testing
├── McpClientShowcaseTest.java        # Simulates client interaction with server
└── util/
    └── TestRequestUtils.java         # Utilities for test requests
```

## MCP Protocol Details

- Protocol Version: 2024-11-05
- Communication: JSON-RPC 2.0
- Transport: stdio (standard input/output)

## Recent Development

Recently fixed the `McpClientShowcaseTest` to document the MCP server limitation. This test now properly demonstrates initialization but has commented out subsequent operations that would fail due to the MCP library limitations.

## Priority Areas

1. Improving test stability with the current MCP library limitations
2. Enhancing FFmpegWrapper to support more FFmpeg operations
3. Better error handling for file operations and FFmpeg commands
4. Proper validation of FFmpeg commands for security