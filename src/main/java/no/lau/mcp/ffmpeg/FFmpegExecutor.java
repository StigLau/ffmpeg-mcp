package no.lau.mcp.ffmpeg;

import java.io.IOException;

/**
 * Interface for executing FFmpeg commands.
 */
public interface FFmpegExecutor {
    /**
     * Executes the given FFmpeg command string.
     *
     * @param command The command string (arguments only, executable path is handled by implementation).
     * @return The output from FFmpeg (stdout and stderr).
     * @throws IOException if an error occurs during execution.
     */
    String execute(String command) throws IOException;
}
