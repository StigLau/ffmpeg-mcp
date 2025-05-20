# FFmpeg MCP - Context for Claude Code

This document provides key information for Claude Code to efficiently work with this project.

## Project Overview

FFmpeg MCP is a Java-based Model Context Protocol (MCP) server and client for FFmpeg operations. It provides:

- An MCP server implementation that wraps FFmpeg functionality
- Tools for processing and managing video files through MCP
- Testing infrastructure for validating server-client interactions

## Key Components

### FFmpegMcpServerAdvanced

Main server implementation that exposes FFmpeg functionality via MCP. It provides several tools:

- `ffmpeg`: Execute FFmpeg commands on video files
- `video_info`: Get information about a source video file
- `list_registered_videos`: List available source videos
- `addTargetVideo`: Register a target video for output

### FFmpegWrapper

Wraps FFmpeg functionality, handling file references and command execution.

### FileManager & FileManagerImpl

Manages video file storage, registration, and access.

### Testing Components

- `FFmpegFake`: Mock implementation of FFmpeg for testing
- `FFmpegMcpServerAdvancedTest`: Tests server functionality
- `McpClientShowcaseTest`: Demonstrates client-server interaction

## Known Limitations

- MCP server (v0.10.0) has limitations with Stdio transport in tests
- Only initialization request works, subsequent requests (tools/list, tools/call) time out
- For full functionality, use an actual MCP client tool rather than tests

## Common Commands

Build the project:
```bash
mvn clean package
```

Run tests:
```bash
mvn test
```

Run specific test:
```bash
mvn test -Dtest=McpClientShowcaseTest
```

Start the server:
```bash
java -jar target/ffmpeg-mcp.jar
```

## Helpful Tips

1. For MCP server interactions, check the FFmpegMcpServerAdvanced class
2. Video processing logic is in FFmpegWrapper
3. For file storage and management, look at FileManagerImpl
4. All tests use FFmpegFake instead of real FFmpeg

## Project Structure
- `src/main/java/no/lau/mcp/ffmpeg/`: Core server implementation
- `src/main/java/no/lau/mcp/file/`: File management functionality
- `src/test/java/com/example/ffmpegmcp/`: Test classes for server and client

## MCP Protocol

- Protocol Version: 2024-11-05
- Communication Method: JSON-RPC 2.0
- Transport Protocol: stdio (standard input/output)