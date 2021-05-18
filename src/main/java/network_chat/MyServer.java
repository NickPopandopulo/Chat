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
        return clients.stream().anyMatch(client -> client.getNick().equals(nick));
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClients();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClients();
    }

    /**
     * (Дополнительно для упрощения) если лист пустой, то отправим всем. Непустой - отфильтруем
     */
    public synchronized void broadcastMessage(String message, List<String> nicknames) {
        clients.stream()
                .filter(client -> !message.isEmpty() && (nicknames.isEmpty() || nicknames.contains(client.getNick())))
                .forEach(client -> client.sendMsg(message));
    }

    /**
     * Личные сообщения, либо всем
     */
    public synchronized void broadcastMessage(String message, String fromNick) {
        String[] partsMsg = message.split("\\s+");
        List<String> nicknames = new ArrayList<>();
        String prefix = "[" + fromNick + "]: ";

        // если первое слово /direct
        if (partsMsg[0].equalsIgnoreCase(ChatConstants.DIRECT)) {

            // получатель и отправитель
            nicknames.addAll(List.of(partsMsg[1], fromNick));

            message = "direct: " + prefix + Arrays.stream(partsMsg)
                    .skip(2)
                    .collect(Collectors.joining(" "));

            // если первое слово /list
        } else if (partsMsg[0].equalsIgnoreCase(ChatConstants.SEND_TO_LIST)) {

            // список получателей
            nicknames = Arrays.stream(partsMsg)
                    .skip(1)
                    .takeWhile(e -> !e.equalsIgnoreCase(ChatConstants.SEND_TO_LIST)) // взять все, что между /list.../list
                    .distinct()
                    .collect(Collectors.toList());
            // ... и отправитель
            nicknames.add(fromNick);

            message = "direct: " + prefix + Arrays.stream(partsMsg)
                    .skip(1)
                    .dropWhile(e -> !e.equalsIgnoreCase(ChatConstants.SEND_TO_LIST))
                    .skip(1)
                    .collect(Collectors.joining(" "));
        } else {
            // сообщение всем
            message = prefix + message;
        }

        broadcastMessage(message, nicknames);
    }

    /**
     * Общие информационные сообщения, либо отправка всем
     */
    public synchronized void broadcastMessage(String message) {
        broadcastMessage(message, List.of());
    }

    /**
     * Список пользователей в сети. Всем
     */
    public synchronized void broadcastClients() {
        broadcastClients(List.of());
    }

    /**
     * Список пользователей в сети. Тому, кто попросил вывести список
     */
    public synchronized void broadcastClients(List<String> list) {
        String clientsMessage = ChatConstants.CLIENTS_LIST +
                " " +
                clients.stream()
                        .map(ClientHandler::getNick)
                        .collect(Collectors.joining(" "));

        broadcastMessage(clientsMessage, list);
    }

}
