package no.lau.mcp.ffmpeg;

import no.lau.mcp.file.FileManager;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdExtractorTest {
    @Test
    public void testIdExtractor() {
        String text = "hello {{myid}} and goodbye {{yourname}} {{ostel  off mordi 123asdSAD asd\n}}";
        assertEquals(List.of("myid", "yourname", "ostel  off mordi 123asdSAD asd\n"), FileManager.extractIds(text));
    }
}