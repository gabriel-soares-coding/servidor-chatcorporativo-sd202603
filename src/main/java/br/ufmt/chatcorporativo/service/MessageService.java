package br.ufmt.chatcorporativo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import br.ufmt.chatcorporativo.exception.ChatException;
import br.ufmt.chatcorporativo.model.Message;
import br.ufmt.chatcorporativo.protocol.ProtocolConstants;
import br.ufmt.chatcorporativo.util.Logger;

/**
 * Serviço responsável pelo envio de mensagens ponto-a-ponto (R10)
 * e armazenamento de histórico em memória (R17).
 *
 * A entrega efetiva da mensagem ao socket do destinatário é feita pelo
 * ClientHandler/ChatServer; este serviço cuida da lógica de negócio.
 */
public class MessageService {

    private static final Logger log = new Logger("MessageService");

    /**
     * Histórico de mensagens diretas (chave = username, valor = lista de
     * mensagens).
     */
    private final ConcurrentHashMap<String, List<Message>> directHistory = new ConcurrentHashMap<>();

    /** Histórico de mensagens de grupo (chave = groupName). */
    private final ConcurrentHashMap<String, List<Message>> groupHistory = new ConcurrentHashMap<>();

    /**
     * Cria e registra uma mensagem direta no histórico.
     *
     * @param sender   username do remetente
     * @param receiver username do destinatário
     * @param content  texto da mensagem
     * @return a Message criada
     * @throws ChatException se argumentos inválidos
     */
    public Message createDirectMessage(String sender, String receiver, String content)
            throws ChatException {
        if (content == null || content.isBlank()) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS, "Mensagem vazia");
        }

        Message msg = new Message(sender, receiver, content);

        // Armazena no histórico de ambos os participantes
        storeInHistory(directHistory, sender, msg);
        storeInHistory(directHistory, receiver, msg);

        log.info("Mensagem direta: " + sender + " -> " + receiver);
        return msg;
    }

    /**
     * Cria e registra uma mensagem de grupo no histórico.
     *
     * @param sender    username do remetente
     * @param groupName nome do grupo
     * @param content   texto da mensagem
     * @return a Message criada
     * @throws ChatException se argumentos inválidos
     */
    public Message createGroupMessage(String sender, String groupName, String content)
            throws ChatException {
        if (content == null || content.isBlank()) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS, "Mensagem vazia");
        }

        Message msg = new Message(sender, groupName, content, true);
        storeInHistory(groupHistory, groupName, msg);

        log.info("Mensagem de grupo: " + sender + " -> #" + groupName);
        return msg;
    }

    /**
     * Retorna o histórico de mensagens diretas de um usuário (R17).
     */
    public List<Message> getDirectHistory(String username) {
        return Collections.unmodifiableList(
                directHistory.getOrDefault(username, Collections.emptyList()));
    }

    /**
     * Retorna o histórico de mensagens de um grupo (R17).
     */
    public List<Message> getGroupHistory(String groupName) {
        return Collections.unmodifiableList(
                groupHistory.getOrDefault(groupName, Collections.emptyList()));
    }

    private void storeInHistory(ConcurrentHashMap<String, List<Message>> store,
            String key, Message msg) {
        store.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(msg);
    }
}
