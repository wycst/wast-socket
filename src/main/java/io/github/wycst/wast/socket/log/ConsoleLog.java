package io.github.wycst.wast.socket.log;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @Author: wangy
 * @Date: 2020/5/5 15:46
 * @Description:
 */
public final class ConsoleLog {

    private static final Map<Class<?>, ConsoleLog> LOGGERS = new ConcurrentHashMap<Class<?>, ConsoleLog>();
    private static final java.util.logging.ConsoleHandler CONSOLE_HANDLER;

    static {
        CONSOLE_HANDLER = new java.util.logging.ConsoleHandler() {
            @Override
            protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
                super.setOutputStream(System.out);
            }
        };
        CONSOLE_HANDLER.setFormatter(new ConsoleFormatter());
    }

    private final String loggerName;
    private final Logger logger;

    public static ConsoleLog getLog(Class<?> logCls) {
        synchronized (logCls) {
            ConsoleLog log = LOGGERS.get(logCls);
            if (log == null) {
                log = new ConsoleLog(logCls);
                log.addHandler(CONSOLE_HANDLER);
                LOGGERS.put(logCls, log);
            }
            return log;
        }
    }

    ConsoleLog(Class<?> nameClass) {
        this.loggerName = getLoggerName(nameClass);
        logger = Logger.getLogger(loggerName);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
    }

    void addHandler(Handler handler) {
        logger.addHandler(handler);
    }

    private String getLoggerName(Class<?> nameClass) {
        String className = nameClass.getName();
        if (className.indexOf('.') == -1) {
            return className;
        }
        if (className.length() < 40) {
            return className;
        }
        return className.replaceAll("(\\w)\\w*[.]", "$1.");
    }

    public void debug(String msg, Object... args) {
        log(Level.CONFIG, msg, args);
    }

    public void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    public void warn(String msg, Object... args) {
        log(Level.WARNING, msg, args);
    }

    public void error(String msg, Object... args) {
        log(Level.SEVERE, msg, args);
    }

    private void log(Level level, String msg, Object[] args) {
        if (logger.isLoggable(level)) {
            logger.log(level, msg, args);
        }
    }

    public void error(String msg, Throwable throwable, Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            LogRecord logRecord = new LogRecord(Level.SEVERE, msg);
            logRecord.setParameters(args);
            logRecord.setLoggerName(loggerName);
            logRecord.setThrown(throwable);
            logger.log(logRecord);
        }
    }

    public void setLevel(Level level) {
        logger.setLevel(level);
    }
}
