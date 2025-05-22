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
mvn test -Dtest=FFmpegMcpServerAdvancedTest
```

Start the server:
```bash
# Advanced server (recommended)
java -jar target/ffmpeg-0.3.0.jar --advanced

# Basic server
java -jar target/ffmpeg-0.3.0.jar
```

Development tools:
```bash
# Debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar target/ffmpeg-0.3.0.jar --advanced

# Dependency analysis
mvn dependency:tree
```

## Key File Locations

- **Main server**: `src/main/java/no/lau/mcp/ffmpeg/FFmpegMcpServerAdvanced.java`
- **FFmpeg wrapper**: `src/main/java/no/lau/mcp/ffmpeg/FFmpegWrapper.java`
- **File management**: `src/main/java/no/lau/mcp/file/FileManagerImpl.java`
- **Main tests**: `src/test/java/com/example/ffmpegmcp/FFmpegMcpServerAdvancedTest.java`
- **Mock implementations**: `src/test/java/no/lau/mcp/ffmpeg/FFmpegFake.java`

## Development Notes

### Current Architecture
- Uses MCP SDK v0.10.0 with stdio transport
- Mock FFmpeg implementation for testing (FFmpegFake)
- In-memory file registry with FileManagerImpl
- JSON-RPC 2.0 communication over stdio

### Recent Security Fixes Applied
- Replaced Runtime.exec() with ProcessBuilder in DefaultFFmpegExecutor
- Fixed static field anti-patterns in FileManager interface
- Improved error handling and input validation
- Enhanced test coverage

### Testing Strategy
- Unit tests use FFmpegFake to avoid FFmpeg dependency
- Integration tests demonstrate MCP protocol interaction
- Known limitation: MCP stdio transport only works for initialization in tests

## MCP Protocol Details

- **Protocol Version**: 2024-11-05
- **Communication**: JSON-RPC 2.0 over stdio
- **Tools**: ffmpeg, video_info, list_registered_videos, addTargetVideo
- **Client Support**: Designed for LLM clients like Claude Desktop