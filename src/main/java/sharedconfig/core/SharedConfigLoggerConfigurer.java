package sharedconfig.core;

import lombok.val;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
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
import java.time.Instant;

public class SharedConfigLoggerConfigurer {
    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfig's logs and targets them to
     * the file located at outputFolderPath
     *
     * @param outputFolderPath output logs folder location
     */
    public static void traceLogsToFile(@NotNull String outputFolderPath) throws UnknownHostException, ParseException {
        val ctx = (LoggerContext) LogManager.getContext(false);
        val config = ctx.getConfiguration();

        var computerName = InetAddress.getLocalHost().getHostName();
        ProcessHandle handle = ProcessHandle.current();
        ProcessHandle.Info info = handle.info();
        var startTime= info.startInstant().map(Instant::toString).orElse("");
        var pid = handle.pid();
        var processName = info.command().get().endsWith("java.exe") ? "java.exe" : "";
        var commandArgs = info.arguments().isEmpty() ? "" : '"' + info.arguments().get().toString() + '"';
        var argsHash = getHashMD5(computerName + processName + commandArgs + pid + startTime);

        var oldFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        var newFormat = new SimpleDateFormat("yyMMdd-HHmmss");
        var oldDate = oldFormat.parse(startTime);
        var newDate = newFormat.format(oldDate);

        val layout = PatternLayout.newBuilder()
                .withHeader("LI:" + argsHash + " s.MachineName=" + computerName + " s.ProcessName=" + processName
                        + " s.CommandLine=" + commandArgs + " s.Id=" + pid + " s.StartTime=" + '"' + startTime + '"' + "%n")
                .withPattern("TE:%snp{6} t=\"%d{yyyy.MM.dd'T'HH:mm:ss.SSS'Z'}\" %level{WARN=Warning, DEBUG=Info, ERROR=Error, TRACE=Info, INFO=Info} m=\"%M : %replace{%m}{\\\\}{\\\\\\\\}\"%n").build();
        val rollingAppender = RollingFileAppender.newBuilder()
                .withFileName(outputFolderPath + newDate + "-" + pid + "-" + argsHash  + "-1-TLOG#last.tlog")
                .setName("sharedconfig-up-logs-appender")
                .setConfiguration(config)
                .withBufferedIo(true)
                .withImmediateFlush(false)
                .withLocking(false)
                .withFilePattern(outputFolderPath + newDate + "-" + pid + "-" + argsHash  + "-1-TLOG#%i.tlog")
                .setIgnoreExceptions(false)
                .setLayout(layout)
                .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
                .build();

        rollingAppender.start();
        config.addAppender(rollingAppender);

        val ref = AppenderRef.createAppenderRef("File", null, null);
        val refs = new AppenderRef[]{ref};

        val loggerConfig = LoggerConfig.createLogger(true, Level.ALL, "sharedconfig", null, refs, null, config, null);
        loggerConfig.addAppender(rollingAppender, Level.TRACE, null);

        config.addLogger("sharedconfig", loggerConfig);
        val loggers = config.getLoggers();

        ctx.updateLoggers();
    }

    /**
     * Creates logger (named 'sharedconfig') that catches all sharedconfigs' logs and targets them to
     * the file located at C:\\ProgramData\\universal-platform\\logs\\
     */
    public static void traceLogsToFileToUPWindowsDefaultLocation() throws UnknownHostException, ParseException {
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