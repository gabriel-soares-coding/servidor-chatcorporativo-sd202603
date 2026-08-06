package br.ufmt.chatcorporativo.model;

/**
 * Representa um usuário cadastrado no sistema.
 * Cada usuário é identificado de forma única pelo seu username (R09).
 */
public class User {

    private final String username;
    private final String password;
    private final String orgao;

    public User(String username, String password, String orgao) {
        this.username = username;
        this.password = password;
        this.orgao = orgao;
    }

    /** Identificador único do usuário. */
    public String getUsername() {
        return username;
    }

    /** Senha do usuário (armazenada em texto simples para o PoC). */
    public String getPassword() {
        return password;
    }

    /** Órgão ou autarquia a que o usuário pertence (R14). */
    public String getOrgao() {
        return orgao;
    }

    /**
     * Verifica se a senha informada corresponde à senha do usuário.
     */
    public boolean checkPassword(String candidatePassword) {
        return password != null && password.equals(candidatePassword);
    }

    @Override
    public String toString() {
        return username + " [" + orgao + "]";
    }
}
