#!/bin/bash

# Simple script to show FFmpeg MCP example requests

# Go to the project directory
cd /Users/stiglau/utvikling/privat/lm-ai/mcp/05.2025/modelcontextprotocol-java-sdk/ffmpeg-mcp

# Create output directory if it doesn't exist
mkdir -p /tmp/vids/output

# Create a temporary file with hardcoded JSON examples
cat > /tmp/ffmpeg_mcp_examples.txt << EOF
FFmpeg MCP JSON-RPC Request Examples
===================================

These examples demonstrate how an LLM would interact with the FFmpeg MCP Server
to extract a few seconds from your video file at /tmp/vids/wZ5.mp4.

[1] Initialize connection with server:
{
  "jsonrpc": "2.0",
  "id": "init123",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "clientInfo": {
      "name": "llm-client",
      "version": "1.0.0"
    },
    "capabilities": {}
  }
}

[2] List available tools:
{
  "jsonrpc": "2.0",
  "id": "list456",
  "method": "tools/list"
}

[3] Register the source video:
{
  "jsonrpc": "2.0",
  "id": "reg789",
  "method": "tools/call",
  "params": {
    "name": "register_video",
    "arguments": {
      "name": "sourceVideo",
      "path": "/tmp/vids/wZ5.mp4"
    }
  }
}

[4] Get information about the video:
{
  "jsonrpc": "2.0",
  "id": "info123",
  "method": "tools/call",
  "params": {
    "name": "video_info",
    "arguments": {
      "videoref": "sourceVideo"
    }
  }
}

[5] Extract a 3-second clip from the video:
{
  "jsonrpc": "2.0",
  "id": "extract456",
  "method": "tools/call",
  "params": {
    "name": "ffmpeg",
    "arguments": {
      "command": "-i {{sourceVideo}} -ss 00:00:10 -t 00:00:03 -c:v copy -c:a copy /tmp/vids/output/clip.mp4"
    }
  }
}

[6] Convert video to lower resolution:
{
  "jsonrpc": "2.0",
  "id": "resize789",
  "method": "tools/call",
  "params": {
    "name": "ffmpeg",
    "arguments": {
      "command": "-i {{sourceVideo}} -vf \"scale=480:360\" -c:v libx264 -crf 23 -preset medium -c:a aac /tmp/vids/output/resized.mp4"
    }
  }
}

[7] Extract audio from the video:
{
  "jsonrpc": "2.0",
  "id": "audio123",
  "method": "tools/call",
  "params": {
    "name": "ffmpeg",
    "arguments": {
      "command": "-i {{sourceVideo}} -q:a 0 -map a /tmp/vids/output/audio.mp3"
    }
  }
}

To use these examples with a real FFmpeg MCP server:
1. Start the server: java -jar target/ffmpeg-mcp.jar --advanced
2. Connect to it from Claude Desktop
3. Ask Claude to help you process the video file at /tmp/vids/wZ5.mp4
EOF

# Display the examples
cat /tmp/ffmpeg_mcp_examples.txt

echo ""
echo "These examples show how you can use the FFmpeg MCP server to process video files."
echo "The examples have been saved to /tmp/ffmpeg_mcp_examples.txt for your reference."