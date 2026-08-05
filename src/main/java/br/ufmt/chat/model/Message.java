package br.ufmt.chat.model;

import java.time.LocalDateTime;

/**
 * Representa uma mensagem trocada no sistema (R10, R17).
 * Armazena remetente, destinatário, conteúdo e timestamp para histórico.
 */
public class Message {

    private final String sender;
    private final String receiver;
    private final String content;
    private final LocalDateTime timestamp;
    private final String groupName;

    /**
     * Mensagem direta (ponto-a-ponto).
     */
    public Message(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.groupName = null;
    }

    /**
     * Mensagem de grupo.
     */
    public Message(String sender, String groupName, String content, boolean isGroup) {
        this.sender = sender;
        this.receiver = null;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.groupName = groupName;
    }

    /** Remetente da mensagem. */
    public String getSender() {
        return sender;
    }

    /** Destinatário (null se for mensagem de grupo). */
    public String getReceiver() {
        return receiver;
    }

    /** Conteúdo textual da mensagem. */
    public String getContent() {
        return content;
    }

    /** Carimbo temporal da mensagem (R17). */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** Nome do grupo (null se for mensagem direta). */
    public String getGroupName() {
        return groupName;
    }

    /** Indica se é uma mensagem de grupo. */
    public boolean isGroupMessage() {
        return groupName != null;
    }

    @Override
    public String toString() {
        if (isGroupMessage()) {
            return "[" + timestamp + "] " + sender + " -> #" + groupName + ": " + content;
        }
        return "[" + timestamp + "] " + sender + " -> " + receiver + ": " + content;
    }
}
