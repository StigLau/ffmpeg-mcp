package no.lau.mcp.ffmpeg;

import no.lau.mcp.file.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static no.lau.mcp.file.FileManager.replaceVideoReferences;

public class FFmpegWrapper {

	static Logger logger = LoggerFactory.getLogger(FFmpegWrapper.class);
	//static String ffprobe = findAppPathUsingProcessBuilder("ffprobe").get();
	static String ffmpeg = findAppPathUsingProcessBuilder("ffmpeg").get();

	private final Map<String, Path> videoReferences;


	FFmpegWrapper(Map<String, Path> videoReferences) {
        this.videoReferences = videoReferences;
    }

	/**
	 * Resolve a video reference to its actual path.
	 * @param videoRef The video reference name
	 * @return The resolved path
	 * @throws IllegalArgumentException if the reference cannot be resolved
	 */
	private Path resolveVideoReference(String videoRef) {
		return videoReferences.get(videoRef);
	}

	/**
	 * Provides access to the source video references known by this wrapper.
	 * @return A map of source video reference names to their paths.
	 */
	public Map<String, Path> getVideoReferences() {
		return this.videoReferences;
	}

	public String doffMPEGStuff(String cmd, Map<String, Path> allReferences) throws IOException {
		String command = replaceVideoReferences(cmd, allReferences);

		// Log the incoming command
		//System.err.println("Executing FFmpeg command: " + command);

		// Execute the command through our wrapper
		return FFmpegWrapper.performFFMPEG(command);
	}

	public static String performFFMPEG(String incommingCommand) throws IOException {
		String command = ffmpeg + " " + incommingCommand;

		// Make sure destination folder has been created!
		//logger.info("Running command: '{}'", command);
		//System.err.println("Running command: '" + command + "'");

		Process p = Runtime.getRuntime().exec(command);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String s;
		String result = "";
		while ((s = stdInput.readLine()) != null) {
			result += s + "\n";
			// Printing to console, to keep end user updated
			System.out.print(s + "\r");
			System.out.print(s);
		}

		BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		result += "\n\n\n";
		while ((s = stdError.readLine()) != null) {
			result += s + "\n";
		}

		return result;
	}

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
