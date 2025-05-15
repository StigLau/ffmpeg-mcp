#!/bin/bash

# Script to demonstrate the FFmpeg MCP Server with a test video

# Ensure the output directory exists
mkdir -p /tmp/vids/output

# Check if test video exists
if [ ! -f "/tmp/vids/wZ5.mp4" ]; then
  echo "Test video file not found at /tmp/vids/wZ5.mp4"
  echo "Please ensure the test video exists, or update the script with the correct path."
  exit 1
fi

# Build the project (skip tests since they might not be compatible yet)
echo "Building the FFmpeg MCP Server..."
mvn clean package -DskipTests

# Start the server in background
echo "Starting the FFmpeg MCP Server in advanced mode..."
SERVER_PID=""
if [ -f "target/ffmpeg-mcp.jar" ]; then
  # If the jar was built successfully
  java -jar target/ffmpeg-mcp.jar --advanced &
  SERVER_PID=$!
else
  # Fallback to running from classes
  echo "Jar not found, running from classes..."
  java -cp target/classes com.example.ffmpegmcp.Main --advanced &
  SERVER_PID=$!
fi

# Wait for server to start
echo "Waiting for server to initialize..."
sleep 2

# Run the CLI test
echo "Running the FFmpeg MCP CLI Test..."
echo ""
echo "This test will show you the JSON-RPC requests an LLM would send to perform:"
echo "1. Registering your video file: /tmp/vids/wZ5.mp4"
echo "2. Getting information about the video"
echo "3. Extracting a 3-second clip"
echo "4. Converting to a lower resolution"
echo "5. Extracting audio from the video"
echo ""
echo "Follow the prompts in the CLI test to see each step."
echo ""

if [ -f "target/test-classes/com/example/ffmpegmcp/FFmpegMcpCliTest.class" ]; then
  # If the test was compiled successfully
  java -cp target/classes:target/test-classes com.example.ffmpegmcp.FFmpegMcpCliTest
else
  # Compile the test on the fly
  echo "Test class not found, compiling on the fly..."
  javac -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=test)" \
    -d target/classes \
    src/test/java/com/example/ffmpegmcp/FFmpegMcpCliTest.java
  
  # Run the test
  java -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=test)" \
    com.example.ffmpegmcp.FFmpegMcpCliTest
fi

# Cleanup
echo "Cleaning up..."
if [ ! -z "$SERVER_PID" ]; then
  kill $SERVER_PID
fi
echo "Demo complete!"