Recommendations for testing the MCP server:

# Create named pipes for bidirectional communication
mkfifo /tmp/mcp-in /tmp/mcp-out

# Start your server with pipes
java -jar /tmp/myapp.jar < /tmp/mcp-in > /tmp/mcp-out 2> /tmp/mcp-stderr.log &

# In another terminal, monitor the output
tail -f /tmp/mcp-out /tmp/mcp-stderr.log

# Initialize request
echo '{"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {"roots": {"listChanged": true}}, "clientInfo": {"name": "test-client", "version": "1.0.0"}}}' > /tmp/mcp-in

# List videos request
echo '{"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}' > /tmp/mcp-in

# Or if you have a custom list_videos method
echo '{"jsonrpc": "2.0", "id": 3, "method": "list_registered_videos", "params": {"name": "test", "path": "test_path"}}' > /tmp/mcp-in

Alternative: Direct stdin approach
bash# Start server and keep stdin open
java -jar /tmp/myapp.jar 2>&1 | tee /tmp/mcp-debug.log &
SERVER_PID=$!

# Send requests directly
echo '{"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "test-client", "version": "1.0.0"}}}' | java -jar /tmp/myapp.jar

