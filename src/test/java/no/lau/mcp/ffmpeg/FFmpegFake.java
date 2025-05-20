package no.lau.mcp.ffmpeg;

import java.io.IOException;

public class FFmpegFake implements FFmpegExecutor {
    @Override
    public String execute(String command) {
        return "Response from fake";
    }
}
