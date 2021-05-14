package network_chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MyServer {

    private AuthService authService;
    private List<ClientHandler> clients;

    public MyServer() {
        try (ServerSocket server = new ServerSocket(ChatConstants.PORT)) {
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            while (true) {
                System.out.println("Wait for clients...");
                Socket socket = server.accept();
                System.out.println(socket.getInetAddress() + " client is connected!");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isNickBusy(String nick) {
//        return clients.stream().anyMatch(client -> client.getName().equals(nick));
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    /**
     * Личные сообщения, либо всем
     */
    public synchronized void broadcastMessage(String message, String fromNick) {
        String[] partsMsg = message.split("\\s+");

        // если личное сообщение кому-то
        // если первое слово /direct
        if (partsMsg[0].equalsIgnoreCase(ChatConstants.DIRECT)) {
            // если в сообщении больше двух слов и верно указан nickname получателя
            if (partsMsg.length > 2 && isNickBusy(partsMsg[1].toLowerCase())) {

                // собрать сообщение без /direct и nickname получателя
                message = "direct: [" + fromNick + "]: " +
                        Arrays.stream(partsMsg).skip(2).collect(Collectors.joining(" "));

                getClientByNick(partsMsg[1]).sendMsg(message);  // сообщение получателю
                getClientByNick(fromNick).sendMsg(message);     // сообщение отправителю

            } else {
                getClientByNick(fromNick).sendMsg("Error in sending a private message: " + message);
            }
        } else {
            // иначе отправляем сообщение всем
            broadcastMessage("[" + fromNick + "]: " + message);
        }
    }

    /**
     * Общие информационные сообщения, либо отправка всем
     */
    public synchronized void broadcastMessage(String message) {
        clients.forEach(clientHandler -> clientHandler.sendMsg(message));
    }

    /**
     * Получить клиента по nickname
     */
    private ClientHandler getClientByNick(String nick) {
        return clients.stream()
                .filter(clientHandler -> clientHandler.getNick().equals(nick))
                .findFirst()
                .get();
    }


}
