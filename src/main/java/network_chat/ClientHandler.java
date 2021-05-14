package network_chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

/**
 * Serves the client (responsible for communication between the client and the server)
 */
public class ClientHandler {

    private MyServer server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nick;

    public ClientHandler(MyServer server, Socket socket) {
        try {
            this.nick = "";
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }

            }).start();
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
            String message = in.readUTF();
            if (message.startsWith(ChatConstants.AUTH_COMMAND) &&
                    (parts = message.split("\\s+")).length > 2) {
                Optional<String> nick = server.getAuthService().getNickByLoginAndPass(parts[1], parts[2]);
                if (nick.isPresent()) {
                    if (!server.isNickBusy(nick.get())) {
                        sendMsg(ChatConstants.AUTH_SUCCESS + " " + nick.get());
                        this.nick = nick.get();
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
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String messageFromClient = in.readUTF();
            System.out.println("[" + nick + "]: " + messageFromClient);
            if (messageFromClient.equals(ChatConstants.STOP_WORD)) {
                return;
            }
            server.broadcastMessage(messageFromClient, nick);
        }
    }

    public void closeConnection() {
        server.broadcastMessage(nick + " exited the chat.");
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

