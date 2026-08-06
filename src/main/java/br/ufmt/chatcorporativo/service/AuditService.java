package br.ufmt.chatcorporativo.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import br.ufmt.chatcorporativo.util.Logger;

/**
 * Serviço de auditoria para garantir o não repúdio (R19).
 * Registra todas as ações dos usuários com timestamp, userId e descrição da ação.
 * Persiste em arquivo texto (append) para trilha de auditoria.
 */
public class AuditService {

    private static final Logger log = new Logger("AuditService");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String AUDIT_FILE = "audit.log";

    private PrintWriter writer;

    public AuditService() {
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(AUDIT_FILE, true)), true);
            log.info("Arquivo de auditoria aberto: " + AUDIT_FILE);
        } catch (IOException e) {
            log.error("Não foi possível abrir arquivo de auditoria", e);
            writer = null;
        }
    }

    /**
     * Registra uma ação no log de auditoria.
     *
     * @param userId  identificador do usuário (username ou IP se não logado)
     * @param action  tipo de ação (LOGIN, MSG, FILE, QUIT, etc.)
     * @param details detalhes adicionais da ação
     */
    public void logAction(String userId, String action, String details) {
        String entry = LocalDateTime.now().format(FMT)
                + " | " + userId
                + " | " + action
                + " | " + details;

        // Escreve no arquivo
        if (writer != null) {
            writer.println(entry);
        }

        // Também loga no console
        log.info("[AUDIT] " + entry);
    }

    /**
     * Registra uma ação sem detalhes adicionais.
     */
    public void logAction(String userId, String action) {
        logAction(userId, action, "");
    }

    /**
     * Fecha o arquivo de auditoria.
     */
    public void close() {
        if (writer != null) {
            writer.close();
            log.info("Arquivo de auditoria fechado");
        }
    }
}
