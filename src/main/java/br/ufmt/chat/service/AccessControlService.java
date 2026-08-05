package br.ufmt.chat.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import br.ufmt.chat.exception.ChatException;
import br.ufmt.chat.protocol.ProtocolConstants;
import br.ufmt.chat.util.Logger;

/**
 * Serviço de controle de acesso entre órgãos e autarquias (R14, R15, R18).
 *
 * Define restrições de comunicação: por padrão todos os órgãos podem
 * se comunicar entre si. É possível bloquear pares de órgãos.
 */
public class AccessControlService {

    private static final Logger log = new Logger("AccessControlService");

    /**
     * Pares de órgãos bloqueados (comunicação bidirecional).
     * Chave: "orgao1|orgao2" (em ordem alfabética).
     */
    private final Set<String> blockedPairs = ConcurrentHashMap.newKeySet();

    /**
     * Bloqueia a comunicação entre dois órgãos (R14).
     */
    public void blockCommunication(String orgao1, String orgao2) {
        String key = buildKey(orgao1, orgao2);
        blockedPairs.add(key);
        log.info("Comunicação bloqueada entre: " + orgao1 + " <-> " + orgao2);
    }

    /**
     * Desbloqueia a comunicação entre dois órgãos.
     */
    public void unblockCommunication(String orgao1, String orgao2) {
        String key = buildKey(orgao1, orgao2);
        blockedPairs.remove(key);
        log.info("Comunicação desbloqueada entre: " + orgao1 + " <-> " + orgao2);
    }

    /**
     * Verifica se dois órgãos podem se comunicar (R14).
     *
     * @param orgaoSender   órgão do remetente
     * @param orgaoReceiver órgão do destinatário
     * @throws ChatException se a comunicação for bloqueada
     */
    public void checkCommunicationAllowed(String orgaoSender, String orgaoReceiver)
            throws ChatException {
        if (orgaoSender == null || orgaoReceiver == null) {
            return; // Se algum órgão não foi informado, permite por padrão
        }

        if (orgaoSender.equals(orgaoReceiver)) {
            return; // Mesmo órgão: sempre permitido
        }

        String key = buildKey(orgaoSender, orgaoReceiver);
        if (blockedPairs.contains(key)) {
            throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED,
                    "Comunicação bloqueada entre " + orgaoSender + " e " + orgaoReceiver);
        }
    }

    /**
     * Verifica se um usuário está autenticado (sessão ativa) (R18).
     *
     * @param username username a verificar (null se não logado)
     * @throws ChatException se não autenticado
     */
    public void requireAuthentication(String username) throws ChatException {
        if (username == null) {
            throw new ChatException(ProtocolConstants.ERR_AUTH_REQUIRED,
                    "Faça login primeiro");
        }
    }

    /**
     * Retorna os pares de órgãos bloqueados.
     */
    public Set<String> getBlockedPairs() {
        return new HashSet<>(blockedPairs);
    }

    /**
     * Constrói a chave do par de órgãos em ordem alfabética para garantir simetria.
     */
    private String buildKey(String orgao1, String orgao2) {
        if (orgao1.compareTo(orgao2) <= 0) {
            return orgao1 + "|" + orgao2;
        }
        return orgao2 + "|" + orgao1;
    }
}
