package no.lau.mcp.ffmpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DefaultFFmpegExecutor implements FFmpegExecutor {

    private final String ffmpegExecutablePath;

    public DefaultFFmpegExecutor(String ffmpegExecutablePath) {
        this.ffmpegExecutablePath = ffmpegExecutablePath;
    }

    @Override
    public String execute(String incomingCommandArguments) throws IOException {
        // Split the incoming command arguments safely
        List<String> command = new ArrayList<>();
        command.add(this.ffmpegExecutablePath);
        
        // Parse the command arguments (handles quoted strings)
        command.addAll(parseCommandArguments(incomingCommandArguments));

        // System.err.println("DefaultFFmpegExecutor Running command: " + command);
        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();

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
    
    /**
     * Parse command arguments string into a list, handling quoted strings.
     * This helps prevent command injection by properly separating arguments.
     * 
     * Supports:
     * - Single and double quoted strings
     * - Escaped quotes within quoted strings
     * - Escaped spaces outside quotes
     * - Multiple consecutive spaces
     * - Mixed quote types
     */
    private List<String> parseCommandArguments(String args) {
        List<String> result = new ArrayList<>();
        if (args == null || args.trim().isEmpty()) {
            return result;
        }
        
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        boolean escaped = false;
        
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            
            if (escaped) {
                // Handle escaped characters
                if (inQuotes && c == quoteChar) {
                    // Escaped quote inside quoted string
                    current.append(c);
                } else if (!inQuotes && (c == ' ' || c == '\t' || c == '"' || c == '\'' || c == '\\')) {
                    // Escaped special character outside quotes
                    current.append(c);
                } else if (inQuotes) {
                    // In quotes, backslash doesn't escape non-quote chars (keep both)
                    current.append('\\').append(c);
                } else {
                    // Outside quotes, non-special char after backslash (keep both)
                    current.append('\\').append(c);
                }
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (inQuotes) {
                if (c == quoteChar) {
                    // End of quoted string
                    inQuotes = false;
                    quoteChar = 0;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    // Start of quoted string
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == ' ' || c == '\t') {
                    // Whitespace - end current argument if any
                    if (current.length() > 0) {
                        result.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            }
        }
        
        // Handle trailing backslash
        if (escaped) {
            current.append('\\');
        }
        
        // Handle unclosed quotes by treating the rest as part of the argument
        if (inQuotes) {
            // Log warning if desired
            // System.err.println("Warning: Unclosed quote in command arguments");
        }
        
        // Add any remaining argument
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
}
