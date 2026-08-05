package br.ufmt.chat.protocol;

/**
 * Responsável por transformar a linha de texto enviada pelo cliente
 * em um objeto {@link Command} estruturado.
 *
 * Protocolo esperado: COMANDO arg1 arg2 ...
 * O parsing extrai o tipo de comando e os argumentos de acordo com
 * a semântica de cada comando definido em {@link ProtocolConstants}.
 */
public class CommandParser {

    /**
     * Faz o parse de uma linha de texto em um Command.
     * Extrai argumentos de forma inteligente conforme o tipo de comando:
     * - LOGIN: [usuario, senha, orgao]
     * - MSG: [destinatario, texto]
     * - GMSG: [grupo, texto]
     * - FILE: [destinatario, nomeArquivo, tamanhoBytes]
     * - GCREATE: [nomeGrupo]
     * - GJOIN/GLEAVE: [grupo]
     * - LIST/GLIST/QUIT: sem argumentos
     *
     * @param rawLine linha de texto recebida do cliente
     * @return Command com tipo e argumentos extraídos
     */
    public Command parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return new Command("UNKNOWN", new String[0], rawLine);
        }

        String trimmed = rawLine.trim();
        String[] parts = trimmed.split("\\s+");

        String type = parts[0].toUpperCase();
        String[] args;

        switch (type) {
            case ProtocolConstants.CMD_LOGIN:
                // LOGIN <usuario> <senha> <orgao>
                args = extractArgs(parts, 3);
                break;

            case ProtocolConstants.CMD_MSG:
                // MSG <destinatario> <texto...>
                args = extractWithTrailingText(parts, 1);
                break;

            case ProtocolConstants.CMD_GMSG:
                // GMSG <grupo> <texto...>
                args = extractWithTrailingText(parts, 1);
                break;

            case ProtocolConstants.CMD_FILE:
                // FILE <destinatario> <nomeArquivo> <tamanhoBytes>
                args = extractArgs(parts, 3);
                break;

            case ProtocolConstants.CMD_GCREATE:
                // GCREATE <nomeGrupo>
                args = extractArgs(parts, 1);
                break;

            case ProtocolConstants.CMD_GJOIN:
            case ProtocolConstants.CMD_GLEAVE:
                // GJOIN/GLEAVE <grupo>
                args = extractArgs(parts, 1);
                break;

            case ProtocolConstants.CMD_LIST:
            case ProtocolConstants.CMD_GLIST:
            case ProtocolConstants.CMD_QUIT:
                args = new String[0];
                break;

            default:
                // Comando desconhecido — repassa tudo como argumento único
                if (parts.length > 1) {
                    args = new String[]{trimmed.substring(parts[0].length()).trim()};
                } else {
                    args = new String[0];
                }
                break;
        }

        return new Command(type, args, rawLine);
    }

    /**
     * Extrai até maxArgs argumentos individuais (tokens separados por espaço).
     */
    private String[] extractArgs(String[] parts, int maxArgs) {
        int count = Math.min(parts.length - 1, maxArgs);
        String[] args = new String[count];
        for (int i = 0; i < count; i++) {
            args[i] = parts[i + 1];
        }
        return args;
    }

    /**
     * Extrai o primeiro argumento como token individual e junta o restante
     * como texto livre (para MSG e GMSG).
     * Resultado: [arg1, textoRestante]
     */
    private String[] extractWithTrailingText(String[] parts, int fixedArgs) {
        if (parts.length <= 1) {
            return new String[0];
        }

        if (parts.length <= fixedArgs + 1) {
            // Só tem o(s) argumento(s) fixo(s), sem texto
            String[] args = new String[parts.length - 1];
            for (int i = 0; i < args.length; i++) {
                args[i] = parts[i + 1];
            }
            return args;
        }

        // Monta: [arg1, ..., argN, "todo o texto restante"]
        String[] args = new String[fixedArgs + 1];
        for (int i = 0; i < fixedArgs; i++) {
            args[i] = parts[i + 1];
        }

        // Reconstrói o texto a partir do token fixedArgs+1
        StringBuilder sb = new StringBuilder();
        for (int i = fixedArgs + 1; i < parts.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        args[fixedArgs] = sb.toString();

        return args;
    }
}
