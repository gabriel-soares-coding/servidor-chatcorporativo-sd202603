package br.ufmt.chatcorporativo.protocol;

/**
 * Constantes do protocolo de aplicação do chat corporativo.
 * Centraliza todos os comandos e códigos de resposta,
 * evitando strings mágicas espalhadas pelo código.
 */
public final class ProtocolConstants {

    private ProtocolConstants() {
        // Classe utilitária, não instanciar
    }

    // ---------------------------------------------------------------
    // Comandos do cliente (C → S)
    // ---------------------------------------------------------------
    public static final String CMD_LOGIN   = "LOGIN";
    public static final String CMD_MSG     = "MSG";
    public static final String CMD_GMSG    = "GMSG";
    public static final String CMD_LIST    = "LIST";
    public static final String CMD_GLIST   = "GLIST";
    public static final String CMD_GCREATE = "GCREATE";
    public static final String CMD_GJOIN   = "GJOIN";
    public static final String CMD_GLEAVE  = "GLEAVE";
    public static final String CMD_FILE    = "FILE";
    public static final String CMD_QUIT    = "QUIT";
    public static final String CMD_HISTORY  = "HISTORY";
    public static final String CMD_GHISTORY = "GHISTORY";

    // ---------------------------------------------------------------
    // Comandos de federação (S <-> S)
    // ---------------------------------------------------------------
    public static final String CMD_FED_CONNECT = "FED_CONNECT";
    public static final String CMD_FED_MSG     = "FED_MSG";
    public static final String CMD_FED_FILE    = "FED_FILE";

    // ---------------------------------------------------------------
    // Respostas do servidor (S → C)
    // ---------------------------------------------------------------
    public static final String RESP_OK    = "OK";
    public static final String RESP_ERR   = "ERR";
    public static final String RESP_RECV  = "RECV";
    public static final String RESP_GRECV = "GRECV";
    public static final String RESP_FRECV = "FRECV";

    // ---------------------------------------------------------------
    // Códigos de erro
    // ---------------------------------------------------------------
    public static final String ERR_AUTH_REQUIRED    = "401";
    public static final String ERR_ALREADY_LOGGED   = "402";
    public static final String ERR_INVALID_ARGS     = "400";
    public static final String ERR_USER_NOT_FOUND   = "404";
    public static final String ERR_GROUP_NOT_FOUND  = "404";
    public static final String ERR_ACCESS_DENIED    = "403";
    public static final String ERR_UNKNOWN_CMD      = "405";
    public static final String ERR_INTERNAL         = "500";
}
