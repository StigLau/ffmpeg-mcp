package no.lau.mcp.config;

import jakarta.inject.Singleton;

@Singleton
public class AppConfig {

    private final String ffmpegPath;
    private final String sourceFolder;
    private final String outputFolder;

    public AppConfig() {
        ffmpegPath = getConfigValue("FFMPEG_PATH", "/usr/local/bin/ffmpeg");
        sourceFolder = getConfigValue("SOURCE_FOLDER", "/tmp/vids/sources");
        outputFolder = getConfigValue("OUTPUT_FOLDER", "/tmp/vids/outputs");
    }

    private String getConfigValue(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }
        value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public String getFfmpegPath() { return ffmpegPath; }
    public String getSourceFolder() { return sourceFolder; }
    public String getOutputFolder() { return outputFolder; }
}
