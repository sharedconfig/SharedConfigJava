package sharedconfig.core;

import lombok.val;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class SharedConfigLoggerConfigurer {
    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfig's logs and targets them to
     * the file located at outputFolderPath
     * @param appName application name
     * @param appVersion application version
     * @param outputFolderPath output logs folder location
     */
    public static void traceLogsToFile(@NotNull String appName, @NotNull String appVersion, @NotNull String outputFolderPath) {
        val ctx = (LoggerContext) LogManager.getContext(false);
        val config = ctx.getConfiguration();
        val layout = PatternLayout.newBuilder().withPattern("[%d] [%-5level] - %msg%n").build();
        val appender = FileAppender.newBuilder()
                .withFileName(outputFolderPath + appName + "_" + appVersion + "_" + new Date().getTime() + ".log")
                .setName("sharedconfig-up-logs-appender")
                .setConfiguration(config)
                .withBufferedIo(true)
                .withImmediateFlush(false)
                .withLocking(false)
                .setIgnoreExceptions(false)
                .setLayout(layout)
                .setFilter(null)
                .build();
        appender.start();
        config.addAppender(appender);

        val ref = AppenderRef.createAppenderRef("File", null, null);
        val refs = new AppenderRef[] { ref };

        val loggerConfig = LoggerConfig.createLogger(true, Level.ALL, "sharedconfig", null, refs, null, config, null);
        loggerConfig.addAppender(appender, Level.TRACE, null);

        config.addLogger("sharedconfig", loggerConfig);
        val loggers = config.getLoggers();

        ctx.updateLoggers();
    }

    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfigs' logs and targets them to
     * the file located at C:\\ProgramData\\universal-platform\\logs\\
     * @param appName application name
     * @param appVersion application version
     */
    public static void traceLogsToFileToUPWindowsDefaultLocation(@NotNull String appName, @NotNull String appVersion) {
        traceLogsToFile(appName, appVersion, "C:\\ProgramData\\universal-platform\\logs\\");
    }
}
