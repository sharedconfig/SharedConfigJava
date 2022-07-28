package sharedconfig.core.loger.configuration;

import lombok.val;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.CronTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.NotNull;
import sharedconfig.core.exceptions.SharedConfigLoggerConfigurerException;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

public class SharedConfigLoggerConfigurer {
    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfig's logs and targets them to
     * the file located at outputFolderPath
     *
     * @param outputFolderPath output logs folder location
     */
    public static void traceLogsToFile(@NotNull String outputFolderPath) throws SharedConfigLoggerConfigurerException {
        val ctx = (LoggerContext) LogManager.getContext(false);
        val config = ctx.getConfiguration();

        String computerName;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new SharedConfigLoggerConfigurerException("не удалось получить хост", e);
        }
        ProcessHandle handle = ProcessHandle.current();
        ProcessHandle.Info info = handle.info();

        var startTime= info.startInstant().orElse(Instant.now());
        var startTimeFormat = new SimpleDateFormat("yyMMdd-HHmmss");
        startTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        var startTimeFormatted = startTimeFormat.format(Date.from(startTime));

        var pid = handle.pid();
        var processName = info.command().isPresent() ? "java.exe" : "";
        var commandArgs = '"' + String.join(" ", info.arguments().orElse(new String[0])) + '"';
        var argsHash = getHashMD5(computerName + processName + commandArgs + pid + startTime);

        val layout = PatternLayout.newBuilder()
                .withHeader("LI:" + argsHash + " s.MachineName=" + computerName + " s.ProcessName=" + processName
                        + " s.CommandLine=" + commandArgs + " s.Id=" + pid + " s.StartTime=" + '"' + startTime + '"' + "%n")
                .withPattern("TE:%snp{6} t=\"%d{yyyy.MM.dd'T'HH:mm:ss.SSS'Z'}{UTC}\" k=%level{FATAL=Error, WARN=Warning, DEBUG=Info, ERROR=Error, TRACE=Info, INFO=Info} m=\"%M : %tlogmsg\"%n").withCharset(StandardCharsets.UTF_8).build();
        val rollingAppender = RollingFileAppender.newBuilder()
                .setName("sharedconfig-up-logs-appender")
                .setConfiguration(config)
                .setBufferedIo(true)
                .setImmediateFlush(false)
                .withLocking(false)
                .withFilePattern(outputFolderPath + startTimeFormatted + "-" + pid + "-" + argsHash + "-1-TLOG#%i.tlog")
                .setIgnoreExceptions(false)
                .setLayout(layout)
                .withPolicy(CompositeTriggeringPolicy.createPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"), CronTriggeringPolicy.createPolicy(config, Boolean.TRUE.toString(), "0 0 * * * ?")))
                .build();

        rollingAppender.start();
        config.addAppender(rollingAppender);

        val ref = AppenderRef.createAppenderRef("File", null, null);
        val refs = new AppenderRef[]{ref};

        val loggerConfig = LoggerConfig.newBuilder().withConfig(config).withAdditivity(true).withLevel(Level.ALL).withLoggerName("sharedconfig").withRefs(refs).build();
        loggerConfig.addAppender(rollingAppender, Level.TRACE, null);

        config.addLogger("sharedconfig", loggerConfig);
//        val loggers = config.getLoggers();

        ctx.updateLoggers();
    }

    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfigs' logs and targets them to
     * the file located at C:\\ProgramData\\universal-platform\\logs\\
     */
    public static void traceLogsToFileToUPWindowsDefaultLocation() throws SharedConfigLoggerConfigurerException {
        traceLogsToFile("C:\\ProgramData\\universal-platform\\logs\\");
    }

    /**
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