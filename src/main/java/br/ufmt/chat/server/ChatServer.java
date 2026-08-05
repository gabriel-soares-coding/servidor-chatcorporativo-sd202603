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

    // Federação e Domínio
    private String domainId = "LOCAL";
    private final ConcurrentHashMap<String, String> peerAddresses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PeerConnection> outboundPeers = new ConcurrentHashMap<>();

    public static class PeerConnection {
        private final Socket socket;
        private final java.io.PrintWriter output;

        public PeerConnection(Socket socket, java.io.PrintWriter output) {
            this.socket = socket;
            this.output = output;
        }

        public synchronized void send(String msg) {
            if (output != null) {
                output.println(msg);
            }
        }

        public void close() {
            try {
                if (output != null) output.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                // Silencioso
            }
        }
    }

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
            log.info("Servidor de chat [" + domainId + "] iniciado na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Novo socket conectado: " + clientSocket.getRemoteSocketAddress());

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
     * Encerra o servidor: fecha o ServerSocket, desliga o pool de threads e limpa peers.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();

            // Fecha conexões de saída da federação
            for (PeerConnection conn : outboundPeers.values()) {
                conn.close();
            }
            outboundPeers.clear();

            auditService.close();
            log.info("Servidor [" + domainId + "] encerrado.");
        } catch (IOException e) {
            log.error("Erro ao encerrar o servidor", e);
        }
    }

    // ---------------------------------------------------------------
    // Métodos de Federação e Roteamento
    // ---------------------------------------------------------------

    public void setDomainId(String domainId) {
        this.domainId = domainId != null ? domainId.toUpperCase() : "LOCAL";
    }

    public String getDomainId() {
        return domainId;
    }

    public void registerPeerAddress(String peerName, String hostPort) {
        if (peerName != null) {
            String upperName = peerName.toUpperCase();
            peerAddresses.put(upperName, hostPort);
            log.info("Peer registrado: " + upperName + " -> " + hostPort);
        }
    }

    public ConcurrentHashMap<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    /**
     * Envia um comando de texto para um broker parceiro da federação.
     */
    public boolean routeMessage(String targetDomain, String cmdLine) {
        PeerConnection conn = getOrCreatePeerConnection(targetDomain);
        if (conn != null) {
            conn.send(cmdLine);
            return true;
        }
        return false;
    }

    /**
     * Transfere um arquivo binário para um broker parceiro da federação.
     */
    public boolean routeFile(String targetDomain, String sender, String receiver, String fileName, byte[] fileData) {
        PeerConnection conn = getOrCreatePeerConnection(targetDomain);
        if (conn != null) {
            try {
                synchronized (conn) {
                    conn.send(br.ufmt.chat.protocol.ProtocolConstants.CMD_FED_FILE + " "
                            + sender + " " + receiver + " " + fileName + " " + fileData.length);
                    conn.socket.getOutputStream().write(fileData);
                    conn.socket.getOutputStream().flush();
                }
                return true;
            } catch (IOException e) {
                log.error("Erro ao transmitir arquivo via federação para " + targetDomain, e);
                conn.close();
                outboundPeers.remove(targetDomain);
            }
        }
        return false;
    }

    private synchronized PeerConnection getOrCreatePeerConnection(String targetDomain) {
        if (targetDomain == null) return null;
        String normalizedDomain = targetDomain.toUpperCase();
        
        PeerConnection conn = outboundPeers.get(normalizedDomain);
        if (conn != null && conn.socket != null && !conn.socket.isClosed()) {
            return conn;
        }

        String address = peerAddresses.get(normalizedDomain);
        if (address == null) {
            log.error("Sem endereço configurado para o peer broker: " + normalizedDomain);
            return null;
        }

        try {
            String[] parts = address.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            log.info("Conectando ao peer broker federado: " + normalizedDomain + " em " + host + ":" + port);
            Socket socket = new Socket(host, port);
            java.io.PrintWriter output = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Realiza handshake
            output.println(br.ufmt.chat.protocol.ProtocolConstants.CMD_FED_CONNECT + " " + domainId);

            PeerConnection newConn = new PeerConnection(socket, output);
            outboundPeers.put(normalizedDomain, newConn);
            return newConn;
        } catch (Exception e) {
            log.error("Erro ao conectar no peer federado " + normalizedDomain + " (" + address + ")", e);
            return null;
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
