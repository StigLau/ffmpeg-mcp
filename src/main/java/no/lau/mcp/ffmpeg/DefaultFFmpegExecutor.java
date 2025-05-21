package no.lau.mcp.ffmpeg;

import jakarta.inject.Inject;
import no.lau.mcp.config.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DefaultFFmpegExecutor implements FFmpegExecutor {

    private final String ffmpegExecutablePath;
    // Assuming AppConfig and Inject are imported. If not, add:
    // import no.lau.mcp.config.AppConfig;
    // import jakarta.inject.Inject;

    @Inject
    public DefaultFFmpegExecutor(AppConfig config) {
        this.ffmpegExecutablePath = config.getFfmpegPath();
    }

    @Override
    public String execute(String incomingCommandArguments) throws IOException {
        String fullCommand = this.ffmpegExecutablePath + " " + incomingCommandArguments;

        // System.err.println("DefaultFFmpegExecutor Running command: '" + fullCommand + "'");
        Process p = Runtime.getRuntime().exec(fullCommand);

        StringBuilder resultBuilder = new StringBuilder();
        // Try-with-resources to ensure streams are closed
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
             BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {

            String s;
            while ((s = stdInput.readLine()) != null) {
                resultBuilder.append(s).append(System.lineSeparator());
                // Printing to console, to keep end user updated
                System.out.print(s + "\r"); // Overwrite line
                System.out.print(s);        // Print line
            }

            resultBuilder.append(System.lineSeparator()).append("--- STDERR ---").append(System.lineSeparator());
            while ((s = stdError.readLine()) != null) {
                resultBuilder.append(s).append(System.lineSeparator());
            }
        }

        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                // Optionally log or include exit code in a more structured error
                // For now, the stderr content should indicate the failure
                System.err.println("FFmpeg process exited with code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg execution interrupted", e);
        }

        return resultBuilder.toString();
    }
}
