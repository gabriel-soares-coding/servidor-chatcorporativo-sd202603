package br.ufmt.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.ufmt.chat.service.AccessControlService;
import br.ufmt.chat.service.AuditService;
import br.ufmt.chat.service.FileTransferService;
import br.ufmt.chat.service.GroupService;
import br.ufmt.chat.service.MessageService;
import br.ufmt.chat.service.UserService;
import br.ufmt.chat.util.Logger;

/**
 * Responsável por:
 * - abrir o ServerSocket;
 * - aceitar conexões;
 * - criar ClientHandler para cada conexão;
 * - enviar o ClientHandler para o pool de threads;
 * - manter o registro de clientes conectados;
 * - instanciar e compartilhar os serviços.
 *
 * Ele NÃO interpreta comandos.
 */
public class ChatServer {

    private static final Logger log = new Logger("ChatServer");

    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;

    /** Clientes conectados e logados (username → ClientHandler). */
    private final ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Serviços compartilhados
    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final FileTransferService fileTransferService;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    public ChatServer(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();

        // Inicializa os serviços
        this.userService = new UserService();
        this.messageService = new MessageService();
        this.groupService = new GroupService();
        this.fileTransferService = new FileTransferService();
        this.accessControlService = new AccessControlService();
        this.auditService = new AuditService();
    }

    /**
     * Inicia o servidor: abre o ServerSocket e entra em loop aceitando conexões.
     * Para cada cliente aceito, cria um ClientHandler e o submete ao pool de threads.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Servidor de chat iniciado na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Novo cliente conectado: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            log.error("Erro ao iniciar o servidor", e);
        } finally {
            stop();
        }
    }

    /**
     * Encerra o servidor: fecha o ServerSocket e desliga o pool de threads.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            auditService.close();
            log.info("Servidor encerrado.");
        } catch (IOException e) {
            log.error("Erro ao encerrar o servidor", e);
        }
    }

    // ---------------------------------------------------------------
    // Registry de clientes conectados
    // ---------------------------------------------------------------

    /**
     * Registra um cliente logado no mapa de conectados.
     */
    public void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
        log.info("Cliente registrado: " + username + " (total: " + connectedClients.size() + ")");
    }

    /**
     * Remove um cliente do mapa de conectados.
     */
    public void unregisterClient(String username) {
        if (username != null) {
            connectedClients.remove(username);
            log.info("Cliente removido: " + username + " (total: " + connectedClients.size() + ")");
        }
    }

    /**
     * Retorna o ClientHandler de um usuário conectado, ou null.
     */
    public ClientHandler getClient(String username) {
        return connectedClients.get(username);
    }

    /**
     * Retorna a lista de usernames conectados.
     */
    public List<String> getConnectedUsers() {
        return new ArrayList<>(connectedClients.keySet());
    }

    /**
     * Verifica se um usuário está online.
     */
    public boolean isOnline(String username) {
        return connectedClients.containsKey(username);
    }

    // ---------------------------------------------------------------
    // Getters dos serviços (injetados no ClientHandler)
    // ---------------------------------------------------------------

    public UserService getUserService() {
        return userService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public FileTransferService getFileTransferService() {
        return fileTransferService;
    }

    public AccessControlService getAccessControlService() {
        return accessControlService;
    }

    public AuditService getAuditService() {
        return auditService;
    }
}
