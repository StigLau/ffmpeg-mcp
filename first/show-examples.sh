#!/bin/bash

# Simple script to show FFmpeg MCP example requests

# First compile the test class
cd /Users/stiglau/utvikling/privat/lm-ai/mcp/05.2025/modelcontextprotocol-java-sdk/ffmpeg-mcp
mvn compile test-compile -DskipTests

# Create output directory if it doesn't exist
mkdir -p /tmp/vids/output

# Run the simple test class directly
echo "Running FFmpegMcpSimpleTest to show example MCP requests..."
java -cp target/classes:target/test-classes:$(mvn dependency:build-classpath -q) com.example.ffmpegmcp.FFmpegMcpSimpleTest

echo ""
echo "These example requests show how an LLM would interact with the FFmpeg MCP server."
echo "To try it out with a real FFmpeg MCP server:"
echo "1. Start the server: java -jar target/ffmpeg-mcp.jar --advanced"
echo "2. Connect to it from Claude Desktop"
echo "3. Ask Claude to help you process a video file"