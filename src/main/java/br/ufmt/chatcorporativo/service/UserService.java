package br.ufmt.chatcorporativo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import br.ufmt.chatcorporativo.exception.ChatException;
import br.ufmt.chatcorporativo.model.User;
import br.ufmt.chatcorporativo.protocol.ProtocolConstants;
import br.ufmt.chatcorporativo.util.Logger;

/**
 * Serviço responsável pelo registro, autenticação e busca de usuários (R09, R11, R18).
 * Armazena usuários em memória (ConcurrentHashMap) para o PoC.
 * Registro automático: se o usuário não existe, cria; se existe, valida a senha.
 */
public class UserService {

    private static final Logger log = new Logger("UserService");

    /** Usuários cadastrados (username → User). */
    private final ConcurrentHashMap<String, User> registeredUsers = new ConcurrentHashMap<>();

    /**
     * Autentica ou registra automaticamente um usuário.
     * Se o username não existe, cria um novo com a senha e órgão informados.
     * Se já existe, valida a senha.
     *
     * @return o objeto User autenticado
     * @throws ChatException se a senha estiver incorreta
     */
    public User authenticate(String username, String password, String orgao) throws ChatException {
        User existing = registeredUsers.get(username);

        if (existing != null) {
            if (!existing.checkPassword(password)) {
                throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED, "Senha incorreta");
            }
            log.info("Usuário autenticado: " + username);
            return existing;
        }

        // Registro automático
        User newUser = new User(username, password, orgao);
        User previous = registeredUsers.putIfAbsent(username, newUser);
        if (previous != null) {
            // Outro thread registrou primeiro — valida senha
            if (!previous.checkPassword(password)) {
                throw new ChatException(ProtocolConstants.ERR_ACCESS_DENIED, "Senha incorreta");
            }
            return previous;
        }

        log.info("Novo usuário registrado: " + newUser);
        return newUser;
    }

    /**
     * Busca um usuário pelo username.
     *
     * @return o User encontrado
     * @throws ChatException se não encontrado
     */
    public User findUser(String username) throws ChatException {
        User user = registeredUsers.get(username);
        if (user == null) {
            throw new ChatException(ProtocolConstants.ERR_USER_NOT_FOUND,
                    "Usuário não encontrado: " + username);
        }
        return user;
    }

    /**
     * Retorna a lista de todos os usernames cadastrados (R11).
     */
    public List<String> listAllUsers() {
        List<String> users = new ArrayList<>(registeredUsers.keySet());
        Collections.sort(users);
        return users;
    }

    /**
     * Verifica se um username está cadastrado.
     */
    public boolean exists(String username) {
        return registeredUsers.containsKey(username);
    }
}
