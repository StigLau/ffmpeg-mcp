#!/bin/bash


echo "Starting MCP server..."
java -jar target/ffmpeg-0.3.6.jar 2>&1 | tee /tmp/mcp-debug.log &
SERVER_PID=$!

sleep 2  # Give server time to start

echo "Sending initialize..."
echo '{"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "test-client", "version": "1.0.0"}}}' >&${SERVER_PID}

sleep 1

echo "Sending list videos..."
echo '{"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}' >&${SERVER_PID}

# Keep running for a bit to see responses
sleep 5

kill $SERVER_PID