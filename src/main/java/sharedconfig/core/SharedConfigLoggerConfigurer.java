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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SharedConfigLoggerConfigurer {
    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfig's logs and targets them to
     * the file located at outputFolderPath
     *
     * @param appName          application name
     * @param appVersion       application version
     * @param outputFolderPath output logs folder location
     */
    public static void traceLogsToFile(@NotNull String appName, @NotNull String appVersion, @NotNull String outputFolderPath) throws UnknownHostException, ParseException {
        val ctx = (LoggerContext) LogManager.getContext(false);
        val config = ctx.getConfiguration();
//      header
        var computerName = InetAddress.getLocalHost().getHostName();
        ProcessHandle handle = ProcessHandle.current();
        ProcessHandle.Info info = handle.info();
        var startTime = info.startInstant().isEmpty() ? "" : info.startInstant().get();
        var pid = handle.pid();
        var processName = info.command().get().endsWith("java.exe") ? "java.exe" : "";
        var commandArgs = info.arguments().isEmpty() ? "" : info.arguments().get();
        var argsHash = getHashMD5(computerName + processName + commandArgs + pid + startTime);
//      header end
//      work with date
        var oldFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        var newFormat = new SimpleDateFormat("yyMMdd-HHmmss");
        var oldDate = oldFormat.parse(startTime.toString());
        var newDate = newFormat.format(oldDate);
//      end work with date
        val layout = PatternLayout.newBuilder()
                .withHeader("LI:" + argsHash + " s.MachineName=" + computerName + " s.ProcessName=" + processName
                        + " s.CommandLine=" + commandArgs + " s.Id=" + pid + " s.StartTime=" + '"' + startTime + '"' + "%n")
                .withPattern("TE:%snp{6} t=\"%d{yyyy.MM.dd'T'HH:mm:ss.SSS'Z'}\" k=%-5level m=\"%M : %replace{%m}{\\\\}{\\\\\\\\}\"%n").build();
        val appender = FileAppender.newBuilder()
                .withFileName(outputFolderPath + newDate + "-" + pid + "-" + argsHash  + "-1-" + "TLOG#" + ".tlog")
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
        val refs = new AppenderRef[]{ref};

        val loggerConfig = LoggerConfig.createLogger(true, Level.ALL, "sharedconfig", null, refs, null, config, null);
        loggerConfig.addAppender(appender, Level.TRACE, null);

        config.addLogger("sharedconfig", loggerConfig);
        val loggers = config.getLoggers();

        ctx.updateLoggers();
    }

    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfigs' logs and targets them to
     * the file located at C:\\ProgramData\\universal-platform\\logs\\
     *
     * @param appName    application name
     * @param appVersion application version
     */
    public static void traceLogsToFileToUPWindowsDefaultLocation(@NotNull String appName, @NotNull String appVersion) throws UnknownHostException, ParseException {
        traceLogsToFile(appName, appVersion, "C:\\ProgramData\\universal-platform\\logs\\");
    }

    /**
     *
     * @param string принимает строку для которой нужно расчитать хеш
     * @return возвращает расчитанный хеш для переданной строки
     */
    private static String getHashMD5(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            BigInteger bi = new BigInteger(1, md.digest(string.getBytes()));
            return bi.toString(16).toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return "";
        }
    }
}
