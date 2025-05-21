package no.lau.mcp.ffmpeg;

import no.lau.mcp.file.FileManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class FFmpegWrapper {

	//static Logger logger = LoggerFactory.getLogger(FFmpegWrapper.class);
	//static String ffprobe = findAppPathUsingProcessBuilder("ffprobe").get();

    private final FileManager fileManager;
    private final FFmpegExecutor executor;


	public FFmpegWrapper(FileManager fileManager, FFmpegExecutor executor) {
        this.fileManager = fileManager;
        this.executor = executor;
    }


	public String doffMPEGStuff(String cmd) throws IOException {
		String commandArguments = fileManager.replaceVideoReferences(cmd);

		// Log the incoming command
		//System.err.println("Executing FFmpeg command (args only): " + commandArguments);

		// Execute the command through the injected executor
		//TODO Replace actual fileref like /tmp/vids/sources/wZ5.mp4 with the videoRef !!
		return this.executor.execute(commandArguments);
	}

	/**
	 * Executes raw FFMPEG command.
	 * @param commandArguments The command arguments to pass to FFmpeg.
	 * @return The output from FFmpeg.
	 * @throws IOException If an error occurs during execution.
	 */
	public String executeDirectCommand(String commandArguments) throws IOException {
		return this.executor.execute(commandArguments);
	}

	public String informationFromVideo(String videoRef) throws IOException {
		Path resolvedVideoPath = fileManager.listVideoReferences().get(videoRef);
		if(resolvedVideoPath != null) {
			return executeDirectCommand("-i " + resolvedVideoPath);
		} else {
			throw new FileNotFoundException(videoRef);
		}
	}

	public FileManager fileManager() {
		return fileManager;
	}
}
