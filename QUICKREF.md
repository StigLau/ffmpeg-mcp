# FFmpeg MCP Quick Reference

## Essential Commands

### Build & Test
```bash
# Clean build
mvn clean package

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=FFmpegMcpServerAdvancedTest

# Build with debug info
mvn clean package -X
```

### Run Server
```bash
# Advanced server (recommended)
java -jar target/ffmpeg-0.3.0.jar --advanced

# Basic server
java -jar target/ffmpeg-0.3.0.jar

# With debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar target/ffmpeg-0.3.0.jar --advanced
```

## JSON-RPC Commands

### Initialize Connection
```json
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "id": "init-1",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "implementation": {
      "name": "mcp-client",
      "version": "1.0.0"
    }
  }
}
```

### List Tools
```json
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": "list-tools-1"
}
```

### List Registered Videos
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "id": "list-videos-1",
  "params": {
    "name": "list_registered_videos",
    "arguments": {}
  }
}
```

### Get Video Info
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "id": "video-info-1",
  "params": {
    "name": "video_info",
    "arguments": {
      "videoref": "video_id_or_path"
    }
  }
}
```

### Add Target Video
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "id": "add-target-1",
  "params": {
    "name": "addTargetVideo",
    "arguments": {
      "targetName": "output_video"
    }
  }
}
```

### Execute FFmpeg Command
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "id": "ffmpeg-1",
  "params": {
    "name": "ffmpeg",
    "arguments": {
      "command": "ffmpeg -i {{source_id}} -c:v copy -an {{target_id}}"
    }
  }
}
```

## Project Structure

- `src/main/java/no/lau/mcp/ffmpeg/`: Core implementation
  - `FFmpegMcpServerAdvanced.java`: Main server class
  - `FFmpegWrapper.java`: Wraps FFmpeg functionality
  - `DefaultFFmpegExecutor.java`: Real FFmpeg executor
  - `FFmpegExecutor.java`: Interface for FFmpeg execution
  - `FileHasher.java`: Hashing utility for file IDs

- `src/main/java/no/lau/mcp/file/`: File management
  - `FileManager.java`: Interface for file operations
  - `FileManagerImpl.java`: Implementation of file management

- `src/test/java/com/example/ffmpegmcp/`: Test classes
  - `FFmpegMcpServerAdvancedTest.java`: Main server tests
  - `FFmpegMcpShowcaseTest.java`: Server showcase test
  - `McpClientShowcaseTest.java`: Client simulation test

## Known Limitations

- **MCP Testing**: Only initialization works in test environment due to stdio transport issues
- **Security**: DefaultFFmpegExecutor uses ProcessBuilder but validate inputs carefully
- **File Paths**: Hardcoded paths in `/tmp/mcp-videos/` - needs configuration
- **Hashing**: Currently uses MD5, consider SHA-256 for production

## Key Classes to Know

| Class | Location | Purpose |
|-------|----------|---------|
| `FFmpegMcpServerAdvanced` | `src/main/java/no/lau/mcp/ffmpeg/` | Main MCP server |
| `FFmpegWrapper` | `src/main/java/no/lau/mcp/ffmpeg/` | FFmpeg command wrapper |
| `FileManagerImpl` | `src/main/java/no/lau/mcp/file/` | File registration & storage |
| `DefaultFFmpegExecutor` | `src/main/java/no/lau/mcp/ffmpeg/` | Real FFmpeg execution |
| `FFmpegFake` | `src/test/java/no/lau/mcp/ffmpeg/` | Mock for testing |

## Development Tips

- Use `FFmpegFake` for unit tests to avoid FFmpeg dependency
- Check `FFmpegMcpServerAdvancedTest` for server behavior examples
- File operations go through `FileManagerImpl` with in-memory registry
- All MCP tools are defined in `FFmpegMcpServerAdvanced.createTools()`