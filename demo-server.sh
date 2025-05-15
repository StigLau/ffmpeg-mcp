#!/bin/bash

# Script to demonstrate a real FFmpeg MCP Server instance

# Go to the project directory
cd /Users/stiglau/utvikling/privat/lm-ai/mcp/05.2025/modelcontextprotocol-java-sdk/ffmpeg-mcp

# Ensure the output directory exists
mkdir -p /tmp/vids/output

# Check if test video exists, create a dummy if it doesn't
if [ ! -f "/tmp/vids/wZ5.mp4" ]; then
  echo "Test video not found at /tmp/vids/wZ5.mp4"
  echo "Creating a dummy file for demonstration purposes."
  echo "DUMMY VIDEO FILE" > /tmp/vids/wZ5.mp4
  echo "(Note: This is just a text file, not a real video)"
fi

# Compile the project (skip tests)
echo "Building FFmpeg MCP Server..."
mvn clean package -DskipTests

# Check if the build succeeded
if [ ! -f "target/classes/com/example/ffmpegmcp/FFmpegMcpServerAdvanced.class" ]; then
  echo "Build failed. Please check the project setup."
  exit 1
fi

# Start the server in the background
echo "Starting FFmpeg MCP Server in advanced mode..."
java -cp target/classes:$(mvn dependency:build-classpath -q) com.example.ffmpegmcp.Main --advanced &
SERVER_PID=$!

# Wait for server to start
echo "Waiting for server to initialize..."
sleep 2

echo "FFmpeg MCP Server is now running (PID: $SERVER_PID)"
echo ""
echo "To interact with the server, you would normally use an MCP client like Claude Desktop."
echo "For demonstration purposes, we can send JSON-RPC requests manually."
echo ""
echo "Choose a demonstration option:"
echo "1. Show example requests"
echo "2. Send actual requests to the server (requires nc or curl)"
echo "3. Exit"
read -p "Option: " option

case $option in
  1)
    # Show example requests
    /Users/stiglau/utvikling/privat/lm-ai/mcp/05.2025/modelcontextprotocol-java-sdk/ffmpeg-mcp/show-ffmpeg-examples.sh
    ;;
  2)
    # Check if netcat is available
    if command -v nc &> /dev/null; then
      echo "You can use this command to send requests manually:"
      echo "echo '{\"jsonrpc\": \"2.0\", \"id\": \"1\", \"method\": \"tools/list\"}' | nc localhost 5000"
      echo ""
      echo "However, the current implementation uses stdio, not a network socket."
      echo "Please connect using Claude Desktop or another MCP client."
    else
      echo "Netcat (nc) not found. The current implementation uses stdio, not a network socket."
      echo "Please connect using Claude Desktop or another MCP client."
    fi
    ;;
  *)
    echo "Exiting..."
    ;;
esac

# Cleanup
echo "Shutting down server..."
kill $SERVER_PID
echo "Demo complete!"