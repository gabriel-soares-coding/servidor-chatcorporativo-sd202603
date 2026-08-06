package br.ufmt.chatcorporativo.model;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa um grupo de comunicação no sistema (R13).
 * Pode ser privado ou institucional.
 * Controla membros e restrições de ingresso (R15) e comunicação (R16).
 */
public class Group {

    private final String name;
    private final String owner;
    private final Set<String> members;
    private final boolean isPrivate;

    public Group(String name, String owner, boolean isPrivate) {
        this.name = name;
        this.owner = owner;
        this.isPrivate = isPrivate;
        this.members = ConcurrentHashMap.newKeySet();
        this.members.add(owner);
    }

    /** Nome do grupo. */
    public String getName() {
        return name;
    }

    /** Username do criador/dono do grupo. */
    public String getOwner() {
        return owner;
    }

    /** Indica se o grupo é privado (requer convite/aprovação). */
    public boolean isPrivate() {
        return isPrivate;
    }

    /** Retorna uma visão imutável dos membros. */
    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    /** Adiciona um membro ao grupo. */
    public boolean addMember(String username) {
        return members.add(username);
    }

    /** Remove um membro do grupo. */
    public boolean removeMember(String username) {
        return members.remove(username);
    }

    /** Verifica se um usuário é membro do grupo. */
    public boolean isMember(String username) {
        return members.contains(username);
    }

    @Override
    public String toString() {
        return name + " (dono: " + owner + ", membros: " + members.size() + ")";
    }
}
