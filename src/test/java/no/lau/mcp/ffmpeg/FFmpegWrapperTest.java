package no.lau.mcp.ffmpeg;

import com.example.ffmpegmcp.FileManagerFake;
import no.lau.mcp.file.FileManager;
import org.junit.jupiter.api.Test;

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
}
