# FFmpeg MCP Quick Reference

## Key Commands

### Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
mvn test -Dtest=McpClientShowcaseTest
```

### Run Server
```bash
java -jar target/ffmpeg-mcp.jar --advanced
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

In tests, only the initialization call works properly due to limitations in the MCP server (v0.10.0) stdio handling. For a full demonstration, use an actual MCP client tool.