package no.lau.mcp.ffmpeg.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

public class FileUtils {
    public static Optional<String> findAppPathUsingProcessBuilder(String appName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("which", appName);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String path = reader.readLine();
            process.waitFor();

            return Optional.of(path);
        }
        catch (Exception e) {
            //logger.debug("Could not find application path for {}", appName);
            System.err.println("Could not find application path for " + appName);
            return Optional.empty();
        }
    }
}
