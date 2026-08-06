package br.ufmt.chatcorporativo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Set;

import br.ufmt.chatcorporativo.exception.ChatException;
import br.ufmt.chatcorporativo.model.User;
import br.ufmt.chatcorporativo.protocol.Command;
import br.ufmt.chatcorporativo.protocol.CommandParser;
import br.ufmt.chatcorporativo.protocol.ProtocolConstants;
import br.ufmt.chatcorporativo.service.AccessControlService;
import br.ufmt.chatcorporativo.service.AuditService;
import br.ufmt.chatcorporativo.service.FileTransferService;
import br.ufmt.chatcorporativo.service.GroupService;
import br.ufmt.chatcorporativo.service.MessageService;
import br.ufmt.chatcorporativo.service.UserService;
import br.ufmt.chatcorporativo.util.Logger;

/**
 * Representa a sessão de um usuário conectado ao servidor.
 * É a classe mais importante do sistema.
 *
 * Responsabilidades:
 * - Receber comandos do cliente via socket;
 * - Chamar o {@link CommandParser} para interpretar a linha recebida;
 * - Chamar os serviços adequados de acordo com o comando;
 * - Enviar respostas ao cliente.
 *
 * Cada cliente conectado possui uma instância própria de ClientHandler,
 * executada em uma thread do pool gerenciado pelo {@link ChatServer}.
 */
public class ClientHandler implements Runnable {

    private static final Logger log = new Logger("ClientHandler");

    private final Socket socket;
    private final ChatServer server;
    private final CommandParser parser;

    // Serviços obtidos do ChatServer
    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final FileTransferService fileTransferService;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    private BufferedReader input;
    private PrintWriter output;
    private InputStream rawInput;
    private OutputStream rawOutput;

    private String username;
    private User user;
    private boolean running;

    // Conexão de federação
    private boolean isPeerConnection = false;
    private String peerDomain = null;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.parser = new CommandParser();
        this.running = true;

        // Obtém referências aos serviços compartilhados
        this.userService = server.getUserService();
        this.messageService = server.getMessageService();
        this.groupService = server.getGroupService();
        this.fileTransferService = server.getFileTransferService();
        this.accessControlService = server.getAccessControlService();
        this.auditService = server.getAuditService();
    }

    @Override
    public void run() {
        try {
            rawInput = socket.getInputStream();
            rawOutput = socket.getOutputStream();
            input = new BufferedReader(new InputStreamReader(rawInput, "UTF-8"));
            output = new PrintWriter(rawOutput, true);

            sendMessage(ProtocolConstants.RESP_OK + " Bem-vindo ao Chat Corporativo. "
                    + "Você está conectado ao domínio: " + server.getDomainId()
                    + ". Use LOGIN <usuario> <senha> <orgao> para entrar.");

            String line;
            while (running && (line = readLineFromStream(rawInput)) != null) {
                Command command = parser.parse(line);
                handleCommand(command);
            }
        } catch (IOException e) {
            log.error("Erro na conexão com cliente: " + getIdentifier(), e);
        } finally {
            disconnect();
        }
    }

    /**
     * Lê uma linha de texto do InputStream sem realizar buffer adiantado (read-ahead).
     * Isso garante que os bytes brutos binários do comando FILE/FED_FILE permaneçam
     * intactos no stream para leitura posterior.
     */
    private String readLineFromStream(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                baos.write(b);
            }
        }
        if (b == -1 && baos.size() == 0) {
            return null;
        }
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Despacha o comando parseado para o serviço adequado.
     * Não implementa lógica de negócio — delega ao parser e aos serviços.
     */
    private void handleCommand(Command command) {
        
        try {
            switch (command.getType()) {
                case ProtocolConstants.CMD_FED_CONNECT:
                    handleFedConnect(command);
                    break;
                case ProtocolConstants.CMD_FED_MSG:
                    handleFedMsg(command);
                    break;
                case ProtocolConstants.CMD_FED_FILE:
                    handleFedFile(command);
                    break;
                case ProtocolConstants.CMD_LOGIN:
                    handleLogin(command);
                    break;
                case ProtocolConstants.CMD_MSG:
                    handleMsg(command);
                    break;
                case ProtocolConstants.CMD_GMSG:
                    handleGMsg(command);
                    break;
                case ProtocolConstants.CMD_LIST:
                    handleList();
                    break;
                case ProtocolConstants.CMD_GLIST:
                    handleGList();
                    break;
                case ProtocolConstants.CMD_GCREATE:
                    handleGCreate(command);
                    break;
                case ProtocolConstants.CMD_GJOIN:
                    handleGJoin(command);
                    break;
                case ProtocolConstants.CMD_GLEAVE:
                    handleGLeave(command);
                    break;
                case ProtocolConstants.CMD_HISTORY:
                    handleHistory();
                    break;
                case ProtocolConstants.CMD_GHISTORY:
                    handleGHistory(command);
                    break;
                case ProtocolConstants.CMD_FILE:
                    handleFile(command);
                    break;
                case ProtocolConstants.CMD_QUIT:
                    handleQuit();
                    break;
                default:
                    sendMessage(ProtocolConstants.RESP_ERR + " "
                            + ProtocolConstants.ERR_UNKNOWN_CMD
                            + " Comando desconhecido: " + command.getType());
                    break;
            }
        } catch (ChatException e) {
            sendMessage(ProtocolConstants.RESP_ERR + " " + e.getErrorCode() + " " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Handlers de comandos — delegam para os services
    // ---------------------------------------------------------------

    /**
     * LOGIN <usuario> <senha> <orgao>
     */
    private void handleLogin(Command command) throws ChatException {
        String[] args = command.getArgs();
        if (args.length < 3) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: LOGIN <usuario> <senha> <orgao>");
        }

        if (this.username != null) {
            throw new ChatException(ProtocolConstants.ERR_ALREADY_LOGGED,
                    "Você já está logado como " + this.username);
        }

        String reqUsername = args[0];
        String reqPassword = args[1];
        String reqOrgao = args[2];

        // Verifica se já está online com outro handler
        if (server.isOnline(reqUsername)) {
            throw new ChatException(ProtocolConstants.ERR_ALREADY_LOGGED,
                    "Usuário " + reqUsername + " já está conectado");
        }

        this.user = userService.authenticate(reqUsername, reqPassword, reqOrgao);
        this.username = reqUsername;

        server.registerClient(username, this);
        auditService.logAction(username, "LOGIN", "orgao=" + reqOrgao);

        sendMessage(ProtocolConstants.RESP_OK + " Login realizado como " + username
                + " [" + reqOrgao + "]");
    }

    /**
     * MSG <destinatario> <texto...>
     */
    private void handleMsg(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 2) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: MSG <destinatario> <texto>");
        }

        String receiver = args[0];
        String text = args[1];

        // Lógica de Federação
        if (receiver.contains("@")) {
            String[] parts = receiver.split("@", 2);
            String targetUser = parts[0];
            String targetDomain = parts[1];

            if (!targetDomain.equalsIgnoreCase(server.getDomainId())) {
                // Roteia mensagem via federação
                String fedCommand = ProtocolConstants.CMD_FED_MSG + " " 
                        + username + "@" + server.getDomainId() + " " 
                        + targetUser + " " + text;
                boolean routed = server.routeMessage(targetDomain, fedCommand);
                if (routed) {
                    messageService.createDirectMessage(username, receiver, text);
                    auditService.logAction(username, "MSG_FEDERADA", "para=" + receiver);
                    sendMessage(ProtocolConstants.RESP_OK + " Mensagem enviada para " + receiver + " (via federação)");
                    return;
                } else {
                    throw new ChatException(ProtocolConstants.ERR_USER_NOT_FOUND, 
                            "Broker do estado '" + targetDomain + "' indisponível ou não cadastrado.");
                }
            } else {
                receiver = targetUser; // Se for o mesmo domínio, trata como local
            }
        }

        // Verifica se o destinatário existe
        User receiverUser = userService.findUser(receiver);

        // Verifica restrições entre órgãos (R14)
        accessControlService.checkCommunicationAllowed(user.getOrgao(), receiverUser.getOrgao());

        // Cria e registra a mensagem (R10, R17)
        messageService.createDirectMessage(username, receiver, text);
        
        auditService.logAction(username, "MSG", "para=" + receiver);

        // Entrega a mensagem ao destinatário se online
        ClientHandler receiverHandler = server.getClient(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(ProtocolConstants.RESP_RECV + " "
                    + username + " " + text);
        }

        sendMessage(ProtocolConstants.RESP_OK + " Mensagem enviada para " + receiver);
    }

    /**
     * GMSG <grupo> <texto...>
     */
    private void handleGMsg(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 2) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: GMSG <grupo> <texto>");
        }

        String groupName = args[0];
        String text = args[1];

        // Verifica se é membro do grupo (R16)
        if (!groupService.isMember(groupName, username)) {
            throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED,
                    "Você não é membro do grupo: " + groupName);
        }

        // Cria e registra a mensagem de grupo
        messageService.createGroupMessage(username, groupName, text);
        auditService.logAction(username, "GMSG", "grupo=" + groupName);

        // Broadcast para membros online do grupo (R13)
        Set<String> members = groupService.getMembers(groupName, username);
        for (String member : members) {
            if (!member.equals(username)) {
                ClientHandler memberHandler = server.getClient(member);
                if (memberHandler != null) {
                    memberHandler.sendMessage(ProtocolConstants.RESP_GRECV + " "
                            + groupName + " " + username + " " + text);
                }
            }
        }

        sendMessage(ProtocolConstants.RESP_OK + " Mensagem enviada para #" + groupName);
    }

    /**
     * LIST — lista usuários conectados (R11)
     */
    private void handleList() throws ChatException {
        accessControlService.requireAuthentication(username);

        List<String> users = server.getConnectedUsers();
        auditService.logAction(username, "LIST", "");

        StringBuilder sb = new StringBuilder();
        sb.append(ProtocolConstants.RESP_OK).append(" Usuários online (")
          .append(users.size()).append("): ");
        sb.append(String.join(", ", users));

        sendMessage(sb.toString());
    }

    /**
     * GLIST — lista todos os grupos (R13)
     */
    private void handleGList() throws ChatException {
        accessControlService.requireAuthentication(username);

        List<String> groups = groupService.listGroups();
        auditService.logAction(username, "GLIST", "");

        StringBuilder sb = new StringBuilder();
        sb.append(ProtocolConstants.RESP_OK).append(" Grupos (")
          .append(groups.size()).append("): ");
        sb.append(String.join(", ", groups));

        sendMessage(sb.toString());
    }

    /**
     * GCREATE <nomeGrupo>
     */
    private void handleGCreate(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 1) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: GCREATE <nomeGrupo>");
        }

        String groupName = args[0];
        groupService.createGroup(groupName, username, false);
        auditService.logAction(username, "GCREATE", "grupo=" + groupName);

        sendMessage(ProtocolConstants.RESP_OK + " Grupo '" + groupName + "' criado");
    }

    /**
     * GJOIN <grupo>
     */
    private void handleGJoin(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 1) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: GJOIN <grupo>");
        }

        String groupName = args[0];
        groupService.joinGroup(groupName, username);
        auditService.logAction(username, "GJOIN", "grupo=" + groupName);

        sendMessage(ProtocolConstants.RESP_OK + " Você entrou no grupo '" + groupName + "'");
    }

    /**
     * GLEAVE <grupo>
     */
    private void handleGLeave(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 1) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: GLEAVE <grupo>");
        }

        String groupName = args[0];
        groupService.leaveGroup(groupName, username);
        auditService.logAction(username, "GLEAVE", "grupo=" + groupName);

        sendMessage(ProtocolConstants.RESP_OK + " Você saiu do grupo '" + groupName + "'");
    }

    /**
     * HISTORY — Retorna o histórico de mensagens diretas do usuário (R17)
     */
    private void handleHistory() throws ChatException {
        accessControlService.requireAuthentication(username);

        List<br.ufmt.chatcorporativo.model.Message> history = messageService.getDirectHistory(username);
        auditService.logAction(username, "HISTORY", "");

        sendMessage(ProtocolConstants.RESP_OK + " Histórico de mensagens diretas (" + history.size() + "):");
        for (br.ufmt.chatcorporativo.model.Message msg : history) {
            sendMessage(msg.toString());
        }
        sendMessage(ProtocolConstants.RESP_OK + " Fim do histórico");
    }

    /**
     * GHISTORY <grupo> — Retorna o histórico de mensagens de um grupo (R17)
     */
    private void handleGHistory(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 1) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: GHISTORY <grupo>");
        }

        String groupName = args[0];

        // Verifica se é membro do grupo (R16)
        if (!groupService.isMember(groupName, username)) {
            throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED,
                    "Você não é membro do grupo: " + groupName);
        }

        List<br.ufmt.chatcorporativo.model.Message> history = messageService.getGroupHistory(groupName);
        auditService.logAction(username, "GHISTORY", "grupo=" + groupName);

        sendMessage(ProtocolConstants.RESP_OK + " Histórico do grupo " + groupName + " (" + history.size() + "):");
        for (br.ufmt.chatcorporativo.model.Message msg : history) {
            sendMessage(msg.toString());
        }
        sendMessage(ProtocolConstants.RESP_OK + " Fim do histórico");
    }

    /**
     * FILE <destinatario> <nomeArquivo> <tamanhoBytes>
     * Após este comando, o cliente envia os bytes brutos do arquivo.
     */
    private void handleFile(Command command) throws ChatException {
        accessControlService.requireAuthentication(username);

        String[] args = command.getArgs();
        if (args.length < 3) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: FILE <destinatario> <nomeArquivo> <tamanhoBytes>");
        }

        String receiver = args[0];
        String fileName = args[1];
        long fileSize;

        try {
            fileSize = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Tamanho do arquivo deve ser um número");
        }

        // Lógica de Federação
        if (receiver.contains("@")) {
            String[] parts = receiver.split("@", 2);
            String targetUser = parts[0];
            String targetDomain = parts[1];

            if (!targetDomain.equalsIgnoreCase(server.getDomainId())) {
                // Lê os bytes do remetente
                byte[] fileData = fileTransferService.receiveFileData(rawInput, fileSize);
                
                // Roteia via federação
                boolean routed = server.routeFile(targetDomain, username + "@" + server.getDomainId(), targetUser, fileName, fileData);
                if (routed) {
                    auditService.logAction(username, "FILE_FEDERADA", "para=" + receiver + " arquivo=" + fileName + " bytes=" + fileSize);
                    sendMessage(ProtocolConstants.RESP_OK + " Arquivo '" + fileName + "' enviado para " + receiver + " (via federação)");
                    return;
                } else {
                    throw new ChatException(ProtocolConstants.ERR_USER_NOT_FOUND, 
                            "Broker do estado '" + targetDomain + "' indisponível para transferência de arquivo.");
                }
            } else {
                receiver = targetUser; // Se for o mesmo domínio, trata como local
            }
        }

        // Lê os bytes do arquivo do remetente (R12)
        byte[] fileData = fileTransferService.receiveFileData(rawInput, fileSize);

        // Verifica se o destinatário existe
        User receiverUser = userService.findUser(receiver);

        // Verifica restrições entre órgãos (R14)
        accessControlService.checkCommunicationAllowed(user.getOrgao(), receiverUser.getOrgao());
        auditService.logAction(username, "FILE",
                "para=" + receiver + " arquivo=" + fileName + " bytes=" + fileSize);

        // Entrega ao destinatário se online
        ClientHandler receiverHandler = server.getClient(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(ProtocolConstants.RESP_FRECV + " "
                    + username + " " + fileName + " " + fileSize);
            try {
                fileTransferService.sendFileData(receiverHandler.getRawOutput(), fileData);
            } catch (ChatException e) {
                log.error("Erro ao enviar arquivo para " + receiver, e);
            }
            sendMessage(ProtocolConstants.RESP_OK + " Arquivo '" + fileName
                    + "' enviado para " + receiver);
        } else {
            sendMessage(ProtocolConstants.RESP_OK + " Arquivo recebido, mas "
                    + receiver + " está offline");
        }
    }

    // ---------------------------------------------------------------
    // Handlers de Federação
    // ---------------------------------------------------------------

    /**
     * FED_CONNECT <brokerOriginador>
     */
    private void handleFedConnect(Command command) throws ChatException {
        String[] args = command.getArgs();
        if (args.length < 1) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: FED_CONNECT <brokerOriginador>");
        }
        this.isPeerConnection = true;
        this.peerDomain = args[0];
        log.info("Conexão de federação aceita do broker regional: " + peerDomain);
        sendMessage(ProtocolConstants.RESP_OK + " Handshake de federação aceito");
    }

    /**
     * FED_MSG <remetenteCompleto> <destinatarioLocal> <texto>
     */
    private void handleFedMsg(Command command) throws ChatException {
        if (!isPeerConnection) {
            throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED, 
                    "Apenas peer brokers podem executar o comando FED_MSG");
        }
        String[] args = command.getArgs();
        if (args.length < 3) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: FED_MSG <remetenteCompleto> <destinatarioLocal> <texto>");
        }
        String sender = args[0];
        String receiver = args[1];
        String text = args[2];

        // Registra mensagem no histórico local
        messageService.createDirectMessage(sender, receiver, text);

        // Entrega ao destinatário local
        ClientHandler receiverHandler = server.getClient(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(ProtocolConstants.RESP_RECV + " " + sender + " " + text);
            sendMessage(ProtocolConstants.RESP_OK + " Mensagem federada entregue para " + receiver);
        } else {
            sendMessage(ProtocolConstants.RESP_ERR + " " + ProtocolConstants.ERR_USER_NOT_FOUND 
                    + " Usuário local " + receiver + " offline");
        }
    }

    /**
     * FED_FILE <remetenteCompleto> <destinatarioLocal> <nomeArquivo> <tamanhoBytes>
     * Seguido pelos bytes brutos do arquivo.
     */
    private void handleFedFile(Command command) throws ChatException {
        if (!isPeerConnection) {
            throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED, 
                    "Apenas peer brokers podem executar o comando FED_FILE");
        }
        String[] args = command.getArgs();
        if (args.length < 4) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Uso correto: FED_FILE <remetenteCompleto> <destinatarioLocal> <nomeArquivo> <tamanhoBytes>");
        }
        String sender = args[0];
        String receiver = args[1];
        String fileName = args[2];
        long fileSize;
        try {
            fileSize = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS, "Tamanho de arquivo deve ser numérico");
        }

        // Lê bytes do socket do broker parceiro
        byte[] fileData = fileTransferService.receiveFileData(rawInput, fileSize);

        // Entrega ao destinatário local
        ClientHandler receiverHandler = server.getClient(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(ProtocolConstants.RESP_FRECV + " " + sender + " " + fileName + " " + fileSize);
            try {
                fileTransferService.sendFileData(receiverHandler.getRawOutput(), fileData);
                sendMessage(ProtocolConstants.RESP_OK + " Arquivo federado entregue para " + receiver);
            } catch (ChatException e) {
                log.error("Erro ao enviar arquivo federado para " + receiver, e);
                sendMessage(ProtocolConstants.RESP_ERR + " " + ProtocolConstants.ERR_INTERNAL + " Erro ao escrever arquivo local");
            }
        } else {
            sendMessage(ProtocolConstants.RESP_ERR + " " + ProtocolConstants.ERR_USER_NOT_FOUND 
                    + " Usuário local " + receiver + " offline");
        }
    }

    /**
     * QUIT — encerra a sessão.
     */
    private void handleQuit() {
        auditService.logAction(getIdentifier(), "QUIT", "");
        sendMessage(ProtocolConstants.RESP_OK + " Até logo!");
        running = false;
    }

    // ---------------------------------------------------------------
    // Comunicação com o cliente
    // ---------------------------------------------------------------

    /**
     * Envia uma mensagem de texto ao cliente.
     */
    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
        }
    }

    /**
     * Retorna o OutputStream bruto do socket (para transferência de arquivo).
     */
    public OutputStream getRawOutput() {
        return rawOutput;
    }

    /**
     * Encerra a conexão e libera os recursos desta sessão.
     */
    private void disconnect() {
        running = false;
        server.unregisterClient(username);
        log.info("Cliente desconectado: " + getIdentifier());

        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Erro ao desconectar cliente", e);
        }
    }

    /**
     * Retorna um identificador legível desta sessão.
     */
    private String getIdentifier() {
        return username != null ? username : socket.getRemoteSocketAddress().toString();
    }

    /** Retorna o nome de usuário desta sessão, ou null se não logado. */
    public String getUsername() {
        return username;
    }

    /** Retorna o User desta sessão. */
    public User getUser() {
        return user;
    }

    /** Verifica se esta sessão está ativa. */
    public boolean isRunning() {
        return running;
    }
}
