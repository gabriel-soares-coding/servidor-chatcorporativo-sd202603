package br.ufmt.chat;

import br.ufmt.chat.server.ChatServer;

public class ServerMain {

    public static void main(String[] args) {
        int port = 5000;
        String domainId = "LOCAL";

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando padrão: " + port);
            }
        }

        if (args.length > 1) {
            domainId = args[1];
        }

        ChatServer server = new ChatServer(port);
        server.setDomainId(domainId);

        // Mapeamentos de peers nos argumentos subsequentes (ex: MS=localhost:5001)
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                String peerName = parts[0];
                String peerAddress = parts[1];
                server.registerPeerAddress(peerName, peerAddress);
            }
        }

        server.start();
    }

}