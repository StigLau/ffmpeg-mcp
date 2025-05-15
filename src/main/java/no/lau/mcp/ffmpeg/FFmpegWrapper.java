package no.lau.mcp.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class FFmpegWrapper {

	static Logger logger = LoggerFactory.getLogger(FFmpegWrapper.class);
	static String ffprobe = findAppPathUsingProcessBuilder("ffprobe").get();
	static String ffmpeg = findAppPathUsingProcessBuilder("ffmpeg").get();

	public static String performFFMPEG(String command) throws IOException {
		String newCommand;
		if (command.startsWith("ffprobe")) {
			newCommand = command.replaceFirst("ffprobe", ffprobe);
		}
		else if (command.startsWith("ffmpeg")) {
			newCommand = command.replaceFirst("ffmpeg", ffmpeg);
		}
		else {
			newCommand = ffmpeg + " " + command;
		}
		return performFFMPEGWrapped(newCommand);
	}

	public static String performFFMPEGWrapped(String command) throws IOException {
		// Make sure destination folder has been created!
		logger.info("Running command: '{}'", command);

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
			logger.debug("Could not find application path for {}", appName);
			return Optional.empty();
		}
	}

}
