package br.ufmt.chatcorporativo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilitário simples de logging com timestamps.
 * Sem dependências externas — usa System.out/System.err.
 */
public final class Logger {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String context;

    public Logger(String context) {
        this.context = context;
    }

    public void info(String message) {
        System.out.println(format("INFO", message));
    }

    public void warn(String message) {
        System.out.println(format("WARN", message));
    }

    public void error(String message) {
        System.err.println(format("ERROR", message));
    }

    public void error(String message, Throwable t) {
        System.err.println(format("ERROR", message + " — " + t.getMessage()));
    }

    private String format(String level, String message) {
        return "[" + LocalDateTime.now().format(FMT) + "] "
                + "[" + level + "] "
                + "[" + context + "] "
                + message;
    }
}
