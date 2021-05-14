package network_chat;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the authentication service that runs on the inner list
 */
public class BaseAuthService implements AuthService {

    private final List<Entry> entries;

    public BaseAuthService() {
        entries = List.of(
                new Entry("nick1", "login1", "pass1"),
                new Entry("nick2", "login2", "pass2"),
                new Entry("nick3", "login3", "pass3")
        );
    }

    @Override
    public void start() {
        System.out.println("BaseAuthService started.");
    }

    @Override
    public void stop() {
        System.out.println("BaseAuthService stopped.");
    }

    @Override
    public Optional<String> getNickByLoginAndPass(String login, String pass) {
//        return entries.stream()
//                .filter(entry -> entry.login.equals(login) && entry.pass.equals(pass))
//                .map(entry -> entry.nick)
//                .findFirst().orElse(null);
        for (Entry entry : entries) {
            if (entry.login.equals(login) && entry.pass.equals(pass)) {
                return Optional.ofNullable(entry.nick);
            }
        }
        return Optional.empty();
    }

    private static class Entry {
        private final String nick;
        private final String login;
        private final String pass;

        public Entry(String nick, String login, String pass) {
            this.nick = nick;
            this.login = login;
            this.pass = pass;
        }
    }


}
