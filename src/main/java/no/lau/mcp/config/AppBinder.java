package no.lau.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import no.lau.mcp.ffmpeg.DefaultFFmpegExecutor;
import no.lau.mcp.ffmpeg.FFmpegExecutor;
import no.lau.mcp.ffmpeg.FFmpegWrapper;
import no.lau.mcp.file.FileManager;
import no.lau.mcp.file.FileManagerImpl;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class AppBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(AppConfig.class).to(AppConfig.class).in(Singleton.class);
        bindAsContract(FileManagerImpl.class).to(FileManager.class).in(Singleton.class);
        bindAsContract(DefaultFFmpegExecutor.class).to(FFmpegExecutor.class).in(Singleton.class);
        bindAsContract(FFmpegWrapper.class).in(Singleton.class);
        bind(ObjectMapper.class).to(ObjectMapper.class).in(Singleton.class);
    }
}
