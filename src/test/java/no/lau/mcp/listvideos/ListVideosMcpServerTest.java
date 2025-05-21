package no.lau.mcp.listvideos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListVideosMcpServerTest {

    private final ListVideosMcpServer server = new ListVideosMcpServer(new StdioServerTransportProvider(new ObjectMapper()));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testListVideos_defaultPath() throws Exception {
        // Prepare arguments for the handler method
        Map<String, Object> args = Collections.emptyMap();

        // Invoke the handler method directly
        CallToolResult result = server.handleListVideos(null, args);

        // Assert that there is no error
        assertFalse(result.getIsError(), "Result should not be an error");

        // Get the text content (JSON string)
        String jsonResult = result.getContent().get(0).getText();
        assertNotNull(jsonResult, "JSON result should not be null");

        // Parse the JSON string into List<Map<String, String>>
        List<Map<String, String>> videos = objectMapper.readValue(jsonResult, new TypeReference<>() {});

        // Assert the list content
        assertNotNull(videos, "Videos list should not be null");
        assertEquals(2, videos.size(), "Videos list should contain 2 items");

        // Assert details of the first video
        Map<String, String> video1 = videos.get(0);
        assertEquals("video1.mp4", video1.get("name"), "First video name mismatch");
        assertEquals("./sample_videos/video1.mp4", video1.get("path"), "First video path mismatch");

        // Assert details of the second video
        Map<String, String> video2 = videos.get(1);
        assertEquals("video2.mov", video2.get("name"), "Second video name mismatch");
        assertEquals("./sample_videos/video2.mov", video2.get("path"), "Second video path mismatch");
    }

    @Test
    void testListVideos_specificPathNotSupported() throws Exception {
        // Prepare arguments for the handler method
        Map<String, Object> args = Map.of("directory_path", "some/other/path");

        // Invoke the handler method directly
        CallToolResult result = server.handleListVideos(null, args);

        // Assert that there is no error (as per current implementation, it returns an empty list)
        assertFalse(result.getIsError(), "Result should not be an error for specific path");

        // Get the text content (JSON string)
        String jsonResult = result.getContent().get(0).getText();
        assertNotNull(jsonResult, "JSON result should not be null for specific path");

        // Parse the JSON string into List<Map<String, String>>
        List<Map<String, String>> videos = objectMapper.readValue(jsonResult, new TypeReference<>() {});

        // Assert that the list is empty
        assertNotNull(videos, "Videos list should not be null for specific path");
        assertTrue(videos.isEmpty(), "Videos list should be empty for a specific, unsupported path");
    }
}
