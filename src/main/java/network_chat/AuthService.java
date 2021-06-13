package network_chat;

import java.util.Optional;

/**
 * Authorization service
 */
public interface AuthService {
    /**
     * Start service
     */
    void start();

    /**
     * Stop service
     */
    void stop();

    /**
     * Get nickname
     */
    Optional<String> getNickByLoginAndPass(String login, String pass);

    /**
     * Get ID
     */
    Optional<String> getIDByLoginAndPass(String login, String pass);

    /**
     * Change nickname
     */
    void changeNick(int id, String newNickName);

    /**
     * Check if nickname is busy in database
     */
    boolean isNickBusyInDB(String s);
}
