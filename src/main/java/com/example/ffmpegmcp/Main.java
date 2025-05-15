package com.example.ffmpegmcp;

/**
 * Main class for launching the FFmpeg MCP Server. This executable class provides options
 * to start either the basic or advanced server.
 */
public class Main {

	/**
	 * Main method to start the FFmpeg MCP Server.
	 * @param args Command line arguments. Use "--advanced" to start the advanced server
	 * with additional tools.
	 */
	public static void main(String[] args) {
		boolean useAdvancedServer = false;

		// Parse command line arguments
		for (String arg : args) {
			if ("--advanced".equals(arg) || "-a".equals(arg)) {
				useAdvancedServer = true;
			}
			else if ("--help".equals(arg) || "-h".equals(arg)) {
				printHelp();
				return;
			}
		}

		// Start the appropriate server
		if (useAdvancedServer) {
			FFmpegMcpServerAdvanced.main(new String[0]);
		}
		else {
			FFmpegMcpServer.main(new String[0]);
		}
	}

	/**
	 * Print help information.
	 */
	private static void printHelp() {
		System.out.println("FFmpeg MCP Server");
		System.out.println("----------------");
		System.out.println("A server that exposes FFmpeg functionality through the Model Context Protocol (MCP).");
		System.out.println();
		System.out.println("Usage: java -jar ffmpeg-mcp.jar [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  --advanced, -a    Start the advanced server with additional tools");
		System.out.println("  --help, -h        Show this help message");
		System.out.println();
		System.out.println("Basic server provides:");
		System.out.println("  - FFmpeg command execution via the 'ffmpeg' tool");
		System.out.println();
		System.out.println("Advanced server adds:");
		System.out.println("  - Video information retrieval via the 'video_info' tool");
		System.out.println("  - Video reference registration via the 'register_video' tool");
	}

}