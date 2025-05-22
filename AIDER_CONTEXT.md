# FFmpeg MCP Project Context for Aider

## Project Overview

FFmpeg MCP is a Java-based Model Context Protocol (MCP) server that enables Large Language Models (LLMs) like Claude to interact with FFmpeg for video/audio processing. The project is built with Java 17, Maven, and the MCP SDK v0.10.0.

## Key Architecture Components

### Core Server Implementation (`src/main/java/no/lau/mcp/ffmpeg/`)
- `FFmpegMcpServerAdvanced`: Main MCP server implementation with stdio transport
- `FFmpegWrapper`: Abstraction layer for FFmpeg command execution  
- `DefaultFFmpegExecutor`: Real FFmpeg command execution (using ProcessBuilder)
- `FFmpegExecutor`: Interface for command execution
- `FileHasher`: Utility for generating file hashes (currently MD5, should migrate to SHA-256)

### File Management (`src/main/java/no/lau/mcp/file/`)
- `FileManager`: Interface for video file registration and management
- `FileManagerImpl`: Implementation with in-memory storage and file operations
- `FileManagerUtils`: Static utilities for file operations

### MCP Tools Exposed
- `ffmpeg`: Execute FFmpeg commands with video reference placeholders
- `video_info`: Retrieve metadata about registered videos
- `list_registered_videos`: List all available source videos
- `addTargetVideo`: Register target video files for output

### Test Infrastructure (`src/test/java/com/example/ffmpegmcp/`)
- `FFmpegFake`: Mock FFmpeg implementation for testing
- `FileManagerFake`: Mock file manager for testing
- Integration and unit tests for server functionality

## Known Issues & Limitations

### MCP Server Transport Issue
- MCP server (v0.10.0) only works for initialization in test environment
- Subsequent requests (tools/list, tools/call) time out with stdio transport
- Use real MCP client tools for full functionality testing

### Security & Architecture Issues
- Uses Runtime.exec() instead of ProcessBuilder (command injection risk)
- Static fields anti-pattern in FileManager interface  
- Mixed logging (System.err.println + SLF4J)
- MD5 hashing instead of SHA-256
- Hardcoded file paths need configuration externalization

## Build & Run Commands

```bash
# Clean build
mvn clean package

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FFmpegMcpServerAdvancedTest

# Start MCP server (advanced mode)
java -jar target/ffmpeg-0.3.0.jar --advanced

# Start basic server
java -jar target/ffmpeg-0.3.0.jar
```

## Development Commands

```bash
# Check for security vulnerabilities
mvn dependency-check:check

# Generate dependency tree
mvn dependency:tree

# Run with debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar target/ffmpeg-0.3.0.jar
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