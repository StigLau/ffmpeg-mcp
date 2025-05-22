# FFmpeg MCP Server

A server that exposes FFmpeg functionality through the Model Context Protocol (MCP). This enables AI Large Language Models (LLMs) such as Claude to interact with FFmpeg to process video and audio files.

## Features

### Basic Server
- Execute FFmpeg commands via the `ffmpeg` tool
- Support for dynamic video reference placeholder (`{{videoref}}`)
- Error handling and reporting

### Advanced Server
- All basic server features
- Video information retrieval via the `video_info` tool
- Video reference registration via the `register_video` tool
- Enhanced reference handling with named video references

## Requirements

- JDK 21 or later
- Maven 3.6 or later
- FFmpeg (for real implementation; the current mock implementation doesn't require it)

## Building the Project

```bash
mvn clean package
```

This will generate an executable JAR file in the `target` directory.

## Running the Server

### Basic Server

```bash
java -jar target/ffmpeg-mcp.jar
```

### Advanced Server

```bash
java -jar target/ffmpeg-mcp.jar --advanced
```

## Using with Claude Desktop

1. Launch the FFmpeg MCP server
2. In Claude Desktop, configure it to connect to the MCP server (usually `localhost` with the standard MCP port)
3. Ask Claude to perform video operations using the provided tools

## Available Tools

### Basic Server

#### ffmpeg

Execute FFmpeg commands to process video and audio files.

**Parameters:**
- `command`: The FFmpeg command to execute. Use `{{videoref}}` as a placeholder for video files.

**Example usage with Claude:**
```
Could you convert a video to MP4 format?
```

Claude can then use the `ffmpeg` tool with a command like:
```
ffmpeg -i {{videoref}} -c:v libx264 -c:a aac output.mp4
```

### Advanced Server

#### ffmpeg

Same as basic server.

#### video_info

Get information about a video file.

**Parameters:**
- `videoref`: The reference to the video file to get information about.

**Example usage with Claude:**
```
What's the resolution and duration of my video?
```

#### register_video

Register a video file with a friendly name for easy reference.

**Parameters:**
- `name`: A friendly name to reference the video by.
- `path`: The file path to the video.

**Example usage with Claude:**
```
I want to process my vacation video. It's located at /videos/vacation2023.mov
```

Claude can register the video:
```
register_video tool with name="vacation" and path="/videos/vacation2023.mov"
```

And then use it in commands:
```
ffmpeg -i {{vacation}} -vf "scale=1280:720" -c:v libx264 -crf 23 -preset medium -c:a aac -b:a 128k vacation_720p.mp4
```

## Implementation Notes

This is currently a mock implementation. The FFmpegWrapper class simulates FFmpeg functionality without actually executing commands. In a real implementation, you would:

1. Replace the FFmpegWrapper with actual FFmpeg command execution
2. Add proper file validation and security checks
3. Enhance error handling for real FFmpeg error scenarios
4. Add real video metadata extraction for the video_info tool

## Testing

### Unit and Integration Tests

Run the tests using Maven:

```bash
mvn test
```

To run a specific test:

```bash
mvn test -Dtest=McpClientShowcaseTest
```

### Test Showcase Classes

- `FFmpegMcpServerAdvancedTest`: Tests individual features of the advanced server
- `FFmpegMcpShowcaseTest`: Showcases server functionality with direct JSON-RPC interactions
- `McpClientShowcaseTest`: Simulates an LLM-based client interacting with the server

### Known Testing Limitations

The MCP server implementation (v0.10.0) has a limitation where only the initialization call works properly when using the Stdio transport in tests. Subsequent requests (tools/list, tools/call) time out without receiving responses. This appears to be an issue with how the MCP server handles stdio communications in a test environment.

For a full demonstration of server capabilities, use an actual MCP client tool rather than tests.

## Example JSON-RPC Commands

Initialize connection:
```
echo '{"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"claude-ai","version":"0.1.0"}},"jsonrpc":"2.0","id":0}' | java -jar target/ffmpeg-0.1.4.jar
```

List tools:
```
echo '{"method":"tools/list","params":null,"jsonrpc":"2.0","id":1}' | java -jar target/ffmpeg-0.1.4.jar
```

## Running with Output Captures

Capture both stdout and stderr to separate files:

```bash
java -jar target/ffmpeg-mcp.jar > stdout.log 2> stderr.log
```

## License

This project is licensed under the same license as the parent Model Context Protocol Java SDK.



##Fixes

Based on my analysis, here are the key improvements for your FFmpeg MCP project:

Critical Issues:
- Replace unsafe Runtime.exec() with ProcessBuilder to prevent command injection
- Fix static fields anti-pattern in FileManager interface
- Standardize logging (currently mixing System.err.println and SLF4J)

Architecture Improvements:
- Extract hardcoded paths to configuration
- Implement async processing for FFmpeg operations
- Add proper error handling with custom exceptions
- Create a dedicated video registry service

Testing & Security:
- Add tests for DefaultFFmpegExecutor and FileHasher
- Replace MD5 hashing with SHA-256
- Add input validation and sanitization
- Implement integration tests with real FFmpeg

Quick Wins:
- Enable the commented-out SLF4J loggers
- Add JavaDoc to public methods
- Configure Maven properly (remove commented dependencies)
- Implement try-with-resources for resource cleanup
