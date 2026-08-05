package br.ufmt.chat.exception;

/**
 * Exceção de negócio do sistema de chat.
 * Carrega um código de erro do protocolo para que o ClientHandler
 * possa enviar a resposta adequada ao cliente.
 */
public class ChatException extends Exception {

    private final String errorCode;

    public ChatException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Código de erro do protocolo (ex: "401", "403", "404"). */
    public String getErrorCode() {
        return errorCode;
    }
}
