package no.lau.mcp.ffmpeg;

import com.example.ffmpegmcp.FileManagerFake;
import no.lau.mcp.file.FileManager;
import no.lau.mcp.file.FileManagerUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegWrapperTest {

    FileManager fileManager = new FileManagerFake(Map.of());

    @Test
    public void testReferenceDoesNotExist() {
        Exception thrown = assertThrows(IllegalArgumentException.class, () ->
                fileManager.replaceVideoReferences("ffmpeg -i {{original}} -ss 0 -t 10 -c:v copy -c:a copy {{tempfile}}")
        );
        assertEquals("Video reference 'original' not found.", thrown.getMessage());
    }
    
    @Test
    public void testOutputSanitization() throws IOException {
        // Create a test implementation of FFmpegExecutor that returns paths in output
        FFmpegExecutor mockExecutor = new FFmpegExecutor() {
            @Override
            public String execute(String command) {
                return "Input file is '/tmp/vids/sources/video1.mp4'\n" +
                       "Output file is '/tmp/vids/output/processed.mp4'\n" +
                       "Processing complete for '/tmp/vids/sources/video1.mp4'";
            }
        };
        
        // Create a file manager with known video references
        Map<String, Path> testRefs = new HashMap<>();
        testRefs.put("video1", Path.of("/tmp/vids/sources/video1.mp4"));
        testRefs.put("output", Path.of("/tmp/vids/output/processed.mp4"));
        FileManager testFileManager = new FileManagerFake(testRefs);
        
        // Create the wrapper with our test components
        FFmpegWrapper wrapper = new FFmpegWrapper(testFileManager, mockExecutor);
        
        // Execute a command
        String result = wrapper.doffMPEGStuff("ffmpeg -i {{video1}} -o {{output}}");
        
        // Verify that the output has been sanitized
        String expected = "Input file is '{{video1}}'\n" +
                          "Output file is '{{output}}'\n" +
                          "Processing complete for '{{video1}}'";
        
        assertEquals(expected, result, "The output should have file paths replaced with videoRef placeholders");
    }
    
    @Test
    public void testSanitizeInformationFromVideo() throws IOException {
        // Create a file manager with known video references
        Map<String, Path> testRefs = new HashMap<>();
        Path videoPath = Path.of("/tmp/vids/sources/video2.mp4");
        testRefs.put("video2", videoPath);
        
        // Create a custom executor that returns paths in the output
        FFmpegExecutor mockExecutor = new FFmpegExecutor() {
            @Override
            public String execute(String command) {
                if (command.contains(videoPath.toString())) {
                    return "File: " + videoPath + "\n" +
                           "Duration: 00:10:00\n" + 
                           "Codec: h264";
                }
                return "Unknown file";
            }
        };
        
        FileManager testFileManager = new FileManagerFake(testRefs);
        FFmpegWrapper wrapper = new FFmpegWrapper(testFileManager, mockExecutor);
        
        // Get info about a video
        String result = wrapper.informationFromVideo("video2");
        
        // Verify paths are sanitized
        String expected = "File: {{video2}}\n" +
                          "Duration: 00:10:00\n" + 
                          "Codec: h264";
        
        assertEquals(expected, result, "The video information should have file paths replaced with videoRef placeholders");
    }
    
    @Test
    public void testSanitizationWithBothSourceAndTargetReferences() throws IOException {
        // Create a mock executor that returns output containing both source and target paths
        FFmpegExecutor mockExecutor = new FFmpegExecutor() {
            @Override
            public String execute(String command) {
                return "Processing source file '/tmp/vids/sources/source.mp4'\n" +
                       "Creating target file '/tmp/vids/targets/target.mp4'\n" +
                       "Command complete - Input: '/tmp/vids/sources/source.mp4', Output: '/tmp/vids/targets/target.mp4'";
            }
        };
        
        // Create a custom FileManagerFake with both source and target references
        Map<String, Path> sourceRefs = new HashMap<>();
        sourceRefs.put("source", Path.of("/tmp/vids/sources/source.mp4"));
        FileManager testFileManager = new FileManagerFake(sourceRefs);
        
        // Add a target reference
        testFileManager.addTargetVideoReference("target", Path.of("/tmp/vids/targets/target.mp4"));
        
        // Create the wrapper with our test components
        FFmpegWrapper wrapper = new FFmpegWrapper(testFileManager, mockExecutor);
        
        // Execute a command
        String result = wrapper.doffMPEGStuff("ffmpeg -i {{source}} -o output.mp4");
        
        // Verify that both source and target paths are sanitized
        String expected = "Processing source file '{{source}}'\n" +
                          "Creating target file '{{target}}'\n" +
                          "Command complete - Input: '{{source}}', Output: '{{target}}'";
        
        assertEquals(expected, result, "Both source and target paths should be replaced with videoRef placeholders");
    }
}
