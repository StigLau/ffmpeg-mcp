package no.lau.mcp.ffmpeg;

import no.lau.mcp.file.FileManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegWrapperTest {

    //FileManager fileManager = new FileManager("/tmp/vids/sources", "/tmp/vids/outputs");

    @Test
    public void testReferenceDoesNotExist() {

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> {
                    Map<String, Path> videoReferences = Map.of();
                    FileManager.replaceVideoReferences("ffmpeg -i {{original}} -ss 0 -t 10 -c:v copy -c:a copy {{tempfile}}", videoReferences);
                }
        );
        assertEquals("Video reference 'original' not found.", thrown.getMessage());
    }
}
