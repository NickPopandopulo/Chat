package network_chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Serves the client (responsible for communication between the client and the server)
 */
public class ClientHandler {

    private final Integer timeForAuth = 60 * 1000;
    private MyServer server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;
    private int id;
    private volatile boolean isAuthorized = false;
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
    private AuthService authService;
    private ExecutorService executorService;

    public ClientHandler(MyServer server, Socket socket, ExecutorService executorService) {
        try {
            this.nick = "";
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.executorService = executorService;
            authService = server.getAuthService();

            executorService.execute(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }

            });
        } catch (IOException ex) {
            System.out.println("Problem when creating a client.");
        }
    }

    public String getNick() {
        return nick;
    }

    private void authentication() throws IOException {
        String[] parts;
        while (true) {
            timeForAuth();
            String message = in.readUTF();

            if (message.startsWith(ChatConstants.AUTH_COMMAND) &&
                    (parts = message.split("\\s+")).length > 2) {

                Optional<String> nick = server.getAuthService().getNickByLoginAndPass(parts[1], parts[2]);
                Optional<String> id = server.getAuthService().getIDByLoginAndPass(parts[1], parts[2]);

                if (nick.isPresent()) {
                    if (!server.isNickBusy(nick.get())) {
                        isAuthorized = true;
                        sendMsg(ChatConstants.AUTH_SUCCESS + " " + nick.get());
                        LOGGER.info("Successful authorization: " + nick.get());
                        this.nick = nick.get();
                        sendMsg(ChatConstants.AUTH_SUCCESS + " " + this.nick);
                        this.id = Integer.parseInt(id.get());
                        server.subscribe(this);
                        server.broadcastMessage(this.nick + " joined the chat.");
                        return;
                    } else {
                        sendMsg("Nick is already in use.");
                    }
                } else {
                    sendMsg("Invalid login/password");
                }
            }
        }
    }

    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e);
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String messageFromClient = in.readUTF();
            System.out.println("[" + nick + "]: " + messageFromClient);

            if (messageFromClient.equals(ChatConstants.STOP_WORD)) {
                LOGGER.info("Received stop-word from " + nick);
                return;

            } else if (messageFromClient.startsWith(ChatConstants.CLIENTS_LIST)) {
                LOGGER.info("Received request for an list of online users from " + nick);
                server.broadcastClients(List.of(nick));

            } else if (messageFromClient.startsWith(ChatConstants.CHANGE_NICK)) { // ?????????? nickname
                String[] partsMsg = messageFromClient.split("\\s+");
                LOGGER.info("Received request for nickname changing from " + nick);

                // ???????? nick ???? ??????????
                if (!authService.isNickBusyInDB(partsMsg[1])) {
                    authService.changeNick(id, partsMsg[1]);
                    server.broadcastMessage("User " + nick + " has changed nickname to " + partsMsg[1]);
                    LOGGER.info("User " + nick + " has changed nickname to " + partsMsg[1]);
                    nick = partsMsg[1];
                    // ???????? nick ??????????
                } else if (authService.isNickBusyInDB(partsMsg[1])) {
                    sendMsg("Nickname " + partsMsg[1] + " is already in use.");
                    LOGGER.info("Nickname changing for " + nick + " is unavailable. " +
                            "Nickname " + partsMsg[1] + " is already in use.");
                }
            } else {
                server.broadcastMessage(messageFromClient, nick);
                LOGGER.info("Received a message from " + nick);
            }
        }
    }

    public void closeConnection() {
        server.broadcastMessage(nick + " exited the chat.");
        LOGGER.info(nick + " exited the chat.");
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e);
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e);
        }
    }

    private void timeForAuth() {
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                while (true) {
                    Thread.sleep(5000);

                    if (isAuthorized) break;

                    if ((System.currentTimeMillis() - startTime) >= timeForAuth) {
                        System.out.println(socket.getInetAddress() + " time is out.");
                        socket.close();
                        break;
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                LOGGER.error(e);
            }
        }).start();
    }
}

