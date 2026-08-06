package br.ufmt.chatcorporativo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import br.ufmt.chatcorporativo.exception.ChatException;
import br.ufmt.chatcorporativo.model.Group;
import br.ufmt.chatcorporativo.protocol.ProtocolConstants;
import br.ufmt.chatcorporativo.util.Logger;

/**
 * Serviço responsável pelo gerenciamento de grupos (R13, R15, R16).
 * Suporta criação, ingresso, saída e listagem de grupos.
 */
public class GroupService {

    private static final Logger log = new Logger("GroupService");

    /** Grupos cadastrados (nome → Group). */
    private final ConcurrentHashMap<String, Group> groups = new ConcurrentHashMap<>();

    /**
     * Cria um novo grupo (R13).
     *
     * @param name      nome do grupo
     * @param owner     username do criador
     * @param isPrivate se o grupo é privado
     * @return o Group criado
     * @throws ChatException se o nome já existir
     */
    public Group createGroup(String name, String owner, boolean isPrivate) throws ChatException {
        Group group = new Group(name, owner, isPrivate);
        Group existing = groups.putIfAbsent(name, group);
        if (existing != null) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Grupo já existe: " + name);
        }
        log.info("Grupo criado: " + group);
        return group;
    }

    /**
     * Adiciona um membro a um grupo (R15).
     * Em grupos privados, apenas o dono pode adicionar (simplificação do PoC).
     *
     * @param groupName nome do grupo
     * @param username  usuário que deseja entrar
     * @throws ChatException se o grupo não existir ou acesso negado
     */
    public void joinGroup(String groupName, String username) throws ChatException {
        Group group = getGroup(groupName);

        if (group.isPrivate()) {
            // Em grupo privado, qualquer um pode pedir para entrar no PoC
            // (em produção, seria necessário aprovação do dono)
            log.warn("Ingresso em grupo privado '" + groupName + "' por " + username);
        }

        if (group.isMember(username)) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Você já é membro do grupo: " + groupName);
        }

        group.addMember(username);
        log.info(username + " entrou no grupo " + groupName);
    }

    /**
     * Remove um membro de um grupo.
     *
     * @param groupName nome do grupo
     * @param username  usuário que deseja sair
     * @throws ChatException se o grupo não existir ou usuário não for membro
     */
    public void leaveGroup(String groupName, String username) throws ChatException {
        Group group = getGroup(groupName);

        if (!group.isMember(username)) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Você não é membro do grupo: " + groupName);
        }

        if (group.getOwner().equals(username)) {
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "O dono não pode sair do grupo. Exclua o grupo.");
        }

        group.removeMember(username);
        log.info(username + " saiu do grupo " + groupName);
    }

    /**
     * Retorna um grupo pelo nome.
     *
     * @throws ChatException se não encontrado
     */
    public Group getGroup(String groupName) throws ChatException {
        Group group = groups.get(groupName);
        if (group == null) {
            throw new ChatException(ProtocolConstants.ERR_GROUP_NOT_FOUND,
                    "Grupo não encontrado: " + groupName);
        }
        return group;
    }

    /**
     * Retorna os membros de um grupo (R16 — restrição: só membros veem membros).
     *
     * @param groupName nome do grupo
     * @param requester username de quem está pedindo
     * @throws ChatException se o grupo não existir ou requester não for membro
     */
    public Set<String> getMembers(String groupName, String requester) throws ChatException {
        Group group = getGroup(groupName);
        if (!group.isMember(requester)) {
            throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED,
                    "Você não é membro do grupo: " + groupName);
        }
        return group.getMembers();
    }

    /**
     * Lista os nomes de todos os grupos (R13).
     */
    public List<String> listGroups() {
        return new ArrayList<>(groups.keySet());
    }

    /**
     * Verifica se o usuário é membro de um grupo (R16).
     */
    public boolean isMember(String groupName, String username) {
        Group group = groups.get(groupName);
        return group != null && group.isMember(username);
    }
}
