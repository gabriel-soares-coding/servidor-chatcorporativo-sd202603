package br.ufmt.chatcorporativo.protocol;

/**
 * Representa um comando parseado recebido do cliente.
 * Contém o tipo do comando e seus argumentos.
 */
public class Command {

    private final String type;
    private final String[] args;
    private final String rawLine;

    public Command(String type, String[] args, String rawLine) {
        this.type = type;
        this.args = args;
        this.rawLine = rawLine;
    }

    /** Tipo do comando (ex: "LOGIN", "MSG", "LIST", "QUIT", etc.) */
    public String getType() {
        return type;
    }

    /** Argumentos do comando. */
    public String[] getArgs() {
        return args;
    }

    /** Linha original enviada pelo cliente. */
    public String getRawLine() {
        return rawLine;
    }
}
