package no.lau.mcp.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for FileManager operations.
 * Contains static helper methods that were previously in the FileManager interface.
 */
public final class FileManagerUtils {
    
    private static final Logger log = LoggerFactory.getLogger(FileManagerUtils.class);
    private static final Pattern VIDEO_REF_PATTERN = Pattern.compile("\\{\\{([^{}]+)}}");
    
    // Private constructor to prevent instantiation
    private FileManagerUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Extract IDs from placeholders in the format {{id}} from the given text.
     * @param text The text containing potential {{id}} placeholders
     * @return List of extracted IDs (without the braces)
     */
    public static List<String> extractIds(String text) {
        Matcher matcher = VIDEO_REF_PATTERN.matcher(text);
        
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            // Extract just the ID part without the braces
            ids.add(matcher.group(1));
        }
        return ids;
    }
    
    /**
     * Replace video references in the command with their actual paths.
     * @param command The command with potential {{videoref}} placeholders
     * @param videoReferences Map of video reference IDs to their paths
     * @return The command with resolved video references
     * @throws IllegalArgumentException if a video reference is not found
     */
    public static String replaceVideoReferences(String command, Map<String, Path> videoReferences) {
        // First check for direct {{name}} references
        log.debug("Replace in FFmpeg command: " + command);
        
        for (String id : extractIds(command)) {
            if (!videoReferences.containsKey(id)) {
                log.error("No video reference found for ID: " + id);
                throw new IllegalArgumentException("Video reference '" + id + "' not found.");
            }
            String path = videoReferences.get(id).toAbsolutePath().toString();
            command = command.replace("{{" + id + "}}", path);
        }
        // If we still have {{videoref}}, use the default replacement from FFmpegWrapper
        return command;
    }
}