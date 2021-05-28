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
}
