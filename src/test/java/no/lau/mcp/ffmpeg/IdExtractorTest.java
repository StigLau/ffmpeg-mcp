package no.lau.mcp.ffmpeg;

import org.junit.jupiter.api.Test;
import java.util.List;
import static no.lau.mcp.file.FileManager.extractIds;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdExtractorTest {
    @Test
    public void testIdExtractor() {
        String text = "hello {{myid}} and goodbye {{yourname}} {{ostel  off mordi 123asdSAD asd\n}}";
        assertEquals(List.of("myid", "yourname", "ostel  off mordi 123asdSAD asd\n"), extractIds(text));
    }
}